package io.vertx.blog.first;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import io.vertx.core.json.Json;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.*;
import static org.hamcrest.Matchers.equalTo;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by erodriguez on 18/10/16.
 */
public class MyRestIT {

    /*
     * We may need to wait in the configureRestAssured method that the HTTP server has been started.
     * We recommend the awaitility test framework to check that the request can be served. It would
     * fail the test if the server does not start.
     */
    @BeforeClass
    public static void configureRestAssured() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = Integer.getInteger("http.port", 8000);
    }

    @AfterClass
    public static void unconfigureRestAssured() {
        RestAssured.reset();
    }

    @Test
    public void checkThatWeCanRetrieveIndividualProduct() {
        // Get the list of bottles, ensure it's a success and extract the first id.
        Response response = get("/api/whiskies");
        System.out.println("response = " + response.asString());
        final int id = response.then()
                .assertThat()
                .statusCode(200)
                .extract()
                .jsonPath().getInt("find { it.name == 'Bowmore 15 years Laimirg' }.id");

        System.out.println("id = " + id);

        // Now get the individual resource and check the content
        get("/api/whiskies/" + id).then()
                .assertThat()
                .statusCode(200)
                .body("name", equalTo("Bowmore 15 years Laimirg"))
                .body("origin", equalTo("Scotland, Islay"))
                .body("id", equalTo(id));
    }

    @Test
    public void checkWeCanAddAndDeleteAProduct() {
        // Create a new bottle and retrieve the result (as a Whisky instance).
        Whisky whisky = given()
                .body("{\"name\":\"Jameson\", \"origin\":\"Ireland\"}")
                .request().post("/api/whiskies")
                .thenReturn().as(Whisky.class);
        assertThat(whisky.getName()).isEqualToIgnoringCase("Jameson");
        assertThat(whisky.getOrigin()).isEqualToIgnoringCase("Ireland");
        assertThat(whisky.getId()).isNotZero();

        System.out.println("new Id = " + whisky.getId());

        // Check that it has created an individual resource, and check the content.
        get("/api/whiskies/" + whisky.getId()).then()
                .assertThat()
                .statusCode(200)
                .body("name", equalTo("Jameson"))
                .body("origin", equalTo("Ireland"))
                .body("id", equalTo(whisky.getId()));

        // Delete the bottle
        delete("/api/whiskies/" + whisky.getId()).then().assertThat().statusCode(204);

        // Check that the resource is not available anymore
        get("/api/whiskies/" + whisky.getId()).then().assertThat().statusCode(404);
    }
}
