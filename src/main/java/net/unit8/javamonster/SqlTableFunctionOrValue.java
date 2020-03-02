package net.unit8.javamonster;

import graphql.language.Value;

import java.util.Map;

import static java.util.Objects.nonNull;

public class SqlTableFunctionOrValue {
    private SqlTableFunction func;
    private String value;

    public SqlTableFunctionOrValue(SqlTableFunction func) {
        this.func = func;
    }

    public SqlTableFunctionOrValue(String value) {
        this.value = value;
    }

    public String getValue(Map<String, Value> args) {
        if (nonNull(func)) {
            return func.apply(args);
        } else {
            return value;
        }
    }
}
