#!/usr/bin/env python3
"""RecipeRev trend analysis: correlation between ingredient amounts and ratings.

RecipeRev (Java) exports one CSV per recipe and runs this script as a batch job.
The Python environment (Python 3.15.1 + pandas) is managed by uv (analysis/pyproject.toml, uv.lock):

    uv run --project analysis python analysis/analyze.py <input.csv> <output.json> --recipe-id <id>

Input CSV columns (one row = one version x one ingredient):
    version_no, ingredient, unit, amount, avg_rating

For each (ingredient, unit) pair the script computes the Pearson correlation
coefficient r between the amount and the version's average rating, and writes
the result as JSON. The Java side only reads the JSON, so the two programs stay
loosely coupled (either side can change as long as the file format holds).

Rules (docs/01_requirements.md F-11):
    * fewer than MIN_SAMPLES versions      -> "insufficient" (NOT_ENOUGH_SAMPLES)
    * amount never changes across versions -> "insufficient" (NO_AMOUNT_VARIATION)
    * rating never changes across versions -> "insufficient" (NO_RATING_VARIATION)
    * |r| >= 0.7 STRONG, 0.4 <= |r| < 0.7 MODERATE, otherwise WEAK
"""

import argparse
import json
import sys
from datetime import datetime
from typing import Any, Dict, List

try:
    import pandas as pd
except ImportError:  # pragma: no cover - depends on the environment
    print("[RecipeRev] pandas is not installed. Run: uv sync --project analysis", file=sys.stderr)
    sys.exit(2)

MIN_SAMPLES = 3
STRONG_THRESHOLD = 0.7
MODERATE_THRESHOLD = 0.4
REQUIRED_COLUMNS = ["version_no", "ingredient", "unit", "amount", "avg_rating"]


def classify_strength(r: float) -> str:
    """Label the absolute size of a correlation coefficient."""
    size = abs(r)
    if size >= STRONG_THRESHOLD:
        return "STRONG"
    if size >= MODERATE_THRESHOLD:
        return "MODERATE"
    return "WEAK"


def analyze(df: pd.DataFrame, recipe_id: int) -> Dict[str, Any]:
    """Compute per-ingredient correlations. Pure function: DataFrame in, dict out (easy to unit test)."""
    missing = [c for c in REQUIRED_COLUMNS if c not in df.columns]
    if missing:
        raise ValueError("missing columns: " + ", ".join(missing))

    df = df.copy()
    # An empty unit is read as NaN; treat it as "" so it can be a group key.
    df["unit"] = df["unit"].fillna("").astype(str)
    df["amount"] = pd.to_numeric(df["amount"], errors="coerce")
    df["avg_rating"] = pd.to_numeric(df["avg_rating"], errors="coerce")
    df = df.dropna(subset=["amount", "avg_rating"])

    results: List[Dict[str, Any]] = []
    insufficient: List[Dict[str, Any]] = []

    for (ingredient, unit), group in df.groupby(["ingredient", "unit"], sort=True):
        samples = int(group["version_no"].nunique())
        base = {"ingredient": ingredient, "unit": unit, "samples": samples}

        if samples < MIN_SAMPLES:
            insufficient.append({**base, "reason": "NOT_ENOUGH_SAMPLES"})
            continue
        if group["amount"].nunique() < 2:
            insufficient.append({**base, "reason": "NO_AMOUNT_VARIATION"})
            continue
        if group["avg_rating"].nunique() < 2:
            insufficient.append({**base, "reason": "NO_RATING_VARIATION"})
            continue

        r = float(group["amount"].corr(group["avg_rating"], method="pearson"))
        results.append({
            **base,
            "r": round(r, 3),
            "strength": classify_strength(r),
            "direction": "POSITIVE" if r > 0 else "NEGATIVE",
            "minAmount": float(group["amount"].min()),
            "maxAmount": float(group["amount"].max()),
        })

    # Strongest relationships first; ties broken by name for stable output.
    results.sort(key=lambda x: (-abs(x["r"]), x["ingredient"], x["unit"]))

    return {
        "recipeId": recipe_id,
        "generatedAt": datetime.now().replace(microsecond=0).isoformat(),
        "versionCount": int(df["version_no"].nunique()) if not df.empty else 0,
        "minSamples": MIN_SAMPLES,
        "results": results,
        "insufficient": insufficient,
    }


def main(argv: List[str]) -> int:
    parser = argparse.ArgumentParser(description="RecipeRev ingredient/rating correlation analysis")
    parser.add_argument("input_csv")
    parser.add_argument("output_json")
    parser.add_argument("--recipe-id", type=int, required=True)
    args = parser.parse_args(argv)

    try:
        df = pd.read_csv(args.input_csv, dtype={"ingredient": str, "unit": str}, keep_default_na=False,
                         na_values={"amount": [""], "avg_rating": [""]})
        result = analyze(df, args.recipe_id)
    except (OSError, ValueError, pd.errors.ParserError) as e:
        print("[RecipeRev] analysis failed: {}".format(e), file=sys.stderr)
        return 1

    with open(args.output_json, "w", encoding="utf-8") as f:
        json.dump(result, f, ensure_ascii=False, indent=2)
    print("[RecipeRev] analysis done: recipe={}, versions={}, results={}, insufficient={}".format(
        args.recipe_id, result["versionCount"], len(result["results"]), len(result["insufficient"])))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
