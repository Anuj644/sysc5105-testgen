package dates;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class DatesImprovementTest {

    static final boolean STRICT = Boolean.getBoolean("strict");
    static final Set<String> FINDINGS = new TreeSet<>();
    static PrintStream oldErr;
    static Locale savedLocale;

    @BeforeAll
    static void silenceStackTraces() {
        savedLocale = Locale.getDefault();
        oldErr = System.err;
        System.setErr(new PrintStream(OutputStream.nullOutputStream()));
    }

    @AfterEach
    void restoreLocale() {
        Locale.setDefault(savedLocale);
    }

    @AfterAll
    static void writeFindings() throws IOException {
        System.setErr(oldErr);
        Files.createDirectories(Paths.get("results"));
        Files.write(Paths.get("results/findings_dates_improvement.txt"), FINDINGS);
    }

    static void known(String id, String msg) {
        FINDINGS.add(id + ": " + msg);
        if (STRICT) fail(id + ": " + msg);
    }

    /** Runs the action with System.err redirected to a buffer and returns what was printed. */
    static String captureErr(Runnable action) {
        PrintStream current = System.err;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(buf);
        System.setErr(ps);
        try {
            action.run();
        } finally {
            ps.flush();
            System.setErr(current);
        }
        return buf.toString();
    }

    static int count(String text, String part) {
        int n = 0;
        for (int i = text.indexOf(part); i >= 0; i = text.indexOf(part, i + 1)) n++;
        return n;
    }

    @Test
    void getDateOnlyRoundTrip() {
        long t = DateHelper.getDateOnly("25/12/2023");
        assertEquals("25/12/2023", DateHelper.getDateOnly(t));
        assertEquals(t, DateHelper.parseDate("2023-12-25", DateHelper.DateFormats.D_YYYYMMDD));
    }

    @Test
    void getDateOnlyFailurePrintsStackTraceAndReturnsZero() {
        long[] r = {-1};
        String err = captureErr(() -> r[0] = DateHelper.getDateOnly("abc"));
        assertEquals(0L, r[0]);
        assertTrue(err.contains("Unparseable date"), "expected a printed stack trace, got: " + err);
    }

    @Test
    void parseDateFailurePrintsStackTraceAndReturnsZero() {
        long[] r = {-1};
        String err = captureErr(() -> r[0] = DateHelper.parseDate("abc", DateHelper.DateFormats.D_YYYYMMDD));
        assertEquals(0L, r[0]);
        assertTrue(err.contains("Unparseable date"), "expected a printed stack trace, got: " + err);
    }

    @Test
    void getDesiredFormatNowMatchesToday() {
        Locale.setDefault(Locale.ENGLISH);
        LocalDate before = LocalDate.now();
        String out = DateHelper.getDesiredFormat(DateHelper.DateFormats.D_YYYYMMDD);
        LocalDate after = LocalDate.now();
        assertTrue(out.equals(before.toString()) || out.equals(after.toString()), "got " + out);
    }

    @Test
    void parseAnyDateUnparsableReturnsZero() {
        long[] r = {-1};
        captureErr(() -> r[0] = DateHelper.parseAnyDate("hello"));
        assertEquals(0L, r[0]);
    }

    static void checkParseAny(String input, DateHelper.DateFormats fmt) {
        long[] r = {0};
        String err = captureErr(() -> r[0] = DateHelper.parseAnyDate(input));
        assertNotEquals(0L, r[0], "a parseable date must not return 0: " + input);
        assertTrue(err.contains("Unparseable date"), "failed formats are expected to print stack traces");
        String back = DateHelper.getDesiredFormat(fmt, r[0]);
        if (!back.equals(input)) {
            known("DT8", "parseAnyDate('" + input + "') returns the date " + back
                    + " (the last matching format wins)");
        }
        known("DT9", "parseAnyDate('" + input + "') prints " + count(err, "Unparseable date")
                + " stack traces although it succeeds");
    }

    @Test
    void parseAnyDateIsoString() {
        checkParseAny("2023-12-25", DateHelper.DateFormats.D_YYYYMMDD);
    }

    @Test
    void parseAnyDateSlashString() {
        checkParseAny("25/12/2023", DateHelper.DateFormats.S_DDMMYYYY);
    }
}