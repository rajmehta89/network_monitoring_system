package org.server.util;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.server.util.Constants.*;
import static org.server.util.ResponseFormatter.sendErrorResponse;

/**
 * The type Error handler.
 */
public class ErrorHandler {

    private static final Logger logger = LoggerFactory.getLogger(ErrorHandler.class);

    /**
     * Determine status code int.
     *
     * @param eventBusAddress the event bus address
     * @return the int
     */
    public static int determineStatusCode(String eventBusAddress) {

        if (eventBusAddress.contains("CREATE")) {

            return 201;

        }

        else if (eventBusAddress.contains("UPDATE") || eventBusAddress.contains("DELETE") || eventBusAddress.contains("READ")) {

            return 200;

        }

        else {

            return 200;

        }

    }


    /**
     * Handle error.
     *
     * @param ctx             the ctx
     * @param err             the err
     * @param eventBusAddress the event bus address
     */
    public static void handleError(RoutingContext ctx, String err, String eventBusAddress) {

        if (err.contains(DUPLICATION_ERROR_CODE)) {

            if (eventBusAddress.contains(CREDENTIAL)) {

                logger.warn("Duplicate key error in credential: {}");

                sendErrorResponse(ctx, 409, "Credential profile name should be unique.");

            } else if (eventBusAddress.contains(DISCOVERy)) {

                logger.warn("Duplicate key error in discovery: {}");

                sendErrorResponse(ctx, 409, "Discovery profile name should be unique.");

            }else if(eventBusAddress.contains(PROVISION)){

                logger.warn("Duplicate key error in provision table  already: {}");

                sendErrorResponse(ctx, 409, "the entry for this ip is already in use and polling is already started");

            }

        }


       else if (err.contains(FOREIGN_KEY_ERROR_CODE)) {

            if (eventBusAddress.contains(CREDENTIAL)) {

                logger.warn("Foreign key constraint error in credential: {}");

                sendErrorResponse(ctx, 400, "Invalid foreign key reference in credential profile (used in discovery or provision table).");

            } else if (eventBusAddress.contains(DISCOVERy)) {

                logger.warn("Foreign key constraint error in discovery: {}");

                sendErrorResponse(ctx, 400, "Invalid foreign key reference in credential profile (used in discovery or provision table).");

            }

        }

       else if (err.startsWith("{")) {

            try {

                var errorJson = new JsonObject(err);

                if (errorJson.containsKey(STATUS) && errorJson.containsKey(MESSAGE)) {

                    sendErrorResponse(ctx, 500, errorJson.getString(MESSAGE));

                }

            } catch (Exception jsonParseErr) {

                logger.error("Error parsing JSON error response: {}", jsonParseErr.getMessage());

            }

        }

        else{

            sendErrorResponse(ctx, 500, err);

        }


    }
}
