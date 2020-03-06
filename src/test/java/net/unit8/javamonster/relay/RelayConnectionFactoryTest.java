package net.unit8.javamonster.relay;

import graphql.language.IntValue;
import graphql.relay.Connection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RelayConnectionFactoryTest {
    RelayConnectionFactory<Map<String, Object>> sat;
    @BeforeEach
    void setUp() {
        sat = new RelayConnectionFactory<>();
    }

    @Test
    void test() {
        Connection<Map<String, Object>> connection = sat.connectionFromArraySlice(
                List.of(
                        Map.of("$total", 100, "country", "Japan"),
                        Map.of("$total", 100, "country", "Germany"),
                        Map.of("$total", 100, "country", "Czech")
                ),
                Map.of("first", new IntValue(new BigInteger("8"))),
                new ArraySliceMetaInfo.Builder()
                        .arrayLength(3)
                        .build()
        );

        assertThat(connection.getEdges().size()).isEqualTo(3);
        assertThat(connection.getPageInfo().isHasNextPage()).isFalse();
        assertThat(connection.getPageInfo().isHasPreviousPage()).isFalse();
    }
}