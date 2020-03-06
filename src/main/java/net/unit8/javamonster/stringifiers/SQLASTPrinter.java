package net.unit8.javamonster.stringifiers;

import graphql.GraphQLContext;
import graphql.language.IntValue;
import net.unit8.javamonster.OrderColumn;
import net.unit8.javamonster.sqlast.ColumnNode;
import net.unit8.javamonster.sqlast.JunctionNode;
import net.unit8.javamonster.sqlast.SQLASTNode;
import net.unit8.javamonster.sqlast.TableNode;
import net.unit8.javamonster.stringifiers.dialects.Dialect;
import net.unit8.javamonster.stringifiers.dialects.PostgresqlDialect;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

public class SQLASTPrinter {
    private Dialect dialect;

    public SQLASTPrinter(Dialect dialect) {
        this.dialect = dialect;
        if (this.dialect == null) {
            this.dialect = new PostgresqlDialect();
        }
    }

    public String stringifySqlAST(SQLASTNode topNode, GraphQLContext context) {
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
            sqlParts.addSelection(dialect.quote(parentTable)
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
            Optional.ofNullable(node.getJunction())
                    .map(JunctionNode::getWhere)
                    .ifPresent(where -> {
                        sqlParts.addWhere(
                                where.apply(dialect.quote(node.getJunction().getAs()),
                                        node.getArgs(), context)
                        );
                    });
            if (nonNull(node.getWhere())) {
                sqlParts.addWhere(
                        node.getWhere().apply(node.getAs(), node.getArgs(), context));
            }
        }

        // order
        if (thisIsNotTheEndOfThisBatch(node, parent)) {
            Optional.ofNullable(node.getJunction())
                    .map(JunctionNode::getOrderBy)
                    .filter(orderColumns -> !orderColumns.isEmpty())
                    .ifPresent(orderColumns -> sqlParts
                            .addOrder(
                                    node.getJunction().getAs(),
                                    node.getJunction().getOrderBy()
                            )
                    );
            if (nonNull(node.getOrderBy()) && !node.getOrderBy().isEmpty()) {
                sqlParts.addOrder(
                        node.getAs(),
                        node.getOrderBy()
                );
            }
        }

        if (nonNull(node.getSqlJoin())) {
            // one-to-many using Join
            String joinCondition = node.getSqlJoin().apply(
                    parent.getAs(),
                    node.getAs(),
                    node.getArgs(),
                    context);

            if (node.isPaginate()) {
                Optional.ofNullable(dialect.handleJoinedOneToManyPaginated(parent, node, context, joinCondition))
                        .ifPresent(sqlParts::addTable);
            } else if (nonNull(node.getLimit())) {
                // TODO handleJoinedOntToManyPaginated
            } else {
                sqlParts.addTable(
                        "LEFT JOIN " + node.getName() +
                                " " + node.getAs() +
                                " ON " + joinCondition
                );
            }
        } else if (Optional.ofNullable(node.getJunction()).map(JunctionNode::getSqlTable).isPresent()) {
            // many-to-many using JOIN
            String inboundJoin = node.getJunction().getInboundJoin().apply(
                    dialect.quote(parent.getAs()),
                    dialect.quote(node.getJunction().getAs()),
                    node.getArgs(),
                    context);
            String outboundJoin = node.getJunction().getOutboundJoin().apply(
                    dialect.quote(node.getJunction().getAs()),
                    dialect.quote(node.getAs()),
                    node.getArgs(),
                    context);

            if (node.isPaginate()) {
                sqlParts.addTable(dialect.handleJoinedManyToManyPaginated(parent, node, context,
                        inboundJoin, outboundJoin));
            } else if (node.hasLimit()) {
                node.getArgs().put("first", new IntValue(BigInteger.valueOf(node.getLimit().longValue())));
                sqlParts.addTable(dialect.handleJoinedManyToManyPaginated(parent, node, context,
                        inboundJoin, outboundJoin));
            } else {
                sqlParts.addTable("LEFT JOIN " + node.getJunction().getSqlTable()
                        + " " + dialect.quote(node.getJunction().getAs())
                        + " ON " + inboundJoin);
            }
            sqlParts.addTable("LEFT JOIN " + node.getName()
                    + " " + dialect.quote(node.getAs())
                    + " ON " + outboundJoin);
        } else if (node.isPaginate()) {
            // otherwise, we aren't joining, so we are at the "root", and this is the start of the From clause
            sqlParts.addTable(dialect.handlePaginationAtRoot(parent, node, context));
        } else {
            sqlParts.addTable("FROM " + node.getName() + " " + node.getAs());
        }

    }

    private boolean whereConditionIsntSupposedTooInsideSubqueryOrOnNextBatch(TableNode node, SQLASTNode parent) {
        return !node.isPaginate()
                && (!(node.isSqlBatch()|| Optional.ofNullable(node.getJunction()).map(JunctionNode::isSQLBatch).orElse(false))
                || parent != null);
    }

    private boolean thisIsNotTheEndOfThisBatch(TableNode node, SQLASTNode parent) {
        return (!node.isSqlBatch() && !(Optional.ofNullable(node.getJunction()).map(JunctionNode::isSQLBatch).orElse(false))) || parent != null;
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

        public void addTable(String table) {
            tables.add(table);
        }

        public void addWhere(String where) {
            wheres.add(where);
        }

        public void addSelection(String selection) {
            selections.add(selection);
        }

        public void addOrder(String table, List<OrderColumn> orderColumns) {
            orders.add(new SQLOrderParts(table, orderColumns));
        }
    }
}
