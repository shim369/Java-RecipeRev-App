# analysis/ — RecipeRev trend analysis (Python)

This script computes the correlation between ingredient amounts and ratings for one recipe.
The Spring Boot app runs it as a batch job, and the two sides talk only through files.

```
Spring Boot ──(1) CSV──▶ uv run … analyze.py ──(2) JSON──▶ Spring Boot (SCR-09)
             analysis/output/recipe_{id}.csv           analysis/output/recipe_{id}.json
```

## Environment (uv)

This directory is a [uv](https://docs.astral.sh/uv/) project:

| File | Purpose |
|---|---|
| `pyproject.toml` | Python requirement (`>=3.15`) and dependencies (`pandas>=3.0`) |
| `.python-version` | Pinned interpreter: `3.15.1` |
| `uv.lock` | Exact versions of every package (commit this file) |

You only need uv. Python 3.15.1 and pandas are installed into `analysis/.venv` automatically on the first `uv run`.
The app runs uv with `--frozen`, so it uses `uv.lock` exactly as committed and never changes it.

```bash
uv sync --project analysis          # optional: prepare analysis/.venv in advance
uv add --project analysis <package> # add a dependency (updates pyproject.toml and uv.lock)
```

## Run manually (from the repository root)

```bash
uv run --project analysis python analysis/analyze.py analysis/output/recipe_1.csv analysis/output/recipe_1.json --recipe-id 1
```

## Test

```bash
uv run --project analysis python -m unittest discover -s analysis/tests -v
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
    { "ingredient": "Sugar", "unit": "g", "samples": 3, "r": 0.945,
      "strength": "STRONG", "direction": "POSITIVE", "minAmount": 30.0, "maxAmount": 50.0 }
  ],
  "insufficient": [
    { "ingredient": "Egg", "unit": "pcs", "samples": 3, "reason": "NO_AMOUNT_VARIATION" }
  ]
}
```

- `strength`: `STRONG` (|r| ≥ 0.7) / `MODERATE` (0.4 ≤ |r| < 0.7) / `WEAK`
- `reason`: `NOT_ENOUGH_SAMPLES` (< 3 versions) / `NO_AMOUNT_VARIATION` / `NO_RATING_VARIATION`
- Exit code: `0` success, `1` bad input, `2` pandas not installed
