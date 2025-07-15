import com.github.javafaker.Faker;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;


@ExtendWith(VertxExtension.class)
class ApiServerTest {

    private static Vertx vertx;

    private static WebClient webClient;

    private final Faker faker = new Faker();

    private static final long DISCOVERY_EVENT_BUS_TIMEOUT = 500000L; // Timeout in milliseconds
    public EventBus eventBus;

    @BeforeAll
    static void setup(VertxTestContext testContext) {

        vertx = Vertx.vertx();

        webClient = WebClient.create(vertx);

//        vertx.deployVerticle(new NmsServerApplication(), testContext.succeeding(id -> testContext.completeNow()));

        vertx.eventBus().consumer("zmq.discovery.run.request", message -> {
            JsonObject request = (JsonObject) message.body();
            JsonObject response = new JsonObject()
                    .put("status", "SUCCESS")
                    .put("result", new JsonObject().put("info", "Discovery completed"));

            message.reply(response);
        });

        testContext.completeNow();

    }

    @BeforeEach
    void setup(Vertx vertx) {

        eventBus = vertx.eventBus();

    }

    @AfterAll
    static void tearDown(VertxTestContext testContext) {

        vertx.close(testContext.succeeding(v -> testContext.completeNow()));

    }

    // Test Case for creating a credential profile
    @Test
    void testCredentialProfileCreate(VertxTestContext testContext) {
        var requestBody = new JsonObject()
                .put("credential_profile_name", faker.name().firstName())
                .put("credentialconfig", new JsonObject()
                        .put("username", faker.name().username())
                        .put("password", faker.internet().password())
                        .put("system_type", faker.name().fullName()));

        webClient.post(8000, "localhost", "/api/credential-profile")
                .sendJsonObject(requestBody, response ->
                {
                    assertEquals(201, response.result().statusCode(), "Expected status code 201");

                    JsonObject responseBody = response.result().bodyAsJsonObject();

                    assertTrue(responseBody.containsKey("id"), "Response should contain an ID");

                    testContext.completeNow();
                });

    }

    //Test case for getting a credential profile
    @Test
    void testCredentialProfileGet(VertxTestContext testContext) {
        var testProfileId = "14";

        webClient.get(8000, "localhost", "/api/credential-profile/" + testProfileId)
                .send(response ->
                {
                    assertEquals(200, response.result().statusCode(), "Expected status code 200");

                    JsonObject responseBody = response.result().bodyAsJsonObject();

                    assertEquals(testProfileId, responseBody.getString("id"), "Expected profile ID to match");

                    testContext.completeNow();
                });
    }

    // Test Case for updating a credential profile
    @Test
    void testCredentialProfileUpdate(VertxTestContext testContext) {
        var testProfileId = 14;

        var updatedRequestBody = new JsonObject()
                .put("id", testProfileId)
                .put("credential_profile_name", faker.lorem().word())
                .put("credentialconfig", new JsonObject().put("username", faker.name().username()).put("password", faker.internet().password()));

        webClient.put(8000, "localhost", "/api/credential-profile")
                .sendJsonObject(updatedRequestBody, response ->
                {
                    assertEquals(200, response.result().statusCode(), "Expected status code 200");

                    testContext.completeNow();
                });
    }

    // Test Case for creating a discovery profile
    @Test
    void testDiscoveryProfileCreate(VertxTestContext testContext) {
        var requestBody = new JsonObject()
                .put("discovery_profile_name", faker.name().firstName())
                .put("credential_profile_id", 14)
                .put("ip", faker.internet().ipV4Address());

        webClient.post(8000, "localhost", "/api/discovery-profile/")
                .sendJsonObject(requestBody, response ->
                {
                    assertEquals(201, response.result().statusCode(), "Expected status code 201");

                    var responseBody = response.result().bodyAsJsonObject();

                    assertTrue(responseBody.containsKey("id"), "Response should contain an ID");

                    testContext.completeNow();
                });
    }

    //    // Test Case for getting a discovery profile
    @Test
    void testDiscoveryProfileGet(VertxTestContext testContext) {
        var testProfileId = "7";

        webClient.get(8000, "localhost", "/api/discovery-profile/" + testProfileId)
                .send(response ->
                {
                    assertEquals(200, response.result().statusCode(), "Expected status code 200");

                    var responseBody = response.result().bodyAsJsonObject();

                    assertEquals(testProfileId, responseBody.getString("id"), "Expected discovery profile ID to match");

                    testContext.completeNow();
                });
    }

    // Test Case for updating a discovery profile
    @Test
    void testDiscoveryProfileUpdate(VertxTestContext testContext) {
        var testProfileId = 7;

        var updatedRequestBody = new JsonObject()
                .put("id", testProfileId)
                .put("ip", faker.internet().ipV4Address())
                .put("credential_profile_id", 14);

        webClient.put(8000, "localhost", "/api/discovery-profile/")
                .sendJsonObject(updatedRequestBody, response ->
                {
                    assertEquals(200, response.result().statusCode(), "Expected status code 200");

                    JsonObject responseBody = response.result().bodyAsJsonObject();

                    testContext.completeNow();
                });
    }

    //        //Test case for updating provision status
    @Test
    void testUpdateProvisionStatus(VertxTestContext testContext) {
        var discoveryId = 7;

        webClient.get(8000, "localhost", "/api/provision/start" + "/" + discoveryId)
                .send(response ->
                {
                    assertEquals(500, response.result().statusCode(), "Expected status code 400");

                    System.out.println(response.result().body());

                    testContext.completeNow();

                });
    }

    //
//    // Test Case for GET polled data
    @Test
    void testGetProvisionData(VertxTestContext testContext) {
        var monitorId = 13;

        webClient.get(8000, "localhost", "/api/data/" + monitorId)
                .send(response ->
                {
                    assertEquals(200, response.result().statusCode(), "Expected status code 200");

                    var responseBody = response.result().bodyAsJsonObject();

                    assertNotNull(responseBody, "Response body should not be null");

                    testContext.completeNow();
                });
    }

    @Test
    void testGetMemoryCheck(VertxTestContext testContext) {
        webClient.get(8000, "localhost", "/system/memory-check")
                .send(response ->
                {
                    assertEquals(200, response.result().statusCode(), "Expected status code 200");

                    var responseBody = response.result().bodyAsJsonObject();

                    assertNotNull(responseBody, "Response body should not be null");

                    testContext.completeNow();
                });
    }

    @Test
    void testGetCpuUsageSpikes(VertxTestContext testContext) {
        webClient.get(8000, "localhost", "/system/cpu-usage-spikes")
                .send(response ->
                {
                    assertEquals(200, response.result().statusCode(), "Expected status code 200");

                    var responseBody = response.result().bodyAsJsonObject();

                    assertNotNull(responseBody, "Response body should not be null");

                    testContext.completeNow();
                });
    }

    //
    @Test
    void testGetCpuSpikes(VertxTestContext testContext) {
        webClient.get(8000, "localhost", "/system/top-cpu-spikes")
                .send(response ->
                {
                    assertEquals(200, response.result().statusCode(), "Expected status code 200");

                    var responseBody = response.result().bodyAsJsonObject();

                    assertNotNull(responseBody, "Response body should not be null");

                    testContext.completeNow();
                });
    }

    @Test
    void testDiscoveryRun(VertxTestContext testContext) {
        var discoveryProfileId = 9;

        webClient.get(8000, "localhost", "/api/discovery-run")
                .sendJsonObject(new JsonObject().put("id", discoveryProfileId), response -> {

                    assertEquals(200, response.result().statusCode(), "Expected status code 200");

                    testContext.completeNow();
                });
    }

    @Test
    void testZmqRunDiscovery(VertxTestContext testContext) {

        JsonObject request = new JsonObject()
                .put("ip", "192.168.1.1")
                .put("username", "admin")
                .put("password", "Mind@123")
                .put("systemType", "windows")
                .put("requestType", "discovery");

        DeliveryOptions options = new DeliveryOptions().setSendTimeout(DISCOVERY_EVENT_BUS_TIMEOUT);

        eventBus.<JsonObject>request("zmq.discovery.run.request", request, options)
                .onSuccess(reply -> {

                    JsonObject response = reply.body();

                    assertEquals("success", response.getString("status"), "Expected SUCCESS status");

                    testContext.completeNow();

                })
                .onFailure(err -> {

                    fail("Request timed out or failed: " + err.getMessage());

                    testContext.failNow(err);

                });
    }


}