package ru.ifmo.genetics;

import ru.ifmo.genetics.dna.Dna;
import ru.ifmo.genetics.utils.tool.ExecutionFailedException;
import ru.ifmo.genetics.utils.tool.Parameter;
import ru.ifmo.genetics.utils.tool.Tool;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileParameterBuilder;
import ru.ifmo.genetics.utils.tool.values.InMemoryValue;
import ru.ifmo.genetics.utils.tool.values.InValue;

import java.io.File;
import java.util.ArrayList;

public class ToolTemplate extends Tool {
    public static final String NAME = "template-abc";
    public static final String DESCRIPTION = "calculates ...";


    // input parameters
    public final Parameter<File> readsFile = addParameter(new FileParameterBuilder("reads-file")
            .mandatory()
            .withShortOpt("f")
            .withDescription("file with all reads")
            .create());


    // internal variables
    private ArrayList<Dna> reads;
    private int contigsNumber = 0;


    // output parameters
    private final InMemoryValue<Integer> thresholdOutValue = new InMemoryValue<Integer>();
    public final InValue<Integer> thresholdOut = addOutput("threshold", thresholdOutValue, Integer.class);



    @Override
    protected void runImpl() throws ExecutionFailedException {
//        try {
            // loadReads();

            info("Calculating...");
            // calculate();
            thresholdOutValue.set(5);

            info("Statistic:\n" + toString());
//        } catch (IOException e) {
//            throw new ExecutionFailedException(e);
//        }
    }



    @Override
    protected void cleanImpl() {
        // type "var = null;" here for any internal variable except variables with primitive type
        reads = null;
    }

    public ToolTemplate() {
        super(NAME, DESCRIPTION);
    }

    /* leave this method if you want to run this class from Runner and IDE */
    public static void main(String[] args) {
        new ToolTemplate().mainImpl(args);
    }
}
