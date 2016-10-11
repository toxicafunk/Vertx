package io.vertx.blog.first;


import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;

/**
 * Created by erodriguez on 10/10/16.
 *
 * Run with:
 *  java -jar target/first-vertx-app-1.0-SNAPSHOT-fat.jar -conf src/main/conf/app-conf.json
 */
public class MyFirstVerticle extends AbstractVerticle {

    @Override
    public void start(Future<Void> fut) {
        vertx
                .createHttpServer()
                .requestHandler(r -> r.response().end("<h1>Hola vertriculos!</h1>"))
                .listen(
                        config().getInteger("http.port", 8000),
                        result -> {
                            if (result.succeeded()) {
                                fut.complete();
                            } else {
                                fut.fail(result.cause());
                            }
                        });
    }
}
