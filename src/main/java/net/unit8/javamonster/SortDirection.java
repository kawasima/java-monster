package net.unit8.javamonster;

public enum SortDirection {
    ASC('>'),
    DESC('<');

    private char operator;
    SortDirection(char operator) {
        this.operator = operator;
    }

    public SortDirection flip() {
        if (this == ASC) {
            return DESC;
        } else {
            return ASC;
        }
    }

    public char sqlOperator() {
        return operator;
    }
}
