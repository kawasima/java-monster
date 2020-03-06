package net.unit8.javamonster;

import net.unit8.hydration.mapping.ColumnProperty;
import net.unit8.hydration.mapping.PropertyMapping;
import net.unit8.javamonster.sqlast.ColumnNode;
import net.unit8.javamonster.sqlast.NoopNode;
import net.unit8.javamonster.sqlast.SQLASTNode;
import net.unit8.javamonster.sqlast.TableNode;

public class ObjectShape {
    public PropertyMapping defineObjectShape(SQLASTNode topNode) {
        return _defineObjectShape(null, "", topNode);
    }

    private PropertyMapping _defineObjectShape(SQLASTNode parent, String prefix, SQLASTNode node) {
        String prefixToPass = parent != null ? prefix + node.getAs() + "__" : prefix;

        PropertyMapping propertyMapping = node.isGrabMany() ? PropertyMapping.createToMany() : PropertyMapping.createToOne();
        for (SQLASTNode child : node.getChildren()) {
            if (child instanceof ColumnNode) {
                propertyMapping.put(child.getFieldName(),
                        new ColumnProperty(prefixToPass + child.getAs()));
            } else if (child instanceof TableNode) {
                TableNode table = (TableNode) child;
                PropertyMapping childProps = _defineObjectShape(node, prefixToPass, child);
                if (table.isGrabMany()) {
                    propertyMapping.putToMany(table.getFieldName(), childProps);
                } else {
                    propertyMapping.putToOne(table.getFieldName(), childProps);
                }
            } else if (child instanceof NoopNode) {
                // Noop
            }
        }

        return  propertyMapping;
    }
}
