package ru.ifmo.genetics.tools.microassembly.readers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordReader;
import org.apache.hadoop.util.LineReader;
import ru.ifmo.genetics.io.formats.QualityFormat;
import ru.ifmo.genetics.io.formats.QualityFormatFactory;
import ru.ifmo.genetics.io.readers.FastqRecordReader;
import ru.ifmo.genetics.tools.microassembly.types.BowtieAlignmentWritable;

import java.io.*;

public class BowtieMapRecordReader implements RecordReader<LongWritable, BowtieAlignmentWritable> {
    private Log log = LogFactory.getLog(BowtieMapRecordReader.class);
    InputStream inputStream;
    LineReader lr;

    long position;
    long start;
    long end;
    
    Text line = new Text();
    private QualityFormat qf;

    public BowtieMapRecordReader(String filename, QualityFormat qf) throws FileNotFoundException {
        this(new BufferedInputStream(new FileInputStream(filename)), qf);
    }
    
    protected BowtieMapRecordReader(InputStream in, QualityFormat qf) {
        position = start = 0;
        end = Long.MAX_VALUE;
        inputStream = in;
        lr = new LineReader(inputStream);
        this.qf = qf;
    }

    public BowtieMapRecordReader(JobConf job, FileSplit split) throws IOException {
        qf = QualityFormatFactory.instance.get(job.get(FastqRecordReader.QUALITY_FORMAT));
        start = split.getStart();
        end = start + split.getLength();
        Path file = split.getPath();
        FileSystem fs = file.getFileSystem(job);
        FSDataInputStream fsIn = fs.open(file);

        fsIn.seek(start);
        position = start;

        inputStream = fsIn;
        lr = new LineReader(inputStream);

        if (start != 0) {
            position += lr.readLine(line);
        }
    }
    
    

    @Override
    public boolean next(LongWritable key, BowtieAlignmentWritable value) throws IOException {
        // if equals - also trying to read
        if (position > end) {
            return false;
        }

        try {
            key.set(position);
            int read = lr.readLine(line);
            if (read == 0) {
                return false;
            }
            position += read;

            value.parseFromLine(line, qf);

        } catch (EOFException e) {
            log.warn("EOFException caught instead of returning zero read bytes");
            return false;
        }

        return true;
    }

    @Override
    public LongWritable createKey() {
        return new LongWritable();
    }

    @Override
    public BowtieAlignmentWritable createValue() {
        return new BowtieAlignmentWritable();
    }

    @Override
    public long getPos() throws IOException {
        return position;
    }

    @Override
    public void close() throws IOException {
        lr.close();
    }

    @Override
    public float getProgress() throws IOException {
        if (start == end) {
            return 0.0f;
        } else {
            return Math.min(1.0f, (getPos() - start) / (float) (end - start));
        }
    }
}
