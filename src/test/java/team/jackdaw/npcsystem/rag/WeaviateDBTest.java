package team.jackdaw.npcsystem.rag;

import io.weaviate.client.base.Result;
import io.weaviate.client.v1.batch.model.ObjectGetResponse;
import io.weaviate.client.v1.graphql.model.GraphQLResponse;
import io.weaviate.client.v1.misc.model.Meta;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

public class WeaviateDBTest {
    private static final WeaviateDB db = new WeaviateDB("http", "jackdaw-v3:8080");
    @Test
    public void testMain() {
        Result<Meta> meta = db.client.misc().metaGetter().run();
        if (meta.getError() == null) {
            System.out.printf("meta.hostname: %s\n", meta.getResult().getHostname());
            System.out.printf("meta.version: %s\n", meta.getResult().getVersion());
            System.out.printf("meta.modules: %s\n", meta.getResult().getModules());
        } else {
            System.out.printf("Error: %s\n", meta.getError().getMessages());
        }
    }

    @Test
    public void testCreateSchema() {
        boolean res = db.createSchema("Document", "A document schema for RAG");
        System.out.println(res);
    }

    @Test
    public void testDeleteSchema() {
        boolean res = db.deleteSchema("Document", true);
        System.out.println(res);
    }

    @Test
    public void testSchemaObjects() {
        GraphQLResponse res = db.getObjects(100, "Document");
        List<String> objects = WeaviateDB.queryGetText(res, "Document");
        System.out.println(objects);
    }

    @Test
    public void testInsertData() {
        Float[] vector = new Float[768];
        Arrays.fill(vector, 0.1f);
        ObjectGetResponse res = db.insertData("Good Bye, world!", vector, "Document");
        System.out.println(res);
    }

    @Test
    public void testQueryVector() {
        Float[] vector = new Float[768];
        Arrays.fill(vector, 0.1f);
        GraphQLResponse res = db.query(vector, 5, "Document");
        System.out.println(WeaviateDB.queryGetText(res, "Document"));
    }

    @Test
    public void testQueryVectors() {
        Float[] vector = new Float[768];
        Arrays.fill(vector, 0.1f);
        Float[] vector2 = new Float[768];
        Arrays.fill(vector2, 0.2f);
        GraphQLResponse res = db.query(List.of(vector, vector2), 5, "Document");
        System.out.println(WeaviateDB.queryGetText(res, "Document"));
    }
}
