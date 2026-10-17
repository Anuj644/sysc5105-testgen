package dates;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;


class DatesMetamorphicTest {

    static final boolean STRICT = Boolean.getBoolean("strict");
    static final Set<String> FINDINGS = new TreeSet<>();
    static final DateTimeFormatter DD_MM_YYYY = DateTimeFormatter.ofPattern("dd/MM/uuuu");
    static final String[] ZONES = {"UTC", "America/Toronto", "Asia/Tokyo", "Pacific/Kiritimati",
            "Pacific/Pago_Pago", "Australia/Lord_Howe", "Europe/London"};
    static final String[] LOCALE_TAGS = {"en-US", "fr-FR", "de-DE", "ja-JP", "ar-EG", "th-TH",
            "hi-IN", "ja-JP-u-ca-japanese", "th-TH-u-nu-thai"};

    static PrintStream oldErr;
    static Locale savedLocale;
    static TimeZone savedZone;

    @BeforeAll
    static void setUp() {
        savedLocale = Locale.getDefault();
        savedZone = TimeZone.getDefault();
        oldErr = System.err;
        System.setErr(new PrintStream(OutputStream.nullOutputStream()));
    }

    @AfterEach
    void restoreDefaults() {
        Locale.setDefault(savedLocale);
        TimeZone.setDefault(savedZone);
    }

    @AfterAll
    static void writeFindings() throws IOException {
        System.setErr(oldErr);
        Files.createDirectories(Paths.get("results"));
        Files.write(Paths.get("results/findings_dates_meta.txt"), FINDINGS);
    }

    static void known(String id, String msg) {
        FINDINGS.add(id + ": " + msg);
        if (STRICT) fail(id + ": " + msg);
    }

    static List<LocalDate> seeds() {
        int[] years = {1900, 1970, 2000, 2023, 2024, 2100};
        TreeSet<LocalDate> set = new TreeSet<>();
        for (int y : years) {
            for (int m = 1; m <= 12; m++) {
                LocalDate first = LocalDate.of(y, m, 1);
                set.add(first);
                set.add(first.withDayOfMonth(15));
                set.add(first.withDayOfMonth(28));
                set.add(first.withDayOfMonth(first.lengthOfMonth()));
            }
        }
        return new ArrayList<>(set);
    }

    static Stream<LocalDate> seedStream() {
        return seeds().stream();
    }

    static String expected(LocalDate d) {
        return String.format(Locale.ROOT, "%02d/%02d/%04d", d.getDayOfMonth(), d.getMonthValue(), d.getYear());
    }

    @ParameterizedTest(name = "MR1 round trip {0}")
    @MethodSource("seedStream")
    void mr1RoundTrip(LocalDate seed) {
        String out = DateHelper.convert(seed.toString());
        long t = DateHelper.getDateOnly(out);
        String back = DateHelper.getDesiredFormat(DateHelper.DateFormats.D_YYYYMMDD, t);
        assertEquals(seed.toString(), back, "round trip changed the date: " + seed + " -> " + out);
    }

    @ParameterizedTest(name = "MR2 day shift {0}")
    @MethodSource("seedStream")
    void mr2DayShift(LocalDate seed) {
        LocalDate a = LocalDate.parse(DateHelper.convert(seed.toString()), DD_MM_YYYY);
        for (long n : new long[] {1, 7, 31, 365}) {
            LocalDate shifted = seed.plusDays(n);
            LocalDate b = LocalDate.parse(DateHelper.convert(shifted.toString()), DD_MM_YYYY);
            assertEquals(n, ChronoUnit.DAYS.between(a, b), "day difference changed for " + seed + " + " + n);
        }
    }

    @ParameterizedTest(name = "MR5 two code paths {0}")
    @MethodSource("seedStream")
    void mr5CrossPath(LocalDate seed) {
        String viaConvert = DateHelper.convert(seed.toString());
        String viaOther = DateHelper.getDateOnly(
                DateHelper.parseDate(seed.toString(), DateHelper.DateFormats.D_YYYYMMDD));
        assertEquals(viaConvert, viaOther, "two code paths disagree for " + seed);
    }

    @Test
    void mr3OrderPreserved() {
        String prev = null;
        LocalDate prevSeed = null;
        for (LocalDate s : seeds()) {
            String out = DateHelper.convert(s.toString());
            String key = out.substring(6, 10) + out.substring(3, 5) + out.substring(0, 2);
            if (prev != null) {
                assertTrue(prev.compareTo(key) < 0, "order violated: " + prevSeed + " vs " + s);
            }
            prev = key;
            prevSeed = s;
        }
    }

    @Test
    void mr4TimeZoneValidDates() {
        for (String id : ZONES) {
            TimeZone.setDefault(TimeZone.getTimeZone(id));
            for (LocalDate s : seeds()) {
                String actual = DateHelper.convert(s.toString());
                if (!expected(s).equals(actual)) {
                    known("DT6", "time zone " + id + " changes the output for valid date " + s + ": " + actual);
                    break;
                }
            }
        }
    }

    @Test
    void mr4TimeZoneInvalidInput() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        String base = DateHelper.convert("abc");
        for (String id : ZONES) {
            TimeZone.setDefault(TimeZone.getTimeZone(id));
            String actual = DateHelper.convert("abc");
            if (!base.equals(actual)) {
                known("DT5", "unparsable input 'abc' gives " + actual + " in " + id + " but " + base + " in UTC");
            }
        }
    }

    @Test
    void mr4LocaleValidDates() {
        for (String tag : LOCALE_TAGS) {
            Locale.setDefault(Locale.forLanguageTag(tag));
            for (LocalDate s : seeds()) {
                String actual = DateHelper.convert(s.toString());
                if (!expected(s).equals(actual)) {
                    known("DT7", "default locale " + tag + " changes the output for valid date " + s + ": " + actual);
                    break;
                }
            }
        }
    }
}