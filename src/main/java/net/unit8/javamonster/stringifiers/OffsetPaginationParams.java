package net.unit8.javamonster.stringifiers;

import java.io.Serializable;
import java.math.BigInteger;

public class OffsetPaginationParams implements Serializable {
    private SQLOrderParts order;
    private String limit;
    private BigInteger offset;

    public OffsetPaginationParams(SQLOrderParts order, String limit, BigInteger offset) {
        this.order = order;
        this.limit = limit;
        this.offset = offset;
    }

    public SQLOrderParts getOrder() {
        return order;
    }

    public String getLimit() {
        return limit;
    }

    public BigInteger getOffset() {
        return offset;
    }
}
