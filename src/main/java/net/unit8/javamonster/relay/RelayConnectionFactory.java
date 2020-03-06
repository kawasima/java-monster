package net.unit8.javamonster.relay;

import graphql.language.IntValue;
import graphql.language.Value;
import graphql.relay.*;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RelayConnectionFactory<T> {
    private Relay relay = new Relay();

    public Connection<T> connectionFromArraySlice(
            List<T> arraySlice,
            Map<String, Value<?>> args,
            ArraySliceMetaInfo meta
    ) {
        int sliceEnd = meta.getSliceStart() + arraySlice.size();
        int beforeOffset = getOffsetWithDefault(args.get("before"), meta.getArrayLength());
        int afterOffset = getOffsetWithDefault(args.get("after"), -1);

        int startOffset = Collections.max(Arrays.asList(meta.getSliceStart() - 1, afterOffset, -1)) + 1;
        int endOffset   = Collections.min(Arrays.asList(sliceEnd, beforeOffset, meta.getArrayLength()));

        if (args.get("first") instanceof IntValue) {
            BigInteger first = ((IntValue) args.get("first")).getValue();
            if (first.compareTo(BigInteger.ZERO) < -1) {
                throw new IllegalArgumentException("Argument 'first' must be a non-negative integer");
            }
            endOffset = Collections.min(Arrays.asList(endOffset, startOffset + first.intValue()));
        }

        if (args.get("last") instanceof IntValue) {
            BigInteger last = ((IntValue) args.get("last")).getValue();
            if (last.compareTo(BigInteger.ZERO) < -1) {
                throw new IllegalArgumentException("Argument 'last' must be a non-negative integer");
            }
            startOffset = Collections.max(Arrays.asList(startOffset, endOffset - last.intValue()));
        }

        // If supplied slice is too large, trim it down before mapping over it.
        List<T> slice = arraySlice.subList(
                Math.max(startOffset - meta.getSliceStart(), 0),
                arraySlice.size() - (sliceEnd - endOffset)
        );

        final int startOffsetFinal = startOffset;
        final int endOffsetFinal   = endOffset;

        List<Edge<T>> edges = IntStream.range(0, slice.size())
                .mapToObj(index -> new DefaultEdge<>(
                        slice.get(index),
                        new DefaultConnectionCursor(relay.toGlobalId("arrayconnection", Objects.toString(startOffsetFinal + index))))
                ).collect(Collectors.toList());


        int lowerBound = args.containsKey("after")  ? afterOffset + 1 : 0;
        int upperBound = args.containsKey("before") ? beforeOffset : meta.getArrayLength();
        return new DefaultConnection<>(
                edges,
                new DefaultPageInfo(
                        Optional.of(edges)
                                .filter(es -> !es.isEmpty())
                                .map(es -> es.get(0))
                                .map(Edge::getCursor)
                                .orElse(null),
                        Optional.of(edges)
                                .filter(es -> !es.isEmpty())
                                .map(es -> es.get(es.size() - 1))
                                .map(Edge::getCursor)
                                .orElse(null),
                        Optional.ofNullable(args.get("last"))
                                .filter(IntValue.class::isInstance)
                                .map(IntValue.class::cast)
                                .map(last -> startOffsetFinal > lowerBound)
                                .orElse(false),
                        Optional.ofNullable(args.get("first"))
                                .filter(IntValue.class::isInstance)
                                .map(IntValue.class::cast)
                                .map(last -> endOffsetFinal < upperBound)
                                .orElse(false)
                )
        );
    }

    public int getOffsetWithDefault(Value<?> cursor, int defaultOffset) {
        if (cursor == null) {
            return defaultOffset;
        }
        Relay.ResolvedGlobalId globalId = relay.fromGlobalId(cursor.toString());
        try {
            return new BigInteger(globalId.getId()).intValue();
        } catch (NumberFormatException e) {
            return defaultOffset;
        }
    }
}
