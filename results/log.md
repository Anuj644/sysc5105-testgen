# Experiment Log

## Run 1: Baseline (1 smoke test per program)
Date: 2026-10-06
Tests: quadratic SmokeTest (1), dates SmokeTest (1, not yet added)

### JaCoCo (from target/site/jacoco/index.html)
| Class      | Line coverage | Branch coverage |
|------------|---------------|-----------------|
| Quadratic  | ___%          | ___%            |
| DateHelper | pending       | pending         |

### PIT (from target/pit-reports/.../index.html)
| Class      | Mutants generated | Killed | Mutation score |
|------------|-------------------|--------|----------------|
| Quadratic  | 60                | 22     | 37%            |
| DateHelper | pending           | pending| pending        |

PIT line coverage (Quadratic): 29% (21/73)
PIT test strength (Quadratic): 71% (22/31)

### Execution time
mvn test total time: ___ s

### Notes
- Quadratic: 29 mutants NO_COVERAGE (complex-root branch lines 24-35, non-integer
  formatting line 91, validateInput lines 110-116, main lines 128-179).
- Quadratic: 9 mutants SURVIVED. Seven are likely killable and two are likely
  equivalent (lines 66 and 71).
- Killed count includes 4 TIMED_OUT mutants (PIT counts these as detected).
- Weak oracle: SmokeTest uses contains("x1 = 2"), so line 86 survived. Exact
  string assertions are needed.
- Test data too "nice": a=1 and discriminant=1 let several arithmetic mutants
  survive (lines 15, 39, 40, 70).
- JaCoCo and PIT measure line coverage differently, so the numbers can differ.
  State which tool each number came from in the report.