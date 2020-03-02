package net.unit8.javamonster.sqlast;

public class NoopNode extends SQLASTNodeBase {
    private String type = "noop";

    public static final NoopNode INSTANCE = new NoopNode();

    private NoopNode() {}

    public String getType() {
        return type;
    }
}
