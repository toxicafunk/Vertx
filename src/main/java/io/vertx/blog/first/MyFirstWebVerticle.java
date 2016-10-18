package io.vertx.blog.first;


import io.netty.util.internal.StringUtil;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by erodriguez on 10/10/16.
 *
 * Run with:
 *  java -jar target/first-vertx-app-1.0-SNAPSHOT-fat.jar -conf src/main/conf/app-conf.json
 */
public class MyFirstWebVerticle extends AbstractVerticle {

    private final String CONTENT_TYPE_LABEL = "Content-Type";
    private final String CONTENT_TYPE_HEADER = "application/json; charset=utf-8";

    // Store our product
    private Map<Integer, Whisky> products = new LinkedHashMap<>();

    private void createSomeData() {
        Whisky bowmore = new Whisky("Bowmore 15 years Laimirg", "Scotland, Islay");
        products.put(bowmore.getId(), bowmore);
        Whisky talisker = new Whisky("Talisker 57º Borth", "Scotland, Islay");
        products.put(talisker.getId(), talisker);
    }

    @Override
    public void start(Future<Void> fut) {

        createSomeData();

        // Create a router object
        Router router = Router.router(vertx);

        // Bind "/" to our message
        router.route("/").handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            response
                    .putHeader(CONTENT_TYPE_LABEL, "text/html")
                    .end("<h1>Hola vertriculos web!</h1>");
        });

        // Serve static resources from the /assets directory
        router.route("/assets/*").handler(StaticHandler.create("assets"));

        // REST API
        router.get("/api/whiskies").handler(this::getAll);
        router.route("/api/whiskies*").handler(BodyHandler.create());
        router.post("/api/whiskies").handler(this::addOne);
        router.get("/api/whiskies/:id").handler(this::getOne);
        router.put("/api/whiskies/:id").handler(this::updateOne);
        router.delete("/api/whiskies/:id").handler(this::deleteOne);



        // Create the HTTP server and pass the "accept" method to the request handler
        vertx
                .createHttpServer()
                .requestHandler(router::accept)
                .listen(
                        config().getInteger("http.port", 8000),
                        result -> {
                            if (result.succeeded()) {
                                fut.complete();
                            } else {
                                fut.fail(result.cause());
                            }
                        }
                );
    }

    private void getOne(RoutingContext routingContext) {
        final String id = routingContext.request().getParam("id");
        if (id == null || !products.containsKey(Integer.valueOf(id))) {
            routingContext.response().setStatusCode(404).end();
        } else {
            Integer idAsInteger = Integer.valueOf(id);
            final Whisky whisky = products.get(idAsInteger);
            routingContext.response()
                    .setStatusCode(200)
                    .putHeader(CONTENT_TYPE_LABEL, CONTENT_TYPE_HEADER)
                    .end(Json.encodePrettily(whisky));
        }
    }

    private void deleteOne(RoutingContext routingContext) {
        String id = routingContext.request().getParam("id");
        if (id == null || id.isEmpty()) {
            routingContext.response().setStatusCode(400).end();
        } else {
            Integer idAsInteger = Integer.valueOf(id);
            products.remove(idAsInteger);
            routingContext.response().setStatusCode(204).end();
        }
    }

    private void addOne(RoutingContext routingContext) {
        final Whisky whisky = Json.decodeValue(routingContext.getBodyAsString(), Whisky.class);
        products.put(whisky.getId(), whisky);
        routingContext.response()
                .setStatusCode(201)
                .putHeader(CONTENT_TYPE_LABEL, CONTENT_TYPE_HEADER)
                .end(Json.encodePrettily(whisky));
    }

    private void getAll(RoutingContext routingContext) {
        routingContext.response()
                .putHeader(CONTENT_TYPE_LABEL, CONTENT_TYPE_HEADER)
                .end(Json.encodePrettily(products.values()));
    }

    private void updateOne(RoutingContext routingContext) {
        final String id = routingContext.request().getParam("id");
        //JsonObject json = routingContext.getBodyAsJson();
        final Whisky whisky = Json.decodeValue(routingContext.getBodyAsString(), Whisky.class);
        if (id == null || whisky == null) {
            routingContext.response().setStatusCode(400).end();
        } else {
            products.put(Integer.valueOf(id), whisky);
            routingContext.response()
                    .putHeader(CONTENT_TYPE_LABEL, CONTENT_TYPE_HEADER)
                    .end(Json.encodePrettily(whisky));
        }
    }
}
