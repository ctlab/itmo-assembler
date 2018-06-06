package ru.ifmo.genetics.utils.tool.parameters;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.ifmo.genetics.utils.tool.values.InValue;

public class ParameterDescription<T> {
    /**
     * Class T or element class E (if T is E[])
     */
    public final @NotNull Class tClass;
    public final @NotNull String name;
    public final @Nullable String shortOpt;
    public final boolean hasArg;
    public final @Nullable InValue<T> defaultValue;
    public final @Nullable String defaultComment;
    public final @NotNull String description;
    public final String descriptionShort, descriptionRu, descriptionRuShort;
    public final boolean mandatory;
    public final boolean important;
    public final boolean memoryParameter;



    public ParameterDescription(
            @NotNull Class tClass,
            @NotNull String name, @Nullable String shortOpt,
            boolean hasArg,
            @Nullable InValue<T> defaultValue, @Nullable String defaultComment,
            @NotNull String description, String descriptionShort, String descriptionRu, String descriptionRuShort,
            boolean mandatory, boolean important, boolean memoryParameter) {
        this.tClass = tClass;
        this.name = name;
        this.shortOpt = shortOpt;
        this.hasArg = hasArg;
        this.defaultValue = defaultValue;
        this.defaultComment = defaultComment;
        this.description = description;
        this.descriptionShort = descriptionShort;
        this.descriptionRu = descriptionRu;
        this.descriptionRuShort = descriptionRuShort;
        this.mandatory = mandatory;
        this.important = important;
        this.memoryParameter = memoryParameter;
    }

    @Override
    public String toString() {
        return "ParameterDescription{" +
                "tClass=" + tClass +
                ", name='" + name + '\'' +
                ", shortOpt='" + shortOpt + '\'' +
                ", hasArg=" + hasArg +
                ", defaultValue=" + defaultValue +
                ", description='" + description + '\'' +
                ", mandatory='" + mandatory + '\'' +
                '}';
    }
    
    public String printHelp() {
        StringBuilder sb = new StringBuilder();
        if (shortOpt != null) {
            sb.append("-" + shortOpt + ", ");
        } sb.append("--" + name + " ");
        if (hasArg) {
            if (this instanceof MultiValuedParameterDescription) {
                sb.append("<args>");
            } else {
                sb.append("<arg>");
            }
        }
        while (sb.length() < 40) {
            sb.append(' ');
        }

        String additionalDescription = mandatory ? "MANDATORY" : "optional";
        if (defaultComment != null) {
            if (!defaultComment.equals("")) {
                additionalDescription += ", default: " + defaultComment;
            }
        } else if (defaultValue != null) {
            additionalDescription += ", default: " + defaultValue;
        }

        sb.append(description);
        sb.append(" (" + additionalDescription + ")");

        return sb.toString();
    }

    public String printConfig() {
        if (name == null) {  // param not used
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(name + " ");
        while (sb.length() < 30) {
            sb.append(' ');
        }
        sb.append("=\t");
        sb.append(defaultValue);

        return sb.toString();
    }

}
