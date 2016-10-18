package io.vertx.blog.first;

import io.vertx.core.json.JsonObject;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by erodriguez on 10/10/16.
 */
public class Whisky {
    //private static final AtomicInteger COUNTER = new AtomicInteger();

    private final int id;
    private String name;
    private String origin;

    public Whisky(String name, String origin) {
        //this.id = COUNTER.getAndIncrement();
        this.id = -1;
        this.name = name;
        this.origin = origin;
    }

    public Whisky(int id, String name, String origin) {
        this.id = id;
        this.name = name;
        this.origin = origin;
    }

    public Whisky(JsonObject json) {
        this.name = json.getString("NAME");
        this.origin = json.getString("ORIGIN");
        this.id = json.getInteger("ID");
    }

    public Whisky() {
        this.id = -1;
    }

    /*public Whisky() {
        this.id = COUNTER.getAndIncrement();
    }*/

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }
}
