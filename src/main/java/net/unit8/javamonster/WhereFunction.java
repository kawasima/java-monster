package net.unit8.javamonster;

import graphql.GraphQLContext;
import graphql.language.Value;

import java.util.Map;

@FunctionalInterface
public interface WhereFunction {
    String apply(String table, Map<String, Value> args, GraphQLContext context);
}
