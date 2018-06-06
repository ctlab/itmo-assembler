package ru.ifmo.genetics.utils;

import org.junit.Test;

import java.io.File;
import static org.junit.Assert.*;

public class FileUtilsTest {
    @Test
    public void testBaseName() throws Exception {
        String baseName = "hello";
        String extension = "fastq";

        File f = new File("baseDir", baseName + "." + extension);
        assertEquals(baseName, FileUtils.baseName(f));
        assertEquals(baseName, FileUtils.baseName(new File(baseName)));

    }

    @Test
    public void testRemoveExtension() {
        assertEquals("hello", FileUtils.removeExtension("hello.fastq", "fasta", "fastq"));
        assertEquals("hello", FileUtils.removeExtension("hello.fastq", ".fasta", ".fastq"));
        assertEquals("hello.fastq", FileUtils.removeExtension("hello.fastq", ".binq", ".tar.gz"));
    }
}
