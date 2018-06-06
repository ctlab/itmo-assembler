package ru.ifmo.genetics.distributed;

import org.apache.hadoop.fs.Path;
import ru.ifmo.genetics.distributed.contigsAssembly.ContigsAssembly;
import ru.ifmo.genetics.distributed.quasicontigsAssembly.QuasicontigsAssembly;
import ru.ifmo.genetics.distributed.errorsCorrection.ErrorsCorrection;

import java.io.IOException;

public class Assembly {
    public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
        Path source = new Path(args[0]);
        Path workdir = new Path(args[1]);
        Path fixedReads = ErrorsCorrection.makeErrorCorrection(source, new Path(workdir, "errorsCorrection"), 19, 3);
        Path quasiContigs = QuasicontigsAssembly.makeClusterizationAndReadsFilling(fixedReads, new Path(workdir, "clusterizationReads"));
        for (int i = 0; i < 4; i++) {
            quasiContigs = ContigsAssembly.makeContigs(quasiContigs, new Path(workdir, "contigs" + i));
        }
    }
}
