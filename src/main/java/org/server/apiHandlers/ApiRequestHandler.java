package org.server.apiHandlers;

import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import java.util.Optional;
import static org.server.util.Constants.*;
import static org.server.util.ErrorHandler.determineStatusCode;
import static org.server.util.ErrorHandler.handleError;
import static org.server.util.ResponseFormatter.sendErrorResponse;
import static org.server.util.ResponseFormatter.sendSuccessResponse;
import static org.server.util.Validation.*;

/**
 * The type Api request handler.
 */
public class ApiRequestHandler {

    private final Logger logger = LoggerFactory.getLogger(ApiRequestHandler.class);

    private final EventBus eventBus;

    /**
     * Instantiates a new Api request handler.
     *
     * @param eventBus the event bus
     * */
    public ApiRequestHandler(EventBus eventBus) {

        this.eventBus = eventBus;

    }

    /**
     * Handle request.
     *
     * @param ctx             the ctx
     * @param eventBusAddress the event bus address
     * @param requestBody     the request body
     */
    public void handleRequest(RoutingContext ctx, String eventBusAddress, JsonObject requestBody) {

        logger.info("Received request for: " + eventBusAddress);

        eventBus.<JsonObject>request(eventBusAddress, requestBody, new DeliveryOptions().setSendTimeout(EVENT_BUS_SEND_TIMEOUT),

                reply -> {

                    if (reply.succeeded()) {

                        logger.info("Request processed successfully for: " + eventBusAddress);

                        sendSuccessResponse(ctx, determineStatusCode(eventBusAddress), reply.result().body());

                    }

                    else {

                        handleError(ctx, reply.cause().getMessage(), eventBusAddress);

                    }
                }
        );

    }

    /**
     * Handle api request.
     *
     * @param ctx       the ctx
     * @param type      the type
     * @param operation the operation
     */
    public void handleApiRequest(RoutingContext ctx, String type, String operation) {

        switch (type) {

            case CREDENTIAL:
                handleCredentialOperation(ctx, operation);
                break;

            case DISCOVERy:
                handleDiscoveryOperation(ctx, operation);
                break;

            case PROVISION:
                handleProvisionOperation(ctx, operation);
                break;

            case MONITOR:
                handleMonitorOperation(ctx, operation);
                break;

            default:
                handleUnsupportedOperation(ctx, operation);

        }

    }

    /**
     * Handles credential-related operations based on the provided operation type.
     *
     * @param ctx       The routing context of the request.
     * @param operation The type of credential operation to be executed.
     */
    private void handleCredentialOperation(RoutingContext ctx, String operation) {

        switch (operation) {
            case ACTION_GET_CREDENTIAL_PROFILE:
                handleGetCredentialProfile(ctx);
                break;

            case ACTION_CREATE_CREDENTIAL_PROFILE:
                handleCreateCredentialProfile(ctx);
                break;

            case ACTION_UPDATE_CREDENTIAL_PROFILE:
                handleUpdateCredentialProfile(ctx);
                break;

            case ACTION_DELETE_CREDENTIAL_PROFILE:
                handleDeleteCredentialProfile(ctx);
                break;

            default:
                handleUnsupportedOperation(ctx, operation);
        }

    }


    /**
     * Handles discovery-related operations based on the provided operation type.
     *
     * @param ctx       The routing context of the request.
     * @param operation The type of discovery operation to be executed.
     */
    private void handleDiscoveryOperation(RoutingContext ctx, String operation) {

        switch (operation) {

            case ACTION_GET_DISCOVERY_PROFILE:
                handleGetDiscoveryProfile(ctx);
                break;

            case ACTION_CREATE_DISCOVERY_PROFILE:
                handleCreateDiscoveryProfile(ctx);
                break;

            case ACTION_UPDATE_DISCOVERY_PROFILE:
                handleUpdateDiscoveryProfile(ctx);
                break;

            case ACTION_DELETE_DISCOVERY_PROFILE:
                handleDeleteDiscoveryProfile(ctx);
                break;

            case ACTION_GET_DISCOVERY_RUN:
                handleDiscoveryRun(ctx);
                break;

            default:
                handleUnsupportedOperation(ctx, operation);

        }

    }

    /**
     * Handles provisioning-related operations based on the provided operation type.
     *
     * @param ctx       The routing context of the request.
     * @param operation The type of provisioning operation to be executed.
     */
    private void handleProvisionOperation(RoutingContext ctx, String operation) {

        switch (operation) {

            case ACTION_START_PROVISION:
                handleProvisionRequest(ctx);
                break;

            case ACTION_FETCH_PROVISIONED_DATA:
                handleGetProvisionedData(ctx);
                break;


            default:
                handleUnsupportedOperation(ctx, operation);

        }

    }

    /**
     * Handles monitoring-related operations based on the provided operation type.
     *
     * @param ctx       The routing context of the request.
     * @param operation The type of monitor operation to be executed.
     */
    private void handleMonitorOperation(RoutingContext ctx, String operation) {

        switch (operation) {

            case ACTION_DELETE_MONITOR:
                handleRemoveMonitor(ctx);
                break;

            case ACTION_GET_MEMORY_CHECKS:
                handleMemoryCheck(ctx);
                break;

            case ACTION_GET_CPU_SPIKES:
                handleCPUSpikes(ctx);
                break;

            case ACTION_GET_TOP_CPU_SPIKES:
                handleTopCPUSpikes(ctx);
                break;

            default:
                handleUnsupportedOperation(ctx, operation);
                break;

        }

    }

    /**
     * Handle request.
     *
     * @param ctx             the ctx
     * @param eventBusAddress the event bus address
     */
    public void handleRequest(RoutingContext ctx, String eventBusAddress) {

        var requestBody = Optional.ofNullable(ctx.body().asJsonObject()).orElse(new JsonObject());

        handleRequest(ctx, eventBusAddress, requestBody);

    }



    /**
     * Validates and retrieves the request body for create/update operations.
     *
     * @param ctx       The routing context of the request.
     * @param operation The type of operation being performed.
     * @return A JsonObject containing the request body if valid, otherwise null.
     */
    private JsonObject validateAndGetRequestBody(RoutingContext ctx, String operation) {

        if (operation.contains("CREATE") || operation.contains("UPDATE")) {

            JsonObject requestBody = Optional.ofNullable(ctx.body()).map(buffer -> buffer.asJsonObject()).orElse(null);

            if (requestBody == null) {

                sendErrorResponse(ctx, 400, "Request body is required for " + operation + " request");

                return null;

            }

            return requestBody;

        }

        return null;

    }



    /**
     * Handles the creation of a credential profile.
     *
     * @param ctx The routing context of the request.
     */
    private void handleCreateCredentialProfile(RoutingContext ctx) {

        var requestBody = validateAndGetRequestBody(ctx, ACTION_CREATE_CREDENTIAL_PROFILE);

        if (requestBody != null && !ctx.response().ended()) {

            validateFields(ctx, requestBody, CREDENTIAL_PROFILE_NAME);

            validateJsonFields(ctx, requestBody,CREDENTIAL_CONFIG,USERNAME,PASSWORD,SYSTEM_TYPE);

            if (!ctx.response().ended()) {

                handleRequest(ctx, ACTION_CREATE_CREDENTIAL_PROFILE, requestBody);

            }

        }

    }


    /**
     * Handles the update of a credential profile.
     *
     * @param ctx The routing context of the request.
     */
    private void handleUpdateCredentialProfile(RoutingContext ctx) {

        var requestBody = validateAndGetRequestBody(ctx, ACTION_UPDATE_CREDENTIAL_PROFILE);

        if (requestBody != null) {

            validateFields(ctx, requestBody, CREDENTIAL_PROFILE_NAME);

            validateJsonFields(ctx, requestBody,CREDENTIAL_CONFIG,USERNAME,PASSWORD,SYSTEM_TYPE);

            if (!ctx.response().ended()) {

                getIdAndProcess(ctx,id -> handleRequest(ctx, ACTION_UPDATE_CREDENTIAL_PROFILE, requestBody.put(ID, id)),"Invalid profile ID for update credential profile", ID);

            }

        }

    }

    /**
     * Handles the creation of a discovery profile.
     *
     * @param ctx The routing context of the request.
     */
    private void handleCreateDiscoveryProfile(RoutingContext ctx) {

        var requestBody = validateAndGetRequestBody(ctx, ACTION_CREATE_DISCOVERY_PROFILE);

        if (requestBody != null) {

            validateFields(ctx, requestBody,DISCOVERY_PROFILE_NAME);

            validatePort(ctx,requestBody,PORT);

            validateIpAddress(ctx, requestBody.getString(IP));

            validateIntegerField(requestBody,CREDENTIAL_PROFILE_ID,ctx);

            if (!ctx.response().ended()) {

                handleRequest(ctx, ACTION_CREATE_DISCOVERY_PROFILE, requestBody);

            }

        }
    }

    /**
     * Handles the update of a discovery profile.
     *
     * @param ctx The routing context of the request.
     */
    private void handleUpdateDiscoveryProfile(RoutingContext ctx) {

        var requestBody = validateAndGetRequestBody(ctx, ACTION_UPDATE_DISCOVERY_PROFILE);

        if (requestBody != null) {

            validateFields(ctx, requestBody, DISCOVERY_PROFILE_NAME);

            validateIpAddress(ctx, requestBody.getString(IP));

            validatePort(ctx,requestBody,PORT);

            validateIntegerField(requestBody,CREDENTIAL_PROFILE_ID,ctx);

            if (!ctx.response().ended()) {

                getIdAndProcess(ctx, id -> handleRequest(ctx, ACTION_UPDATE_DISCOVERY_PROFILE, requestBody.put(ID, id)),"Invalid profile ID for discovery profile update",ID);

            }

        }

    }

    /**
     * Handles retrieval of a credential profile.
     *
     * @param ctx The routing context of the request.
     */
    private void handleGetCredentialProfile(RoutingContext ctx) {

        getIdAndProcess(ctx, id -> handleRequest(ctx, ACTION_GET_CREDENTIAL_PROFILE, new JsonObject().put(ID, id)),"Invalid profile ID for credential get request",ID);

    }

    /**
     * Handles deletion of a credential profile.
     *
     * @param ctx The routing context of the request.
     */
    private void handleDeleteCredentialProfile(RoutingContext ctx) {

        getIdAndProcess(ctx, id -> handleRequest(ctx, ACTION_DELETE_CREDENTIAL_PROFILE, new JsonObject().put(ID, id)), "Invalid profile ID for credential delete request",ID);

    }

    /**
     * Handles retrieval of a discovery profile.
     *
     * @param ctx The routing context of the request.
     */
    private void handleGetDiscoveryProfile(RoutingContext ctx) {

        getIdAndProcess(ctx,id -> handleRequest(ctx, ACTION_GET_DISCOVERY_PROFILE, new JsonObject().put(ID, id)),"Invalid profile ID for get discovery profile",ID);

    }

    /**
     * Handles deletion of a discovery profile.
     *
     * @param ctx The routing context of the request.
     */
    private void handleDeleteDiscoveryProfile(RoutingContext ctx) {

        getIdAndProcess(ctx,  id -> handleRequest(ctx, ACTION_DELETE_DISCOVERY_PROFILE, new JsonObject().put(ID, id)),"Invalid profile ID for delete discovery profile",ID);

    }

    /**
     * Handles execution of a discovery run.
     *
     * @param ctx The routing context of the request.
     */
    private void handleDiscoveryRun(RoutingContext ctx) {

        getIdAndProcess(ctx, id -> handleRequest(ctx, ACTION_GET_DISCOVERY_RUN, new JsonObject().put(ID, id)),"Invalid profile ID for discovery run ",ID);

    }

    /**
     * Handles provisioning requests.
     *
     * @param ctx The routing context of the request.
     */
    private void handleProvisionRequest(RoutingContext ctx) {

        getIdAndProcess(ctx,  id -> handleRequest(ctx, ACTION_START_PROVISION, new JsonObject().put(DISCOVERY_PROFILE_ID, id)), "Invalid discovery ID for provisioning",ID);

    }


    /**
     * Handles retrieval of provisioned data.
     *
     * @param ctx The routing context of the request.
     */
    private void handleGetProvisionedData(RoutingContext ctx) {

        getIdAndProcess(ctx,id -> handleRequest(ctx, ACTION_FETCH_PROVISIONED_DATA, new JsonObject().put(MONITOR_ID, id)),"Invalid monitor ID to get provisioned data",MONITOR_ID);

    }

    /**
     * Handles removal of a monitor.
     *
     * @param ctx The routing context of the request.
     */
    private void handleRemoveMonitor(RoutingContext ctx) {

        getIdAndProcess(ctx, id -> handleRequest(ctx, ACTION_DELETE_MONITOR, new JsonObject().put(MONITOR_ID, id)),"Invalid monitor ID to delete monitor",MONITOR_ID);

    }

    /**
     * Handles memory check requests.
     *
     * @param ctx The routing context of the request.
     */
    private void handleMemoryCheck(RoutingContext ctx) {

        handleRequest(ctx, ACTION_GET_MEMORY_CHECKS, new JsonObject());

    }

    /**
     * Handles CPU spike check requests.
     *
     * @param ctx The routing context of the request.
     */
    private void handleCPUSpikes(RoutingContext ctx) {

        handleRequest(ctx, ACTION_GET_CPU_SPIKES, new JsonObject());

    }

    /**
     * Handles requests to get the top CPU spikes.
     *
     * @param ctx The routing context of the request.
     */
    private void handleTopCPUSpikes(RoutingContext ctx) {

        handleRequest(ctx, ACTION_GET_TOP_CPU_SPIKES, new JsonObject());

    }

    /**
     * Handles unsupported operations.
     *
     * @param ctx       The routing context of the request.
     * @param operation The unsupported operation.
     */
    private void handleUnsupportedOperation(RoutingContext ctx, String operation) {

        logger.warn("Unsupported operation: " + operation);

        sendErrorResponse(ctx, 400, "Unsupported operation");

    }

}