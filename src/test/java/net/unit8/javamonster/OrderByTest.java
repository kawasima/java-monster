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
import static net.unit8.javamonster.SortDirection.ASC;
import static net.unit8.javamonster.SortDirection.DESC;

public class OrderByTest {
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
                .locations("classpath:db/migration/orderBy")
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
        GraphQLObjectType commentType = GraphQLObjectType.newObject()
                .name("Comment")
                .field(newFieldDefinition()
                        .name("id")
                        .type(Scalars.GraphQLID)
                )
                .field(newFieldDefinition()
                        .name("body")
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
                        .name("email")
                        .type(Scalars.GraphQLString)
                )
                .field(newFieldDefinition()
                        .name("comments")
                        .type(list(commentType))
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
                                .sqlTable("accounts")
                                .dbCall(dbCall)
                                .build())
                .dataFetcher(coordinates("User", "comments"),
                        new JavaMonsterResolver.Builder<>()
                                .sqlJoin((userTable, commentTable, args, context) ->
                                        userTable + ".id = " + commentTable + ".author_id")
                                .orderBy(
                                        new OrderColumn("body", ASC),
                                        new OrderColumn("id", DESC)
                                        )
                                .build())
                .dataFetcher(coordinates("User", "email"),
                        new JavaMonsterResolver.Builder<>()
                                .sqlColumn("email_address")
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
    void orderBy() throws JsonProcessingException {
        String graphQuery = "query { users { email comments { id body }}}";
        LOG.info("GraphQL Query={}", graphQuery);
        GraphQL graphql = createSchema();
        ExecutionResult result = graphql.execute(graphQuery);
        if (!result.getErrors().isEmpty()) {
            LOG.warn("{}", result.getErrors());
        } else {
            LOG.info(mapper.writeValueAsString(result.toSpecification()));
        }
    }
}
