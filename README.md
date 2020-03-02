# java-monster

This is [Join Monster](https://github.com/acarl005/join-monster) clone in Java.
Efficient query planning and data fetching for SQL.
Use JOINs and/or batched requests to retrieve all your data. It takes a GraphQL query and your schema and automatically generates the SQL. Send that SQL to your database and get back all the data needed to resolve with only one or a few round-trips to the database.

## Usage with GraphQL

Define the GraphQL schema using by graphql-java and `JavaMonsterResolver`.
JavaMonsterResolver extends the standard GraphQL Type for generating a JOIN sql.

```java
GraphQLCodeRegistry codeRegistry = GraphQLCodeRegistry.newCodeRegistry()
    .dataFetcher(coordinates("Query", "users"),
        new JavaMonsterResolver.Builder<>()
            .sqlTable("user_table")
            .dbCall(dbCall)
            .build())
    .dataFetcher(coordinates("User", "user_addresses"),
        new JavaMonsterResolver.Builder<>()
            .sqlTable("user_address_table")
            .sqlJoin((userTable, userAddressTable, args) ->
                userTable + ".id=" + userAddressTable + ".user_id"
            )
            .build())
.build();

GraphQLSchema schema = GraphQLSchema.newSchema()
    .query(query)
    .codeRegistry(codeRegistry)
    .build();
```

If you execute a query using by the above schema, you can get the result as following.

Query:

```graphql
query {
  users {
    name
    user_addresses {
      address
    }
  }
}
```

Response:

```json
{
  "data": {
    "users":[
      {
        "name":"AAA",
        "user_addresses":[
          {
            "address":"Tokyo"
          },
          {
            "address":"Osaka"
          }
        ]
      },
      {
        "name":"BBB",
        "user_addresses":[]
      },
      {
        "name":"CCC",
        "user_addresses":[]
      }
    ]
  }
}
```