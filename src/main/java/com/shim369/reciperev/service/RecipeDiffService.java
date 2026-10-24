package com.shim369.reciperev.service;

import com.shim369.reciperev.domain.ChangeType;
import com.shim369.reciperev.domain.IngredientDiff;
import com.shim369.reciperev.domain.IngredientLine;
import com.shim369.reciperev.domain.RecipeDiff;
import com.shim369.reciperev.domain.StepDiff;
import com.shim369.reciperev.domain.VersionContent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 2つの版の差分を計算する(F-07)。
 *
 * <p>DB・HTTP に一切依存しない「純粋な計算」だけのクラスにしている。
 * そのため {@code new RecipeDiffService()} だけで単体テストでき、ロジックの正しさを素早く確かめられる。
 *
 * <ul>
 *   <li>材料: 材料名で照合する。片方にしかない → 追加/削除、両方にあり値が違う → 変更</li>
 *   <li>手順: 行単位の LCS(最長共通部分列)で一致する行を見つけ、残りを削除/追加にする。
 *       同じ位置で連続する「削除 k 行 + 追加 m 行」は、先頭から min(k, m) 組を変更として対にする</li>
 * </ul>
 */
@Service
public class RecipeDiffService {

    public RecipeDiff diff(VersionContent from, VersionContent to) {
        return new RecipeDiff(diffIngredients(from.ingredients(), to.ingredients()), diffSteps(from.steps(), to.steps()));
    }

    // ===== 材料 =====

    /**
     * 並び順は「比較先(to)の順番」を基本にし、削除された材料は比較元(from)での位置に差し込む
     * (04_er_diagram.md 5.1 の SQL の ORDER BY COALESCE(b.sort_order, a.sort_order) と同じ考え方)。
     */
    List<IngredientDiff> diffIngredients(List<IngredientLine> from, List<IngredientLine> to) {
        // LinkedHashMap: 挿入順を保つ Map(PHP の連想配列と同じ感覚で使える)
        Map<String, IngredientLine> fromByName = new LinkedHashMap<>();
        from.forEach(line -> fromByName.put(line.name(), line));
        Map<String, Integer> fromIndex = new LinkedHashMap<>();
        for (int i = 0; i < from.size(); i++) {
            fromIndex.put(from.get(i).name(), i);
        }

        record Sorted(int position, int tieBreak, IngredientDiff diff) {
        }
        List<Sorted> result = new ArrayList<>();

        for (int i = 0; i < to.size(); i++) {
            IngredientLine after = to.get(i);
            IngredientLine before = fromByName.get(after.name());
            ChangeType type = (before == null) ? ChangeType.ADDED
                    : before.sameContentAs(after) ? ChangeType.UNCHANGED
                    : ChangeType.CHANGED;
            result.add(new Sorted(i, 1, new IngredientDiff(type, before, after)));
        }
        List<String> toNames = to.stream().map(IngredientLine::name).toList();
        for (IngredientLine before : from) {
            if (!toNames.contains(before.name())) {
                // 同じ位置に比較先の行もある場合は、削除行を先に表示する(tieBreak = 0)
                result.add(new Sorted(fromIndex.get(before.name()), 0,
                        new IngredientDiff(ChangeType.REMOVED, before, null)));
            }
        }
        return result.stream()
                .sorted(Comparator.comparingInt(Sorted::position).thenComparingInt(Sorted::tieBreak))
                .map(Sorted::diff)
                .toList();
    }

    // ===== 手順 =====

    List<StepDiff> diffSteps(List<String> from, List<String> to) {
        List<StepDiff> result = new ArrayList<>();
        List<String> pendingRemoved = new ArrayList<>();
        List<String> pendingAdded = new ArrayList<>();

        for (Op op : lcsOperations(from, to)) {
            if (op.kind == OpKind.EQUAL) {
                flushHunk(pendingRemoved, pendingAdded, result);
                result.add(new StepDiff(ChangeType.UNCHANGED, op.line, op.line));
            } else if (op.kind == OpKind.DELETE) {
                pendingRemoved.add(op.line);
            } else {
                pendingAdded.add(op.line);
            }
        }
        flushHunk(pendingRemoved, pendingAdded, result);
        return result;
    }

    /** 一致しない行のかたまり(hunk)を、変更(対にできた分)→ 残りの削除 → 残りの追加 の順で出力する */
    private static void flushHunk(List<String> removed, List<String> added, List<StepDiff> out) {
        int pairs = Math.min(removed.size(), added.size());
        for (int i = 0; i < pairs; i++) {
            out.add(new StepDiff(ChangeType.CHANGED, removed.get(i), added.get(i)));
        }
        for (int i = pairs; i < removed.size(); i++) {
            out.add(new StepDiff(ChangeType.REMOVED, removed.get(i), null));
        }
        for (int i = pairs; i < added.size(); i++) {
            out.add(new StepDiff(ChangeType.ADDED, null, added.get(i)));
        }
        removed.clear();
        added.clear();
    }

    private enum OpKind { EQUAL, DELETE, INSERT }

    private record Op(OpKind kind, String line) {
    }

    /**
     * 動的計画法で LCS の長さ表を作り、先頭からたどって「一致/削除/追加」の並びを作る。
     * lcs[i][j] = from[i..] と to[j..] の最長共通部分列の長さ。手順は最大50行なので O(n*m) で十分速い。
     */
    private static List<Op> lcsOperations(List<String> from, List<String> to) {
        int n = from.size();
        int m = to.size();
        int[][] lcs = new int[n + 1][m + 1];
        for (int i = n - 1; i >= 0; i--) {
            for (int j = m - 1; j >= 0; j--) {
                lcs[i][j] = from.get(i).equals(to.get(j))
                        ? lcs[i + 1][j + 1] + 1
                        : Math.max(lcs[i + 1][j], lcs[i][j + 1]);
            }
        }
        List<Op> ops = new ArrayList<>();
        int i = 0;
        int j = 0;
        while (i < n && j < m) {
            if (from.get(i).equals(to.get(j))) {
                ops.add(new Op(OpKind.EQUAL, from.get(i)));
                i++;
                j++;
            } else if (lcs[i + 1][j] >= lcs[i][j + 1]) {
                ops.add(new Op(OpKind.DELETE, from.get(i++))); // 削除を先に出すと、削除→追加の順に並び「変更」の対を作りやすい
            } else {
                ops.add(new Op(OpKind.INSERT, to.get(j++)));
            }
        }
        while (i < n) {
            ops.add(new Op(OpKind.DELETE, from.get(i++)));
        }
        while (j < m) {
            ops.add(new Op(OpKind.INSERT, to.get(j++)));
        }
        return ops;
    }
}
