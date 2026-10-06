# Experiment Log

## Run 1: Baseline (1 smoke test per program)
Date: 2026-10-06
Commit: c033db9 ("Baseline: setup, smoke tests, both case studies")
Tests: quadratic.SmokeTest (1), dates.SmokeTest (1). Total 2 tests.
Tools: Java 17, Maven 3.10.0, JUnit 5.10.2, JaCoCo 0.8.12, PIT 1.16.1

### JaCoCo (from target/site/jacoco/index.html)
| Class      | Instruction cov. | Line cov.        | Branch cov.     |
|------------|------------------|------------------|-----------------|
| Quadratic  | 33% (211/319 missed) | 29% (21/73)  | 23% (8/34)      |
| DateHelper | 10% (278/310 missed) | 8% (6/71)    | 0% (0/2)        |

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

### Decisions pending
- [ ] Instructor approval of DateHelper as the date case study.
- [ ] Scope of DateHelper evaluation (whole class vs conversion-related
      methods only).

### AI assistance used so far (for the disclosure section)
- Project plan and structure: AI-assisted (Claude).
- pom.xml, smoke tests, and the edited DateHelper.java: AI-assisted (Claude).
- Interpretation of the baseline reports: AI-assisted analysis, to be verified
  and rewritten by me.



  ## Phase 4: Combinatorial generation (quadratic)
Tool: PICT, seed 42. Model: testdata/quadratic.pict (a: 8 values, b: 5, c: 5).
Exhaustive: 200 | Pairwise (2-way): ___ tests | 3-way: ___ tests
Note: with 3 parameters, 3-way == exhaustive.