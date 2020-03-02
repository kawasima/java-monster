package net.unit8.javamonster.stringifiers;

import graphql.GraphQLContext;
import net.unit8.javamonster.OrderColumn;
import net.unit8.javamonster.sqlast.ColumnNode;
import net.unit8.javamonster.sqlast.SQLASTNode;
import net.unit8.javamonster.sqlast.TableNode;
import net.unit8.javamonster.stringifiers.dialects.Dialect;
import net.unit8.javamonster.stringifiers.dialects.H2Dialect;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class SQLASTPrinter {
    public String stringifySqlAST(SQLASTNode topNode, GraphQLContext context) {
        Dialect dialect = new H2Dialect();

        SQLParts sqlParts = new SQLParts();
        _stringifySqlAST(null, topNode, new ArrayList<>(),
                context, sqlParts, dialect);
        StringBuilder sql = new StringBuilder("SELECT\n  ")
                .append(String.join(",\n  ", sqlParts.getSelections()))
                .append("\n")
                .append(String.join("\n", sqlParts.getTables()));

        String where = sqlParts.getWheres()
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" AND "));
        if (!where.isEmpty()) {
            sql.append("\nWHERE ").append(where);
        }

        if (!sqlParts.getOrders().isEmpty()) {
            sql.append("\nORDER BY ")
                    .append(stringifyOuterOrder(sqlParts.getOrders(), dialect));
        }
        return sql.toString();
    }

    SQLParts _stringifySqlAST(SQLASTNode parent, SQLASTNode node, List<String> prefix,
            GraphQLContext context, SQLParts sqlParts, Dialect dialect) {
        String parentTable = Optional.ofNullable(parent)
                .filter(TableNode.class::isInstance)
                .map(TableNode.class::cast)
                .map(TableNode::getAs)
                .orElse(null);
        if (node instanceof TableNode) {
            TableNode tableNode = (TableNode) node;
            handleTable(parent, tableNode, prefix, context, sqlParts);

            for(SQLASTNode childNode : tableNode.getChildren()) {
                List<String> childPrefix = new ArrayList<>(prefix);
                childPrefix.add(tableNode.getAs());

                _stringifySqlAST(node, childNode, childPrefix, context, sqlParts, dialect);
            }
        } else if (node instanceof ColumnNode){
            ColumnNode columnNode = (ColumnNode) node;
            sqlParts.getSelections().add(dialect.quote(parentTable)
                    + "."
                    + dialect.quote(columnNode.getName())
                    + " AS "
                    + dialect.quote(joinPrefix(prefix) + columnNode.getAs())
            );
        }
        return sqlParts;
    }

    void handleTable(SQLASTNode parent, TableNode node, List<String> prefix,
                     GraphQLContext context, SQLParts sqlParts) {
        // generate the "where" condition, if applicable
        if (whereConditionIsntSupposedTooInsideSubqueryOrOnNextBatch(node, parent)) {
            if (Objects.nonNull(node.getWhere())) {
                sqlParts.getWheres().add(
                        node.getWhere().apply(node.getAs(), node.getArgs(), context));
            }
        }

        // order
        if (thisIsNotTheEndOfThisBatch(node, parent)) {
            if (Objects.nonNull(node.getOrderBy())) {
                // TODO order by
            }
        }

        if (Objects.nonNull(node.getSqlJoin())) {
            String joinCondition = node.getSqlJoin().apply(
                    parent.getAs(),
                    node.getAs(),
                    node.getArgs());

            if (node.isPaginate()) {
                // TODO handleJoinedOneToManyPaginated
            } else if (Objects.nonNull(node.getLimit())) {
                // TODO handleJoinedOntToManyPaginated
            } else {
                sqlParts.getTables().add(
                        "LEFT JOIN " + node.getName() +
                                " " + node.getAs() +
                                " ON " + joinCondition
                );
            }
        } else {
            sqlParts.getTables().add("FROM " + node.getName() + " " + node.getAs());
        }

    }

    private boolean whereConditionIsntSupposedTooInsideSubqueryOrOnNextBatch(TableNode node, SQLASTNode parent) {
        return !node.isPaginate()
                && (!(node.isSqlBatch()|| Optional.ofNullable(node.getJunction()).map(j -> j.isSqlBatch()).orElse(false))
                || parent != null);
    }

    private boolean thisIsNotTheEndOfThisBatch(TableNode node, SQLASTNode parent) {
        return (!node.isSqlBatch() && !(Optional.ofNullable(node.getJunction()).map(j->j.isSqlBatch()).orElse(false))) || parent != null;
    }

    private String stringifyOuterOrder(List<SQLOrderParts> orders, Dialect dialect) {
        List<String> conditions = new ArrayList<>();
        for (SQLOrderParts condition : orders) {
            for (OrderColumn column : condition.orderColumns()) {
                conditions.add(dialect.quote(condition.getTable())
                        + "."
                        + dialect.quote(column.getColumn())
                        + " "
                        + column.getDirection());
            }
        }
        return String.join(", ", conditions);
    }

    private String joinPrefix(List<String> prefix) {
        return prefix.stream()
                .skip(1)
                .map(name -> name + "__")
                .collect(Collectors.joining());
    }

    private static class SQLOrderParts {
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

    private static class SQLParts {
        private List<String> selections;
        private List<String> tables;
        private List<String> wheres;
        private List<SQLOrderParts> orders;

        public SQLParts() {
            this.selections = new ArrayList<>();
            this.tables = new ArrayList<>();
            this.wheres = new ArrayList<>();
            this.orders = new ArrayList<>();
        }

        public List<String> getSelections() {
            return selections;
        }

        public List<String> getTables() {
            return tables;
        }

        public List<String> getWheres() {
            return wheres;
        }

        public List<SQLOrderParts> getOrders() {
            return orders;
        }
    }
}
