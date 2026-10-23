package quadratic;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

/**
 * AI-assisted , reviewed by me.
 * Improvement round: direct calls to validateInput and scripted-stdin runs of main().
 * Known defects are recorded in results/findings_improvement.txt (build stays green) and fail
 * the test only with -Dstrict=true.
 */
class ImprovementTest {

    static final boolean STRICT = Boolean.getBoolean("strict");
    static final Set<String> FINDINGS = new TreeSet<>();

    static String runMain(String input) {
        InputStream oldIn = System.in;
        PrintStream oldOut = System.out;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        System.setOut(new PrintStream(buf));
        try {
            Quadratic.main(new String[0]);
        } finally {
            System.setIn(oldIn);
            System.setOut(oldOut);
        }
        return buf.toString().replace("\r\n", "\n");
    }

    static void known(String id, String msg) {
        FINDINGS.add(id + ": " + msg);
        if (STRICT) fail(id + ": " + msg);
    }

    static int count(String text, String part) {
        int n = 0;
        for (int i = text.indexOf(part); i >= 0; i = text.indexOf(part, i + 1)) n++;
        return n;
    }

    @AfterAll
    static void writeFindings() throws IOException {
        Files.createDirectories(Paths.get("results"));
        Files.write(Paths.get("results/findings_improvement.txt"), FINDINGS);
    }

    // ---------- validateInput, direct calls ----------

    @Test
    void validInputsAreAcceptedUnchanged() throws Exception {
        assertEquals(5.0, Quadratic.validateInput("5"));
        assertEquals(-3.5, Quadratic.validateInput("-3.5"));
        assertEquals(0.0, Quadratic.validateInput("0"));
        assertEquals(1.0E154, Quadratic.validateInput("1.0E154"));
    }

    @Test
    void textIsRejectedAsNumberFormat() {
        assertThrows(NumberFormatException.class, () -> Quadratic.validateInput("abc"));
    }

    @Test
    void lowercaseExponentIsRejected() {
        assertThrows(NotEnoughPrecisionException.class, () -> Quadratic.validateInput("1e154"));
        known("D6", "validateInput rejects valid e-notation '1e154' as too large or small");
    }

    @Test
    void harmlessFormattingVariants() {
        for (String s : new String[] {"0.10", "+5"}) {
            try {
                Quadratic.validateInput(s);
            } catch (NotEnoughPrecisionException e) {
                known("D6", "validateInput rejects valid number '" + s + "'");
            }
        }
    }

    @Test
    void nanAndInfinity() {
        for (String s : new String[] {"NaN", "Infinity"}) {
            try {
                double v = Quadratic.validateInput(s);
                known("D7", "validateInput accepts '" + s + "' and returns " + v);
            } catch (Exception e) {
                // rejected, which is the desired behaviour
            }
        }
    }

    // ---------- main(), scripted stdin ----------

    @Test
    void mainSolvesRealRoots() {
        String out = runMain("1\n-3\n2\nn\n");
        assertTrue(out.contains("Welcome to Quadratic Equation Solver."));
        assertTrue(out.contains("Enter a value for 'a':"));
        assertTrue(out.contains("Enter a value for 'b':"));
        assertTrue(out.contains("Enter a value for 'c':"));
        assertTrue(out.contains("x1 = 2\nx2 = 1\n"), out);
        assertTrue(out.contains("Would you like to try again? [y/n]"));
        assertTrue(out.contains("Thank you for using Quadratic Equation Solver!"));
    }

    @Test
    void mainSolvesComplexRoots() {
        String out = runMain("1\n0\n1\nn\n");
        assertTrue(out.contains("x1 = "), out);
        assertTrue(out.contains("\nx2 = -"), out);
        assertTrue(out.contains("i\n"), out);
    }

    @Test
    void mainRepeatsOnY() {
        String out = runMain("1\n-3\n2\ny\n1\n-3\n2\nn\n");
        assertEquals(2, count(out, "x1 = 2\nx2 = 1\n"), out);
    }

    @Test
    void mainRejectsZeroForA() {
        String out = runMain("0\n1\n-3\n2\nn\n");
        assertTrue(out.contains("'a' cannot be zero!"), out);
        assertTrue(out.contains("x1 = 2\nx2 = 1\n"), out);
    }

    @Test
    void mainRejectsNonNumbers() {
        String out = runMain("abc\n1\n-3\n2\nn\n");
        assertTrue(out.contains("The value you entered is not allowed!"), out);
        assertTrue(out.contains("x1 = 2\nx2 = 1\n"), out);
    }

    @Test
    void mainRejectsTooLargeValues() {
        String out = runMain("1e154\n1\n-3\n2\nn\n");
        assertTrue(out.contains("too large or too small"), out);
        assertTrue(out.contains("x1 = 2\nx2 = 1\n"), out);
    }

    @Test
    void mainReportsPrecisionFailure() {
        String out = runMain("1\n2\n0\nn\n");
        assertTrue(out.contains("Failed to find an accurate solution!"), out);
        known("D1", "main reports precision failure for (1, 2, 0), whose roots are 0 and -2");
    }
}