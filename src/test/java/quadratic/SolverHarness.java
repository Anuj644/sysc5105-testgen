package quadratic;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** AI-assisted (Claude), reviewed by me. Shared helpers for all quadratic test suites. */
final class SolverHarness {

    private SolverHarness() {}

    /** Calls the solver and returns what it printed. */
    static String solve(double a, double b, double c) throws NotEnoughPrecisionException {
        PrintStream old = System.out;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buf));
        try {
            Quadratic.solveQuadratic(a, b, c);
        } finally {
            System.setOut(old);
        }
        return buf.toString().trim();
    }

    /** Parses printed roots into {real, imaginary} pairs. */
    static List<double[]> parseRoots(String out) {
        List<double[]> roots = new ArrayList<>();
        for (String line : out.split("\\R")) {
            line = line.trim();
            int eq = line.indexOf('=');
            if (!line.startsWith("x") || eq < 0) continue;
            roots.add(parseValue(line.substring(eq + 1).trim()));
        }
        return roots;
    }

    private static double[] parseValue(String v) {
        if (!v.endsWith("i")) return new double[] {Double.parseDouble(v), 0};
        String body = v.substring(0, v.length() - 1);
        double re = 0;
        String im;
        if (body.contains(" + ")) {
            String[] p = body.split(" \\+ ", 2);
            re = Double.parseDouble(p[0]);
            im = p[1];
        } else if (body.contains(" - ")) {
            String[] p = body.split(" - ", 2);
            re = Double.parseDouble(p[0]);
            im = "-" + p[1];
        } else {
            im = body;
        }
        return new double[] {re, parseImag(im)};
    }

    private static double parseImag(String s) {
        if (s.isEmpty()) return 1;
        if (s.equals("-")) return -1;
        if (s.startsWith("--")) return Double.parseDouble(s.substring(2));
        return Double.parseDouble(s);
    }

    /** |a z^2 + b z + c| relative to the size of its terms; ~0 means z is a root. */
    static double residualRatio(double a, double b, double c, double[] z) {
        double re = z[0], im = z[1];
        double zr = re * re - im * im, zi = 2 * re * im;
        double pr = a * zr + b * re + c;
        double pi = a * zi + b * im;
        double res = Math.hypot(pr, pi);
        double scale = Math.abs(a) * Math.hypot(zr, zi)
                + Math.abs(b) * Math.hypot(re, im) + Math.abs(c);
        return scale == 0 ? res : res / scale;
    }

    /** Sign of b^2 - 4ac computed exactly (no rounding). */
    static int exactDiscriminantSign(double a, double b, double c) {
        BigDecimal A = new BigDecimal(a), B = new BigDecimal(b), C = new BigDecimal(c);
        return B.multiply(B).subtract(A.multiply(C).multiply(BigDecimal.valueOf(4))).signum();
    }
}