package ru.ifmo.genetics.utils;

import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.*;

import static ru.ifmo.genetics.utils.NumUtils.*;

public class NumUtilsTest {
    @Test
    public void testMakeHumanReadable() throws Exception {
        Locale.setDefault(Locale.US);

        assertEquals("0", makeHumanReadable(0));
        assertEquals("1", makeHumanReadable(1));
        assertEquals("11", makeHumanReadable(11));
        assertEquals("111", makeHumanReadable(111));
        assertEquals("1.23 K", makeHumanReadable(1234));
        assertEquals("12.3 K", makeHumanReadable(12345));
        assertEquals("123 K", makeHumanReadable(123456));
        assertEquals("123 K", makeHumanReadable(122556));
    }
}
