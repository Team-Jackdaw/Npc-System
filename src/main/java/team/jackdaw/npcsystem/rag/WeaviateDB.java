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
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class WeaviateDB {
    public WeaviateClient client;

    public WeaviateDB(Config config) {
        this.client = new WeaviateClient(config);
    }

    public boolean createSchema() {
        WeaviateClass documentClass = WeaviateClass.builder()
                .className("Document")
                .description("A document schema for RAG")
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

    public WeaviateClass getSchema() {
        return client.schema().classGetter().withClassName("Document").run().getResult();
    }

    public boolean deleteSchema(boolean areYouSure) {
        if (!areYouSure) {
            return false;
        }
        return client.schema().classDeleter().withClassName("Document").run().getResult();
    }

    public ObjectGetResponse insertData(String text, Float[] vector) {
        ObjectsBatcher batcher = client.batch().objectsBatcher();
        return batcher.withObject(WeaviateObject.builder()
                        .className("Document")
                        .properties(Map.of("text", text))
                        .vector(vector)
                        .build())
                .run().getResult()[0];
    }

    public ObjectGetResponse[] insertData(@NotNull List<String> texts, List<Float[]> vectors) {
        ObjectsBatcher batcher = client.batch().objectsBatcher();
        for (int i = 0; i < texts.size(); i++) {
            batcher.withObject(WeaviateObject.builder()
                            .className("Document")
                            .properties(Map.of("text", texts.get(i)))
                            .vector(vectors.get(i))
                            .build());
        }
        return batcher.run().getResult();
    }

    public GraphQLResponse query(@NotNull Float[] vector, int limit) {
        NearVectorArgument nearVector = NearVectorArgument.builder()
                .vector(vector)
                .build();

        Fields fields = Fields.builder()
                .fields(new Field[]{
                        Field.builder().name("text").build(),
                })
                .build();

        String query = GetBuilder.builder()
                .className("Document")
                .fields(fields)
                .withNearVectorFilter(nearVector)
                .limit(limit)
                .build()
                .buildQuery();

        return client.graphQL().raw().withQuery(query).run().getResult();
    }

    public GraphQLResponse query(@NotNull List<Float[]> vectors, int limit) {
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
                .className("Document")
                .fields(fields)
                .withNearVectorFilter(vector)
                .limit(limit)
                .build()
                .buildQuery();

        return client.graphQL().raw().withQuery(query).run().getResult();
    }

    public GraphQLResponse getObjects(int num) {
        Fields fields = Fields.builder()
                .fields(new Field[]{
                        Field.builder().name("text").build(),
                })
                .build();

        String query = GetBuilder.builder()
                .className("Document")
                .fields(fields)
                .limit(num)
                .build()
                .buildQuery();

        return client.graphQL().raw().withQuery(query).run().getResult();
    }

    public static List<String> queryGetText(GraphQLResponse res) {
        return QueryGet.fromResponse(res).Get.Document.stream().map(c -> c.text).toList();
    }

    static class QueryGet {
        Get Get;
        static class Get {
            List<Chunk> Document;
            static class Chunk {
                String text;
            }
        }
        static QueryGet fromResponse(@NotNull GraphQLResponse res) {
            return new Gson().fromJson(new Gson().toJson(res.getData()), QueryGet.class);
        }
    }
}
