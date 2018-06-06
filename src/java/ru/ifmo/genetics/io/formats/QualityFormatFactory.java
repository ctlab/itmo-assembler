package ru.ifmo.genetics.io.formats;

import ru.ifmo.genetics.utils.tool.ParameterValueFactory;

import java.util.*;

public class QualityFormatFactory implements ParameterValueFactory<QualityFormat> {

    private static Map <String, QualityFormat> map = new HashMap<String, QualityFormat>();

    public final static QualityFormatFactory instance = new QualityFormatFactory();

    private QualityFormatFactory() {
        register(Illumina.instance);
        register(Sanger.instance);
    }

    public void register(QualityFormat qf) {
        map.put(qf.toString().toLowerCase(), qf);
    }

    public QualityFormat get(String name) {
        name = name.toLowerCase();
        if (!map.containsKey(name)) {
            throw new NoSuchElementException("no such element'" + name + "' in qf map");
        }
        return map.get(name);
    }

    @Override
    public Set<String> options() {
        return map.keySet();
    }

    public static QualityFormatFactory getInstance() {
        return instance;
    }

}
