package quadratic;

import org.junit.jupiter.api.Test;
import java.io.*;
import static org.junit.jupiter.api.Assertions.*;

class SmokeTest {
    static String solve(double a, double b, double c) throws Exception {
        PrintStream old = System.out;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buf));
        try { Quadratic.solveQuadratic(a, b, c); }
        finally { System.setOut(old); }
        return buf.toString().trim();
    }

    @Test
    void simpleRealRoots() throws Exception {
        String out = solve(1, -3, 2);
        assertTrue(out.contains("x1 = 2") && out.contains("x2 = 1"), out);
    }
}