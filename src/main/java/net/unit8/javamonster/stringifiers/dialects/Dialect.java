package net.unit8.javamonster.stringifiers.dialects;

import graphql.GraphQLContext;
import net.unit8.javamonster.sqlast.SQLASTNode;
import net.unit8.javamonster.sqlast.TableNode;

import java.util.List;

public interface Dialect {
    String getName();
    String quote(String str);
    String compositeKey(String parent, List<String>keys);

    default String handleJoinedOneToManyPaginated(
            SQLASTNode parent,
            SQLASTNode node,
            GraphQLContext context,
            String joinCondition) {
        throw new UnsupportedOperationException("one-to-many pagination is unsupported");
    }

    default String handleJoinedManyToManyPaginated(
            SQLASTNode parent,
            SQLASTNode node,
            GraphQLContext context,
            String inboundJoinCondition,
            String outboundJoinCondition) {
        throw new UnsupportedOperationException("one-to-many pagination is unsupported");
    }

    default String handlePaginationAtRoot(SQLASTNode parent, TableNode node, GraphQLContext context) {
        return null;
    }
}
