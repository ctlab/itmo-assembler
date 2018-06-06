package ru.ifmo.genetics.utils.tool.parameters;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.ifmo.genetics.utils.tool.values.InValue;

public class MultiValuedParameterDescription<T> extends ParameterDescription<T> {

    public MultiValuedParameterDescription(@NotNull Class elementClass, @NotNull String name, @Nullable String shortOpt,
                                           @Nullable InValue<T> defaultValue, @Nullable String defaultComment,
                                           @NotNull String description, String descriptionShort,
                                           String descriptionRu, String descriptionRuShort,
                                           boolean mandatory, boolean important) {
        super(elementClass, name, shortOpt, true, defaultValue, defaultComment,
                description, descriptionShort, descriptionRu, descriptionRuShort, mandatory, important, false);
    }


    @Override
    public String toString() {
        return "MultiValuedParameterDescription{" +
                "elementClass=" + tClass +
                ", name='" + name + '\'' +
                ", shortOpt='" + shortOpt + '\'' +
                ", defaultValue=" + defaultValue +
                ", description='" + description + '\'' +
                ", mandatory='" + mandatory + '\'' +
                '}';
    }
}
