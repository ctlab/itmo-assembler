package ru.ifmo.genetics.utils.tool.values;

public class StringUnionYielder extends Yielder<String> {
    private final InValue<String>[] inValues;

    public StringUnionYielder(InValue<String>... inValues) {
        this.inValues = inValues;
    }
    public StringUnionYielder(InValue<String> firstInValue, String secondValue) {
        inValues = new InValue[]{firstInValue, new SimpleInValue<String>(secondValue)};
    }

    @Override
    public String yield() {
        StringBuilder sb = new StringBuilder();

        for (InValue<String> inValue: inValues) {
            String value = inValue.get();
            sb.append(value);
        }
        return sb.toString();
    }

    @Override
    public String description() {
        return null;
    }
}
