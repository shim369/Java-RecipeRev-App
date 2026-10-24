package com.shim369.reciperev.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.shim369.reciperev.dto.AnalysisResult.Correlation;
import com.shim369.reciperev.dto.AnalysisRow;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** 分析用CSVの書き出しと、結果の表示用文言(UT-ANA-xx) */
class AnalysisCsvTest {

    @Test
    @DisplayName("UT-ANA-01: CSV はヘッダ付き。文字列は \"\" で囲み、中の \" は二重にする")
    void writesRfc4180Csv(@TempDir Path dir) throws Exception {
        Path csv = dir.resolve("r.csv");
        AnalysisService.writeCsv(csv, List.of(
                new AnalysisRow(1, "卵, L\"サイズ\"", null, new BigDecimal("2.00"), new BigDecimal("4.5000"))));
        assertThat(Files.readAllLines(csv)).containsExactly(
                "version_no,ingredient,unit,amount,avg_rating",
                "1,\"卵, L\"\"サイズ\"\"\",,2.00,4.5000");
    }

    @Test
    @DisplayName("UT-ANA-02: 相関の向きと強さから傾向の文章のメッセージキーを決め、分量の範囲は末尾の0を落として表示する")
    void sentence() {
        Correlation strongPositive = new Correlation("砂糖", "g", 3, 0.9, "STRONG", "POSITIVE", 30, 50);
        assertThat(strongPositive.sentenceKey()).isEqualTo("positive");
        assertThat(strongPositive.hasUnit()).isTrue();
        Correlation moderateNegative = new Correlation("塩", "", 3, -0.5, "MODERATE", "NEGATIVE", 1, 3);
        assertThat(moderateNegative.sentenceKey()).isEqualTo("negative");
        assertThat(moderateNegative.hasUnit()).isFalse();
        assertThat(new Correlation("塩", "g", 3, 0.1, "WEAK", "POSITIVE", 1, 3).sentenceKey()).isEqualTo("weak");
        Correlation range = new Correlation("塩", "g", 3, 0.1, "WEAK", "POSITIVE", 0, 12.5);
        assertThat(range.minLabel() + "-" + range.maxLabel()).isEqualTo("0-12.5");
    }
}
