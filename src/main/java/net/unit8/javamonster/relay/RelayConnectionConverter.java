package net.unit8.javamonster.relay;

import graphql.relay.Connection;
import graphql.relay.DefaultConnection;
import graphql.relay.Relay;
import net.unit8.javamonster.queryast.Junction;
import net.unit8.javamonster.sqlast.JunctionNode;
import net.unit8.javamonster.sqlast.SQLASTNode;
import net.unit8.javamonster.sqlast.TableNode;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.nonNull;

public class RelayConnectionConverter {
    private RelayConnectionFactory relayConnectionFactory;

    public RelayConnectionConverter() {
        relayConnectionFactory = new RelayConnectionFactory();
    }

    public Object arrayToConnection(Object data, SQLASTNode node) {
        for (SQLASTNode child : node.getChildren()) {
            if (data instanceof List) {
                for (Map<String, Object> dataItem : (List<Map<String, Object>>) data) {
                    recurseOnObjInData(dataItem, child);
                }
            } else if (data != null) {
                recurseOnObjInData((Map<String, Object>) data, child);
            }
        }

        if (!(node instanceof TableNode)) {
            return data;
        }

        TableNode tableNode = (TableNode) node;
        if (tableNode.isPaginate()) { // TODO data._paginated = false
            if (nonNull(tableNode.getSortKey()) || Optional.ofNullable(tableNode.getJunction()).map(JunctionNode::getSortKey).isPresent()) {

            }

            if (nonNull(tableNode.getOrderBy()) || Optional.ofNullable(tableNode.getJunction()).map(JunctionNode::getOrderBy).isPresent()) {
                BigInteger offset = BigInteger.ZERO;
                if (tableNode.getArgs().containsKey("after")) {
                    Relay.ResolvedGlobalId after = new Relay().fromGlobalId(tableNode.getArgs().get("after").toString());
                    offset = new BigInteger(after.getId());
                }
                int arrayLength = calcArrayLength(data);
                Connection<Map<String, Object>> connection = relayConnectionFactory.connectionFromArraySlice(
                        (List<Map<String, Object>>) data,
                        node.getArgs(),
                        new ArraySliceMetaInfo.Builder()
                                .sliceStart(offset.intValue())
                                .arrayLength(arrayLength)
                                .build()
                );
                return connection;
            }
        }
        return data;
    }

    private int calcArrayLength(Object data) {
        if (data instanceof List) {
            List<Map<String, Object>> array = (List<Map<String, Object>>) data;
            if (!array.isEmpty()) {
                Object total = array.get(0).get("$total");
                if (total instanceof Number) {
                    return ((Number) total).intValue();
                } else if (total != null) {
                    return new BigInteger(total.toString()).intValue();
                }
            }
        }
        return 0;
    }

    private void recurseOnObjInData(Map<String, Object> dataObj, SQLASTNode child) {
        Object dataChild = dataObj.get(child.getFieldName());
        if (dataChild != null) {
            dataObj.put(child.getFieldName(),
                    arrayToConnection(dataChild, child));
        }
    }
}
