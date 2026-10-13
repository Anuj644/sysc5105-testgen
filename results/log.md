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
mvn test total time: not recorded for the baseline run.

### Notes
Quadratic:
- 29 mutants NO_COVERAGE: complex-root branch (lines 24-35), non-integer
  formatting (line 91), validateInput (lines 110-116), main (lines 128-179).
- 9 mutants SURVIVED, on lines 15, 23, 39, 40, 54, 66, 70, 71, 86. Seven are
  likely killable; lines 66 and 71 are likely equivalent mutants.
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
- Execution time: mvn clean test total 8.926 s; CombinatorialTest class 0.867 s.
- PIT run: 26 s mutation analysis (31.6 s total), 1231 test executions
  (12.96 per mutant), 3 mutants timed out (counted as killed).

### JaCoCo (Quadratic class)
| Metric      | Baseline | Combinatorial        |
|-------------|----------|----------------------|
| Instruction | 33%      | 62% (121/319 missed) |
| Branch      | 23%      | 61% (13/34 missed)   |
| Line        | 29%      | 49% (36/73 covered)  |
Per method: solveQuadratic 99% instr / 87% branch, sqrtByNewton 93% / 75%,
formatDouble 100% / 100%, sign 100% / 100%, main 0%, validateInput 0%.
All 37 missed lines are in main (29), validateInput (7) and the default
constructor (1).

### PIT (Quadratic only; project row is diluted by dates = 0/35)
| Line cov.   | Mutants | Killed | Survived | No coverage | Mutation score | Test strength |
|-------------|---------|--------|----------|-------------|----------------|---------------|
| 49% (36/73) | 60      | 36     | 7        | 17          | 60%            | 84% (36/43)   |
Survived (7): line 23 (boundary), 39 (math), 44 (negate), 54 (boundary and
negate), 66 (math), 71 (boundary).

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
- No model value gives discriminant exactly 0, so the double-root case is not
  exercised: this explains the survivors at line 23 (boundary) and line 44
  (the "print the same root twice" check).
- Line 54 survivors (b > 0 boundary and negation) only change which root is
  printed first and the cancellation behaviour; the oracle compares roots as a
  set with a numeric residual, so it cannot see them.
- Line 71 "<" vs "<=" is probably an equivalent mutant (equality of the step
  and the threshold practically never occurs). Line 66 changes only the
  starting guess; probably equivalent as well. Neither is proven.
- The NaN branch (Double.isNaN(discriminant)) was not reached: it needs
  b*b and 4ac both to overflow, i.e. |values| around 1e155 or larger. The
  model's maximum was 1e154.
- Lowercase "1e154" is rejected by validateInput ("too large or small")
  while "1.0E154" is accepted: valid e-notation is rejected.
- validateInput and main cannot be reached through numeric inputs; they
  need direct calls and stdin-driven tests.
- PIT reruns the tests and can overwrite results/findings_combinatorial.txt.
  A copy from a clean run is kept as findings_combinatorial_run2.txt.

### Screenshots
results/screenshots/ (04 pict, 05 normal run, 06 strict run, 07 jacoco,
08-09 pit, 10 hang).


## Run 3: Quadratic, category-partition testing
Date: 2026-10-06
Test class: quadratic.CategoryPartitionTest, frames in testdata/quad_cp_frames.csv
Frames: 17 (F01-F17), expected roots derived by hand. F18 added in Run 6.
Categories: discriminant (positive, zero, negative, equals b^2, NaN), a (positive,
negative, zero [error]), b (zero, non-zero), c (zero, non-zero), root kind.
Modes: normal (0 failures, class time 0.411 s) and strict (4 failures = known defects).

### JaCoCo (Quadratic)
Instruction 63% (118/319 missed), branch 70% (10/34 missed), line 49% (36/73).
solveQuadratic 100%/100%, sqrtByNewton 100%/100%, formatDouble 100%, sign 100%,
main 0%, validateInput 0%.

### PIT (Quadratic only)
60 mutants: 38 killed (63%), 5 survived, 17 no coverage. Test strength 88% (38/43).
Timed out: 4 (counted as killed). PIT run 19 s analysis (25.5 s total),
109 test executions (1.15 per mutation).
Survived (5): line 23 (boundary), 25 (math), 26 (math), 66 (math), 71 (boundary).

### Comparison with Run 2
17 frames vs 242 generated tests: branch coverage 70% vs 61%, mutation score
63% vs 60%, survivors 5 vs 7. Frames target specific conditions (NaN overflow,
b = 0, a != 1). Line coverage is identical (49%).

### Defects (results/findings_catpart_run3.txt)
D1: F12 aborts although roots exist (a=1, b=2, c=0)
D3: F11 root clamped to int range (a=1, b=0, c=-1.0E20) -> 2147483647 / -2147483648
D4: F09 prints '+ -' for negative imaginary part (a=-1, b=2, c=-5) -> "x1 = 1 + -2i"
D5: F13 wrong roots, tiny discriminant (a=1.0E-9, b=0, c=1.0E-9) -> +-3.81i
Exactly the four defect types that were predicted from the frame design.

### Observations
- F15 (a=b=c=1e200) reaches the isNaN branch that the combinatorial model missed.
- CORRECTION: frames F04/F05 (discriminant exactly 0) did NOT kill the "<" vs
  "<=" mutant at line 23. Cause was a flaw in my test code: the D5 exemption
  (|discriminant| < 1e-6) also matched a discriminant of exactly 0, so the
  mutant's wrong output was recorded as a known defect instead of failing.
  Fixed in Run 6 (exemption now requires discriminant != 0).
- Survivors at lines 25 and 26: all complex-root frames used a = 1 or a = -1,
  where 2*a equals 2/a, so the mutants on "2*a" could not be detected.
  Frame F18 (a=2, b=4, c=10) added in Run 6.
- The remaining uncovered lines were in main and validateInput; they needed
  direct calls and stdin-driven tests (Run 6).


## Run 4: Quadratic, metamorphic testing
Date: 2026-10-13
Test class: quadratic.MetamorphicTest (seed grid 6x6x6 = 216, c != 0, a != 0)
Relations: MR1 scale all coefficients (k = 2, 0.5, -1, 4, 1e9, 1e-9),
MR2 negate b (roots negate), MR3 reverse coefficients (roots invert),
MR4 (a, kb, k^2 c) (roots scale by k). Root sets compared numerically.
Tests: 864 (216 seeds x 4 relations). Normal mode: 0 failures, 0.539 s
(mvn clean test total 3.480 s).
Strict mode: 208 failures (= 216 seeds minus 8 whose discriminant is exactly 0),
all D5 at k = 1e-9.

### JaCoCo (Quadratic)
Instruction 61%, branch 64% (22/34 covered), line 48% (35/73).

### PIT (Quadratic only)
60 mutants: 37 killed (62%), 6 survived, 17 no coverage. Test strength 86% (37/43).
PIT analysis 9 s (13.7 s total), 4581 test executions (48.22 per mutation).
Survived (6): line 23 (boundary), 39 (math), 54 (boundary and negate),
66 (math), 71 (boundary).

### Defects found (results/findings_metamorphic_run4.txt)
D5: 208 entries. No other defect type was found.

### Observations
- Oracle-free: no expected outputs stored; found D5 (silent wrong roots) only.
- Cannot find D1-D4: seeds avoid c = 0, overflow, large roots; numeric root
  comparison ignores formatting and root order.
- Single printed roots are duplicated before comparison, so the double-root
  mutants (lines 23, 44) are invisible to this oracle as well.
- Similar mutation score to category-partition at 40x the PIT test executions.


## Run 5: Quadratic, all three techniques combined (before improvement)
Date: 2026-10-13
Command: -Dtest=CombinatorialTest,CategoryPartitionTest,MetamorphicTest
Tests: 1123 (17 + 242 + 864), 0 failures. Class times 0.151 s, 0.172 s, 0.331 s.
mvn clean test total 3.849 s.

### JaCoCo (Quadratic)
Instruction 63%, branch 70%, line 53% (39/73 covered, 34 missed).
Same branch coverage as category-partition alone: the combined suite adds
nothing structurally; the remaining gap is main and validateInput.

### PIT (Quadratic only)
60 mutants: 40 killed (67%), 3 survived, 17 no coverage. Test strength 93% (40/43).
Survived (3): line 23 (boundary), 66 (math), 71 (boundary).
PIT analysis 24 s (27.4 s total), 5340 test executions (56.21 per mutation).

### Technique comparison (Quadratic)
| Technique          | Tests | Branch | Mutation | Survivors | PIT executions | Defect types |
|--------------------|-------|--------|----------|-----------|----------------|--------------|
| Baseline           | 1     | 23%    | 37%      | 9         | n/a            | n/a          |
| Combinatorial      | 242   | 61%    | 60%      | 7         | 1231           | D1-D5        |
| Category-partition | 17    | 70%    | 63%      | 5         | 109            | D1,D3,D4,D5  |
| Metamorphic        | 864   | 64%    | 62%      | 6         | 4581           | D5           |
| All three          | 1123  | 70%    | 67%      | 3         | 5340           | D1-D5        |

### Observations
- Category-partition gives the best score per test (17 frames, best single
  technique on branch coverage and mutation score).
- The techniques are complementary: survivors at lines 25/26 (category-partition)
  and 39/44/54 (combinatorial, metamorphic) are killed by the other suites, so
  the union leaves only 23, 66 and 71.
- Of the 3 combined survivors, line 23 is killable (needs a discriminant-0 test
  that is not swallowed by the D5 exemption); lines 66 and 71 are probably
  equivalent mutants.
- Combining techniques raises the mutation score (67% vs 63%) but not branch
  coverage; the remaining 17 no-coverage mutants and uncovered branches are
  in main and validateInput.
- Defect detection depends on what each technique reaches, not on how many
  tests it runs.


## Run 6: Quadratic, improvement round (final suite)
Date: 2026-10-13
Changes made after reading the Run 5 reports:
1. CategoryPartitionTest: D5 exemption now requires discriminant != 0
   (fixes the flaw that hid the line-23 mutant).
2. Frame F18 added (a=2, b=4, c=10, complex roots -1 +- 2i), for the 2*a
   mutants on lines 25 and 26. Hand-derived expected roots were correct.
3. New class ImprovementTest (12 tests): direct validateInput tests and
   scripted-stdin tests of main().

### Test runs
- CategoryPartitionTest: 18 tests, 0 failures, 0.137 s (mvn total 3.262 s).
- ImprovementTest normal mode: 12 tests, 0 failures, 0.103 s (mvn total 3.252 s).
- ImprovementTest strict mode: 4 failures (one per test method that records a
  known defect; JUnit stops at the first record in each method).
- Final suite (four classes): 1136 tests (18 + 242 + 864 + 12), 0 failures.
  Class times: CategoryPartition 0.188 s, Combinatorial 0.183 s,
  Improvement 0.008 s, Metamorphic 0.306 s. mvn clean test total 3.975 s.
  (Earlier text planned 13 ImprovementTest tests: that was a miscount; the
  class has 12 test methods.)

### Before / after (Quadratic, all techniques combined)
| Metric             | Before (Run 5)  | After (Run 6)    |
|--------------------|-----------------|------------------|
| Tests              | 1123            | 1136             |
| JaCoCo instruction | 63%             | 99% (3/319 missed) |
| JaCoCo branch      | 70%             | 100% (0/34 missed) |
| JaCoCo line        | 53% (39/73)     | 99% (72/73)      |
| PIT killed         | 40/60 (67%)     | 58/60 (97%)      |
| PIT survivors      | 3 (23, 66, 71)  | 2 (66, 71)       |
| PIT no coverage    | 17              | 0                |
| Test strength      | 93%             | 97% (58/60)      |
JaCoCo: all methods 100% (solveQuadratic, main, validateInput, sqrtByNewton,
formatDouble, sign). The only uncovered line is the default constructor
Quadratic().
PIT: analysis 34 s (35 s total), 3677 test executions (38.71 per mutation,
whole project; 35 dates mutants are uncovered and counted in the project row).
Project row: 58/95 killed (61%), line coverage 72/168 (43%); the quadratic
row alone is 58/60 (97%), line 72/73 (99%).

### New defects (results/findings_improvement_run6.txt)
D1: main reports precision failure for (1, 2, 0), whose roots are 0 and -2
D6: validateInput rejects valid e-notation '1e154' as too large or small
D6: validateInput rejects valid number '+5'
D6: validateInput rejects valid number '0.10'
D7: validateInput accepts 'Infinity'
D7: validateInput accepts 'NaN' and returns NaN
D6 = valid numeric input rejected; D7 = non-finite input accepted.
D1 now also confirmed through the user-facing main().

### Observations
- Line 23 is now killed: removing the D5 exemption for discriminant 0 let the
  double-root frames F04/F05 detect the boundary mutant.
- Lines 25 and 26 are killed by F18 (a = 2) in the category-partition suite
  alone.
- The 17 no-coverage mutants (main, validateInput, formatting) are all covered
  and killed by ImprovementTest.
- The two remaining survivors, line 66 (math, starting guess of the Newton
  iteration) and line 71 (boundary, "<" vs "<=" in the stopping test), are
  probably equivalent mutants: the first only changes the starting value and
  Newton's method converges to the same value within the tolerance, the
  second needs the step to equal 1e-8 exactly. Not proven formally.
- Branch coverage reached 100% only after adding input-handling tests; the
  numeric-input techniques alone plateaued at 70%.
- Structural coverage and mutation score reached near-ceiling values while
  defects D1-D7 remain in the code: tests that cover code do not fix it. The
  defects were found by the tests' oracles, not by the coverage numbers.

### Quadratic summary (for the report)
| Suite                    | Tests | Branch | Mutation | Survivors | No cov. |
|--------------------------|-------|--------|----------|-----------|---------|
| Baseline                 | 1     | 23%    | 37%      | 9         | 29      |
| Combinatorial            | 242   | 61%    | 60%      | 7         | 17      |
| Category-partition       | 17    | 70%    | 63%      | 5         | 17      |
| Metamorphic              | 864   | 64%    | 62%      | 6         | 17      |
| Three techniques         | 1123  | 70%    | 67%      | 3         | 17      |
| Final (+ improvement)    | 1136  | 100%   | 97%      | 2         | 0       |

### Screenshots
23 improvement run, 24 strict run, 25 final JaCoCo, 26 final PIT summary.


## Decisions pending
- [ ] Instructor approval of DateHelper as the date case study.
- [ ] Scope of DateHelper evaluation (whole class vs conversion-related
      methods only).
- [ ] Dates: all three techniques and an improvement round still to do.

## AI assistance used so far (for the disclosure section)
- Project plan and structure: AI-assisted (Claude).
- pom.xml, smoke tests, and the edited DateHelper.java: AI-assisted (Claude).
- PICT model values, SolverHarness.java, CombinatorialTest.java, and the D5
  handling edit: AI-assisted (Claude), reviewed by me.
- CategoryPartitionTest.java, frame design and expected values: AI-assisted
  (Claude), reviewed by me.
- MetamorphicTest.java, MR design and seed grid: AI-assisted (Claude),
  reviewed by me.
- CategoryPartitionTest fix (discriminant != 0), frame F18 and
  ImprovementTest.java: AI-assisted (Claude), reviewed by me.
- Interpretation of the reports and defect analysis: AI-assisted, to be
  verified and rewritten by me.