package net.unit8.javamonster.queryast;

import net.unit8.javamonster.SortDirection;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SortKey implements Serializable {
    private List<String> key;
    private SortDirection order;

    public SortKey(String key, SortDirection order) {
        this.order = order;
        this.key = Collections.singletonList(key);
    }

    public SortKey(String... keys) {
        this.key = Arrays.asList(keys);
        this.order = SortDirection.ASC;
    }

    public SortKey(List<String> keys, SortDirection order) {
        this.key = keys;
        this.order = order;
    }

    public List<String> getKey() {
        return key;
    }

    public SortDirection getOrder() {
        return order;
    }
}
