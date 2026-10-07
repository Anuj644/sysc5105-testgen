# Experiment Log

## Run 1: Baseline (1 smoke test per program)
Date: 2026-10-06
Commit: c033db9 ("Baseline: setup, smoke tests, both case studies")
Tests: quadratic.SmokeTest (1), dates.SmokeTest (1). Total 2 tests.
Tools: Java 17, Maven 3.10.0, JUnit 5.10.2, JaCoCo 0.8.12, PIT 1.16.1

### JaCoCo (from target/site/jacoco/index.html)
| Class      | Instruction cov.     | Line cov.     | Branch cov. |
|------------|----------------------|---------------|-------------|
| Quadratic  | 33% (211/319 missed) | 29% (21/73)   | 23% (8/34)  |
| DateHelper | 10% (278/310 missed) | 8% (6/71)     | 0% (0/2)    |

### PIT (from target/pit-reports/.../index.html)
| Class      | Line cov. (PIT) | Mutants | Killed | Survived | No coverage | Mutation score | Test strength |
|------------|-----------------|---------|--------|----------|-------------|----------------|---------------|
| Quadratic  | 29% (21/73)     | 60      | 22     | 9        | 29          | 37%            | 71% (22/31)   |
| DateHelper | 32% (30/95)     | 35      | 4      | 0        | 31          | 11%            | 100% (4/4)    |

### Execution time
mvn test total time: ___ s   (fill in from the "Total time:" line)

### Notes
Quadratic:
- 29 mutants NO_COVERAGE: complex-root branch (lines 24-35), non-integer
  formatting (line 91), validateInput (lines 110-116), main (lines 128-179).
- 9 mutants SURVIVED. Seven are likely killable and two are likely equivalent
  (lines 66 and 71).
- Killed count includes 4 TIMED_OUT mutants (PIT counts these as detected).
- Weak oracle: SmokeTest uses contains("x1 = 2"), so line 86 survived. Exact
  string assertions are needed.
- Test data too "nice": a=1 and discriminant=1 let several arithmetic mutants
  survive (lines 15, 39, 40, 70).
- JaCoCo counts the default constructor Quadratic() as an uncovered method;
  it can be ignored.
- NotEnoughPrecisionException was missing from the source repo and was created
  for this project (empty class extending Exception).

DateHelper:
- All 31 non-killed mutants are NO_COVERAGE; the smoke test kills every mutant
  it reaches (lines 48, 200, 215, 230).
- DateHelper has only 2 branches, so branch coverage is nearly meaningless as
  a metric here. Use line coverage and mutation score.
- Many methods are outside the conversion case study: getToday, getTomorrow,
  getTodayWithTime, getDesiredFormat(formats) (clock-dependent), the
  getDays/Hours/MinutesBetween methods, and getDateAndTime(String), which
  always throws. Scope decision pending (see below).
- Known suspected bugs to verify: lenient SimpleDateFormat parsing (2023-02-30
  becomes March 2), parseDate returns 0 on failure, parseAnyDate never breaks
  out of its loop, getDateAndTime(String) passes a String to format().

General:
- JaCoCo and PIT count lines differently (e.g. DateHelper 6/71 vs 30/95
  because PIT includes the nested enum). State which tool each number is from.
- Android-dependent code removed from DateHelper.java (DateUtils,
  DatePickerDialog, TimePickerDialog, prettifyDate); convert() added.


## Run 2: Quadratic, combinatorial testing (PICT, seed 42)
Date: 2026-10-06
Test class: quadratic.CombinatorialTest (+ SolverHarness helper)
Model: testdata/quadratic.pict  (a: 8 values, b: 5, c: 5)
Suites: exhaustive 200 | pairwise (2-way) 42 | 3-way 200  -> 242 test executions
Note: with 3 parameters, 3-way == exhaustive (no reduction).
Oracle: no stored expected outputs. Root residual |a z^2 + b z + c| relative
to term size <= 1e-6; real/complex type checked against the exact
discriminant sign (BigDecimal).
Modes: normal (known defects written to results/findings_*.txt, build green)
and strict -Dstrict=true (known defects fail the test).

### Results
- Normal mode: 0 failures. Strict mode: fails on known defects (screenshot taken).
- Execution time: mvn clean test total 8.926 s s; CombinatorialTest class 0.867 s s.
- PIT run: 26 s mutation analysis (31.6 s total), 1231 test executions
  (12.96 per mutant), 3 mutants timed out (counted as killed).

### JaCoCo (Quadratic class)
| Metric      | Baseline | Combinatorial |
|-------------|----------|---------------|
| Instruction | 33%      | 62% (121/319 missed) |
| Branch      | 23%      | 61% (13/34 missed)   |
| Line        | 29%      | 49% (36/73 covered)  |
Per method: solveQuadratic 99% instr / 87% branch, sqrtByNewton 93% / 75%,
formatDouble 100% / 100%, sign 100% / 100%, main 0%, validateInput 0%.
All 37 missed lines are in main (29), validateInput (7) and the default
constructor (1).

### PIT (Quadratic only; project row is diluted by dates = 0/35)
| Line cov. | Mutants | Killed | Survived | No coverage | Mutation score | Test strength |
|-----------|---------|--------|----------|-------------|----------------|---------------|
| 49% (36/73) | 60    | 36     | 7        | 17          | 60%            | 84% (36/43)   |
Survived mutants by mutator: ConditionalsBoundary 3, Math 2, NegateConditionals 2.
Survived lines: ___ (read from the annotated Quadratic.java page).

### Defects found (results/findings_combinatorial_run2.txt)
| ID | Defect | Distinct failing inputs | Status |
|----|--------|-------------------------|--------|
| D1 | Solver aborts with NotEnoughPrecisionException when c = 0 although roots 0 and -b/a exist. discriminant == b*b is true whenever c = 0. | 35 (every a != 0, b with c = 0) | Executed |
| D2 | Infinite loop: 4ac overflows to Infinity, discriminant becomes -Infinity, sqrtByNewton never terminates. | 5 (a = c = 1e154, every b) | Confirmed manually: a=1.0E154, b=1, c=1.0E154 gives no output (hang) |
| D3 | formatDouble casts integral roots to int, so roots beyond +-2^31 print as 2147483647 / -2147483648 (silent wrong answer). | 57 | Executed |
| D4 | Cosmetic: prints "x + -yi" for a negative imaginary part. | 3 | Executed |
| D5 | Wrong roots when |discriminant| is tiny: Newton stops at absolute step 1e-8. Input (1e-9, 0, 1e-9) prints +-3.81i, true roots +-i. | 1 | Executed |
Pairwise (42 tests) exposed the same defect types as 3-way (200 tests).

### Observations
- No model value gives discriminant exactly 0, so the double-root case
  and the "<" vs "<=" mutant at line 23 are not exercised.
- The oracle compares roots as a set, so it does not notice root order;
  the b > 0 boundary mutant (line 54) probably survives for that reason.
- Line 71 "<" vs "<=" is probably an equivalent mutant.
- The NaN branch (Double.isNaN(discriminant)) was not reached: it needs
  b*b and 4ac both to overflow, i.e. |values| around 1e155 or larger. The
  model's maximum was 1e154, so add 1e200 for category-partition.
- Lowercase "1e154" is rejected by validateInput ("too large or small")
  while "1.0E154" is accepted: valid e-notation is rejected.
- validateInput and main cannot be reached through numeric inputs; they
  need direct calls and stdin-driven tests.
- PIT reruns the tests and can overwrite results/findings_combinatorial.txt.
  A copy from a clean run is kept as findings_combinatorial_run2.txt.

### Screenshots
results/screenshots/ (04 pict, 05 normal run, 06 strict run, 07 jacoco,
08-09 pit, 10 hang).

### Decisions pending
- [ ] Instructor approval of DateHelper as the date case study.
- [ ] Scope of DateHelper evaluation (whole class vs conversion-related
      methods only).

### AI assistance used so far (for the disclosure section)
- Project plan and structure: AI-assisted (Claude).
- pom.xml, smoke tests, and the edited DateHelper.java: AI-assisted (Claude).
- PICT model values, SolverHarness.java, CombinatorialTest.java, and the D5
  handling edit: AI-assisted (Claude), reviewed by me.
- Interpretation of the reports and defect analysis: AI-assisted, to be
  verified and rewritten by me.