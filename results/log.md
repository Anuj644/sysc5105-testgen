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
  always throws. Scope decided by the instructor: see Decisions.
- Suspected bugs, verified in Runs 8-9: lenient SimpleDateFormat parsing
  (2023-02-29 becomes 01/03/2023) and parseDate returning 0 on failure.
  Not yet tested: parseAnyDate never breaks out of its loop,
  getDateAndTime(String) passes a String to format().

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


## Run 7: Dates, scoped baseline (smoke test only)
Date: 2026-10-16
Scope, confirmed by the instructor: convert, parseDate, getDesiredFormat (2
overloads), getDateOnly (2 overloads), parseAnyDate, DateFormats.getDateFormat.
Removing the Android-only methods was also confirmed acceptable. (Removed:
prettifyDate x2, datePickerDialog, timePickerDialog.)
PIT exclusion: -DexcludedMethods=getToday,getTomorrow,getTodayWithTime,
getDaysBetweenTwoDate,getHoursBetweenTwoDate,getMinutesBetweenTwoDates,
getDateAndTime,getTimeOnly,getDateFromDays
Tests: dates.SmokeTest (1), 0.050 s. mvn clean test total 3.944 s.
JaCoCo (scoped; per-method counters summed from jacoco.xml): line 7 covered /
19 missed (27% of 26), branch 0 of 2, instruction 35 covered / 79 missed (31% of 114).
PIT (scoped): 11 mutants (71 in the project minus the 60 in Quadratic), 4 killed
(36%), 0 survived, 7 no coverage, test strength 100% (4/4).
Killed: lines 48, 200, 215, 230. No coverage: 58, 60, 69 (getDateOnly), 191, 194
(parseAnyDate), 202 (catch block of parseDate), 210 (getDesiredFormat).
Note: PIT console figures such as "4 of 71 killed" and "line coverage 30/168"
cover the whole project (Quadratic + dates). Quote the scoped figures.


## Run 8: Dates, combinatorial testing (PICT, seed 42)
Date: 2026-10-16
Test class: dates.DatesCombinatorialTest. Model testdata/dates.pict:
Year 1900, 1970, 2000, 2023, 2024, 2100, 9999; Month 01, 02, 04, 12, 00, 13;
Day 01, 28, 29, 30, 31, 00, 32; Sep -, /.
Suites: exhaustive 588 | pairwise 50 | 3-way 294 -> 344 executions.
The 3-way suite equals Year x Month x Day (7x6x7): every triple once.
Oracle: java.time.LocalDate, strict ISO. Valid dates must convert exactly;
invalid ones must be rejected. DateHelper has no rejection mechanism, so the
outputs are recorded as defects in results/findings_dates_comb.txt (normal mode
green; -Dstrict=true fails on them).
Normal mode: 344 tests, 0 failures, 0.502 s and 0.531 s in two runs (mvn clean
test total 3.430 s and 3.540 s).
Strict mode: 272 of 344 tests fail (first: DT2 for '1900/02/31' -> 31/12/1969).

### JaCoCo (scoped)
Line 10 covered / 16 missed (38% of 26), branch 0 of 2, instruction 40 covered /
74 missed (35% of 114). The 3 extra lines versus the baseline are the catch
block of parseDate, reached by invalid inputs.

### PIT (scoped)
11 mutants: 4 killed (36%), 1 survived, 6 no coverage, test strength 80% (4/5).
Survived: line 202 parseDate (VoidMethodCall: removed e.printStackTrace()).
No coverage: lines 58, 60, 69 (getDateOnly), 191, 194 (parseAnyDate),
210 (getDesiredFormat). PIT analysis 2 s (mvn total 4.910 s), 198 test
executions (2.79 per mutation). Same 4 mutants killed as with the smoke test.

### Defects (results/findings_dates_comb_run7.txt)
DT1: invalid calendar date silently converted to a different date (lenient
     SimpleDateFormat), e.g. month 13, day 32, Feb 29 in a non-leap year.
DT2: unparsable input silently converted to the epoch date: parseDate returns
     0. Prints 31/12/1969 in America/Toronto (UTC-5), 01/01/1970 on a UTC
     machine. By the design of the model this covers the '/' separator inputs.
Distinct failing inputs: DT1 93, DT2 159 (252 in total). Strict mode reports 272
failing tests; the difference is inputs that occur in both suites.
Examples: '1900-00-00' converts to 30/11/1899 (the year changes);
'1900-02-29' converts to 01/03/1900; '1900-04-00' converts to 31/03/1900.

### Observations
- No valid date was converted incorrectly (leap days, century years, 9999 all
  correct); every defect is about invalid input being accepted silently.
- Mutation score did not change versus the smoke test (4 of 11), although 272
  of 344 tests exposed defects: coverage-based measures cannot see what the
  oracle detects.
- The 6 uncovered mutants are in getDateOnly, parseAnyDate and
  getDesiredFormat(formats); convert never calls them, so only direct tests
  can reach them.
- The survivor at line 202 removes e.printStackTrace(). It is observable by
  capturing System.err; the test classes silence System.err, which is why it
  survives. Killable in the improvement round. (An earlier note calling it
  unobservable was wrong.)
- Pairwise (50) reached both defect types, like the 3-way suite (294).
- Classifier limitation: DT2 is assigned when the output equals the epoch
  string, so an input such as 1970-00-31 (rolls over to 31/12/1969) is counted
  as DT2 instead of DT1; effect at most a few inputs.


## Run 9: Dates, category-partition testing
Date: 2026-10-16
Test class: dates.DatesCategoryPartitionTest, frames in testdata/dates_cp_frames.csv
Frames: 28 (F01-F28: 12 valid, 16 invalid) plus a null-input test = 29 tests.
Categories: leap vs non-leap February (F02/F03 vs F13-F15), month length
(F09-F11, F17), year and month boundaries (F05-F08, F12), invalid month and day
parts (F18-F21), malformed shape (F22-F28).
Oracle: valid frames have hand-derived output; the test also checks each frame
against a strict java.time reference (so a wrong hand-derived value fails
loudly). INVALID frames record what DateHelper does as defects in
results/findings_dates_catpart.txt (normal mode green; -Dstrict=true fails).
Normal mode: 29 tests, 0 failures, 0.215 s (mvn clean test total 3.508 s).
Strict mode: 17 failures (16 invalid frames + null test), 0.198 s.

### JaCoCo (scoped)
Line 10 covered / 16 missed (38% of 26), branch 0 of 2, instruction 40 / 74
missed (35%). Identical to the combinatorial suite.

### PIT (scoped)
11 mutants: 4 killed (36%), 1 survived (line 202), 6 no coverage (58, 60, 69,
191, 194, 210), test strength 80% (4/5). PIT analysis under 1 s (mvn total
3.320 s), 16 test executions (0.23 per mutation, whole project).

### Defects (results/findings_dates_catpart_run9.txt)
DT1 (9): impossible but well-formed date silently converted:
  2023-02-29 -> 01/03/2023, 1900-02-29 -> 01/03/1900, 2100-02-29 -> 01/03/2100,
  2023-02-30 -> 02/03/2023, 2023-04-31 -> 01/05/2023, 2023-13-01 -> 01/01/2024,
  2023-00-10 -> 10/12/2022, 2023-01-00 -> 31/12/2022, 2023-01-32 -> 01/02/2023.
DT2 (3): unparsable input silently becomes the epoch date (31/12/1969 in
  America/Toronto): '2023/12/25', '' (empty), 'abc'.
DT3 (4): malformed input accepted and converted (lenient parsing):
  '2023-1-5' -> 05/01/2023, '2023-12-25abc' -> 25/12/2023,
  '23-12-25' -> 25/12/0023 (year 23 taken literally; a wrong date),
  '2023-12-25T10:00' -> 25/12/2023.
DT4 (1): convert(null) throws NullPointerException.
Counts match the prediction made from the frame design (9, 3, 4, 1).

### Comparison with Run 8
Same line coverage, mutation score and test strength as the 344-test PICT
suite, with 29 tests (8%) and about 40% of the class time. It also finds two
defect types that the PICT suite cannot reach, because its model only contains
well-shaped strings with a '-' or '/' separator: DT3 and DT4.
All 12 valid frames passed and every hand-derived value equals the reference:
no valid date is converted incorrectly.
DT3 severity varies: only '23-12-25' yields a wrong date; the other three give
the right date but skip validation.

### Dates summary (scoped to the conversion methods)
| Suite              | Tests | Line (JaCoCo) | Branch | Mutation (PIT) | Strength | Defect types   |
|--------------------|-------|---------------|--------|----------------|----------|----------------|
| Baseline           | 1     | 27% (7/26)    | 0/2    | 36% (4/11)     | 100%     | none           |
| Combinatorial      | 344   | 38% (10/26)   | 0/2    | 36% (4/11)     | 80%      | DT1, DT2       |
| Category-partition | 29    | 38% (10/26)   | 0/2    | 36% (4/11)     | 80%      | DT1-DT4        |
| Metamorphic        | 856   | 50% (13/26)   | 0/2    | 55% (6/11)     | 86%      | DT5, DT7       |

### Screenshots
results/screenshots/ (27-31 dates baseline and combinatorial, 32-35 dates
category-partition).


## Run 10: Dates, metamorphic testing
Date: 2026-10-16
Test class: dates.DatesMetamorphicTest. Seeds: 284 valid dates (years 1900, 1970,
2000, 2023, 2024, 2100; every month; days 1, 15, 28 and the last day of the
month). No expected outputs stored; each test checks a relation:
MR1 round trip (convert, getDateOnly(String), format back to YYYY-MM-DD);
MR2 day shift (+1, +7, +31, +365 days: outputs differ by exactly n days);
MR3 order preservation; MR5 cross-path consistency (convert vs
getDateOnly(parseDate(...))); MR4 time zone and locale invariance (7 zones,
9 locales).
Tests: 856 (284 x 3 parameterized + 4 single). Normal mode: 0 failures, 0.675 s
(mvn clean test total 3.759 s). Strict mode: 2 failures (mr4TimeZoneInvalidInput
-> DT5, mr4LocaleValidDates -> DT7), 0.584 s.

### JaCoCo (scoped)
Line 13 covered / 13 missed (50% of 26), branch 0 of 2, instruction 60 covered /
54 missed (53% of 114). Newly reached: getDateOnly(String) and getDateOnly(long).

### PIT (scoped)
11 mutants: 6 killed (55%), 1 survived (line 202 parseDate printStackTrace),
4 no coverage (60 getDateOnly catch, 191 and 194 parseAnyDate, 210
getDesiredFormat), test strength 86% (6/7). Newly killed versus Run 8: lines
58 and 69 (getDateOnly return values), by MR1 and MR5. PIT analysis 2 s
(mvn total 5.029 s), 7 test executions.

### Defects (results/findings_dates_meta_run10.txt)
DT5 (3): unparsable input 'abc' prints 31/12/1969 in America/Toronto,
  Pacific/Kiritimati and Pacific/Pago_Pago, but 01/01/1970 in UTC (the epoch
  fallback of parseDate formatted in the default time zone).
DT6 (0): no valid date changes with the time zone.
DT7 (4): default locale changes the output of valid dates:
  ar-EG (1900-01-01): Arabic-Indic digits instead of ASCII digits;
  th-TH-u-nu-thai (1900-01-01): Thai digits;
  ja-JP-u-ca-japanese (2000-02-29) -> 01/03/2000 (wrong date);
  th-TH (2000-02-29) -> 01/03/2000 (wrong date).
  Only the first mismatching date per locale is recorded.
  Explanation (not verified): the Japanese and Thai Buddhist locales use a
  different calendar, so "yyyy" 2000 is not 2000 CE and is not a leap year.

### Observations
- MR1, MR2, MR3 and MR5 found no violation: valid dates convert correctly in
  the default environment. All defects found are environmental.
- Metamorphic testing found DT5 and DT7, which the other two techniques cannot
  see because they run in one fixed zone and locale; it cannot see DT1-DT4
  because its seeds are valid dates.
- Every technique finds a different set of defects (comb: DT1-2, cat-part:
  DT1-4, metamorphic: DT5, DT7).
- Coverage rose from 10 to 13 lines with 856 tests versus 344 because the
  relations call getDateOnly, which convert never calls.



  ## Run 11: Dates, improvement round (final suite)
Date: 2026-10-20
Targets (from Run 10): 4 uncovered mutants (lines 60, 191, 194, 210), 1 survivor
(line 202), 0 of 2 branches, 13 of 26 lines.
New class dates.DatesImprovementTest (7 tests): direct tests of getDateOnly (both
overloads), parseDate, parseAnyDate and getDesiredFormat(formats), plus capture
of System.err to observe the printed stack traces (the other test classes
silence System.err, which is why line 202 survived).
Normal mode: 7 tests, 0 failures, 0.465 s (mvn total 12.086 s).
Strict mode: 2 failures (parseAnyDateIsoString -> DT8, parseAnyDateSlashString
-> DT9), 0.403 s.

### Before / after (dates, scoped, all techniques combined)
| Metric             | Before (3 techniques, 1229 tests) | After (+ improvement, 1236 tests) |
|--------------------|-----------------------------------|-----------------------------------|
| JaCoCo line        | 13/26 (50%)                       | 26/26 (100%)                      |
| JaCoCo branch      | 0/2                               | 2/2 (100%)                        |
| JaCoCo instruction | 60/114 (53%)                      | 114/114 (100%)                    |
| PIT killed         | 6/11 (55%)                        | 11/11 (100%)                      |
| PIT survivors      | 1 (line 202)                      | 0                                 |
| PIT no coverage    | 4 (60, 191, 194, 210)             | 0                                 |
| Test strength      | 86% (6/7)                         | 100%                              |
Class times, before run: CategoryPartition 0.504 s, Combinatorial 1.204 s,
Metamorphic 1.176 s (mvn total 11.685 s). After run: CategoryPartition 0.528 s,
Combinatorial 1.084 s, Improvement 0.094 s, Metamorphic 1.019 s (mvn total
11.289 s). PIT (after): analysis 9 s, 180 test executions (2.54 per mutation),
whole-project console line "11 of 71 killed" because the 60 quadratic mutants
were not run in this session.
Timing note: this session ran about 2x slower than the 2026-10-16 session
(e.g. DatesCombinatorialTest 1.2 s versus 0.5 s), so class times must not be
compared across sessions. A single-session timing run is planned.

### New defects (results/findings_dates_improvement_run11.txt)
DT8: parseAnyDate('2023-12-25') returns the date 0031-06-15. The loop never
     breaks, so the LAST matching format wins: dd-MM-yyyy reads the string as
     day 2023, month 12, year 25. Input dependent: '25/12/2023' came back
     correct (last matching format is dd/MM/yyyy; explanation from reading the
     enum order, not tested).
DT9: parseAnyDate prints 40 stack traces per call even when it succeeds (the
     enum has 44 formats; 40 fail and print).

### Observations
- All five remaining mutants were killed by direct calls to methods that
  convert() never reaches, and by capturing System.err.
- As with the quadratic solver, the three technique suites plateaued
  (13/26 lines) because they drive one entry point; the remaining code needed
  direct tests.
- Structural coverage and mutation score are 100% while defects DT1-DT5 and
  DT7-DT9 remain in the code.

### Dates summary (scoped to the conversion methods)
| Suite                 | Tests | Line (JaCoCo) | Branch | Mutation (PIT) | Strength | Defect types       |
|-----------------------|-------|---------------|--------|----------------|----------|--------------------|
| Baseline              | 1     | 27% (7/26)    | 0/2    | 36% (4/11)     | 100%     | none               |
| Combinatorial         | 344   | 38% (10/26)   | 0/2    | 36% (4/11)     | 80%      | DT1, DT2           |
| Category-partition    | 29    | 38% (10/26)   | 0/2    | 36% (4/11)     | 80%      | DT1-DT4            |
| Metamorphic           | 856   | 50% (13/26)   | 0/2    | 55% (6/11)     | 86%      | DT5, DT7           |
| Three techniques      | 1229  | 50% (13/26)   | 0/2    | 55% (6/11)     | 86%      | DT1-DT5, DT7       |
| Final (+ improvement) | 1236  | 100% (26/26)  | 2/2    | 100% (11/11)   | 100%     | + DT8, DT9         |







## Run 12: Test execution time, all suites, one session
Date: 2026-10-20
Method: scripts/timing.ps1. mvn -q test -Dtest=<class> -Djacoco.skip=true, 5 runs
per suite after one clean test-compile; time = the "time" attribute of the
surefire XML (class time; excludes JVM and Maven start-up).
Machine: Windows 11, 24 logical processors, Java 17.0.20, Maven 3.10.0.
| Program   | Suite              | Tests | Mean (s) | Runs (s)                                | Per test |
|-----------|--------------------|-------|----------|-----------------------------------------|----------|
| Quadratic | Combinatorial      | 242   | 0.695    | 0.713, 0.695, 0.659, 0.681, 0.727       | 2.9 ms   |
| Quadratic | Category-partition | 18    | 0.241    | 0.288, 0.236, 0.238, 0.226, 0.219       | 13.4 ms  |
| Quadratic | Metamorphic        | 864   | 1.391    | 1.369, 1.414, 1.381, 1.419, 1.372       | 1.6 ms   |
| Quadratic | Improvement        | 12    | 0.143    | 0.127, 0.127, 0.153, 0.159, 0.149       | 11.9 ms  |
| Dates     | Combinatorial      | 344   | 1.182    | 1.205, 1.095, 1.234, 1.167, 1.208       | 3.4 ms   |
| Dates     | Category-partition | 29    | 0.260    | 0.311, 0.281, 0.295, 0.301, 0.112       | 9.0 ms   |
| Dates     | Metamorphic        | 856   | 1.694    | 0.873, 1.991, 1.961, 1.804, 1.839       | 2.0 ms   |
| Dates     | Improvement        | 7     | 0.257    | 0.259, 0.261, 0.265, 0.268, 0.233       | 36.7 ms  |
Three-technique totals: quadratic 2.327 s, dates 3.136 s.
Observations:
- Category-partition is the cheapest suite on both programs (0.24 s and 0.26 s);
  metamorphic is the slowest (1.39 s and 1.69 s) but cheapest per test.
- All suites finish in under 2 s; cost matters mainly for mutation analysis:
  PIT test executions on the quadratic solver were 109 (category-partition),
  1231 (combinatorial) and 4581 (metamorphic).
- Two outliers (dates metamorphic run 1: 0.873 s; dates category-partition run 5:
  0.112 s); means reported together with the run values.
- Class times recorded in Runs 2-11 come from other sessions and are not
  comparable with this table (this session was slower). Use Run 12 for any
  time comparison in the report.






## Run 13: Quadratic, category-partition alone, re-measured (18 frames, fixed test)
Date: 2026-10-20
Reason: the Run 3 numbers were taken before the line-23 fix and frame F18.
Tests: 18, 0 failures, 0.390 s (mvn clean test total 9.117 s, JaCoCo on).
JaCoCo (Quadratic): instruction 63% (118/319 missed), branch 70% (10/34 missed),
line 49% (36/73 covered). Unchanged from Run 3; all missed lines are in main,
validateInput and the default constructor.
PIT (Quadratic only): 41/60 killed (68%), 2 survived (line 71 ConditionalsBoundary,
line 66 Math), 17 no coverage, test strength 95% (41/43). PIT analysis 19 s
(20 s; mvn total 25.4 s), 93 test executions (0.98 per mutation, whole project).
Versus Run 3: killed 38 -> 41, survivors 5 -> 2 (lines 23, 25, 26 now killed).
Matches the prediction made before the run.
Caveat: category-partition was tuned using its survivor list (the line-23 fix and
frame F18); combinatorial and metamorphic were not. Report both: 63% untuned,
68% tuned.
Replaces the category-partition row in the comparison tables.






## Final comparison (source for the report tables)

### Table A: Quadratic solver (all 60 mutants, JaCoCo on Quadratic.java)
| Suite                      | Tests | Branch | Mutation     | Survivors | No cov. | Strength | Class time (s) |
|----------------------------|-------|--------|--------------|-----------|---------|----------|----------------|
| Baseline (smoke test)      | 1     | 23%    | 37% (22/60)  | 9         | 29      | 71%      | n/a            |
| Combinatorial              | 242   | 61%    | 60% (36/60)  | 7         | 17      | 84%      | 0.695          |
| Category-partition (tuned) | 18    | 70%    | 68% (41/60)  | 2         | 17      | 95%      | 0.241          |
| Metamorphic                | 864   | 64%    | 62% (37/60)  | 6         | 17      | 86%      | 1.391          |
| Three techniques (as first written) | 1123 | 70% | 67% (40/60) | 3     | 17      | 93%      | 2.327 (sum)    |
| Final (+ improvement tests)| 1136  | 100%   | 97% (58/60)  | 2         | 0       | 97%      | 2.470 (sum)    |
Category-partition before tuning (17 frames): 63% (38/60), 5 survivors, strength 88%.
Times from Run 12 (one session, JaCoCo off, mean of 5). Sums of class times.

### Table B: Dates (scoped to the conversion methods: 26 lines, 2 branches, 11 mutants)
| Suite                 | Tests | Line (JaCoCo) | Branch | Mutation     | Strength | Class time (s) |
|-----------------------|-------|---------------|--------|--------------|----------|----------------|
| Baseline (smoke test) | 1     | 27% (7/26)    | 0/2    | 36% (4/11)   | 100%     | n/a            |
| Combinatorial         | 344   | 38% (10/26)   | 0/2    | 36% (4/11)   | 80%      | 1.182          |
| Category-partition    | 29    | 38% (10/26)   | 0/2    | 36% (4/11)   | 80%      | 0.260          |
| Metamorphic           | 856   | 50% (13/26)   | 0/2    | 55% (6/11)   | 86%      | 1.694          |
| Three techniques      | 1229  | 50% (13/26)   | 0/2    | 55% (6/11)   | 86%      | 3.136 (sum)    |
| Final (+ improvement) | 1236  | 100% (26/26)  | 2/2    | 100% (11/11) | 100%     | 3.393 (sum)    |

### Table C: Which technique found which defect
| Defect                                              | Combinatorial | Category-part. | Metamorphic | Improvement |
|-----------------------------------------------------|---------------|----------------|-------------|-------------|
| D1 abort when c = 0 (roots exist)                   | yes (35)      | yes            | no          | yes (main)  |
| D2 hang when 4ac overflows                          | predicted, confirmed by hand | no | no       | no          |
| D3 large roots clamped to int range                 | yes (57)      | yes            | no          | no          |
| D4 prints "x + -yi"                                 | yes (3)       | yes            | no          | no          |
| D5 wrong roots, tiny discriminant                   | yes (1)       | yes            | yes (208)   | no          |
| D6 validateInput rejects valid numbers              | no            | no             | no          | yes         |
| D7 validateInput accepts NaN / Infinity             | no            | no             | no          | yes         |
| DT1 impossible date converted to another date       | yes (93)      | yes (9)        | no          | no          |
| DT2 unparsable input becomes the epoch date         | yes (159)     | yes (3)        | no          | no          |
| DT3 malformed input accepted                        | no            | yes (4)        | no          | no          |
| DT4 convert(null) throws NullPointerException       | no            | yes (1)        | no          | no          |
| DT5 epoch output depends on the time zone           | no            | no             | yes (3)     | no          |
| DT6 time zone changes a valid date                  | tested        | tested         | not found   | not found   |
| DT7 locale changes the output of valid dates        | no            | no             | yes (4)     | no          |
| DT8 parseAnyDate: last matching format wins         | no            | no             | no          | yes         |
| DT9 parseAnyDate prints 40 stack traces per call    | no            | no             | no          | yes         |
Counts are distinct failing inputs where the findings file lists them.
D2 note: the combinatorial test does not execute these inputs (it would hang);
the hang was confirmed by running the program by hand.

### Points for the discussion
1. No technique wins everywhere: category-partition has the best score per test
   and per second; combinatorial found the most quadratic defect types;
   metamorphic is the only one that found the zone/locale defects and needs no
   expected outputs.
2. Every technique found defects the others missed (Table C).
3. All three plateau on structure (quadratic 70% branch, dates 50% lines) because
   they drive one entry point; direct and stdin tests were needed for 100%.
4. 100% coverage and 97-100% mutation score coexist with 7 + 8 known defects.
5. Cost: metamorphic is the slowest suite (1.4-1.7 s) and needs the most PIT
   test executions (4581 on the quadratic solver vs 93 for category-partition).
6. Limits: two quadratic mutants (lines 66, 71) probably equivalent; dates scope
   narrowed with the instructor's approval; one machine; category-partition tuned
   after reading survivors while the others were not.




## Remaining work
- Single-session timing run of all eight suites (scripts/timing.ps1). (done)
- Cross-program comparison table and gaps discussion. (done)
- README, IEEE report (6 pages) with the AI disclosure, video (10 min max).


## Decisions
- [x] DateHelper approved as case study (provided on the course page).
- [x] Instructor confirmed: removing Android-only methods is OK; coverage and mutation
      score may be measured on the conversion-related methods only.
Scope for dates: convert, parseDate, getDesiredFormat (2 overloads), getDateOnly
(2 overloads), parseAnyDate, DateFormats.getDateFormat.
- [ ] Not yet answered: does the 6-page limit include the AI disclosure and
      references?

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
- dates.pict, DatesCombinatorialTest.java and the scoped measurement scripts
  (JaCoCo per-method sums, PIT CSV reader): AI-assisted (Claude), reviewed by me.
- dates_cp_frames.csv and DatesCategoryPartitionTest.java: AI-assisted
  (Claude), reviewed by me.
- Interpretation of the reports and defect analysis: AI-assisted, to be
  verified and rewritten by me.