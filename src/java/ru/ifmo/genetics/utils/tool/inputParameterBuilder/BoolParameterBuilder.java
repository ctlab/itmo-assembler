package ru.ifmo.genetics.utils.tool.inputParameterBuilder;

import org.jetbrains.annotations.NotNull;
import ru.ifmo.genetics.utils.tool.parameters.ParameterDescription;

public class BoolParameterBuilder extends ParameterBuilder<Boolean> {

    public BoolParameterBuilder(@NotNull String name) {
        super(Boolean.class, name);
        withDefaultValue(false);
    }

    @Override
    public ParameterDescription<Boolean> create() {
        if (defaultComment == null) {
            withDefaultComment("");
        }
        return super.create();
    }
}
