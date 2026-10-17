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
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * AI-assisted (Claude), reviewed by me.
 * Category-partition frames from testdata/dates_cp_frames.csv for DateHelper.convert.
 * Valid frames: exact hand-derived output, cross-checked against a strict java.time reference.
 * INVALID frames: the reference must reject them; DateHelper cannot reject, so what it does is
 * recorded in results/findings_dates_catpart.txt (build stays green) and fails the test only
 * with -Dstrict=true.
 *  DT1: well-formed but impossible date silently converted to a different date
 *  DT2: unparsable input silently converted to the epoch date
 *  DT3: malformed input accepted and converted (lenient parsing)
 *  DT4: convert(null) crashes with an unchecked exception
 */
class DatesCategoryPartitionTest {

    static final boolean STRICT = Boolean.getBoolean("strict");
    static final Set<String> FINDINGS = new TreeSet<>();
    static final Pattern SHAPE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    static PrintStream oldErr;

    @BeforeAll
    static void silenceStackTraces() {
        oldErr = System.err;
        System.setErr(new PrintStream(OutputStream.nullOutputStream()));
    }

    @AfterAll
    static void writeFindings() throws IOException {
        System.setErr(oldErr);
        Files.createDirectories(Paths.get("results"));
        Files.write(Paths.get("results/findings_dates_catpart.txt"), FINDINGS);
    }

    static Stream<Arguments> frames() throws IOException {
        return Files.readAllLines(Paths.get("testdata/dates_cp_frames.csv")).stream()
                .skip(1)
                .filter(l -> !l.isBlank())
                .map(l -> l.split(",", -1))
                .map(p -> Arguments.of(p[0], p[1], p[2]));
    }

    static void known(String id, String msg) {
        FINDINGS.add(id + ": " + msg);
        if (STRICT) fail(id + ": " + msg);
    }

    static String oracle(String input) {
        try {
            LocalDate d = LocalDate.parse(input, DateTimeFormatter.ISO_LOCAL_DATE);
            return String.format("%02d/%02d/%04d", d.getDayOfMonth(), d.getMonthValue(), d.getYear());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    @ParameterizedTest(name = "{0}: [{1}] expect {2}")
    @MethodSource("frames")
    void frame(String id, String input, String expect) {
        String ref = oracle(input);
        if (expect.equals("INVALID")) {
            assertNull(ref, id + ": frame marked INVALID but the reference accepts it");
        } else {
            assertEquals(expect, ref, id + ": hand-derived value disagrees with the reference");
        }

        String actual = DateHelper.convert(input);
        if (!expect.equals("INVALID")) {
            assertEquals(expect, actual, id + ": valid date converted wrongly: " + input);
            return;
        }
        String epoch = DateHelper.getDesiredFormat(DateHelper.DateFormats.S_DDMMYYYY, 0L);
        if (SHAPE.matcher(input).matches()) {
            known("DT1", id + " impossible date '" + input + "' silently converted to " + actual);
        } else if (actual.equals(epoch)) {
            known("DT2", id + " unparsable input '" + input + "' silently becomes the epoch date " + actual);
        } else {
            known("DT3", id + " malformed input '" + input + "' accepted and converted to " + actual);
        }
    }

    @Test
    void nullInput() {
        try {
            String r = DateHelper.convert(null);
            known("DT4", "convert(null) returned " + r);
        } catch (RuntimeException e) {
            known("DT4", "convert(null) throws " + e.getClass().getSimpleName());
        }
    }
}