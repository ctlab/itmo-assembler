package ru.ifmo.genetics.utils;

import org.apache.hadoop.io.Text;
import org.junit.Test;
import static org.junit.Assert.*;

public class TextUtilsTest {
    Text line = new Text("asd123456fgh");

    @Test
    public void testParseInt() throws Exception {
        assertEquals(TextUtils.parseInt(line.getBytes(), 4, 8), 2345);
    }
    
    @Test(expected = NumberFormatException.class)
    public void testParseIntFails() {
        TextUtils.parseInt(line.getBytes(), 0, 3);
    }

    @Test
    public void testStartsWith() throws Exception {
        Text t = new Text("asdf");
        assertTrue(TextUtils.startsWith(t, "as"));
        assertTrue(TextUtils.startsWith(t, "a"));
        assertTrue(TextUtils.startsWith(t, ""));
        assertTrue(TextUtils.startsWith(new Text(""), ""));

        assertFalse(TextUtils.startsWith(t, "d"));
        assertFalse(TextUtils.startsWith(new Text(""), "d"));
    }
}
