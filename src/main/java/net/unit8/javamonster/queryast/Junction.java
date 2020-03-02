package net.unit8.javamonster.queryast;

import net.unit8.javamonster.OrderColumn;
import net.unit8.javamonster.SQLJoinFunction;
import net.unit8.javamonster.SqlTableFunctionOrValue;

import java.util.List;

public class Junction {
    private SqlTableFunctionOrValue sqlTable;
    private String as;
    private SQLJoinFunction inboundJoin;
    private SQLJoinFunction outboundJoin;
    private ThunkWithArgsCtx<List<OrderColumn>> orderBy;
    private boolean sqlBatch;

    private Junction() {

    }

    public boolean isSqlBatch() {
        return sqlBatch;
    }

    public static class Builder {
        private Junction junction;
        public Builder() {
            junction = new Junction();
        }

        public Builder sqlTable(String sqlTable) {
            junction.sqlTable = new SqlTableFunctionOrValue(sqlTable);
            return this;
        }
    }
}
