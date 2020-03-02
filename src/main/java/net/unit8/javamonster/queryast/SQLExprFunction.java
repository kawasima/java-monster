package net.unit8.javamonster.queryast;

import graphql.GraphQLContext;
import graphql.language.Value;
import net.unit8.javamonster.sqlast.SQLASTNode;

import java.util.Map;

@FunctionalInterface
public interface SQLExprFunction {
    String apply(String table, Map<String, Value> args, GraphQLContext context, SQLASTNode node);
}
