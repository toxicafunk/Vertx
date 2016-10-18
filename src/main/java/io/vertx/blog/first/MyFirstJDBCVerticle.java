package io.vertx.blog.first;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import java.util.List;
import java.util.stream.Collectors;

/**
 * https://github.com/cescoffier/my-vertx-first-app
 * Created by erodriguez on 18/10/16.
 */
public class MyFirstJDBCVerticle extends MyFirstWebVerticle {

    private JDBCClient jdbc;
    private final static String CreateWhiskyTableSQL = "CREATE TABLE IF NOT EXISTS Whisky (id INTEGER IDENTITY, name varchar(100), origin varchar(100))";

    @Override
    public void start(Future<Void> fut) {
        jdbc = JDBCClient.createShared(vertx, config(), "My-Whisky-Collection");

        startBackend(
                (connection) -> createSomeData(connection,
                        (nothing) -> startWebApp(
                                (http) -> completeStartup(http, fut)
                        ), fut
                ), fut
        );
    }

    private void startWebApp(Handler<AsyncResult<HttpServer>> next) {
        Router router = configRoutes();
        // Create the HTTP server and pass the "accept" method to the request handler.
        vertx
                .createHttpServer()
                .requestHandler(router::accept)
                .listen(
                        // Retrieve the port from the configuration,
                        // default to 8080.
                        config().getInteger("http.port", 8080),
                        next::handle
                );
    }

    private void startBackend(Handler<AsyncResult<SQLConnection>> next, Future<Void> fut) {
        jdbc.getConnection(ar -> {
            if (ar.failed()) {
                fut.fail(ar.cause());
            } else {
                next.handle(Future.succeededFuture(ar.result()));
            }
        });
    }

    private void completeStartup(AsyncResult<HttpServer> http, Future<Void> fut) {
        if (http.succeeded()) {
            fut.complete();
        } else {
            fut.fail(http.cause());
        }
    }


    protected void createSomeData(AsyncResult<SQLConnection> result, Handler<AsyncResult<Void>> next, Future<Void> fut) {
        if (result.failed()) {
            fut.fail(result.cause());
        } else {
            SQLConnection connection = result.result();
            connection.execute(CreateWhiskyTableSQL,
                    ar -> {
                        if (ar.failed()) {
                            fut.fail(ar.cause());
                            connection.close();
                            return;
                        }
                        connection.query("SELECT * FROM Whisky",
                                select -> {
                                    if (select.failed()) {
                                        fut.fail(ar.cause());
                                        connection.close();
                                        return;
                                    }
                                    if (select.result().getNumRows() == 0) {
                                        insert(
                                                new Whisky("Bowmore 15 years Laimirg", "Scotland, Islay"),
                                                connection,
                                                (v) -> insert(new Whisky("Talisker 57º Borth", "Scotland, Island"),
                                                        connection,
                                                        (r) -> {
                                                            next.handle(Future.<Void>succeededFuture());
                                                            connection.close();
                                                        })
                                        );
                                    } else {
                                        next.handle(Future.<Void>succeededFuture());
                                        connection.close();
                                    }
                                });
                    }
            );
        }
    }

    private void insert(Whisky whisky, SQLConnection connection, Handler<AsyncResult<Whisky>> next) {
        String sql = "INSERT INTO Whisky (name, origin) VALUES ?, ?";
        connection.updateWithParams(sql,
                new JsonArray().add(whisky.getName()).add(whisky.getOrigin()),
                (ar) -> {
                    if (ar.failed()) {
                        next.handle(Future.failedFuture(ar.cause()));
                        return;
                    }
                    UpdateResult result = ar.result();
                    // Build a new whisky instance with the generated id.
                    Whisky w = new Whisky(result.getKeys().getInteger(0), whisky.getName(), whisky.getOrigin());
                    next.handle(Future.succeededFuture(w));
                });
    }

    @Override
    protected void getAll(RoutingContext routingContext) {
        jdbc.getConnection(ar -> {
            SQLConnection connection = ar.result();
            connection.query("SELECT * from Whisky", result -> {
                List<Whisky> whiskies = result.result().getRows().stream().map(Whisky::new).collect(Collectors.toList());
                routingContext.response()
                        .putHeader(CONTENT_TYPE_LABEL, CONTENT_TYPE_HEADER)
                        .end(Json.encodePrettily(whiskies));
                connection.close();
            });
        });
    }
}
