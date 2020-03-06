package net.unit8.javamonster.stringifiers.dialects;

public class SQLValue {
    private Object value;
    private Dialect dialect;

    public SQLValue(Object value, Dialect dialect) {
        this.value = value;
        this.dialect = dialect;
    }

    @Override
    public String toString() {
        if (value == null) {
            return "NULL";
        }

        if (value instanceof Number) {
            return value.toString();
        }

        boolean hasBackslash = false;
        StringBuilder escaped = new StringBuilder("'");

        for (char c : value.toString().toCharArray()) {
            if (c == '\'') {
                escaped.append("''");
            } else if (c == '\\') {
                escaped.append("\\\\");
                hasBackslash = true;
            } else {
                escaped.append(c);
            }
        }
        escaped.append('\'');

        if (hasBackslash) {
            return " E" + escaped.toString();
        } else {
            return escaped.toString();
        }
    }
}
