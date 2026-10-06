package dates;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SmokeTest {
    @Test
    void basicConversion() {
        assertEquals("25/12/2023", DateHelper.convert("2023-12-25"));
    }
}