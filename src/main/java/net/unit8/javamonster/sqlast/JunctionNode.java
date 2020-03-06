package net.unit8.javamonster.sqlast;

import net.unit8.javamonster.OrderColumn;
import net.unit8.javamonster.SQLJoinFunction;
import net.unit8.javamonster.WhereFunction;
import net.unit8.javamonster.queryast.SQLBatch;
import net.unit8.javamonster.queryast.SortKey;

import java.util.List;

public class JunctionNode extends SQLASTNodeBase {
    private String sqlTable;
    private String as;
    private SQLJoinFunction inboundJoin;
    private SQLJoinFunction outboundJoin;
    private SortKey sortKey;
    private List<OrderColumn> orderBy;
    private WhereFunction where;
    private SQLBatch sqlBatch;
    private String[] uniqueKeys;

    public JunctionNode() {

    }

    public boolean isSQLBatch() {
        return sqlBatch != null;
    }

    public String getSqlTable() {
        return sqlTable;
    }

    public void setSqlTable(String sqlTable) {
        this.sqlTable = sqlTable;
    }

    @Override
    public String getAs() {
        return as;
    }

    public void setAs(String as) {
        this.as = as;
    }

    public SQLJoinFunction getInboundJoin() {
        return inboundJoin;
    }

    public void setInboundJoin(SQLJoinFunction inboundJoin) {
        this.inboundJoin = inboundJoin;
    }

    public SQLJoinFunction getOutboundJoin() {
        return outboundJoin;
    }

    public void setOutboundJoin(SQLJoinFunction outboundJoin) {
        this.outboundJoin = outboundJoin;
    }

    public List<OrderColumn> getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(List<OrderColumn> orderBy) {
        this.orderBy = orderBy;
    }

    public SortKey getSortKey() {
        return sortKey;
    }

    public void setSortKey(SortKey sortKey) {
        this.sortKey = sortKey;
    }

    public WhereFunction getWhere() {
        return where;
    }

    public void setWhere(WhereFunction where) {
        this.where = where;
    }

    public SQLBatch getSqlBatch() {
        return sqlBatch;
    }

    public void setSqlBatch(SQLBatch sqlBatch) {
        this.sqlBatch = sqlBatch;
    }

    public String[] getUniqueKeys() {
        return uniqueKeys;
    }

    public void setUniqueKeys(String[] uniqueKeys) {
        this.uniqueKeys = uniqueKeys;
    }
}
