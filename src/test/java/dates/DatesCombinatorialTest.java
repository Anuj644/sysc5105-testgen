package dates;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DatesCombinatorialTest {

    static final boolean STRICT = Boolean.getBoolean("strict");
    static final Set<String> FINDINGS = new TreeSet<>();
    static PrintStream oldErr;

    @BeforeAll
    static void silenceStackTraces() {
        oldErr = System.err; // DateHelper prints a stack trace on every failed parse
        System.setErr(new PrintStream(OutputStream.nullOutputStream()));
    }

    @AfterAll
    static void writeFindings() throws IOException {
        System.setErr(oldErr);
        Files.createDirectories(Paths.get("results"));
        Files.write(Paths.get("results/findings_dates_comb.txt"), FINDINGS);
    }

    static Stream<Arguments> pairwise() throws IOException {
        return load("testdata/dates_pairwise.tsv");
    }

    static Stream<Arguments> threeWay() throws IOException {
        return load("testdata/dates_3way.tsv");
    }

    static Stream<Arguments> load(String path) throws IOException {
        return Files.readAllLines(Paths.get(path)).stream()
                .skip(1)
                .filter(l -> !l.isBlank())
                .map(l -> l.trim().split("\\s+"))
                .map(p -> Arguments.of(p[0], p[1], p[2], p[3]));
    }

    @ParameterizedTest(name = "pairwise {0}{3}{1}{3}{2}")
    @MethodSource("pairwise")
    void pairwiseSuite(String y, String m, String d, String sep) {
        runCase(y, m, d, sep);
    }

    @ParameterizedTest(name = "3-way {0}{3}{1}{3}{2}")
    @MethodSource("threeWay")
    void threeWaySuite(String y, String m, String d, String sep) {
        runCase(y, m, d, sep);
    }

    static void known(String id, String msg) {
        FINDINGS.add(id + ": " + msg);
        if (STRICT) fail(id + ": " + msg);
    }

    /** Expected DD/MM/YYYY for a valid ISO date, or null if the input is not a valid date. */
    static String oracle(String input) {
        try {
            LocalDate dt = LocalDate.parse(input, DateTimeFormatter.ISO_LOCAL_DATE);
            return String.format("%02d/%02d/%04d", dt.getDayOfMonth(), dt.getMonthValue(), dt.getYear());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    static void runCase(String y, String m, String d, String sep) {
        String input = y + sep + m + sep + d;
        String actual = DateHelper.convert(input);
        String expected = oracle(input);

        if (expected != null) {
            assertEquals(expected, actual, "valid date converted wrongly: " + input);
            return;
        }
        String epoch = DateHelper.getDesiredFormat(DateHelper.DateFormats.S_DDMMYYYY, 0L);
        if (actual.equals(epoch)) {
            known("DT2", "unparsable input '" + input + "' silently becomes the epoch date " + actual);
        } else {
            known("DT1", "invalid date '" + input + "' silently converted to " + actual);
        }
    }
}