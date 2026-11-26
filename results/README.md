# results/

Raw output of every experiment run for the report.

| File | What it is |
|---|---|
| `log.md` | Full experiment log: every run (1 to 13), every number, the survivors, and the final comparison tables. Report tables are taken from here. |
| `findings_*.txt` | Defects recorded by the tests (one file per suite). Files ending in `_runN.txt` are copies taken right after a clean normal-mode run; these are the ones quoted in the report. The files without `_runN` are rewritten whenever the tests or PIT run. |
| `SYSC_5105_Project_Evidence.pdf` | Screenshots of the runs, with the method for each step. |

Run numbers in the file names match the run numbers in `log.md`.
To regenerate the numbers, see "How to run" in the top-level `README.md`.