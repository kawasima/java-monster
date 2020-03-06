package net.unit8.javamonster.stringifiers.dialects;

import graphql.language.IntValue;
import graphql.language.Value;
import graphql.relay.Relay;
import net.unit8.javamonster.OrderColumn;
import net.unit8.javamonster.SortDirection;
import net.unit8.javamonster.ValueUtils;
import net.unit8.javamonster.queryast.SortKey;
import net.unit8.javamonster.relay.ObjectCursor;
import net.unit8.javamonster.sqlast.TableNode;
import net.unit8.javamonster.stringifiers.KeysetPaginationParams;
import net.unit8.javamonster.stringifiers.OffsetPaginationParams;
import net.unit8.javamonster.stringifiers.SQLOrderParts;

import java.math.BigInteger;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

public interface PaginationSupport {
    default String joinPrefix(List<String> prefix) {
        return prefix.stream()
                .skip(1)
                .map(name -> name + "__")
                .collect(Collectors.joining());
    }

    Map<Class<?>, String> CAST_TYPES = Map.of(
            String.class, "TEXT"
    );

    default String generateCastExpressionFromValueType(String key, Object val) {
        return Optional.ofNullable(val)
                .filter(v -> CAST_TYPES.containsKey(v.getClass()))
                .map(v -> "CAST(" + key + " AS " + CAST_TYPES.get(v.getClass()) + ")")
                .orElse(key);
    }

    default String keysetPagingSelect(String table,
                                      List<String> whereConditions,
                                      SQLOrderParts order,
                                      String limit,
                                      String unquotedAs,
                                      PaginationOption option) {
        String whereCondition = Optional.of(String.join(" AND ", whereConditions))
                .filter(s -> !s.isEmpty())
                .orElse("TRUE");
        String as = option.getQuoteFunc().apply(unquotedAs);

        return option.getJoinCondition()
                .map(joinCondition ->
                        option.getJoinType().orElse("") + " JOIN LATERAL (" +
                                "SELECT " + as + ".*" +
                                "  FROM " + table + " " + as +
                                " WHERE " + whereCondition +
                                " ORDER BY " + orderColumnsToString(order.orderColumns(), option.getQuoteFunc(), order.getTable()) +
                                " LIMIT " + limit +
                                ") " + as + " ON " + joinCondition
                )
                .orElse(
                        "FROM (" +
                                "SELECT " + as + ".*" +
                                "  FROM " + table + " " + as +
                                " WHERE " + whereCondition +
                                " ORDER BY " + orderColumnsToString(order.orderColumns(), option.getQuoteFunc(), order.getTable()) +
                                " LIMIT " + limit +
                                ") " + as
                );
    }

    default String offsetPagingSelect(String table,
                                            List<String> pagingWhereConditions,
                                            SQLOrderParts order,
                                            String limit,
                                            BigInteger offset,
                                            String unquotedAs,
                                            PaginationOption option) {

        String whereCondition = Optional.of(String.join(" AND ", pagingWhereConditions))
                .filter(s -> !s.isEmpty())
                .orElse("TRUE");
        String as = option.getQuoteFunc().apply(unquotedAs);

        return option.getJoinCondition()
                .map(joinCondition ->
                        option.getJoinType().orElse("") + " JOIN LATERAL (" +
                                "SELECT " + as + ".*" +
                                "  FROM " + table + " " + as +
                                " WHERE " + whereCondition +
                                " ORDER BY " + orderColumnsToString(order.orderColumns(), option.getQuoteFunc(), order.getTable()) +
                                " LIMIT " + limit +
                                ") " + as + " ON " + joinCondition
                )
                .orElse(
                        "FROM (" +
                                "SELECT " + as + ".*, COUNT(*) OVER() AS" + option.getQuoteFunc().apply("$total") +
                                "  FROM " + table + " " + as +
                                " WHERE " + whereCondition +
                                " ORDER BY " + orderColumnsToString(order.orderColumns(), option.getQuoteFunc(), order.getTable()) +
                                " LIMIT " + limit +
                                " OFFSET " + offset +
                                ") " + as
                );
    }

    default String orderColumnsToString(List<OrderColumn> orderColumns,
                                              Function<String, String> quoteFunc,
                                              String unquotedAs) {
        String asPrefix = Optional.ofNullable(unquotedAs)
                .map(as -> quoteFunc.apply(as) + ".")
                .orElse("");
        return orderColumns.stream()
                .map(ocol -> asPrefix
                        + quoteFunc.apply(ocol.getColumn())
                        + " "
                        + ocol.getDirection().name())
                .collect(Collectors.joining(", "));
    }

    Set<String> UNSUPPORTED_LIMIT_ALL = Set.of("mariadb", "oracle", "mysql");

    default OffsetPaginationParams interpretForOffsetPaging(TableNode node, Dialect dialect) {
        if (node.getArgs().containsKey("last")) {
            throw new UnsupportedOperationException("Backward pagination not supported with offsets. Consider using keyset pagination instead");
        }

        SQLOrderParts order;
        if (nonNull(node.getOrderBy())) {
            order = new SQLOrderParts(node.getAs(), node.getOrderBy());
        } else {
            order = new SQLOrderParts(node.getAs(), node.getJunction().getOrderBy());
        }
        String limit = UNSUPPORTED_LIMIT_ALL.contains(dialect.getName()) ? "18446744073709551615" : "ALL";
        BigInteger offset = BigInteger.ZERO;
        if (node.getArgs().containsKey("first")) {
            Value firstValue = node.getArgs().get("first");
            BigInteger limitArg = firstValue instanceof IntValue ? ((IntValue)firstValue).getValue()
                    : new BigInteger(firstValue.toString());
            if (node.isPaginate()) {
                limitArg.add(BigInteger.ONE);
            }
            limit = limitArg.toString(10);

            if (node.getArgs().containsKey("after")) {
                Relay.ResolvedGlobalId after = new Relay().fromGlobalId(node.getArgs().get("after").toString());
                offset = new BigInteger(after.getId());
            }
        }

        return new OffsetPaginationParams(order, limit, offset);
    }

    default KeysetPaginationParams interpretForKeySetPaging(TableNode node, Dialect dialect) {
        String sortTable;
        SortKey sortKey;
        SortDirection direction;
        SQLOrderParts order;

        if (nonNull(node.getSortKey())) {
            sortKey = node.getSortKey();
            direction = sortKey.getOrder();
            sortTable = node.getAs();

            // flip the sort order if doing backwards paging
            if (node.getArgs().containsKey("last")) {
                direction = direction.flip();
            }
            SortDirection sd = direction;
            order = new SQLOrderParts(
                    node.getAs(),
                    sortKey.getKey().stream()
                            .map(column -> new OrderColumn(column, sd))
                            .collect(Collectors.toList()));
        } else {
            sortKey = node.getJunction().getSortKey();
            direction = sortKey.getOrder();
            sortTable = node.getJunction().getAs();

            // flip the sort order if doing backwards paging
            if (node.getArgs().containsKey("last")) {
                direction = direction.flip();
            }
            SortDirection sd = direction;
            order = new SQLOrderParts(
                    node.getJunction().getAs(),
                    sortKey.getKey().stream()
                            .map(column -> new OrderColumn(column, sd))
                            .collect(Collectors.toList()));
        }

        String limit = UNSUPPORTED_LIMIT_ALL.contains(dialect.getName()) ? "18446744073709551615" : "ALL";
        String whereCondition = "";

        if (node.getArgs().containsKey("first")) {
            limit = ValueUtils.intValue(node.getArgs().get("first"))
                    .map(v -> v + 1)
                    .map(Objects::toString)
                    .orElseThrow(() -> new IllegalArgumentException("first parameter must be integer"));
            if (node.getArgs().containsKey("after")) {
                Optional<String> after = ValueUtils.stringValue(node.getArgs().get("after"));
                ObjectCursor cursor = after
                        .map(ObjectCursor::fromEncoded)
                        .orElse(ObjectCursor.INVALID);
                cursor.validate(sortKey.getKey());
                whereCondition = sortKeyToWhereCondition(cursor, direction, sortTable, dialect);
            }
            if (node.getArgs().containsKey("before")) {
                throw new IllegalArgumentException("Using 'before' with 'first' is nonsensical.");
            }

        } else if (node.getArgs().containsKey("last")) {
            limit = ValueUtils.intValue(node.getArgs().get("last"))
                    .map(v -> v + 1)
                    .map(Objects::toString)
                    .orElseThrow(() -> new IllegalArgumentException("last parameter must be integer"));
            if (node.getArgs().containsKey("before")) {
                Optional<String> before = ValueUtils.stringValue(node.getArgs().get("before"));
                ObjectCursor cursor = before
                        .map(ObjectCursor::fromEncoded)
                        .orElse(ObjectCursor.INVALID);
                cursor.validate(sortKey.getKey());
                whereCondition = sortKeyToWhereCondition(cursor, direction, sortTable, dialect);
            }
            if (node.getArgs().containsKey("after")) {
                throw new IllegalArgumentException("Using 'after' with 'last' is nonsensical.");
            }
        }

        return new KeysetPaginationParams(
                limit,
                order,
                whereCondition);
    }

    default String sortKeyToWhereCondition(
            ObjectCursor cursor,
            SortDirection direction,
            String sortTable,
            Dialect dialect) {
        List<String> sortColumns = new ArrayList<>();
        List<String> sortValues = new ArrayList<>();

        for (String key : cursor.keySet()) {
            sortColumns.add(dialect.quote(sortTable) + "." + dialect.quote(key));
            sortValues.add(new SQLValue(cursor.get(key), dialect).toString());
        }
        RecursiveWhereJoinHelper recursiveWhereJoinHelper = new RecursiveWhereJoinHelper(sortColumns, sortValues, direction.sqlOperator());
        return Objects.equals(dialect.getName(), "oracle")
                ? recursiveWhereJoinHelper.toWhereString()
                : "(" + String.join(", ", sortColumns) + ") " +
                direction.sqlOperator() + " (" +
                String.join(", ", sortValues) + ")";
    }

}
