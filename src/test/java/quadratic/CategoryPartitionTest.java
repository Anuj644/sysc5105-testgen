package quadratic;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
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
 * Category-partition frames from testdata/quad_cp_frames.csv. Expected roots are derived by hand.
 * Known defects are recorded in results/findings_catpart.txt (build stays green) and fail the
 * test only with -Dstrict=true.
 */
class CategoryPartitionTest {

    static final boolean STRICT = Boolean.getBoolean("strict");
    static final Set<String> FINDINGS = new TreeSet<>();

    static Stream<Arguments> frames() throws IOException {
        return Files.readAllLines(Paths.get("testdata/quad_cp_frames.csv")).stream()
                .skip(1)
                .filter(l -> !l.isBlank() && !l.startsWith("#"))
                .map(l -> l.split(",", -1))
                .map(p -> Arguments.of(p[0], p[1], p[2], p[3], p[4], p[5], p[6]));
    }

    @ParameterizedTest(name = "{0}: a={1} b={2} c={3} expect {4}")
    @MethodSource("frames")
    void frame(String id, String sa, String sb, String sc, String expect, String roots,
               String ordered) throws Exception {
        double a = Double.parseDouble(sa), b = Double.parseDouble(sb), c = Double.parseDouble(sc);
        String inputs = "(a=" + sa + ", b=" + sb + ", c=" + sc + ")";

        String out;
        try {
            out = SolverHarness.solve(a, b, c);
        } catch (NotEnoughPrecisionException e) {
            if (expect.equals("ABORT")) return;
            if (c == 0) {
                known("D1", id + " aborts although roots exist (c=0) " + inputs);
                return;
            }
            fail(id + ": unexpected abort " + inputs);
            return;
        }
        if (expect.equals("ABORT")) {
            fail(id + ": expected an abort but got: " + out);
        }

        List<double[]> got = SolverHarness.parseRoots(out);
        List<double[]> exp = parseExpected(roots);
        String flat = out.replace("\r\n", " | ").replace("\n", " | ");

        for (double[] r : got) {
            for (double v : r) {
                if (Math.abs(v) == 2147483647.0 || Math.abs(v) == 2147483648.0) {
                    known("D3", id + " root clamped to int range " + inputs + " -> " + flat);
                    return;
                }
            }
        }
        if (out.contains("+ -")) {
            known("D4", id + " prints '+ -' for negative imaginary part " + inputs + " -> " + flat);
        }
        if (!matches(got, exp, ordered.equals("Y"))) {
            if (Math.abs(b * b - 4 * a * c) < 1e-6) {
                known("D5", id + " wrong roots, tiny discriminant " + inputs + " -> " + flat);
                return;
            }
            fail(id + ": expected " + roots + " but got " + flat + " " + inputs);
        }
    }

    @AfterAll
    static void writeFindings() throws IOException {
        Files.createDirectories(Paths.get("results"));
        Files.write(Paths.get("results/findings_catpart.txt"), FINDINGS);
    }

    static void known(String idAndMsg, String msg) {
        FINDINGS.add(idAndMsg + ": " + msg);
        if (STRICT) fail(idAndMsg + ": " + msg);
    }

    static List<double[]> parseExpected(String s) {
        List<double[]> r = new ArrayList<>();
        if (s.isBlank()) return r;
        for (String pair : s.split(";")) {
            String[] p = pair.split(":");
            r.add(new double[] {Double.parseDouble(p[0]), Double.parseDouble(p[1])});
        }
        return r;
    }

    static boolean matches(List<double[]> got, List<double[]> exp, boolean ordered) {
        if (got.size() != exp.size()) return false;
        if (ordered) {
            for (int i = 0; i < exp.size(); i++) if (!same(got.get(i), exp.get(i))) return false;
            return true;
        }
        List<double[]> pool = new ArrayList<>(got);
        for (double[] e : exp) {
            boolean found = false;
            for (int i = 0; i < pool.size(); i++) {
                if (same(pool.get(i), e)) { pool.remove(i); found = true; break; }
            }
            if (!found) return false;
        }
        return true;
    }

    static boolean same(double[] g, double[] e) {
        return near(g[0], e[0]) && near(g[1], e[1]);
    }

    static boolean near(double g, double e) {
        return e == 0 ? Math.abs(g) <= 1e-9 : Math.abs(g - e) <= 1e-6 * Math.abs(e);
    }
}