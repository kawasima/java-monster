package net.unit8.javamonster.queryast;

import graphql.GraphQLContext;
import graphql.language.Value;

import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

public class ThunkWithArgsCtx<T> {
    private BiFunction<Map<String, Value>, GraphQLContext, T> func;
    private T value;

    public ThunkWithArgsCtx(BiFunction<Map<String, Value>, GraphQLContext, T> func) {
        this.func = func;
    }

    public ThunkWithArgsCtx(T value) {
        this.value = value;
    }

    public T getValue(Map<String, Value> args, GraphQLContext context) {
        if (Objects.nonNull(this.func)) {
            return this.func.apply(args, context);
        } else {
            return this.value;
        }
    }

}
