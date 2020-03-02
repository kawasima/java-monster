package net.unit8.javamonster;

import java.io.Serializable;

public class OrderColumn implements Serializable {
    private String column;
    private SortDirection direction;

    public OrderColumn(String column, SortDirection direction) {
        this.column = column;
        this.direction = direction;
    }
    public OrderColumn(String column) {
        this(column, SortDirection.ASC);
    }

    public String getColumn() {
        return column;
    }

    public SortDirection getDirection() {
        return direction;
    }
}
