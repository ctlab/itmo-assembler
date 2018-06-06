package ru.ifmo.genetics.utils.tool;

import org.apache.commons.cli.Option;
import org.jetbrains.annotations.NotNull;
import ru.ifmo.genetics.utils.tool.parameters.MultiValuedParameter;
import ru.ifmo.genetics.utils.tool.parameters.MultiValuedParameterDescription;
import ru.ifmo.genetics.utils.tool.parameters.ParameterDescription;
import ru.ifmo.genetics.utils.tool.values.InOutValue;
import ru.ifmo.genetics.utils.tool.values.InValue;
import ru.ifmo.genetics.utils.tool.values.SimpleInValue;

public class Parameter<T> extends InOutValue<T> {
    public @NotNull ParameterDescription<T> description;
    InValue<T> internalValue = null;


    public Parameter(@NotNull ParameterDescription<T> description) {
        this.description = description;
    }


    public void replaceDescription(@NotNull ParameterDescription<T> newDescription) {
        description = newDescription;
    }


    public void set(T value) {
        set(new SimpleInValue<T>(value));
    }

    public void set(InValue<T> inValue) {
        this.internalValue = inValue;
    }

    @Override
    public T get() {
        determineIfNeeded();
        if (internalValue == null) {
            return null;
        }
        return internalValue.get();
    }

    public void determineIfNeeded() {
        if ((internalValue == null || internalValue.get() == null) && (description.defaultValue != null)) {
            internalValue = description.defaultValue;
        }
    }


    public Option getAsOption() {
        Option option = new Option(description.shortOpt, description.name, description.hasArg, description.description);
        if (description.tClass == Boolean.class) {
            option.setArgs(1);
            option.setOptionalArg(true);
        }
        return option;
    }

    @Override
    public String toString() {
        return "Parameter{" +
                "description=" + description +
                ", internalValue=" + internalValue +
                '}';
    }


    public static <T> Parameter<T> createParameter(ParameterDescription<T> description) {
        if (description instanceof MultiValuedParameterDescription) {
            return new MultiValuedParameter<T>(description);
        } else {
            return new Parameter<T>(description);
        }
    }
}
