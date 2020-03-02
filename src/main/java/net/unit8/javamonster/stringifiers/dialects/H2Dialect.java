package net.unit8.javamonster.stringifiers.dialects;

import java.util.List;
import java.util.stream.Collectors;

public class H2Dialect implements Dialect {
    @Override
    public String quote(String str) {
        return '"' + str + '"';
    }

    @Override
    public String compositeKey(String parent, List<String> keys) {
        return keys.stream()
                .map(key -> quote(parent) + "." + quote(key))
                .collect(Collectors.joining(" || "));
    }
}
