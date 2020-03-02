package net.unit8.javamonster.stringifiers.dialects;

import java.util.List;

public interface Dialect {
    String quote(String str);
    String compositeKey(String parent, List<String>keys);
}
