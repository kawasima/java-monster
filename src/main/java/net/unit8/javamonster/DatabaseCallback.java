package net.unit8.javamonster;

import java.sql.SQLException;

@FunctionalInterface
public interface DatabaseCallback<T> {
    T apply(String sql) throws SQLException;
}
