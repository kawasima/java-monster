package net.unit8.javamonster;

import graphql.GraphQLContext;
import graphql.language.Value;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.PropertyDataFetcherHelper;
import net.unit8.hydration.NestHydration;
import net.unit8.hydration.mapping.PropertyMapping;
import net.unit8.javamonster.jdbc.StandardJdbcDatabaseCallback;
import net.unit8.javamonster.queryast.Junction;
import net.unit8.javamonster.queryast.SQLExprFunction;
import net.unit8.javamonster.queryast.SortKey;
import net.unit8.javamonster.queryast.ThunkWithArgsCtx;
import net.unit8.javamonster.relay.RelayConnectionConverter;
import net.unit8.javamonster.sqlast.SQLASTNode;
import net.unit8.javamonster.stringifiers.SQLASTPrinter;
import net.unit8.javamonster.stringifiers.dialects.Dialect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

public class JavaMonsterResolver<T> implements DataFetcher<T> {
    private static final Logger LOG = LoggerFactory.getLogger(JavaMonsterResolver.class);
    private NestHydration nestHydration;
    private ObjectShape objectShape;
    private RelayConnectionConverter relayConnectionConverter;

    private ThunkWithArgsCtx<String> sqlTable;
    private ThunkWithArgsCtx<List<OrderColumn>> orderBy;
    private ThunkWithArgsCtx<SortKey> sortKey;
    private ThunkWithArgsCtx<Long> limit;

    private DatabaseCallback<T> databaseCallback;
    private SQLASTPrinter sqlAstPrinter;
    private Dialect dialect;
    private String sqlColumn;
    private SQLExprFunction sqlExprFunction;
    private SQLJoinFunction sqlJoin;
    private Junction junction;
    private boolean sqlPaginate;

    private WhereFunction where;

    private JavaMonsterResolver() {
        nestHydration = new NestHydration();
        objectShape = new ObjectShape();
        relayConnectionConverter = new RelayConnectionConverter();
    }

    public String sqlTable(Map<String, Value> args, GraphQLContext context) {
        return Optional.ofNullable(sqlTable)
                .map(table -> table.getValue(args, context))
                .orElse(null);
    }

    public String sqlColumn() {
        return this.sqlColumn;
    }

    public boolean hasSQLJoin() {
        return this.sqlJoin != null;
    }

    public SQLJoinFunction sqlJoin() {
        return this.sqlJoin;
    }


    public boolean hasJunction() {
        return this.junction != null;
    }

    public Junction getJunction() {
        return this.junction;
    }

    public boolean hasOrderBy() {
        return this.orderBy != null;
    }
    public ThunkWithArgsCtx<List<OrderColumn>> orderBy() {
        return this.orderBy;
    }

    public boolean hasWhere() {
        return this.where != null;
    }
    public WhereFunction where() {
        return this.where;
    }

    public SQLExprFunction sqlExpr() {
        return this.sqlExprFunction;
    }

    public boolean isSQLPaginate() {
        return sqlPaginate;
    }

    @Override
    public T get(DataFetchingEnvironment environment) throws Exception {
        if (databaseCallback == null) {
            return (T) PropertyDataFetcherHelper.getPropertyValue(
                    environment.getField().getName(),
                    environment.getSource(),
                    environment.getFieldType());
        }
        QueryAST queryAST = new QueryAST(environment.getGraphQLSchema().getCodeRegistry());
        SQLASTNode sqlAST = queryAST.toSqlAST(environment);
        String sql = sqlAstPrinter.stringifySqlAST(sqlAST, environment.getContext());
        PropertyMapping propertyMapping = objectShape.defineObjectShape(sqlAST);
        LOG.info("sql={}", sql);
        T rows = databaseCallback.apply(sql);
        T data = (T) nestHydration.nest((List<Map<String, Object>>) rows, propertyMapping);
        data = (T) relayConnectionConverter.arrayToConnection(data, sqlAST);
        return data;
    }

    public static class Builder<T> {
        private JavaMonsterResolver<T> resolver;

        public Builder() {
            this.resolver = new JavaMonsterResolver<>();
        }

        public Builder sqlTable(BiFunction<Map<String, Value>, GraphQLContext, String> sqlTableFunction) {
            resolver.sqlTable = new ThunkWithArgsCtx<>(sqlTableFunction);
            return this;
        }

        public Builder sqlTable(String sqlTable) {
            resolver.sqlTable = new ThunkWithArgsCtx<>(sqlTable);
            return this;
        }

        public Builder where(WhereFunction where) {
            resolver.where = where;
            return this;
        }

        public Builder sqlColumn(String sqlColumn){
            resolver.sqlColumn = sqlColumn;
            return this;
        }

        public Builder sqlJoin(SQLJoinFunction sqlJoinFunction) {
            resolver.sqlJoin = sqlJoinFunction;
            return this;
        }

        public Builder junction(Junction junction) {
            resolver.junction = junction;
            return this;
        }

        public Builder orderBy(OrderColumn... orderColumn) {
            resolver.orderBy = new ThunkWithArgsCtx<>(Arrays.asList(orderColumn));
            return this;
        }

        public Builder orderBy(BiFunction<Map<String, Value>, GraphQLContext, List<OrderColumn>> orderFunction) {
            resolver.orderBy = new ThunkWithArgsCtx<>(orderFunction);
            return this;
        }

        public Builder sortKey(SortKey sortKey) {
            resolver.sortKey = new ThunkWithArgsCtx<>(sortKey);
            return this;
        }

        public Builder sortKey(BiFunction<Map<String, Value>, GraphQLContext, SortKey> sortKeyFunction) {
            resolver.sortKey = new ThunkWithArgsCtx<>(sortKeyFunction);
            return this;
        }

        public Builder sqlExpr(SQLExprFunction sqlExprFunction) {
            resolver.sqlExprFunction = sqlExprFunction;
            return this;
        }

        public Builder sqlPaginate(boolean sqlPaginate) {
            resolver.sqlPaginate = sqlPaginate;
            return this;
        }

        public Builder limit(long limit) {
            resolver.limit = new ThunkWithArgsCtx<>(limit);
            return this;
        }

        public Builder limit(BiFunction<Map<String, Value>, GraphQLContext, Long> limitFunction) {
            resolver.limit = new ThunkWithArgsCtx<>(limitFunction);
            return this;
        }

        public Builder dbCall(DatabaseCallback<T> callback) {
            resolver.databaseCallback = callback;
            return this;
        }

        public Builder dialect(Dialect dialect) {
            resolver.dialect = dialect;
            return this;
        }

        public JavaMonsterResolver<T> build() {
            resolver.sqlAstPrinter = new SQLASTPrinter(resolver.dialect);
            return resolver;
        }
    }
}
