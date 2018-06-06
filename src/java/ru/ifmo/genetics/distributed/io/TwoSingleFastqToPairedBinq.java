package ru.ifmo.genetics.distributed.io;

import org.apache.hadoop.io.Text;
import ru.ifmo.genetics.distributed.io.writable.Int128WritableComparable;
import ru.ifmo.genetics.distributed.io.writable.PairWritable;
import ru.ifmo.genetics.distributed.io.writable.PairedDnaQWritable;
import ru.ifmo.genetics.io.formats.QualityFormat;
import ru.ifmo.genetics.io.formats.QualityFormatFactory;
import ru.ifmo.genetics.io.readers.PairedFastqIterator;
import ru.ifmo.genetics.utils.iterators.IterableIterator;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;

public class TwoSingleFastqToPairedBinq {
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        if  (args.length != 3) {
            System.err.println("Usage: convert <quality-format> <file1> <file2>");
            System.exit(1);
        }

        QualityFormat qf = QualityFormatFactory.instance.get(args[0].toLowerCase());
        String filename1 = args[1];
        String filename2 = args[2];
        
        String outputFilename;
        {
            int i = 0;
            while (i < filename1.length() && filename1.charAt(i) == filename2.charAt(i)) {
                i++;
            }

            while (i > 0 && (filename1.charAt(i - 1) == '_' || filename1.charAt(i - 1) == '.')) {
                i--;
            }
            outputFilename = filename1.substring(0, i) + ".2binq";
        }

        Iterator<PairWritable<Text, PairedDnaQWritable>> pairedIterator =
                new PairedFastqIterator(filename1, filename2, qf);
        SequencedWritableOutput<PairWritable<Int128WritableComparable, PairedDnaQWritable>> output =
                new SequencedWritableOutput<PairWritable<Int128WritableComparable, PairedDnaQWritable>>(outputFilename);

        PairWritable<Int128WritableComparable, PairedDnaQWritable> p =
                new PairWritable<Int128WritableComparable, PairedDnaQWritable>(null, null);

        MessageDigest md5 = MessageDigest.getInstance("MD5");

        Int128WritableComparable idWritable = new Int128WritableComparable();
        p.first = idWritable;


        for (PairWritable<Text, PairedDnaQWritable> idPdnaq: IterableIterator.makeIterable(pairedIterator)) {
            idPdnaq.second.updateDigest(md5);
            idWritable.set(md5.digest());
            p.second = idPdnaq.second;

            output.append(p);

        }
        output.close();
    }
}
