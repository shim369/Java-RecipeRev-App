"""Unit tests for analysis/analyze.py (run: python3 -m unittest discover -s analysis/tests)."""

import json
import os
import sys
import tempfile
import unittest

import pandas as pd

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
import analyze  # noqa: E402


def frame(rows):
    return pd.DataFrame(rows, columns=["version_no", "ingredient", "unit", "amount", "avg_rating"])


class AnalyzeTest(unittest.TestCase):

    def test_positive_strong_correlation(self):
        df = frame([(1, "砂糖", "g", 30, 3.0), (2, "砂糖", "g", 40, 4.0), (3, "砂糖", "g", 50, 5.0)])
        result = analyze.analyze(df, recipe_id=1)
        self.assertEqual(1, len(result["results"]))
        sugar = result["results"][0]
        self.assertAlmostEqual(1.0, sugar["r"])
        self.assertEqual("STRONG", sugar["strength"])
        self.assertEqual("POSITIVE", sugar["direction"])
        self.assertEqual(3, result["versionCount"])

    def test_negative_correlation(self):
        df = frame([(1, "塩", "g", 1, 5.0), (2, "塩", "g", 2, 4.0), (3, "塩", "g", 3, 2.0)])
        salt = analyze.analyze(df, 1)["results"][0]
        self.assertEqual("NEGATIVE", salt["direction"])
        self.assertLess(salt["r"], -0.9)

    def test_not_enough_samples(self):
        df = frame([(1, "砂糖", "g", 30, 3.0), (2, "砂糖", "g", 40, 4.0)])
        result = analyze.analyze(df, 1)
        self.assertEqual([], result["results"])
        self.assertEqual("NOT_ENOUGH_SAMPLES", result["insufficient"][0]["reason"])

    def test_no_amount_variation(self):
        df = frame([(1, "卵", "個", 2, 3.0), (2, "卵", "個", 2, 4.0), (3, "卵", "個", 2, 5.0)])
        self.assertEqual("NO_AMOUNT_VARIATION", analyze.analyze(df, 1)["insufficient"][0]["reason"])

    def test_no_rating_variation(self):
        df = frame([(1, "砂糖", "g", 30, 4.0), (2, "砂糖", "g", 40, 4.0), (3, "砂糖", "g", 50, 4.0)])
        self.assertEqual("NO_RATING_VARIATION", analyze.analyze(df, 1)["insufficient"][0]["reason"])

    def test_same_name_different_unit_are_separate(self):
        df = frame([
            (1, "砂糖", "g", 30, 3.0), (2, "砂糖", "g", 40, 4.0), (3, "砂糖", "g", 50, 5.0),
            (1, "砂糖", "大さじ", 1, 3.0), (2, "砂糖", "大さじ", 1, 4.0), (3, "砂糖", "大さじ", 2, 5.0),
        ])
        result = analyze.analyze(df, 1)
        self.assertEqual({"g", "大さじ"}, {r["unit"] for r in result["results"]})

    def test_strength_thresholds(self):
        self.assertEqual("STRONG", analyze.classify_strength(-0.7))
        self.assertEqual("MODERATE", analyze.classify_strength(0.4))
        self.assertEqual("WEAK", analyze.classify_strength(0.39))

    def test_results_sorted_by_absolute_r(self):
        df = frame([
            (1, "A", "g", 1, 3.0), (2, "A", "g", 3, 4.0), (3, "A", "g", 2, 5.0),   # weaker
            (1, "B", "g", 1, 3.0), (2, "B", "g", 2, 4.0), (3, "B", "g", 3, 5.0),   # r = 1
        ])
        self.assertEqual(["B", "A"], [r["ingredient"] for r in analyze.analyze(df, 1)["results"]])

    def test_missing_column_raises(self):
        with self.assertRaises(ValueError):
            analyze.analyze(pd.DataFrame({"version_no": [1]}), 1)

    def test_main_reads_csv_with_empty_unit_and_writes_json(self):
        with tempfile.TemporaryDirectory() as tmp:
            csv_path = os.path.join(tmp, "in.csv")
            json_path = os.path.join(tmp, "out.json")
            with open(csv_path, "w", encoding="utf-8") as f:
                f.write("version_no,ingredient,unit,amount,avg_rating\n")
                f.write('1,"卵, Lサイズ",,1,3.0\n2,"卵, Lサイズ",,2,4.0\n3,"卵, Lサイズ",,3,5.0\n')
            self.assertEqual(0, analyze.main([csv_path, json_path, "--recipe-id", "7"]))
            with open(json_path, encoding="utf-8") as f:
                result = json.load(f)
            self.assertEqual(7, result["recipeId"])
            self.assertEqual("卵, Lサイズ", result["results"][0]["ingredient"])
            self.assertEqual("", result["results"][0]["unit"])


if __name__ == "__main__":
    unittest.main()
