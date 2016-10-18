package io.vertx.blog.first;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Created by erodriguez on 10/10/16.
 */
@RunWith(VertxUnitRunner.class)
public class MyFirstVerticleTest {

    private final String CONTENT_TYPE_LABEL = "Content-Type";
    private final String CONTENT_TYPE_HEADER = "application/json; charset=utf-8";

    private Vertx vertx;
    private int port;

    @Before
    public void setUp(TestContext context) throws IOException {
        vertx = Vertx.vertx();
        // port = 8081;
        // pick a random port
        ServerSocket socket = new ServerSocket(0);
        port = socket.getLocalPort();
        socket.close();

        DeploymentOptions options = new DeploymentOptions()
                .setConfig(new JsonObject().put("http.port", port)
                        .put("url", "jdbc:hsqldb:mem:test?shutdown=true")
                        .put("driver_class", "org.hsqldb.jdbcDriver")
                );
        vertx.deployVerticle(MyFirstJDBCVerticle.class.getName(), options, context.asyncAssertSuccess());
    }

    @After
    public void tearDown(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void testMyApplication(TestContext context) {
        final Async async = context.async();

        vertx.createHttpClient().getNow(port, "localhost", "/",
                response -> {
                    response.handler(body -> {
                        context.assertTrue(body.toString().contains("Hola"));
                        async.complete();
                    });
                });
    }

    @Test
    public void checkThatTheIndexPageIsServed(TestContext context) {
        Async async = context.async();
        vertx.createHttpClient().getNow(port, "localhost", "/", response -> {
            context.assertEquals(response.statusCode(), 200);
            MultiMap headers = response.headers();
            headers.forEach(header -> System.out.println(header.getKey() + " " + header.getValue()));
            String type = headers.get(CONTENT_TYPE_LABEL);
            System.out.println(type);
            context.assertEquals(response.headers().get(CONTENT_TYPE_LABEL), "text/html");
            response.bodyHandler(body -> {
                context.assertTrue(body.toString().contains("vertriculos"));
                async.complete();
            });
        });
    }

    @Test
    public void checkThatTheIndexPageAssetIsServed(TestContext context) {
        final Async async = context.async();
        vertx.createHttpClient().getNow(port, "localhost", "/assets/index.html", response -> {
            context.assertEquals(response.statusCode(), 200);
            context.assertEquals(response.headers().get(CONTENT_TYPE_LABEL), "text/html");
            response.bodyHandler(body -> {
                context.assertTrue(body.toString().contains("<h1>My Whisky Collection</h1>"));
                async.complete();
            });
        });
    }

    @Test
    public void checkThatWeCanAdd(TestContext context) {
        Async async = context.async();
        final String json = Json.encodePrettily(new Whisky("Jameson", "Ireland"));
        final String length = Integer.toString(json.length());
        vertx.createHttpClient().post(port, "localhost", "/api/whiskies")
                .putHeader(CONTENT_TYPE_LABEL, CONTENT_TYPE_HEADER)
                .putHeader("Content-Length", length)
                .handler(response -> {
                    context.assertEquals(response.statusCode(), 201);
                    context.assertTrue(response.headers().get(CONTENT_TYPE_LABEL).contains("application/json"));
                    response.bodyHandler(body -> {
                        final Whisky whisky = Json.decodeValue(body.toString(), Whisky.class);
                        context.assertEquals(whisky.getName(), "Jameson");
                        context.assertEquals(whisky.getOrigin(), "Ireland");
                        context.assertNotNull(whisky.getId());
                        async.complete();
                    });
                })
                .write(json)
                .end();
    }
}
