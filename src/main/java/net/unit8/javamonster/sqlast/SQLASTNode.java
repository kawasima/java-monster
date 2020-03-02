package net.unit8.javamonster.sqlast;

import graphql.language.Value;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public interface SQLASTNode {
    Map<String, Value> getArgs();
    void setArgs(Map<String, Value> args);
    default String getAs() {
        return "";
    }

    default List<SQLASTNode> getChildren() {
        return Collections.emptyList();
    }

    default String getFieldName() {
        return "";
    }

    default boolean isGrabMany() {
        return false;
    }
}
