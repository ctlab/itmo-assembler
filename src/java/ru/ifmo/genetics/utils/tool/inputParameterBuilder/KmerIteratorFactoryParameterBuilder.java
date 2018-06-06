package ru.ifmo.genetics.utils.tool.inputParameterBuilder;

import org.jetbrains.annotations.NotNull;
import ru.ifmo.genetics.dna.kmers.KmerIteratorFactory;

public class KmerIteratorFactoryParameterBuilder extends ParameterBuilder<KmerIteratorFactory> {

    public KmerIteratorFactoryParameterBuilder(@NotNull String name) {
        super(KmerIteratorFactory.class, name);
    }
}
