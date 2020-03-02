package net.unit8.javamonster;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AliasNamespace {
    private boolean minify = false;
    private Set<String> usedTableAliases;
    private Map<String, String> columnAssignments;

    public AliasNamespace(boolean minify) {
        this.minify = minify;
        this.usedTableAliases = new HashSet<>();
        this.columnAssignments = new HashMap<>();
    }

    public String generate(String type, String name) {
        if (type.equals("column")) {
            return name;
        }
        name = name.replaceAll("\\s+", "")
                .replaceAll("[^a-zA-Z0-9]", "_")
                .substring(0, Math.min(name.length(), 9));

        while(this.usedTableAliases.contains(name)) {
            name += "$";
        }
        this.usedTableAliases.add(name);
        return name;
    }
}
