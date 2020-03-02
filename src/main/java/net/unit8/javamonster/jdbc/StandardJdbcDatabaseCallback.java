package net.unit8.javamonster.jdbc;

import net.unit8.javamonster.DatabaseCallback;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StandardJdbcDatabaseCallback implements DatabaseCallback<List<Map<String, Object>>> {
    private final DataSource dataSource;

    public StandardJdbcDatabaseCallback(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<Map<String, Object>> apply(String sql) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            final ResultSet rs = stmt.executeQuery();
            List<Map<String, Object>> result = new ArrayList<>();
            int cols = rs.getMetaData().getColumnCount();
            List<String> columnNames = new ArrayList<>(cols);
            for (int i=0; i<cols; i++) {
                columnNames.add(rs.getMetaData().getColumnLabel(i+1));
            }
            while(rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i=0; i<cols; i++) {
                    row.put(columnNames.get(i), rs.getObject(i+1));
                }
                result.add(row);
            }

            return result;
        }
    }
}
