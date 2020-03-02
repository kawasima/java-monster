package net.unit8.javamonster;

import graphql.language.Value;

import java.util.Map;

@FunctionalInterface
public interface SQLJoinFunction {
    String apply(String parentTable,
                 String targetTable,
                 Map<String, Value> args);
}
