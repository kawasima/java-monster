package net.unit8.javamonster.stringifiers;

import java.io.Serializable;

public class KeysetPaginationParams implements Serializable {
    private String limit;
    private SQLOrderParts order;
    private String whereCondition;

    public KeysetPaginationParams(String limit, SQLOrderParts order, String whereCondition) {
        this.limit = limit;
        this.order = order;
        this.whereCondition = whereCondition;
    }

    public String getLimit() {
        return limit;
    }

    public SQLOrderParts getOrder() {
        return order;
    }

    public String getWhereCondition() {
        return whereCondition;
    }
}
