package quadratic;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * AI-assisted (Claude), reviewed by me.
 * Metamorphic tests: no expected output is stored. Each seed is solved, a follow-up equation is
 * built by a metamorphic relation, and the follow-up roots must match the transformed seed roots.
 * Violations in the tiny-discriminant region are recorded as D5 (known cause); any other violation
 * fails the test. Findings go to results/findings_metamorphic.txt; -Dstrict=true fails on them.
 */
class MetamorphicTest {

    static final boolean STRICT = Boolean.getBoolean("strict");
    static final Set<String> FINDINGS = new TreeSet<>();
    static final double[] A = {-4, -1, 0.5, 1, 2, 3};
    static final double[] B = {-5, -2, 0, 1, 3, 4};
    static final double[] C = {-4, -1, 0.5, 1, 2, 3};

    static Stream<Arguments> seeds() {
        List<Arguments> l = new ArrayList<>();
        for (double a : A) for (double b : B) for (double c : C) l.add(Arguments.of(a, b, c));
        return l.stream();
    }

    @ParameterizedTest(name = "MR1 scale all, seed ({0}, {1}, {2})")
    @MethodSource("seeds")
    void mr1ScaleAllCoefficients(double a, double b, double c) throws Exception {
        for (double k : new double[] {2, 0.5, -1, 4, 1e9, 1e-9}) {
            check("MR1(k=" + k + ")", a, b, c, k * a, k * b, k * c, z -> z);
        }
    }

    @ParameterizedTest(name = "MR2 negate b, seed ({0}, {1}, {2})")
    @MethodSource("seeds")
    void mr2NegateB(double a, double b, double c) throws Exception {
        check("MR2", a, b, c, a, -b, c, z -> new double[] {-z[0], -z[1]});
    }

    @ParameterizedTest(name = "MR3 reverse coefficients, seed ({0}, {1}, {2})")
    @MethodSource("seeds")
    void mr3ReverseCoefficients(double a, double b, double c) throws Exception {
        check("MR3", a, b, c, c, b, a, MetamorphicTest::reciprocal);
    }

    @ParameterizedTest(name = "MR4 scale roots, seed ({0}, {1}, {2})")
    @MethodSource("seeds")
    void mr4ScaleRoots(double a, double b, double c) throws Exception {
        for (double k : new double[] {2, 0.5, 4, -1}) {
            check("MR4(k=" + k + ")", a, b, c, a, k * b, k * k * c,
                    z -> new double[] {k * z[0], k * z[1]});
        }
    }

    @AfterAll
    static void writeFindings() throws IOException {
        Files.createDirectories(Paths.get("results"));
        Files.write(Paths.get("results/findings_metamorphic.txt"), FINDINGS);
    }

    static void known(String id, String msg) {
        FINDINGS.add(id + ": " + msg);
        if (STRICT) fail(id + ": " + msg);
    }

    static void check(String mr, double a, double b, double c,
                      double a2, double b2, double c2, UnaryOperator<double[]> tx) throws Exception {
        String in = "seed(" + a + ", " + b + ", " + c + ") -> follow-up(" + a2 + ", " + b2 + ", " + c2 + ")";
        List<double[]> src = rootsOf(a, b, c); // an abort on a seed is a real failure
        List<double[]> exp = new ArrayList<>();
        for (double[] z : src) exp.add(tx.apply(z));

        double disc2 = b2 * b2 - 4 * a2 * c2;
        boolean tiny = disc2 != 0 && Math.abs(disc2) < 1e-6;

        List<double[]> got;
        try {
            got = rootsOf(a2, b2, c2);
        } catch (NotEnoughPrecisionException e) {
            if (tiny) { known("D5", mr + " follow-up aborted, tiny discriminant " + in); return; }
            fail(mr + ": follow-up aborted " + in);
            return;
        }
        if (!sameSet(got, exp)) {
            if (tiny) { known("D5", mr + " roots changed, tiny discriminant " + in); return; }
            fail(mr + " violated " + in + " expected " + show(exp) + " got " + show(got));
        }
    }

    static List<double[]> rootsOf(double a, double b, double c) throws NotEnoughPrecisionException {
        List<double[]> r = SolverHarness.parseRoots(SolverHarness.solve(a, b, c));
        if (r.size() == 1) r.add(r.get(0).clone()); // a single printed root is a double root
        assertEquals(2, r.size(), "unparsable output for (" + a + ", " + b + ", " + c + ")");
        return r;
    }

    static double[] reciprocal(double[] z) {
        double d = z[0] * z[0] + z[1] * z[1];
        return new double[] {z[0] / d, -z[1] / d};
    }

    static boolean sameSet(List<double[]> got, List<double[]> exp) {
        if (got.size() != exp.size()) return false;
        List<double[]> pool = new ArrayList<>(got);
        for (double[] e : exp) {
            boolean found = false;
            for (int i = 0; i < pool.size(); i++) {
                if (near(pool.get(i)[0], e[0]) && near(pool.get(i)[1], e[1])) {
                    pool.remove(i);
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }

    static boolean near(double g, double e) {
        return Math.abs(g - e) <= 1e-6 * Math.max(1, Math.abs(e));
    }

    static String show(List<double[]> roots) {
        StringBuilder sb = new StringBuilder();
        for (double[] r : roots) sb.append("(").append(r[0]).append(", ").append(r[1]).append(") ");
        return sb.toString().trim();
    }
}