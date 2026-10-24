package com.shim369.reciperev.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shim369.reciperev.config.RecipeRevProperties;
import com.shim369.reciperev.dto.AnalysisResult;
import com.shim369.reciperev.dto.AnalysisRow;
import com.shim369.reciperev.exception.AnalysisException;
import com.shim369.reciperev.repository.RecipeQueryRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Python による傾向分析(F-11)の連携。Java と Python はファイルだけでつながる(疎結合)。
 * <pre>
 *   ① DB → CSV(recipe_{id}.csv)   … Java
 *   ② uv run ... analyze.py csv json … ProcessBuilder でバッチ実行(Python 環境は uv が管理)
 *   ③ JSON(recipe_{id}.json)→ 画面 … Java
 * </pre>
 * Python を Web サーバとして常駐させないので、構成が単純で、Python 側は単体でもテスト・実行できる。
 */
@Service
public class AnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisService.class);

    private final RecipeQueryRepository queryRepository;
    private final RecipeService recipeService;
    private final ObjectMapper objectMapper;
    private final RecipeRevProperties.Analysis config;

    public AnalysisService(RecipeQueryRepository queryRepository, RecipeService recipeService,
                           ObjectMapper objectMapper, RecipeRevProperties properties) {
        this.queryRepository = queryRepository;
        this.recipeService = recipeService;
        this.objectMapper = objectMapper;
        this.config = properties.analysis();
    }

    /** 前回の分析結果(まだ分析していなければ空) */
    public Optional<AnalysisResult> loadLatest(long recipeId) {
        Path json = jsonPath(recipeId);
        if (!Files.exists(json)) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json.toFile(), AnalysisResult.class));
        } catch (IOException e) {
            throw new AnalysisException(e, "error.analysis.readJson");
        }
    }

    /** 前回の分析結果JSONそのもの(画面で「結果JSON」として見せる) */
    public Optional<String> loadLatestJson(long recipeId) {
        Path json = jsonPath(recipeId);
        try {
            return Files.exists(json) ? Optional.of(Files.readString(json)) : Optional.empty();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * 分析を実行する。このメソッドには @Transactional を付けない。
     * 時間のかかる Python の実行(②)中に、トランザクションと DB 接続を握り続けないようにするため。
     * ①は SELECT 1回だけなので、トランザクションなし(自動コミット)で問題ない。
     */
    public AnalysisResult run(long recipeId) {
        recipeService.getActive(recipeId); // 存在チェック(404)
        List<AnalysisRow> rows = queryRepository.findAnalysisRows(recipeId);
        try {
            Files.createDirectories(config.workDir());
            writeCsv(csvPath(recipeId), rows);
            execute(recipeId);
        } catch (IOException e) {
            throw new AnalysisException(e, "error.analysis.writeFiles");
        }
        AnalysisResult result = loadLatest(recipeId)
                .orElseThrow(() -> new AnalysisException("error.analysis.noJson"));
        log.info("Analysis finished: recipeId={}, versions={}, correlations={}",
                recipeId, result.versionCount(), result.results().size());
        return result;
    }

    private void execute(long recipeId) throws IOException {
        Path logFile = config.workDir().resolve("recipe_" + recipeId + ".log");
        // コマンドはリストで渡す(シェルを経由しないので、引数に記号が入ってもコマンドインジェクションにならない)
        // 例: uv run --project analysis --frozen --quiet python analysis/analyze.py in.csv out.json --recipe-id 1
        List<String> command = new ArrayList<>(config.command());
        command.addAll(List.of(config.script().toString(),
                csvPath(recipeId).toString(), jsonPath(recipeId).toString(),
                "--recipe-id", String.valueOf(recipeId)));
        ProcessBuilder pb = new ProcessBuilder(command)
                .redirectErrorStream(true)
                // 出力はファイルへ。パイプのまま読まずに待つと、出力が多いときにバッファが詰まって止まることがある
                .redirectOutput(logFile.toFile());
        pb.environment().put("PYTHONIOENCODING", "utf-8");

        Files.deleteIfExists(jsonPath(recipeId)); // 失敗したときに古い結果を新しい結果と取り違えないように
        log.info("Analysis started: {}", String.join(" ", pb.command()));

        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            throw new AnalysisException(e, "error.analysis.commandNotFound", config.command().get(0));
        }
        try {
            if (!process.waitFor(config.timeoutSeconds(), TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new AnalysisException("error.analysis.timeout", config.timeoutSeconds());
            }
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt(); // 割り込みフラグを戻しておく(Java の作法)
            throw new AnalysisException(e, "error.analysis.interrupted");
        }
        String output = Files.readString(logFile, StandardCharsets.UTF_8).strip();
        if (process.exitValue() != 0) {
            log.warn("Analysis failed: exit={}, output={}", process.exitValue(), output);
            throw new AnalysisException("error.analysis.failed", process.exitValue(), lastLine(output));
        }
        log.info("Analysis output: {}", lastLine(output));
    }

    /** RFC 4180 形式のCSVを書く。材料名にカンマや " が入っても壊れないよう、文字列は必ず "" で囲む */
    static void writeCsv(Path path, List<AnalysisRow> rows) throws IOException {
        try (Writer w = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            w.write("version_no,ingredient,unit,amount,avg_rating\n");
            for (AnalysisRow row : rows) {
                w.write(row.versionNo() + "," + quote(row.ingredient()) + "," + quote(row.unit()) + ","
                        + row.amount().toPlainString() + "," + row.avgRating().toPlainString() + "\n");
            }
        }
    }

    static String quote(String s) {
        return s == null ? "" : "\"" + s.replace("\"", "\"\"") + "\"";
    }

    private static String lastLine(String text) {
        int idx = text.lastIndexOf('\n');
        return idx < 0 ? text : text.substring(idx + 1);
    }

    private Path csvPath(long recipeId) {
        return config.workDir().resolve("recipe_" + recipeId + ".csv");
    }

    private Path jsonPath(long recipeId) {
        return config.workDir().resolve("recipe_" + recipeId + ".json");
    }
}
