# SYSC 5105: Automated Test Case Generation

Comparison of three automated test-generation techniques (combinatorial,
category-partition, metamorphic) on two Java programs, measured with JaCoCo
(branch/line coverage), PIT (mutation score) and wall-clock execution time.

Repository: https://github.com/Anuj644/sysc5105-testgen
Author: [your name], [student number]

## Programs under test

| Program | Source | Changes made for this project |
|---|---|---|
| Quadratic equation solver (`src/main/java/quadratic/Quadratic.java`) | https://github.com/pof-mchliakh/Quadratic-Equation-Solver (original authors named in the file header) | Added `package quadratic;`. Created the missing `NotEnoughPrecisionException` class (empty class extending `Exception`). Logic unchanged. |
| Date converter (`src/main/java/dates/DateHelper.java`) | https://github.com/k0shk0sh/The-Flash (`DateHelper.java`), provided on the course page | Removed the Android-only code (`prettifyDate` x2, `datePickerDialog`, `timePickerDialog` and their imports), added `package dates;`, replaced one dead Javadoc link, added `convert(String)` (YYYY-MM-DD to DD/MM/YYYY) built from the original `parseDate` and `getDesiredFormat`. Removal of the Android code was confirmed by the instructor. All other logic unchanged, bugs included. |

The code of both programs belongs to its original authors; check the upstream
repositories for their licences before redistributing.

**Scope for the dates (confirmed by the instructor):** coverage and mutation
score are measured only on the conversion-related methods: `convert`,
`parseDate`, `getDesiredFormat` (both overloads), `getDateOnly` (both
overloads), `parseAnyDate`, `DateFormats.getDateFormat` (26 lines, 2 branches,
11 mutants). The other methods depend on the current time or are unrelated.

## Repository layout

```
pom.xml                         JUnit 5, JaCoCo 0.8.12, PIT 1.16.1
src/main/java/quadratic/        Quadratic.java, NotEnoughPrecisionException.java
src/main/java/dates/            DateHelper.java
src/test/java/quadratic/        SmokeTest, SolverHarness (helper), CombinatorialTest,
                                CategoryPartitionTest, MetamorphicTest, ImprovementTest
src/test/java/dates/            SmokeTest, DatesCombinatorialTest, DatesCategoryPartitionTest,
                                DatesMetamorphicTest, DatesImprovementTest
testdata/                       PICT models (quadratic.pict, dates.pict), generated suites
                                (*_pairwise.tsv, *_3way.tsv), category-partition frames (*_cp_frames.csv)
scripts/measure.ps1             scoped JaCoCo and PIT readers for the dates
scripts/timing.ps1              timing of every suite in one session
results/log.md                  full experiment log (every run, every number)
results/findings_*.txt          defects recorded by the tests (one clean copy per run)
results/screenshots/            screenshots of the runs
```

## Requirements

Java 17, Maven 3.10, PowerShell (Windows). PICT (https://github.com/microsoft/pict)
is only needed to regenerate the suites; the generated files are committed.

## How to run

Run everything from the repository root (the tests read `testdata/` by relative path).

All tests, with the JaCoCo report (`target/site/jacoco/index.html`):

```powershell
mvn clean test
```

One class (examples):

```powershell
mvn clean test "-Dtest=quadratic.CombinatorialTest"
mvn clean test "-Dtest=dates.DatesMetamorphicTest"
```

### Normal mode and strict mode

The programs contain real defects. In **normal mode** the tests record each known
defect in `results/findings_*.txt` and the build stays green, so PIT can run (PIT
requires a passing suite). With **`-Dstrict=true`** every recorded defect fails the
test, which shows the fault-detection result:

```powershell
mvn clean test "-Dtest=dates.DatesCategoryPartitionTest" "-Dstrict=true"
```

Each test class rewrites its `findings_*.txt` file when it runs, and PIT re-runs
the tests, so the findings file in `results/` can change when PIT runs. The
`*_runN.txt` copies in `results/` were taken right after a clean normal-mode run
and are the ones quoted in the report.

### Regenerating the PICT suites (seed 42)

```powershell
pict testdata\quadratic.pict /o:2 /r:42 | Out-File -Encoding ascii testdata\quad_pairwise.tsv
pict testdata\quadratic.pict /o:3 /r:42 | Out-File -Encoding ascii testdata\quad_3way.tsv
pict testdata\dates.pict     /o:2 /r:42 | Out-File -Encoding ascii testdata\dates_pairwise.tsv
pict testdata\dates.pict     /o:3 /r:42 | Out-File -Encoding ascii testdata\dates_3way.tsv
```

### Mutation testing (PIT)

Quadratic (read the `quadratic.Quadratic` rows; the project summary also counts the dates):

```powershell
mvn org.pitest:pitest-maven:mutationCoverage "-DtargetTests=quadratic.CategoryPartitionTest" "-DoutputFormats=CSV,HTML"
```

Several classes are given as a comma-separated list in `-DtargetTests`. Reports are
written to `target/pit-reports/`.

Dates (scoped to the conversion methods):

```powershell
. .\scripts\measure.ps1          # defines $ex, Get-DatesJacoco, Get-DatesPit
mvn clean test "-Dtest=dates.DatesImprovementTest"
Get-DatesJacoco                  # scoped line / branch / instruction counts
mvn org.pitest:pitest-maven:mutationCoverage "-DtargetTests=dates.DatesImprovementTest" "-DexcludedMethods=$ex" "-DoutputFormats=CSV,HTML"
Get-DatesPit                     # scoped mutants, killed, survivors, uncovered lines
```

Run `Get-DatesJacoco` right after a `mvn clean test` (it reads
`target/site/jacoco/jacoco.xml`) and `Get-DatesPit` right after a PIT run.

### Execution time

```powershell
mvn clean test-compile "-Djacoco.skip=true"
. .\scripts\timing.ps1           # 5 runs per suite, mean class time from the surefire XML
```

## Test suites

| Technique | Quadratic solver | Dates |
|---|---|---|
| Combinatorial (PICT, seed 42) | 42 pairwise + 200 3-way = 242 tests (3-way equals exhaustive with 3 parameters) | 50 pairwise + 294 3-way = 344 tests (exhaustive would be 588) |
| Category-partition | 18 hand-written frames (`quad_cp_frames.csv`) | 28 frames + a null test = 29 tests (`dates_cp_frames.csv`) |
| Metamorphic | 864 tests: 216 seeds x 4 relations | 856 tests: 284 seeds, 5 relations |
| Improvement round | 12 tests: direct `validateInput` calls and scripted-stdin runs of `main()` | 7 tests: direct calls plus capture of `System.err` |

Oracles: the quadratic tests check the root residual and the exact discriminant sign
(no stored expected outputs); the date tests compare with `java.time.LocalDate`.

## Results

Quadratic solver (all 60 mutants):

| Suite | Tests | Branch cov. | Mutation score | Survivors |
|---|---|---|---|---|
| Baseline (smoke test) | 1 | 23% | 37% (22/60) | 9 |
| Combinatorial | 242 | 61% | 60% (36/60) | 7 |
| Category-partition (after tuning) | 18 | 70% | 68% (41/60) | 2 |
| Metamorphic | 864 | 64% | 62% (37/60) | 6 |
| Final (all + improvement tests) | 1136 | 100% | 97% (58/60) | 2 |

Dates (scoped, 26 lines, 2 branches, 11 mutants):

| Suite | Tests | Line cov. | Mutation score |
|---|---|---|---|
| Baseline (smoke test) | 1 | 27% (7/26) | 36% (4/11) |
| Combinatorial | 344 | 38% (10/26) | 36% (4/11) |
| Category-partition | 29 | 38% (10/26) | 36% (4/11) |
| Metamorphic | 856 | 50% (13/26) | 55% (6/11) |
| Final (all + improvement tests) | 1236 | 100% (26/26), branches 2/2 | 100% (11/11) |

The two remaining quadratic survivors (lines 66 and 71) are probably equivalent
mutants. Mean class times, one session, JaCoCo off: category-partition 0.24 s and
0.26 s, combinatorial 0.70 s and 1.18 s, metamorphic 1.39 s and 1.69 s.
Full numbers, run by run, are in `results/log.md`.

## Defects found

Quadratic solver: **D1** aborts when c = 0 although roots exist; **D2** infinite loop
when 4ac overflows (confirmed by running the program by hand); **D3** roots beyond
2^31 are clamped to the `int` range; **D4** prints `x + -yi`; **D5** wrong roots when
the discriminant is tiny; **D6** `validateInput` rejects valid numbers (`1e154`, `+5`,
`0.10`); **D7** `validateInput` accepts `NaN` and `Infinity`.

Date converter: **DT1** impossible dates are silently converted to another date
(2023-02-29 becomes 01/03/2023); **DT2** unparsable input becomes the epoch date
(31/12/1969 in America/Toronto, 01/01/1970 in UTC); **DT3** malformed input is
accepted; **DT4** `convert(null)` throws `NullPointerException`; **DT5** the epoch
output depends on the time zone; **DT7** the default locale changes the output of
valid dates; **DT8** `parseAnyDate` returns the last matching format
(`2023-12-25` gives 0031-06-15); **DT9** `parseAnyDate` prints 40 stack traces per
call. (DT6, a time zone changing a valid date, was tested and not found.)

The programs were not fixed; the defects are documented so that mutation scores stay
comparable.

## Limitations

- One machine (Windows 11, Java 17), default time zone America/Toronto; timing varies
  between sessions, so only times from one session (`results/log.md`, Run 12) are compared.
- Category-partition was tuned after its survivors were read (a test fix and one extra
  frame); the other techniques were not. Both numbers are in the log.
- Equivalent mutants cannot be killed; two remain in the quadratic solver.
- The dates are evaluated on the conversion-related methods only.

## AI assistance disclosure

AI assistance (Claude, Anthropic) was used for: the project plan; `pom.xml`; the PICT
models; all test classes and helper scripts (`SolverHarness`, the combinatorial,
category-partition, metamorphic and improvement test classes for both programs,
`scripts/measure.ps1`, `scripts/timing.ps1`); removing the Android code from
`DateHelper.java`; the first interpretation of the reports; and this README. Test
classes carry a comment marking them as AI-assisted. The experiments were run by the
author, and the numbers in `results/log.md` come from the author's own runs.
