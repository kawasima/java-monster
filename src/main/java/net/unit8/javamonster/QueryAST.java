package net.unit8.javamonster;

import graphql.GraphQLContext;
import graphql.language.*;
import graphql.schema.*;
import net.unit8.javamonster.queryast.QueryASTFactory;
import net.unit8.javamonster.sqlast.ColumnNode;
import net.unit8.javamonster.sqlast.SQLASTNode;
import net.unit8.javamonster.sqlast.TableNode;

import java.util.*;

import static java.util.Objects.nonNull;

public class QueryAST {
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
        return new QueryASTFactory(codeRegistry, environment.getGraphQLSchema(), environment.getFragmentsByName(), namespace)
                .create(queryAST, parentType, environment.getContext());
    }


}
