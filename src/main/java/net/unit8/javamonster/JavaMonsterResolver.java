package net.unit8.javamonster;

import graphql.GraphQLContext;
import graphql.language.Value;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.PropertyDataFetcherHelper;
import net.unit8.hydration.NestHydration;
import net.unit8.hydration.mapping.PropertyMapping;
import net.unit8.javamonster.queryast.SQLExprFunction;
import net.unit8.javamonster.queryast.ThunkWithArgsCtx;
import net.unit8.javamonster.sqlast.SQLASTNode;
import net.unit8.javamonster.stringifiers.SQLASTPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public class JavaMonsterResolver<T> implements DataFetcher<T> {
    private static final Logger LOG = LoggerFactory.getLogger(JavaMonsterResolver.class);
    private NestHydration nestHydration;
    private ObjectShape objectShape;

    private ThunkWithArgsCtx<String> sqlTable;
    private ThunkWithArgsCtx<List<OrderColumn>> orderBy;
    private ThunkWithArgsCtx<Long> limit;

    private DatabaseCallback<T> databaseCallback;
    private SQLASTPrinter sqlAstPrinter = new SQLASTPrinter();
    private String sqlColumn;
    private SQLExprFunction sqlExprFunction;
    private SQLJoinFunction sqlJoin;
    private Boolean sqlPaginate;

    private WhereFunction where;

    private JavaMonsterResolver() {
        nestHydration = new NestHydration();
        objectShape = new ObjectShape();
    }

    public String sqlTable(Map<String, Value> args, GraphQLContext context) {
        return this.sqlTable.getValue(args, context);
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

    public boolean hasOrderBy() {
        return this.orderBy != null;
    }
    public ThunkWithArgsCtx<List<OrderColumn>> orderBy() {
        return this.orderBy;
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
        return (T) nestHydration.nest((List<Map<String, Object>>) rows, propertyMapping);
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

        public Builder sqlColumn(String sqlColumn){
            resolver.sqlColumn = sqlColumn;
            return this;
        }

        public Builder sqlJoin(SQLJoinFunction sqlJoinFunction) {
            resolver.sqlJoin = sqlJoinFunction;
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

        public Builder sqlExpr(SQLExprFunction sqlExprFunction) {
            resolver.sqlExprFunction = sqlExprFunction;
            return this;
        }

        public Builder sqlPaginate(boolean sqlPaginate) {
            resolver.sqlPaginate = sqlPaginate;
            return this;
        }

        public Builder dbCall(DatabaseCallback<T> callback) {
            resolver.databaseCallback = callback;
            return this;
        }

        public JavaMonsterResolver<T> build() {
            return resolver;
        }
    }
}
