package net.unit8.javamonster;

import graphql.language.*;

import java.math.BigInteger;
import java.util.Optional;

public class ValueUtils {
    public static Optional<Integer> intValue(Value<?> value) {
        if (value instanceof IntValue) {
            return Optional.of(((IntValue) value).getValue().intValue());
        } else if (value instanceof StringValue) {
            return Optional.of(new BigInteger(((StringValue) value).getValue()).intValue());
        } else if (value instanceof FloatValue) {
            return Optional.of(((FloatValue) value).getValue().intValue());
        }
        return Optional.empty();
    }

    public static Optional<String> stringValue(Value<?> value) {
        if (value instanceof StringValue) {
            return Optional.of(((StringValue) value).getValue());
        } else if (value instanceof IntValue) {
            return Optional.of(((IntValue) value).getValue().toString(10));
        } else if (value instanceof FloatValue) {
            return Optional.of(((FloatValue) value).getValue().toPlainString());
        } else if (value instanceof BooleanValue) {
            return Optional.of(((BooleanValue) value).isValue() ? "true" : "false");
        } else if (value instanceof EnumValue) {
            return Optional.of(((EnumValue) value).getName());
        }
        return Optional.empty();
    }
}
