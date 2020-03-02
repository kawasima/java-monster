package net.unit8.javamonster;

import graphql.GraphQLContext;
import graphql.language.*;
import graphql.schema.*;
import net.unit8.javamonster.sqlast.ColumnNode;
import net.unit8.javamonster.sqlast.NoopNode;
import net.unit8.javamonster.sqlast.SQLASTNode;
import net.unit8.javamonster.sqlast.TableNode;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

public class QueryAST {
    private static final Set<Class<? extends GraphQLType>> TABLE_TYPES = Set.of(
            GraphQLObjectType.class,
            GraphQLUnionType.class,
            GraphQLInterfaceType.class
    );

    private GraphQLCodeRegistry codeRegistry;
    private AliasNamespace namespace;

    public QueryAST(GraphQLCodeRegistry codeRegistry) {
        this.codeRegistry = codeRegistry;
        this.namespace = new AliasNamespace(false); // TODO minify
    }

    Field merge(Field dest, Field src) {
        Field newDest = dest.deepCopy();
        newDest.getSelectionSet().getSelections().addAll(src.getSelectionSet().getSelections());
        return newDest;
    }

    List<Field> mergeAll(List<Field> fieldNodes) {
        List<Field> newFieldNodes = new ArrayList<>(fieldNodes);
        while (newFieldNodes.size() > 1) {
            newFieldNodes.add(merge(newFieldNodes.remove(0), newFieldNodes.remove(0)));
        }
        return newFieldNodes;
    }

    public SQLASTNode toSqlAST(DataFetchingEnvironment environment) {
        List<Field> fieldNodes = environment.getMergedField().getFields();
        GraphQLType parentType = environment.getParentType();
        Field queryAST = fieldNodes.get(0);
        SQLASTNode sqlAST = populateASTNode(queryAST, parentType, environment.getContext());
        return sqlAST;
    }

    GraphQLType stripNonNullType(GraphQLType type) {
        if (type instanceof GraphQLNonNull) {
            return ((GraphQLNonNull) type).getWrappedType();
        } else {
            return type;
        }

    }

    SQLASTNode populateASTNode(Field queryASTNode,
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

        if (TABLE_TYPES.contains(gqlType.getClass())) {
            TableNode tableNode = handleTable(queryASTNode, gqlType, (JavaMonsterResolver<?>) dataFetcher, args, context, grabMany);
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
                          boolean grabMany) {
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

        if (resolver.hasSQLJoin()) {
            tableNode.setSqlJoin(resolver.sqlJoin());
        } // TODO many-to-many

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
                SQLASTNode child = populateASTNode((Field) selection, gqlType, context);
                tableNode.getChildren().add(child);
            } else if (selection instanceof InlineFragment) {
                // TODO Inline Fragment
            } else if (selection instanceof FragmentSpread) {
                // TODO Fragment Spread
            }
        }
    }
}
