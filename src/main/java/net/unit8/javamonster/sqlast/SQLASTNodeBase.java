package net.unit8.javamonster.sqlast;

import graphql.language.Value;

import java.util.Collections;
import java.util.Map;

public class SQLASTNodeBase implements SQLASTNode {
    private Map<String, Value> args;

    @Override
    public Map<String, Value> getArgs() {
        return args != null ? args : Collections.emptyMap();
    }

    @Override
    public void setArgs(Map<String, Value> args) {
        this.args = args;
    }
}
