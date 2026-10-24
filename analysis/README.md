# analysis/ — RecipeRev trend analysis (Python)

This script computes the correlation between ingredient amounts and ratings for one recipe.
The Spring Boot app runs it as a batch job, and the two sides talk only through files.

```
Spring Boot ──(1) CSV──▶ analyze.py ──(2) JSON──▶ Spring Boot (SCR-09)
             analysis/output/recipe_{id}.csv     analysis/output/recipe_{id}.json
```

## Setup

```bash
python3 -m pip install -r analysis/requirements.txt   # pandas (Python 3.9+)
```

## Run manually

```bash
python3 analysis/analyze.py analysis/output/recipe_1.csv analysis/output/recipe_1.json --recipe-id 1
```

## Test

```bash
python3 -m unittest discover -s analysis/tests -v
```

## Input CSV

| column | description |
|---|---|
| version_no | version number |
| ingredient | ingredient name (quoted) |
| unit | unit (quoted, may be empty) |
| amount | amount; `0` if the version does not contain the ingredient |
| avg_rating | average rating of the version (only versions with cook logs) |

## Output JSON

```json
{
  "recipeId": 1,
  "generatedAt": "2026-10-24T16:30:00",
  "versionCount": 3,
  "minSamples": 3,
  "results": [
    { "ingredient": "砂糖", "unit": "g", "samples": 3, "r": 0.945,
      "strength": "STRONG", "direction": "POSITIVE", "minAmount": 30.0, "maxAmount": 50.0 }
  ],
  "insufficient": [
    { "ingredient": "卵", "unit": "個", "samples": 3, "reason": "NO_AMOUNT_VARIATION" }
  ]
}
```

- `strength`: `STRONG` (|r| ≥ 0.7) / `MODERATE` (0.4 ≤ |r| < 0.7) / `WEAK`
- `reason`: `NOT_ENOUGH_SAMPLES` (< 3 versions) / `NO_AMOUNT_VARIATION` / `NO_RATING_VARIATION`
- Exit code: `0` success, `1` bad input, `2` pandas not installed
