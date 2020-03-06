package net.unit8.javamonster.queryast;

import graphql.GraphQLContext;
import graphql.language.*;
import graphql.schema.*;
import net.unit8.javamonster.AliasNamespace;
import net.unit8.javamonster.JavaMonsterResolver;
import net.unit8.javamonster.sqlast.*;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

public class QueryASTFactory {
    private static final Set<Class<? extends GraphQLType>> TABLE_TYPES = Set.of(
            GraphQLObjectType.class,
            GraphQLUnionType.class,
            GraphQLInterfaceType.class
    );


    private GraphQLCodeRegistry codeRegistry;
    private GraphQLSchema schema;
    private Map<String, FragmentDefinition> fragments;
    private AliasNamespace namespace;

    public QueryASTFactory(GraphQLCodeRegistry codeRegistry, GraphQLSchema schema, Map<String, FragmentDefinition> fragments, AliasNamespace namespace) {
        this.codeRegistry = codeRegistry;
        this.schema = schema;
        this.fragments = fragments;
        this.namespace = namespace;
    }

    public SQLASTNode create(Field queryASTNode,
                             GraphQLType parentTypeNode,
                             GraphQLContext context) {
        String fieldName = queryASTNode.getName();
        if (fieldName.startsWith("__")) {
            return NoopNode.INSTANCE;
        }

        GraphQLFieldDefinition fieldDefinition = ((GraphQLObjectType) parentTypeNode).getFieldDefinition(fieldName);
        DataFetcher dataFetcher = codeRegistry.getDataFetcher(FieldCoordinates.coordinates(
                ((GraphQLObjectType) parentTypeNode).getName(), fieldName),
                fieldDefinition);
        boolean grabMany = false;
        GraphQLType gqlType = stripNonNullType(fieldDefinition.getType());

        // args
        Map<String, Value> args = queryASTNode.getArguments().stream()
                .map(argument -> Map.entry(argument.getName(), argument.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // if list then mark flag true & get the type inside the GraphQLList container type
        if (gqlType instanceof GraphQLList) {
            gqlType = stripNonNullType(((GraphQLList) gqlType).getWrappedType());
            grabMany = true;
        }

        // if its a relay connection, there are several things we need to do
        if (gqlType instanceof GraphQLObjectType
                && nonNull(((GraphQLObjectType) gqlType).getFieldDefinition("edges"))
                && nonNull(((GraphQLObjectType) gqlType).getFieldDefinition("pageInfo"))
        ) {
            grabMany = true;
            QueryASTParseContext queryASTParseContext = stripRelayConnection((GraphQLObjectType) gqlType, queryASTNode);

            gqlType = stripNonNullType(queryASTParseContext.getType());
            queryASTNode = queryASTParseContext.getQueryASTNode();
        } else if (dataFetcher instanceof JavaMonsterResolver<?> && ((JavaMonsterResolver<?>) dataFetcher).isSQLPaginate()) {
            throw new IllegalArgumentException("To paginate the " + gqlType + " type, it must be a GraphQLObjectType that fulfills the relay spec.\n" +
                    "      The type must have a \"pageInfo\" and \"edges\" field. https://facebook.github.io/relay/graphql/connections.htm");
        }

        // is this a table in SQL?
        if (TABLE_TYPES.contains(gqlType.getClass())) {
            TableNode tableNode = handleTable(queryASTNode, gqlType, (JavaMonsterResolver<?>) dataFetcher, args, context, grabMany, fragments);
            tableNode.setArgs(args);
            return tableNode;
        } else if (dataFetcher instanceof PropertyDataFetcher) {
            ColumnNode columnNode = new ColumnNode();
            columnNode.setName(((PropertyDataFetcher) dataFetcher).getPropertyName());
            columnNode.setFieldName(queryASTNode.getName());
            String aliasForm = columnNode.getFieldName();
            columnNode.setAs(namespace.generate("column", aliasForm));
            columnNode.setArgs(args);
            return columnNode;
        } else if (dataFetcher instanceof JavaMonsterResolver
                && ((JavaMonsterResolver) dataFetcher).sqlColumn() != null) {
            ColumnNode columnNode = new ColumnNode();
            columnNode.setName(Optional.of(dataFetcher)
                    .map(JavaMonsterResolver.class::cast)
                    .map(JavaMonsterResolver::sqlColumn)
                    .orElse(queryASTNode.getName()));
            columnNode.setFieldName(queryASTNode.getName());
            String aliasForm = columnNode.getFieldName();
            columnNode.setAs(namespace.generate("column", aliasForm));
            columnNode.setArgs(args);
            return columnNode;
        } else {
            return NoopNode.INSTANCE;
        }
    }
    TableNode handleTable(Field queryASTNode,
                          GraphQLType gqlType,
                          JavaMonsterResolver<?> resolver,
                          Map<String, Value> args,
                          GraphQLContext context,
                          boolean grabMany,
                          Map<String, FragmentDefinition> fragments) {
        TableNode tableNode = new TableNode();
        tableNode.setName(Optional.ofNullable(resolver)
                .map(r -> r.sqlTable(args, context))
                .orElse(queryASTNode.getName()));
        // the graphQL field name will be the default alias for the table
        // if that's taken, this function will just add an underscore to the end to make it unique
        tableNode.setAs(namespace.generate("table", queryASTNode.getName()));

        if (resolver.hasOrderBy()) {
            tableNode.setOrderBy(resolver.orderBy().getValue(args, context));
        }

        tableNode.setFieldName(queryASTNode.getName());
        tableNode.setGrabMany(grabMany);
        if (nonNull(resolver.where())) {
            tableNode.setWhere(resolver.where());
        }
        tableNode.setPaginate(resolver.isSQLPaginate());

        if (resolver.hasSQLJoin()) {
            tableNode.setSqlJoin(resolver.sqlJoin());
        } else if (resolver.hasJunction()) {
            Junction junction = resolver.getJunction();
            JunctionNode junctionNode = new JunctionNode();
            junctionNode.setSqlTable(junction.getSQLTable(args, context));
            junctionNode.setAs(namespace.generate("table", junctionNode.getSqlTable()));
            junctionNode.setOrderBy(junction.getOrderBy(args, context));
            junctionNode.setWhere(junction.where());
            if (junction.hasSQLJoins()) {
                junctionNode.setInboundJoin(junction.inboundJoin());
                junctionNode.setOutboundJoin(junction.outboundJoin());

                // TODO SQLBatch
            } else {
                throw new IllegalArgumentException("junction requires either a `sqlJoins` or `sqlBatch`");
            }
            tableNode.setJunction(junctionNode);
        } // TODO SQLBatch

        if (resolver.isSQLPaginate()) {
            handleSortColumns(resolver, tableNode, context);
        }

        if (resolver.isSQLPaginate()) {
            handleColumnsRequiredForPagination(tableNode);
        }
        if(nonNull(queryASTNode.getSelectionSet())) {
            handleSelections(tableNode, queryASTNode.getSelectionSet(), gqlType, context);
        }

        return tableNode;
    }

    void handleSelections(TableNode tableNode,
                          SelectionSet selections,
                          GraphQLType gqlType,
                          GraphQLContext context) {
        for(Selection selection : selections.getSelections()) {
            if (selection instanceof Field) {
                SQLASTNode child = create((Field) selection, gqlType, context);
                tableNode.getChildren().add(child);
            } else if (selection instanceof InlineFragment) {
                // TODO Inline Fragment
            } else if (selection instanceof FragmentSpread) {
                FragmentSpread spread = (FragmentSpread) selection;
                FragmentDefinition fragment = fragments.get(spread.getName());
                String fragmentNameOfType = fragment.getTypeCondition().getName();
                boolean sameType = fragmentNameOfType.equals(((GraphQLNamedType) gqlType).getName());
                Optional<GraphQLNamedOutputType> interfaceType = ((GraphQLObjectType) gqlType).getInterfaces()
                        .stream()
                        .filter(iface -> iface.getName().equals(fragmentNameOfType))
                        .findAny();
                if (sameType || interfaceType.isPresent()) {
                    handleSelections(tableNode,
                            fragment.getSelectionSet(),
                            gqlType,
                            context);
                }
            } else {
                throw new IllegalArgumentException("Unknown selection kind: " + selection.toString());
            }
        }
    }

    ColumnNode columnToASTChild(String columnName) {
        ColumnNode column =  new ColumnNode();
        column.setName(columnName);
        column.setFieldName(columnName);
        column.setAs(namespace.generate("column", columnName));
        return column;
    }

    void handleColumnsRequiredForPagination(TableNode node) {
        if (nonNull(node.getSortKey())) {
            // TODO
        } else if (nonNull(node.getOrderBy())) {
            ColumnNode newChild = columnToASTChild("$total");
            if (nonNull(node.getJunction())) {
                newChild.setFromOtherTable(node.getJunction().getAs());
            }
            node.getChildren().add(newChild);
        }
    }
    void handleSortColumns(JavaMonsterResolver<?> resolver, TableNode tableNode, GraphQLContext context) {
        if (resolver.hasOrderBy()) {
            tableNode.setOrderBy(resolver.orderBy().getValue(tableNode.getArgs(), context));
        }
    }

    GraphQLType stripNonNullType(GraphQLType type) {
        if (type instanceof GraphQLNonNull) {
            return ((GraphQLNonNull) type).getWrappedType();
        } else {
            return type;
        }
    }

    QueryASTParseContext stripRelayConnection(GraphQLObjectType gqlType,
                                              Field queryASTNode) {
        GraphQLType edgeType = stripNonNullType(gqlType.getFieldDefinition("edges").getType());
        GraphQLType strippedType = Optional.of(edgeType)
                .filter(GraphQLList.class::isInstance)
                .map(GraphQLList.class::cast)
                .map(GraphQLList::getWrappedType)
                .filter(GraphQLObjectType.class::isInstance)
                .map(GraphQLObjectType.class::cast)
                .map(type -> type.getFieldDefinition("node"))
                .map(GraphQLFieldDefinition::getType)
                .map(this::stripNonNullType)
                .orElse(null);
        Optional<Field> edge = queryASTNode.getSelectionSet().getSelections()
                .stream()
                .filter(selection -> {
                    if (selection instanceof Field) {
                        return ((Field) selection).getName().equals("edges");
                    } else {
                        return false;
                    }
                })
                .map(Field.class::cast)
                .findAny();
        return new QueryASTParseContext(
                strippedType,
                edge.map(Field::getSelectionSet)
                        .map(SelectionSet::getSelections)
                        .map(selections -> selections.stream()
                                .filter(selection -> {
                                    if (selection instanceof Field) {
                                        return ((Field) selection).getName().equals("node");
                                    } else {
                                        return false;
                                    }
                                })
                                .findAny()
                                .orElse(null))
                        .map(Field.class::cast)
                        .orElse(null)
        );

    }
}
