package net.unit8.javamonster.stringifiers.dialects;

import java.awt.image.AreaAveragingScaleFilter;
import java.util.ArrayDeque;
import java.util.List;

class RecursiveWhereJoinHelper {
    private List<String> columns;
    private List<String> values;
    private char operator;

    public RecursiveWhereJoinHelper(List<String> columns, List<String> values, char operator) {
        this.columns = columns;
        this.values = values;
        this.operator = operator;
    }

    public String toWhereString() {
        ArrayDeque<String> cols = new ArrayDeque<>(columns);
        ArrayDeque<String> vals = new ArrayDeque<>(values);
        String condition = cols.pop() + " " + operator + " " + vals.pop();
        return _toWhereString(cols, vals, condition);
    }

    private String _toWhereString(ArrayDeque<String> cols, ArrayDeque<String> vals, String condition) {
        if (columns.isEmpty()) {
            return condition;
        }
        String column = cols.pop();
        String value = vals.pop();
        return _toWhereString(cols, vals, "("
                + column + " " + operator + " " + value
                + " OR (" + column + " = " + value + " AND " + condition
                + "))");
    }
}
