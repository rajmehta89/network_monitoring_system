package org.server.api;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import org.server.apiHandlers.ApiRequestHandler;

import java.util.Set;

import static org.server.util.Constants.*;

/**
 * The type Rest api server.
 */
public class RestApiServer extends AbstractVerticle {

    private final Logger LOGGER = LoggerFactory.getLogger(RestApiServer.class);

    private final int DEFAULT_PORT = 8000;

    /**
     * Main Vert.x service class responsible for setting up the HTTP server,
     * configuring routes, and handling API requests.
     */
    @Override
    public void start(Promise<Void> startPromise) {

        var router = createRouter();

        ApiRequestHandler apiHandler = new ApiRequestHandler(vertx.eventBus());

        registerAllRoutes(router, apiHandler);

        startServer(router).onSuccess(httpRes -> {

            startPromise.complete();

        }).onFailure(err -> {

            LOGGER.error("RestApiServer failed to start", err);

            startPromise.fail(err.getMessage());

        });

    }


    /**
     * Creates a new router and adds a body handler to handle incoming requests.
     *
     * @return a configured Router instance
     */
    private Router createRouter() {

        var router = Router.router(vertx);

        router.route()
                .handler(createCorsHandler())
                .handler(BodyHandler.create());

        return router;

    }

    private CorsHandler createCorsHandler() {

        return CorsHandler.create()
                .addRelativeOrigin(".*")
                .allowedMethods(Set.of(
                        HttpMethod.GET,
                        HttpMethod.POST,
                        HttpMethod.PUT,
                        HttpMethod.DELETE,
                        HttpMethod.OPTIONS
                ))
                .allowedHeaders(Set.of(
                        "Access-Control-Request-Method",
                        "Access-Control-Allow-Credentials",
                        "Access-Control-Allow-Origin",
                        "Access-Control-Allow-Headers",
                        "Content-Type"
                ));

    }


    /**
     * Registers all API routes with the given router.
     *
     * @param router     the router to register routes on
     * @param apiHandler the API request handler
     */
    private void registerAllRoutes(Router router, ApiRequestHandler apiHandler) {

        registerCredentialProfileRoutes(router, apiHandler);

        registerDiscoveryProfileRoutes(router, apiHandler);

        registerProvisioningRoutes(router, apiHandler);

        registerSystemMonitorRoutes(router, apiHandler);

    }


    /**
     * Registers routes for credential profile operations.
     */
    private void registerCredentialProfileRoutes(Router router, ApiRequestHandler apiHandler) {

        router.get(API_GET_CREDENTIAL_PROFILE).handler(ctx -> apiHandler.handleApiRequest(ctx, CREDENTIAL, ACTION_GET_CREDENTIAL_PROFILE));

        router.get(API_GET_ALL_CREDENTIAL_PROFILES).handler(ctx -> apiHandler.handleRequest(ctx, ACTION_GET_ALL_CREDENTIALS));

        router.post(API_CREATE_CREDENTIAL_PROFILE).handler(ctx -> apiHandler.handleApiRequest(ctx, CREDENTIAL, ACTION_CREATE_CREDENTIAL_PROFILE));

        router.put(API_UPDATE_CREDENTIAL_PROFILE).handler(ctx -> apiHandler.handleApiRequest(ctx, CREDENTIAL, ACTION_UPDATE_CREDENTIAL_PROFILE));

        router.delete(API_DELETE_CREDENTIAL_PROFILE).handler(ctx -> apiHandler.handleApiRequest(ctx, CREDENTIAL, ACTION_DELETE_CREDENTIAL_PROFILE));

    }



    /**
     * Registers routes for discovery profile operations.
     */
    private void registerDiscoveryProfileRoutes(Router router, ApiRequestHandler apiHandler) {

        router.get(API_GET_DISCOVERY_PROFILE).handler(ctx -> apiHandler.handleApiRequest(ctx, "DISCOVERY", ACTION_GET_DISCOVERY_PROFILE));

        router.get(API_GET_ALL_DISCOVERY_PROFILES).handler(ctx -> apiHandler.handleRequest(ctx, ACTION_GET_ALL_DISCOVERYPROFILES));

        router.get(API_GET_DISCOVERY_RUN).handler(ctx -> apiHandler.handleApiRequest(ctx, "DISCOVERY", ACTION_GET_DISCOVERY_RUN));

        router.post(API_CREATE_DISCOVERY_PROFILE).handler(ctx -> apiHandler.handleApiRequest(ctx, "DISCOVERY", ACTION_CREATE_DISCOVERY_PROFILE));

        router.put(API_UPDATE_DISCOVERY_PROFILE).handler(ctx -> apiHandler.handleApiRequest(ctx, "DISCOVERY", ACTION_UPDATE_DISCOVERY_PROFILE));

        router.delete(API_DELETE_DISCOVERY_PROFILE).handler(ctx -> apiHandler.handleApiRequest(ctx, "DISCOVERY", ACTION_DELETE_DISCOVERY_PROFILE));

    }

    /**
     * Registers routes for provisioning operations.
     */
    private void registerProvisioningRoutes(Router router, ApiRequestHandler apiHandler) {

        router.get(API_START_PROVISION).handler(ctx -> apiHandler.handleApiRequest(ctx, PROVISION, ACTION_START_PROVISION));

        router.get(API_GET_PROVISIONED_DATA).handler(ctx -> apiHandler.handleApiRequest(ctx, PROVISION, ACTION_FETCH_PROVISIONED_DATA));

        router.delete(API_DELETE_MONITOR).handler(ctx -> apiHandler.handleApiRequest(ctx, MONITOR, ACTION_DELETE_MONITOR));

    }

    /**
     * Registers routes for system monitoring operations.
     */
    private void registerSystemMonitorRoutes(Router router, ApiRequestHandler apiHandler) {

        router.get(API_GET_MEMORY_CHECKS).handler(ctx -> apiHandler.handleApiRequest(ctx, MONITOR, ACTION_GET_MEMORY_CHECKS));

        router.get(API_GET_CPU_SPIKES).handler(ctx -> apiHandler.handleApiRequest(ctx, MONITOR, ACTION_GET_CPU_SPIKES));

        router.get(API_GET_TOP_CPU_SPIKES).handler(ctx -> apiHandler.handleApiRequest(ctx, MONITOR, ACTION_GET_TOP_CPU_SPIKES));

    }

    /**
     * Starts the HTTP server and binds it to the configured port.
     *
     * @param router       the configured Router instance
     */
    private Future<Void> startServer(Router router) {

        Promise<Void>startPromise = Promise.promise();

        vertx.createHttpServer()

                .requestHandler(router)

                .listen(DEFAULT_PORT)

                .onSuccess(server -> {

                    LOGGER.info("HTTP Server running on port " + DEFAULT_PORT);

                    startPromise.complete();

                })
                .onFailure(error -> {

                    LOGGER.error("Failed to start HTTP Server: " + error.getMessage(), error);

                    startPromise.fail(error);

                });

        return startPromise.future();

    }

}