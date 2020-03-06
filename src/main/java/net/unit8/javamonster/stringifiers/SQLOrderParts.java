package net.unit8.javamonster.stringifiers;

import net.unit8.javamonster.OrderColumn;

import java.io.Serializable;
import java.util.List;

public class SQLOrderParts implements Serializable {
    private String table;
    private List<OrderColumn> orderColumns;

    public SQLOrderParts(String table, List<OrderColumn> orderColumns) {
        this.table = table;
        this.orderColumns = orderColumns;
    }

    public String getTable() {
        return table;
    }
    public List<OrderColumn> orderColumns() {
        return orderColumns;
    }
}
