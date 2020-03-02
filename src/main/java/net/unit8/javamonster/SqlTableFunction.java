package net.unit8.javamonster;

import graphql.language.Value;

import java.util.Map;

public interface SqlTableFunction {
    String apply(Map<String, Value> args);
}
