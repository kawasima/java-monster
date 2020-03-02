package net.unit8.javamonster.sqlast;

public class ColumnNode extends SQLASTNodeBase {
    private String type = "column";
    private String name;
    private String fieldName;
    private String as;

    public String getName() {
        return name;
    }

    @Override
    public String getFieldName() {
        return fieldName;
    }

    @Override
    public String getAs() {
        return as;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public void setAs(String as) {
        this.as = as;
    }
}
