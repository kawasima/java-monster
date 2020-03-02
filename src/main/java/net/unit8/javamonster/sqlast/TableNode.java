package net.unit8.javamonster.sqlast;

import net.unit8.javamonster.OrderColumn;
import net.unit8.javamonster.SQLJoinFunction;
import net.unit8.javamonster.WhereFunction;

import java.util.ArrayList;
import java.util.List;

public class TableNode extends SQLASTNodeBase {
    private String name;
    private String fieldName;
    private String as;
    private List<OrderColumn> orderBy;
    private boolean grabMany;
    private SQLJoinFunction sqlJoin;
    private boolean sqlBatch;
    private boolean paginate;
    private Integer limit;
    private JunctionNode junction;
    private WhereFunction where;

    private List<SQLASTNode> children;

    public TableNode() {
        this.children = new ArrayList<>();
        this.orderBy = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<SQLASTNode> getChildren() {
        return children;
    }

    @Override
    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getAs() {
        return as;
    }

    public void setAs(String as) {
        this.as = as;
    }

    public List<OrderColumn> getOrderBy() {
        return this.orderBy;
    }

    public void setOrderBy(List<OrderColumn> orderBy) {
        this.orderBy = orderBy;
    }

    @Override
    public boolean isGrabMany() {
        return grabMany;
    }
    public void setGrabMany(boolean grabMany) {
        this.grabMany = grabMany;
    }

    public void setChildren(List<SQLASTNode> children) {
        this.children = children;
    }

    public WhereFunction getWhere() {
        return where;
    }

    public void setWhere(WhereFunction where) {
        this.where = where;
    }

    public boolean isPaginate() {
        return paginate;
    }

    public void setPaginate(boolean paginate) {
        this.paginate = paginate;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public SQLJoinFunction getSqlJoin() {
        return sqlJoin;
    }

    public void setSqlJoin(SQLJoinFunction sqlJoin) {
        this.sqlJoin = sqlJoin;
    }

    public boolean isSqlBatch() {
        return sqlBatch;
    }

    public void setSqlBatch(boolean sqlBatch) {
        this.sqlBatch = sqlBatch;
    }

    public JunctionNode getJunction() {
        return junction;
    }

    public void setJunction(JunctionNode junction) {
        this.junction = junction;
    }
}
