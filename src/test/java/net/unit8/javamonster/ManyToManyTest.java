package net.unit8.javamonster;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.Scalars;
import graphql.relay.Relay;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import net.unit8.javamonster.jdbc.StandardJdbcDatabaseCallback;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static graphql.schema.FieldCoordinates.coordinates;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLList.list;
import static net.unit8.javamonster.queryast.Junction.newJunction;
import static org.assertj.core.api.Assertions.assertThat;

public class ManyToManyTest {
    private static final Logger LOG = LoggerFactory.getLogger(HogeTest.class);
    private DataSource dataSource;
    private ObjectMapper mapper = new ObjectMapper();

    private HikariDataSource createDataSource() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:h2:mem:test;DATABASE_TO_LOWER=TRUE");
        hikariConfig.setUsername("sa");
        return new HikariDataSource(hikariConfig);
    }

    @BeforeEach
    void setup() throws SQLException {
        dataSource = createDataSource();
        final Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/manyToMany")
                .load();
        flyway.migrate();

        try(Connection conn = dataSource.getConnection();
            PreparedStatement stmt = conn.prepareStatement("SHOW TABLES");
            final ResultSet rs = stmt.executeQuery()) {
            while(rs.next()) {
                LOG.debug("CREATED TABLE={}", rs.getString(1));
            }
        }
    }

    @Test
    public GraphQL createSchema() {
        DatabaseCallback<List<Map<String, Object>>> dbCall = new StandardJdbcDatabaseCallback(dataSource);
        GraphQLObjectType groupType = GraphQLObjectType.newObject()
                .name("Group")
                .field(newFieldDefinition()
                        .name("id")
                        .type(Scalars.GraphQLID)
                )
                .field(newFieldDefinition()
                        .name("name")
                        .type(Scalars.GraphQLString)
                )
                .build();
        GraphQLObjectType userType = GraphQLObjectType.newObject()
                .name("User")
                .field(newFieldDefinition()
                        .name("id")
                        .type(Scalars.GraphQLID)
                )
                .field(newFieldDefinition()
                        .name("name")
                        .type(Scalars.GraphQLString)
                )
                .field(newFieldDefinition()
                        .name("groups")
                        .type(GraphQLList.list(groupType))
                )
                .build();
        GraphQLObjectType query = GraphQLObjectType.newObject()
                .name("Query")
                .field(newFieldDefinition()
                        .name("users")
                        .type(list(userType))
                )
                .build();

        GraphQLCodeRegistry codeRegistry = GraphQLCodeRegistry.newCodeRegistry()
                .dataFetcher(coordinates("Query", "users"),
                        new JavaMonsterResolver.Builder<>()
                                .sqlTable("users")
                                .dbCall(dbCall)
                                .build())
                .dataFetcher(coordinates("User", "groups"),
                        new JavaMonsterResolver.Builder<>()
                                .junction(newJunction()
                                        .sqlTable("memberships")
                                        .sqlJoins(
                                                (userTable, junctionTable, args, context) ->
                                                        userTable + ".id = " + junctionTable + ".user_id",
                                                (junctionTable, groupTable, args, context) ->
                                                        junctionTable + ".group_id = " + groupTable + ".id"
                                        )
                                        .build()
                                )
                                .build())
                .build();
        GraphQLSchema schema = GraphQLSchema.newSchema()
                .query(query)
                .additionalType(Relay.pageInfoType)
                .codeRegistry(codeRegistry)
                .build();

        return GraphQL.newGraphQL(schema).build();
    }

    @Test
    void manyToMany() throws JsonProcessingException {
        String graphQuery = "query { users { id name groups { id name }}}";
        LOG.info("GraphQL Query={}", graphQuery);
        GraphQL graphql = createSchema();
        ExecutionResult result = graphql.execute(graphQuery);
        if (!result.getErrors().isEmpty()) {
            LOG.warn("{}", result.getErrors());
        } else {
            LOG.info(mapper.writeValueAsString(result.toSpecification()));
            assertThat(result.toSpecification()).containsKeys("data");
            assertThat(result.toSpecification().get("data")).isInstanceOf(Map.class);
            Map<String, Object> data = (Map<String, Object>) result.toSpecification().get("data");
            assertThat(data.get("users")).isInstanceOf(List.class);
            List<Map<String, Object>> users = (List<Map<String, Object>>) data.get("users");
            assertThat(users).hasSize(4);
        }
    }

}
