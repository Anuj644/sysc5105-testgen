# Category-partition design

This file documents the category-partition models used in the project.
The test frames themselves are in `testdata/quad_cp_frames.csv` and
`testdata/dates_cp_frames.csv`; they are run by
`src/test/java/quadratic/CategoryPartitionTest.java` and
`src/test/java/dates/DatesCategoryPartitionTest.java`. Each JUnit test is named
after its frame ID (F01, F02, ...).

Notation: **[error]** = the input is invalid, so a correct program must reject it.
Each frame is one choice from each category; choices that cannot occur together
are excluded by the constraints.

---

## 1. Quadratic solver: `solveQuadratic(a, b, c)`

### 1.1 Categories, choices and constraints

| Category | Choices (blocks) | Constraint / property |
|---|---|---|
| a | positive, a = 1 | none |
| a | positive, a ≠ 1 | none |
| a | negative | none |
| a | tiny (1e-9) | none |
| a | huge (1e200) | none |
| a | zero | **[error]** whatever b and c are |
| b | zero | none |
| b | non-zero | none |
| b | large (1e8, 1e10) | none |
| c | zero | none |
| c | non-zero | none |
| c | large negative (-1e20) | none |
| c | large (1e200) | none |
| Discriminant (derived from a, b, c) | positive | real roots |
| Discriminant | zero | real double root; needs b² = 4ac |
| Discriminant | negative | complex roots |
| Discriminant | equal to b² in floating point | **[error]**: the solver aborts |
| Discriminant | overflow (NaN) | **[error]**: the solver aborts |
| Root kind (derived) | real, complex | follows from the discriminant |

### 1.2 Test frames

| Frame | a | b | c | Choices covered | Expected |
|---|---|---|---|---|---|
| F01 | 2 | -7 | 3 | disc > 0, a > 0 | real: 3, 0.5 |
| F02 | 1 | -3 | 2 | disc > 0 | real: 2, 1 |
| F03 | 2 | -1 | -1 | disc > 0, c < 0 | real: 1, -0.5 |
| F04 | 1 | -2 | 1 | disc = 0 | real: 1 |
| F05 | 4 | -4 | 1 | disc = 0, a ≠ 1 | real: 0.5 |
| F06 | 1 | 0 | -4 | b = 0, disc > 0 | real: 2, -2 (order checked) |
| F07 | 1 | 2 | 5 | disc < 0 | complex: -1 ± 2i |
| F08 | 1 | 0 | 1 | b = 0, disc < 0 | complex: ±i |
| F09 | -1 | 2 | -5 | a < 0, disc < 0 | complex: 1 ± 2i |
| F10 | 1 | 1e8 | 1 | large b | real: -1e8, -1e-8 |
| F11 | 1 | 0 | -1e20 | large roots | real: ±1e10 |
| F12 | 1 | 2 | 0 | c = 0 | real: 0, -2 |
| F13 | 1e-9 | 0 | 1e-9 | tiny a, tiny disc | complex: ±i |
| F14 | 1 | 1e10 | 1 | disc equals b² | **[error]** abort |
| F15 | 1e200 | 1e200 | 1e200 | overflow (NaN) | **[error]** abort |
| F16 | 0 | 1 | 1 | a = 0 | **[error]** abort |
| F17 | 0 | 0 | 0 | a = 0 | **[error]** abort |
| F18 | 2 | 4 | 10 | a ≠ 1, disc < 0 | complex: -1 ± 2i |

Notes:
- F12 expects real roots; the program aborts instead, which is defect D1.
- F18 was added after reading the survivors: all earlier complex-root frames used
  a = 1 or a = -1, which hid mutants on `2*a`.

---

## 2. Date converter: `convert("YYYY-MM-DD")` gives `DD/MM/YYYY`

### 2.1 Categories, choices and constraints

| Category | Choices (blocks) | Constraint / property |
|---|---|---|
| Year type | ordinary (2023) | none |
| Year type | leap (2000, 2024) | none |
| Year type | century non-leap (1900, 2100) | none |
| Year type | boundary (1970, 9999) | none |
| Month | valid | none |
| Month | 00 | **[error]** |
| Month | 13 | **[error]** |
| Day | valid | none |
| Day | month end (28 to 31) | none |
| Day | 00 | **[error]** |
| Day | 32 | **[error]** |
| Day | impossible for the month | **[error]**; only with a month and year that make it impossible: 29 Feb in a non-leap year, 30 Feb, 31 Apr |
| Format | YYYY-MM-DD | valid |
| Format | other separator (/) | **[error]** |
| Format | no zero padding | **[error]** |
| Format | two-digit year | **[error]** |
| Format | trailing text | **[error]** |
| Format | time suffix | **[error]** |
| Format | empty | **[error]** |
| Format | garbage text | **[error]** |
| Format | null | **[error]** |

Constraint for the whole model: an **[error]** choice appears in a frame only once,
with all other parts valid, so every failure has a single cause.

### 2.2 Test frames

| Frame | Input | Choices covered | Expected |
|---|---|---|---|
| F01 | 2023-12-25 | ordinary year | 25/12/2023 |
| F02 | 2024-02-29 | leap year, 29 Feb | 29/02/2024 |
| F03 | 2000-02-29 | century leap year | 29/02/2000 |
| F04 | 1900-02-28 | century non-leap, Feb | 28/02/1900 |
| F05 | 2024-12-31 | year end | 31/12/2024 |
| F06 | 2023-01-01 | year start | 01/01/2023 |
| F07 | 1970-01-01 | epoch boundary | 01/01/1970 |
| F08 | 9999-12-31 | maximum year | 31/12/9999 |
| F09 | 2023-04-30 | 30-day month end | 30/04/2023 |
| F10 | 2023-03-31 | 31-day month end | 31/03/2023 |
| F11 | 2023-02-28 | non-leap Feb end | 28/02/2023 |
| F12 | 2100-02-28 | century non-leap, Feb | 28/02/2100 |
| F13 | 2023-02-29 | impossible day (non-leap) | **[error]** INVALID |
| F14 | 1900-02-29 | impossible day (century) | **[error]** INVALID |
| F15 | 2100-02-29 | impossible day (century) | **[error]** INVALID |
| F16 | 2023-02-30 | impossible day | **[error]** INVALID |
| F17 | 2023-04-31 | impossible day | **[error]** INVALID |
| F18 | 2023-13-01 | month 13 | **[error]** INVALID |
| F19 | 2023-00-10 | month 00 | **[error]** INVALID |
| F20 | 2023-01-00 | day 00 | **[error]** INVALID |
| F21 | 2023-01-32 | day 32 | **[error]** INVALID |
| F22 | 2023/12/25 | other separator | **[error]** INVALID |
| F23 | (empty) | empty | **[error]** INVALID |
| F24 | abc | garbage | **[error]** INVALID |
| F25 | 2023-1-5 | no zero padding | **[error]** INVALID |
| F26 | 2023-12-25abc | trailing text | **[error]** INVALID |
| F27 | 23-12-25 | two-digit year | **[error]** INVALID |
| F28 | 2023-12-25T10:00 | time suffix | **[error]** INVALID |
| extra | null | null input | **[error]** INVALID |

INVALID means a correct program should reject the input. `DateHelper` has no way to
reject input, so what it returns instead is recorded as a defect (DT1 to DT4).
Every frame is also checked against a strict `java.time` reference.