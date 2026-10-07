package quadratic;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * AI-assisted (Claude), reviewed by me.
 * Runs PICT-generated pairwise and 3-way suites. The oracle is mathematical (root residual,
 * exact discriminant sign), not a stored expected output.
 *
 * Known defects are recorded in results/findings_combinatorial.txt. In the default mode they do
 * not fail the build (so PIT can run); with -Dstrict=true they fail the test.
 */
class CombinatorialTest {

    static final boolean STRICT = Boolean.getBoolean("strict");
    static final double TOL = 1e-6;
    static final Set<String> FINDINGS = new TreeSet<>();

    static Stream<Arguments> pairwise() throws IOException {
        return load("testdata/quad_pairwise.tsv");
    }

    static Stream<Arguments> threeWay() throws IOException {
        return load("testdata/quad_3way.tsv");
    }

    static Stream<Arguments> load(String path) throws IOException {
        return Files.readAllLines(Paths.get(path)).stream()
                .skip(1) // header
                .filter(l -> !l.isBlank())
                .map(l -> l.trim().split("\\s+"))
                .map(p -> Arguments.of(p[0], p[1], p[2]));
    }

    @ParameterizedTest(name = "pairwise a={0} b={1} c={2}")
    @MethodSource("pairwise")
    void pairwiseSuite(String a, String b, String c) throws Exception {
        runCase(a, b, c);
    }

    @ParameterizedTest(name = "3-way a={0} b={1} c={2}")
    @MethodSource("threeWay")
    void threeWaySuite(String a, String b, String c) throws Exception {
        runCase(a, b, c);
    }

    @AfterAll
    static void writeFindings() throws IOException {
        Files.createDirectories(Paths.get("results"));
        Files.write(Paths.get("results/findings_combinatorial.txt"), FINDINGS);
    }

    /** Records a known defect; fails the test only in strict mode. */
    static void known(String id, String msg) {
        FINDINGS.add(id + ": " + msg);
        if (STRICT) fail(id + ": " + msg);
    }

    static void runCase(String sa, String sb, String sc) throws Exception {
        double a = Double.parseDouble(sa);
        double b = Double.parseDouble(sb);
        double c = Double.parseDouble(sc);
        String inputs = "(a=" + sa + ", b=" + sb + ", c=" + sc + ")";

        // D2 (predicted): 4ac overflows to infinity while b*b does not, so sqrtByNewton(infinity)
        // never terminates. Not executed here to avoid hanging the build; verify manually.
        if (Double.isInfinite(4 * a * c) && !Double.isInfinite(b * b)) {
            known("D2", "predicted infinite loop, 4ac overflows " + inputs);
            return;
        }

        String out;
        try {
            out = SolverHarness.solve(a, b, c);
        } catch (NotEnoughPrecisionException e) {
            checkAbort(a, b, c, inputs);
            return;
        }

        assertTrue(a != 0, "a=0 must not produce roots " + inputs + " -> " + out);

        List<double[]> roots = SolverHarness.parseRoots(out);
        assertTrue(roots.size() == 1 || roots.size() == 2, "unexpected output " + inputs + " -> " + out);

        // D3: formatDouble casts integral values to int, so |root| >= 2^31 is clamped.
        for (double[] r : roots) {
            for (double v : r) {
                if (Math.abs(v) == 2147483647.0 || Math.abs(v) == 2147483648.0) {
                    known("D3", "root clamped to int range " + inputs + " -> " + out);
                    return;
                }
            }
        }

        boolean complex = out.endsWith("i");
        if (SolverHarness.exactDiscriminantSign(a, b, c) < 0) {
            assertTrue(complex, "expected complex roots " + inputs + " -> " + out);
        } else {
            assertFalse(complex, "expected real roots " + inputs + " -> " + out);
        }

        // D4: cosmetic, prints "1 + -2i" when the imaginary part is negative.
        if (out.contains("+ -")) {
            known("D4", "prints '+ -' for negative imaginary part " + inputs + " -> " + out);
        }

        double absDisc = Math.abs(b * b - 4 * a * c);
        for (double[] r : roots) {
            double ratio = SolverHarness.residualRatio(a, b, c, r);
            if (ratio > TOL && absDisc < 1e-6) {
                // D5: sqrtByNewton stops at an absolute step of 1e-8, so sqrt(|disc|) below
                // about 1e-3 comes out inaccurate.
                known("D5", "wrong roots when |discriminant| is tiny " + inputs + " -> "
                        + out.replace("\r\n", " | ").replace("\n", " | "));
                return;
            }
            assertTrue(ratio <= TOL, "root (" + r[0] + ", " + r[1] + ") residual " + ratio
                    + " " + inputs + " -> " + out);
        }
    }

    static void checkAbort(double a, double b, double c, String inputs) {
        if (a == 0) return; // spec: a must be non-zero (message is misleading but abort is acceptable)
        double disc = b * b - 4 * a * c;
        if (Double.isNaN(disc)) return; // overflow, abort by design
        if (c == 0) {
            // D1: roots 0 and -b/a exist and are representable, but the solver aborts.
            known("D1", "aborts although roots exist (c=0) " + inputs);
            return;
        }
        assertEquals(b * b, disc, "abort without precision loss " + inputs);
    }
}