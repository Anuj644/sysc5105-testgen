# testdata/

Inputs for the test suites. The tests read these files by relative path, so run Maven from the repository root.

| File | What it is |
|---|---|
| `quadratic.pict`, `dates.pict` | PICT models (parameters and values) for combinatorial testing. |
| `quad_pairwise.tsv`, `quad_3way.tsv`, `dates_pairwise.tsv`, `dates_3way.tsv` | Test suites generated from the models by PICT with seed 42. |
| `quad_cp_frames.csv`, `dates_cp_frames.csv` | Category-partition test frames (inputs and expected results). |
| `category_partition_design.md` | The category-partition design: categories, choices, constraints and frames. |

To regenerate the `.tsv` files, see "Regenerating the PICT suites (seed 42)" in the top-level `README.md`.