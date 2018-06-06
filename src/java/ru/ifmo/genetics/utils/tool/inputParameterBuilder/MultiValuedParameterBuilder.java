package ru.ifmo.genetics.utils.tool.inputParameterBuilder;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import ru.ifmo.genetics.utils.tool.Tool;
import ru.ifmo.genetics.utils.tool.parameters.MultiValuedParameterDescription;
import ru.ifmo.genetics.utils.tool.parameters.ParameterDescription;

public class MultiValuedParameterBuilder<E> extends ParameterBuilder<E[]> {
    Logger logger = Logger.getLogger(MultiValuedParameterBuilder.class);

    public MultiValuedParameterBuilder(@NotNull Class<E[]> tClass, @NotNull String name) {
        super(tClass, name);
    }

    public MultiValuedParameterBuilder(@NotNull MultiValuedParameterDescription<E[]> other) {
        super(other);
    }


    @Override
    public ParameterDescription<E[]> create() {
        if (description == null) {
            throw new IllegalArgumentException("description shouldn't be null");
        }
        assert check();

        important |= mandatory;

        return new MultiValuedParameterDescription<E[]>(tClass, name, shortOpt, defaultValue, defaultComment,
                description, descriptionShort, descriptionRu, descriptionRuShort, mandatory, important);
    }

    private boolean check() {
        if (defaultValue != null && mandatory) {
            Tool.warn(logger, "Parameter " + name + " is mandatory but has the default value " + defaultValue);
        }
        return true;
    }
}
