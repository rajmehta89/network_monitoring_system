package org.server.util;

import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import static org.server.util.Constants.*;

/**
 * The type Response formatter.
 */
public class ResponseFormatter {

    /**
     * Send success response.
     *
     * @param ctx        the ctx
     * @param statusCode the status code
     * @param body       the body
     */
    public static void sendSuccessResponse(RoutingContext ctx, int statusCode, Object body) {

        ctx.response()
                .setStatusCode(statusCode)
                .putHeader(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON)
                .end(body.toString());

    }

    /**
     * Send error response.
     *
     * @param ctx          the ctx
     * @param statusCode   the status code
     * @param errorMessage the error message
     */
    public static void sendErrorResponse(RoutingContext ctx, int statusCode, String errorMessage) {

        ctx.response()
                .setStatusCode(statusCode)
                .putHeader(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON)
                .end(new JsonObject().put(STATUS,FAIL).put(ERROR, errorMessage).encodePrettily());

    }

    /**
     * Send success.
     *
     * @param message the message
     * @param data    the data
     */
    public static void sendSuccess(Message<?> message, Object data) {

        var response = data;

        message.reply(response);

    }

    /**
     * Send error.
     *
     * @param message  the message
     * @param code     the code
     * @param errorMsg the error msg
     */

    public static void sendError(Message<?> message, int code, String errorMsg) {

        message.fail(code, errorMsg);

    }

}