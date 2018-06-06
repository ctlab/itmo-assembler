package ru.ifmo.genetics.utils.tool.parameters;

import org.apache.commons.cli.Option;
import org.jetbrains.annotations.NotNull;
import ru.ifmo.genetics.utils.tool.Parameter;

public class MultiValuedParameter<T> extends Parameter<T> {

    public MultiValuedParameter(@NotNull ParameterDescription<T> description) {
        super(description);
    }

    public Option getAsOption() {
        Option option = new Option(description.shortOpt, description.name, description.hasArg, description.description);
        option.setArgs(Option.UNLIMITED_VALUES);
        option.setOptionalArg(true);
        return option;
    }
}
