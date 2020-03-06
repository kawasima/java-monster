package net.unit8.javamonster.stringifiers.dialects;

import graphql.GraphQLContext;
import net.unit8.javamonster.sqlast.SQLASTNode;
import net.unit8.javamonster.sqlast.TableNode;
import net.unit8.javamonster.stringifiers.KeysetPaginationParams;
import net.unit8.javamonster.stringifiers.OffsetPaginationParams;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

public class PostgresqlDialect implements Dialect, PaginationSupport {
    @Override
    public String getName() {
        return "pg";
    }

    @Override
    public String quote(String str) {
        return "\"" + str + "\"";
    }

    @Override
    public String compositeKey(String parent, List<String> keys) {
        return "NULLIF(CONCAT(" +
                keys.stream()
                        .map(key -> quote(parent) + "." + quote(key))
                        .collect(Collectors.joining(", ")) +
                "), '')";
    }

    @Override
    public String handleJoinedOneToManyPaginated(SQLASTNode parent,
                                        SQLASTNode node,
                                        GraphQLContext context,
                                        String joinCondition) {
        TableNode tableNode = (TableNode) node;
        List<String> pagingWhereConditions = new ArrayList<>();
        pagingWhereConditions.add(tableNode.getSqlJoin().apply(quote(parent.getAs()), quote(node.getAs()), node.getArgs(), context));

        if (nonNull(tableNode.getWhere())) {
            pagingWhereConditions.add(tableNode.getWhere().apply(quote(node.getAs()), node.getArgs(), context));
        }

        // which type of pagination are they using?
        if (nonNull(tableNode.getSortKey())) {
            return null;
        } else if (nonNull(tableNode.getOrderBy())) {
            OffsetPaginationParams paginationParams = interpretForOffsetPaging(tableNode, this);
            return offsetPagingSelect(tableNode.getName(),
                    pagingWhereConditions,
                    paginationParams.getOrder(),
                    paginationParams.getLimit(),
                    paginationParams.getOffset(),
                    tableNode.getAs(),
                    new PaginationOption.Builder()
                            .joinType("LEFT")
                            .build()
            );
        }

        return null;
    }

    @Override
    public String handleJoinedManyToManyPaginated(
            SQLASTNode parent,
            SQLASTNode node,
            GraphQLContext context,
            String inboundJoinCondition,
            String outboundJoinCondition) {
        if (!(node instanceof TableNode)) throw new IllegalArgumentException("node must be TableNode");

        TableNode tableNode = (TableNode) node;
        List<String> pagingWhereConditions = new ArrayList<>();
        pagingWhereConditions.add(
                tableNode.getJunction().getInboundJoin().apply(
                        parent.getAs(),
                        tableNode.getJunction().getAs(),
                        tableNode.getArgs(),
                        context)
        );
        if (nonNull(tableNode.getJunction().getWhere())) {
            pagingWhereConditions.add(
                    tableNode.getJunction().getWhere().apply(
                            tableNode.getJunction().getAs(),
                            tableNode.getArgs(),
                            context
                    )
            );
        }
        if (nonNull(tableNode.getWhere())) {
            pagingWhereConditions.add(
                    tableNode.getWhere().apply(
                            tableNode.getAs(),
                            tableNode.getArgs(),
                            context
                    )
            );
        }

        PaginationOption.Builder optionBuilder = new PaginationOption.Builder()
                .joinType("LEFT")
                .joinCondition(inboundJoinCondition);

        if (nonNull(tableNode.getWhere()) || nonNull(tableNode.getOrderBy())) {
            optionBuilder.extraJoin(tableNode.getName(), tableNode.getAs(), outboundJoinCondition);
        }
        PaginationOption paginationOption = optionBuilder.build();

        if (nonNull(tableNode.getSortKey()) || nonNull(tableNode.getJunction().getSortKey())) {
            KeysetPaginationParams keysetPaginationParams = interpretForKeySetPaging(tableNode, this);
            pagingWhereConditions.add(keysetPaginationParams.getWhereCondition());
            return keysetPagingSelect(
                    tableNode.getJunction().getSqlTable(),
                    pagingWhereConditions,
                    keysetPaginationParams.getOrder(),
                    keysetPaginationParams.getLimit(),
                    tableNode.getJunction().getAs(),
                    paginationOption
            );
        } else if (nonNull(tableNode.getOrderBy())
                || nonNull(tableNode.getJunction().getOrderBy())) {
            OffsetPaginationParams offsetPaginationParams = interpretForOffsetPaging(
                    tableNode,
                    this);

            return offsetPagingSelect(
                    tableNode.getJunction().getSqlTable(),
                    pagingWhereConditions,
                    offsetPaginationParams.getOrder(),
                    offsetPaginationParams.getLimit(),
                    offsetPaginationParams.getOffset(),
                    tableNode.getJunction().getAs(),
                    paginationOption
            );
        }
        throw new IllegalArgumentException();

    }

    @Override
    public String handlePaginationAtRoot(SQLASTNode parent, TableNode node, GraphQLContext context) {
        List<String> pagingWhereConditions = new ArrayList<>();

        if (nonNull(node.getSortKey())) {
            return null;
        } else if (nonNull(node.getOrderBy())) {
            OffsetPaginationParams paginationParams = interpretForOffsetPaging(node, this);
            if (nonNull(node.getWhere())) {
                pagingWhereConditions.add(node.getWhere().apply(quote(node.getAs()), node.getArgs(), context));
            }

            return offsetPagingSelect(node.getName(),
                    pagingWhereConditions,
                    paginationParams.getOrder(),
                    paginationParams.getLimit(),
                    paginationParams.getOffset(),
                    node.getAs(),
                    new PaginationOption.Builder()
                            .build()
            );
        }
        return null;
    }
}
