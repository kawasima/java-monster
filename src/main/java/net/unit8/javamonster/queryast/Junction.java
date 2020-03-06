package net.unit8.javamonster.queryast;

import graphql.GraphQLContext;
import graphql.language.Value;
import net.unit8.javamonster.OrderColumn;
import net.unit8.javamonster.SQLJoinFunction;
import net.unit8.javamonster.SqlTableFunctionOrValue;
import net.unit8.javamonster.WhereFunction;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Junction {
    private SqlTableFunctionOrValue sqlTable;
    private String as;
    private SQLJoinFunction inboundJoin;
    private SQLJoinFunction outboundJoin;
    private ThunkWithArgsCtx<List<OrderColumn>> orderBy;
    private WhereFunction where;
    private SQLBatch sqlBatch;
    private String[] uniqueKeys;

    private Junction() {

    }

    public static Junction.Builder newJunction() {
        return new Junction.Builder();
    }

    public String getSQLTable(Map<String, Value> args, GraphQLContext context) {
        return Optional.ofNullable(sqlTable)
                .map(t -> t.getValue(args))
                .orElse(null);
    }

    public List<OrderColumn> getOrderBy(Map<String, Value> args, GraphQLContext context) {
        return Optional.ofNullable(orderBy)
                .map(o -> o.getValue(args, context))
                .orElse(Collections.emptyList());
    }

    public WhereFunction where() {
        return where;
    }

    public boolean hasSQLJoins() {
        return inboundJoin != null && outboundJoin != null;
    }

    public SQLJoinFunction inboundJoin() {
        return inboundJoin;
    }
    public SQLJoinFunction outboundJoin() {
        return outboundJoin;
    }

    public boolean isSqlBatch() {
        return sqlBatch != null;
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

        public Builder orderBy(ThunkWithArgsCtx<List<OrderColumn>> orderBy) {
            junction.orderBy = orderBy;
            return this;
        }

        public Builder sqlBatch(SQLBatch sqlBatch) {
            junction.sqlBatch = sqlBatch;
            return this;
        }

        public Builder sqlJoins(SQLJoinFunction inboundJoin, SQLJoinFunction outboundJoin) {
            junction.inboundJoin  = inboundJoin;
            junction.outboundJoin = outboundJoin;
            return this;
        }

        public Builder uniqueKey(String uniqueKey) {
            junction.uniqueKeys = new String[]{ uniqueKey };
            return this;
        }

        public Builder uniqueKeys(String... uniqueKey) {
            junction.uniqueKeys = uniqueKey;
            return this;
        }

        public Junction build() {
            if ((junction.inboundJoin != null && junction.outboundJoin == null)
                    || (junction.inboundJoin == null && junction.outboundJoin != null)) {
                throw new IllegalArgumentException("Junction join must be inbound and outbound.");
            }
            return junction;
        }
    }
}
