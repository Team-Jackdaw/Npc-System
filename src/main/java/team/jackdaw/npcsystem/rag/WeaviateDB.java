package team.jackdaw.npcsystem.rag;

import com.google.gson.Gson;
import io.weaviate.client.Config;
import io.weaviate.client.WeaviateClient;
import io.weaviate.client.v1.batch.api.ObjectsBatcher;
import io.weaviate.client.v1.batch.model.ObjectGetResponse;
import io.weaviate.client.v1.data.model.WeaviateObject;
import io.weaviate.client.v1.graphql.model.GraphQLResponse;
import io.weaviate.client.v1.graphql.query.argument.NearVectorArgument;
import io.weaviate.client.v1.graphql.query.builder.GetBuilder;
import io.weaviate.client.v1.graphql.query.fields.Field;
import io.weaviate.client.v1.graphql.query.fields.Fields;
import io.weaviate.client.v1.schema.model.Property;
import io.weaviate.client.v1.schema.model.WeaviateClass;

import java.util.List;
import java.util.Map;

class WeaviateDB {
    WeaviateClient client;

    WeaviateDB(Config config) {
        this.client = new WeaviateClient(config);
    }

    WeaviateDB(String scheme, String host) {
        this(new Config(scheme, host));
    }

    /**
     * Create a schema that will be used to store the text data and its embedded vector.
     * @param name The name of the schema
     * @param description The description of the schema
     * @return true if the schema is created successfully, false otherwise
     */
    boolean createSchema(String name, String description) {
        WeaviateClass documentClass = WeaviateClass.builder()
                .className(name)
                .description(description)
                .properties(List.of(
                        Property.builder()
                                .name("text")
                                .dataType(List.of("text"))
                                .description("Text")
                                .build()
                ))
                .build();
        return client.schema().classCreator().withClass(documentClass).run().getResult();
    }

    /**
     * Delete a schema by its class name.
     * @param className The name of the schema
     * @param areYouSure If true, the schema will be deleted
     * @return true if the schema is deleted successfully, false otherwise
     */
    boolean deleteSchema(String className, boolean areYouSure) {
        if (!areYouSure) {
            return false;
        }
        return client.schema().classDeleter().withClassName(className).run().getResult();
    }

    /**
     * Insert an object into the schema.
     * @param text The text data
     * @param vector The embedded vector of the text
     * @param className The name of the schema
     * @return The response of the insertion
     */
    ObjectGetResponse insertData(String text, Float[] vector, String className) {
        ObjectsBatcher batcher = client.batch().objectsBatcher();
        return batcher.withObject(WeaviateObject.builder()
                        .className(className)
                        .properties(Map.of("text", text))
                        .vector(vector)
                        .build())
                .run().getResult()[0];
    }

    /**
     * Insert multiple objects into the schema.
     * @param texts The list of text data
     * @param vectors The list of embedded vectors of the text
     * @param className The name of the schema
     * @return The response of the insertion
     */
    ObjectGetResponse[] insertData(List<String> texts, List<Float[]> vectors, String className) {
        ObjectsBatcher batcher = client.batch().objectsBatcher();
        for (int i = 0; i < texts.size(); i++) {
            batcher.withObject(WeaviateObject.builder()
                            .className(className)
                            .properties(Map.of("text", texts.get(i)))
                            .vector(vectors.get(i))
                            .build());
        }
        return batcher.run().getResult();
    }

    /**
     * Query the schema by a vector. The query will return the text data but query by the similarity of the vector.
     * @param vector The vector to query
     * @param limit The number of results to return
     * @param className The name of the schema
     * @return The response of the query
     */
    GraphQLResponse query(Float[] vector, int limit, String className) {
        NearVectorArgument nearVector = NearVectorArgument.builder()
                .vector(vector)
                .build();

        Fields fields = Fields.builder()
                .fields(new Field[]{
                        Field.builder().name("text").build(),
                })
                .build();

        String query = GetBuilder.builder()
                .className(className)
                .fields(fields)
                .withNearVectorFilter(nearVector)
                .limit(limit)
                .build()
                .buildQuery();

        return client.graphQL().raw().withQuery(query).run().getResult();
    }

    /**
     * Query the schema by multiple vectors. The query will return the text data but query by the similarity of the vectors.
     * @param vectors The list of vectors to query
     * @param limit The number of results to return
     * @param className The name of the schema
     * @return The response of the query
     */
    GraphQLResponse query(List<Float[]> vectors, int limit, String className) {
        NearVectorArgument.NearVectorArgumentBuilder nearVectorBuilder = NearVectorArgument.builder();
        for (Float[] vector : vectors) {
            nearVectorBuilder.vector(vector);
        }
        NearVectorArgument vector = nearVectorBuilder.build();

        Fields fields = Fields.builder()
                .fields(new Field[]{
                        Field.builder().name("text").build(),
                })
                .build();

        String query = GetBuilder.builder()
                .className(className)
                .fields(fields)
                .withNearVectorFilter(vector)
                .limit(limit)
                .build()
                .buildQuery();

        return client.graphQL().raw().withQuery(query).run().getResult();
    }

    /**
     * Get a number of objects from the schema.
     * @param num The number of objects to get
     * @param className The name of the schema
     * @return The response of the query
     */
    GraphQLResponse getObjects(int num, String className) {
        Fields fields = Fields.builder()
                .fields(new Field[]{
                        Field.builder().name("text").build(),
                })
                .build();

        String query = GetBuilder.builder()
                .className(className)
                .fields(fields)
                .limit(num)
                .build()
                .buildQuery();

        return client.graphQL().raw().withQuery(query).run().getResult();
    }

    /**
     * Get the text data from the query response.
     * @param res The response of the query
     * @return The list of text data
     */
    static List<String> queryGetText(GraphQLResponse res, String className) {
        Map<String, Map<String, List<Map<String, String>>>> get = new Gson().fromJson(new Gson().toJson(res.getData()), Map.class);
        return get.get("Get").get(className).stream().map(c -> c.get("text")).toList();
    }
}
