package org.server.database;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Pool;
import org.server.service.ProfileService;
import org.server.util.ResponseFormatter;
import java.util.Map;
import static org.server.util.Constants.*;

/**
 * The type Database repository.
 */
public class DatabaseRepository extends AbstractVerticle {

    private final Logger LOGGER = LoggerFactory.getLogger(DatabaseRepository.class);

    private final Pool dbPool;

    private ProfileService credentialProfileService;

    private ProfileService discoveryProfileService;

    private ProfileService systemMetricsService;

    private ProfileService provisionService;

    /**
     * Instantiates a new Database repository.
     *
     * @param databaseConnectionManager the database connection manager
     */
    public DatabaseRepository(DatabaseConnectionManager databaseConnectionManager) {

        this.dbPool = databaseConnectionManager.getPool();

    }


    // Field mappings for different profile types
    private final Map<String, String> credentialMappings = Map.of(
            CREDENTIAL_PROFILE_NAME, CREDENTIAL_PROFILE_NAME,
            CREDENTIAL_CONFIG, CREDENTIAL_CONFIG
    );

    private final Map<String, String> discoveryMappings = Map.of(
            DISCOVERY_PROFILE_NAME, DISCOVERY_PROFILE_NAME,
            CREDENTIAL_PROFILE_ID, CREDENTIAL_PROFILE_ID,
            IP, IP,
            PORT, PORT
    );


    /**
     * Starts the profile manager by:
     * 1. Initializing all profile services
     * 2. Registering event bus consumers
     * 3. Testing database connection
     *
     * @param startPromise The promise to complete when initialization finishes
     */
    @Override
    public void start(Promise<Void> startPromise) {

        credentialProfileService = new ProfileService(dbPool, TABLE_CREDENTIAL_PROFILES, credentialMappings, vertx);

        discoveryProfileService = new ProfileService(dbPool, TABLE_DISCOVERY_PROFILES, discoveryMappings, vertx);

        systemMetricsService = new ProfileService(dbPool, TABLE_SYSTEM_DATA, null, vertx);

        provisionService = new ProfileService(dbPool, TABLE_PROVISION_PROFILES, null, vertx);

        LOGGER.info("Profile services initialized successfully!");

        registerEventBusConsumers();

        testDatabaseConnection(startPromise);

    }

    /**
     * Registers all event bus consumers for profile operations.
     * Handles CRUD operations for:
     * - Credential profiles
     * - Discovery profiles
     * - Provisioning operations
     * - System metrics queries
     * Also registers a special handler for system data insertion.
     */
    private void registerEventBusConsumers() {

        registerConsumer(ACTION_CREATE_CREDENTIAL_PROFILE, credentialProfileService, CREATE);

        registerConsumer(ACTION_GET_CREDENTIAL_PROFILE, credentialProfileService, READ);

        registerConsumer(ACTION_UPDATE_CREDENTIAL_PROFILE, credentialProfileService, UPDATE);

        registerConsumer(ACTION_DELETE_CREDENTIAL_PROFILE, credentialProfileService, DELETE);

        registerConsumer(ACTION_GET_ALL_CREDENTIALS, credentialProfileService, READALL);

        registerConsumer(ACTION_CREATE_DISCOVERY_PROFILE, discoveryProfileService, CREATE);

        registerConsumer(ACTION_GET_DISCOVERY_PROFILE, discoveryProfileService, READ);

        registerConsumer(ACTION_UPDATE_DISCOVERY_PROFILE, discoveryProfileService, UPDATE);

        registerConsumer(ACTION_DELETE_DISCOVERY_PROFILE, discoveryProfileService, DELETE);

        registerConsumer(ACTION_GET_ALL_DISCOVERYPROFILES, discoveryProfileService, READALL);

        registerConsumer(ACTION_GET_DISCOVERY_RUN, discoveryProfileService, DISCOVERY);

        registerProvisionConsumer(ACTION_START_PROVISION, provisionService);

        registerProvisionConsumer(ACTION_FETCH_PROVISIONED_DATA, provisionService);

        registerProvisionConsumer(ACTION_DELETE_MONITOR, provisionService);

        registerQueryConsumer(ACTION_GET_MEMORY_CHECKS, systemMetricsService);

        registerQueryConsumer(ACTION_GET_CPU_SPIKES, systemMetricsService);

        registerQueryConsumer(ACTION_GET_TOP_CPU_SPIKES, systemMetricsService);

        vertx.eventBus().localConsumer(SYSTEM_DATA_INSERT, this::handleInsertSystemData);

        LOGGER.info("All Event Bus consumers registered successfully.");

    }


    /**
     * Handles insertion of system metrics data into the database.
     *
     * @param message The event bus message containing system data to insert
     */
    private void handleInsertSystemData(Message<JsonObject> message) {

        LOGGER.info("Received request for inserting system data for " + message.body().getString(MONITOR_ID));

        systemMetricsService.insertSystemData(message.body())
                .onSuccess(response -> ResponseFormatter.sendSuccess(message, response))
                .onFailure(err -> ResponseFormatter.sendError(message, 500, err.getMessage()));

    }

    /**
     * Tests database connectivity by executing a simple query.
     * Completes or fails the startup promise based on the test result.
     *
     * @param startPromise The startup promise to complete or fail
     */
    private void testDatabaseConnection(Promise<Void> startPromise) {

        dbPool.query("SELECT 1").execute()

                .onSuccess(result -> {

                    LOGGER.info("Database connection successful!");

                    startPromise.complete();

                })
                .onFailure(error -> {

                    LOGGER.error("Database connection failed!", error);

                    startPromise.fail(error);

                });

    }


    /**
     * Registers a local event bus consumer for handling various profile operations.
     *
     * @param action   The event bus address to listen on.
     * @param service  The ProfileService instance used to process requests.
     * @param operation The operation type (e.g., CREATE, READ, UPDATE, DELETE).
     */
    private void registerConsumer(String action, ProfileService service, String operation) {

        vertx.eventBus().localConsumer(action, (Message<JsonObject> msg) -> handleOperation(msg, service, operation));

    }



    /**
     * Registers a local event bus consumer for handling provisioning-related operations.
     *
     * @param action   The event bus address to listen on.
     * @param service  The ProfileService instance used to process provisioning requests.
     */
    private void registerProvisionConsumer(String action, ProfileService service) {

        vertx.eventBus().localConsumer(action, (Message<JsonObject> msg) -> handleProvisionOperation(msg, service, action));

    }


    /**
     * Registers a local event bus consumer for handling query-related operations.
     *
     * @param action   The event bus address to listen on.
     * @param service  The ProfileService instance used to process query requests.
     */
    private void registerQueryConsumer(String action, ProfileService service) {

        vertx.eventBus().localConsumer(action, (Message<JsonObject> msg) -> handleQueryOperation(msg, service, action));

    }


    /**
     * Handles various profile-related operations and sends responses based on the operation result.
     *
     * @param message   The incoming message containing the request.
     * @param service   The ProfileService instance used to process the request.
     * @param operation The type of operation to be executed.
     */
    private void handleOperation(Message<JsonObject> message, ProfileService service, String operation) {

        Future<JsonObject> future = switch (operation) {

            case CREATE -> service.createProfile(message.body());

            case READ -> service.getProfileById(message.body());

            case UPDATE -> service.updateProfile(message.body());

            case DELETE -> service.deleteProfile(message.body());

            case READALL -> service.getAllProfiles().map(array -> message.body().put(PROFILES, array));

            case DISCOVERY -> service.runDiscovery(message.body());

            default -> Future.failedFuture("Invalid operation");

        };

        future.onSuccess(response -> {

            LOGGER.info("Operation successful for this operation "+ operation);

            ResponseFormatter.sendSuccess(message, response);

        }).onFailure(err -> {

            LOGGER.error("Operation failed: " + err.getMessage(), err);

            ResponseFormatter.sendError(message, 500, err.getMessage());

        });
    }


    /**
     * Handles provisioning-related operations and sends responses based on the operation result.
     *
     * @param message   The incoming message containing the request.
     * @param service   The ProfileService instance used to process provisioning requests.
     * @param operation The type of provisioning operation to be executed.
     */
    private void handleProvisionOperation(Message<JsonObject> message, ProfileService service, String operation) {

        Future<JsonObject> future = switch (operation) {

            case ACTION_START_PROVISION  -> service.provisionDevice(message.body());

            case ACTION_FETCH_PROVISIONED_DATA -> service.getProvisionDeviceData(message.body());

            case ACTION_DELETE_MONITOR -> service.deleteMonitor(message.body());

            default -> Future.failedFuture("Unsupported operation: " + operation);

        };

        future.onSuccess(response -> {

            LOGGER.info("Operation successful for this operation "+ operation);

            ResponseFormatter.sendSuccess(message, response);

        }).onFailure(err -> {

            LOGGER.error("Operation failed: " + err.getMessage(), err);

            ResponseFormatter.sendError(message, 500, err.getMessage());

        });
    }

    /**
     * Handles query-related operations and sends responses based on the operation result.
     *
     * @param message   The incoming message containing the request.
     * @param service   The ProfileService instance used to process query requests.
     * @param operation The type of query operation to be executed.
     */
    private void handleQueryOperation(Message<JsonObject> message, ProfileService service, String operation) {

        Future<JsonObject> future = switch (operation) {

            case ACTION_GET_MEMORY_CHECKS -> service.getMemoryCheck(message.body());

            case ACTION_GET_CPU_SPIKES -> service.getCPUSpikes(message.body());

            case ACTION_GET_TOP_CPU_SPIKES -> service.getTopCPUSpikes(message.body());

            default -> Future.failedFuture("Unsupported operation: " + operation);

        };

        future.onSuccess(response -> {

            LOGGER.info("Operation successful for this operation "+ operation);

            ResponseFormatter.sendSuccess(message, response);

        }).onFailure(err -> {

            LOGGER.error("Operation failed: " + err.getMessage(), err);

            ResponseFormatter.sendError(message, 500, err.getMessage());

        });
    }

    /**
     * Stops the DatabaseRepository Vertical.
     * This method is invoked when the Vert.x application is shutting down.
     */
    @Override
    public void stop() {

        LOGGER.info("Stopping DatabaseRepository Vertical and closing database pool...");

        dbPool.close()
                .onSuccess(v -> LOGGER.info("Database pool closed successfully."))
                .onFailure(err -> LOGGER.error("Failed to close database pool: " + err.getMessage(), err));

    }


}
