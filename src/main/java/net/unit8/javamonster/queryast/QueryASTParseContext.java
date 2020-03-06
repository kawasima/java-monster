package net.unit8.javamonster.queryast;

import graphql.language.Field;
import graphql.language.Node;
import graphql.schema.GraphQLType;

import java.io.Serializable;

public class QueryASTParseContext implements Serializable {
    private GraphQLType type;
    private Field queryASTNode;

    public QueryASTParseContext(GraphQLType type,
                                Field queryASTNode) {
        this.queryASTNode = queryASTNode;
        this.type = type;
    }

    public GraphQLType getType() {
        return type;
    }

    public Field getQueryASTNode() {
        return queryASTNode;
    }
}
