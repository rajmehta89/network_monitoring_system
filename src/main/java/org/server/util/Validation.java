package org.server.util;

import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.function.Consumer;

import static org.server.util.Constants.IPV4_PATTERN;
import static org.server.util.Constants.IPV6_PATTERN;
import static org.server.util.ResponseFormatter.sendErrorResponse;

/**
 * The type Validation.
 */
public class Validation {

    private static final Logger logger = LoggerFactory.getLogger(Validation.class);

    /**
     * Validate fields.
     *
     * @param ctx         the ctx
     * @param requestBody the request body
     * @param fields      the fields
     */
    public static void validateFields(RoutingContext ctx, JsonObject requestBody, String... fields) {

        for (String field : fields) {

            if (!requestBody.containsKey(field) || requestBody.getValue(field) == null || requestBody.getString(field).isEmpty()) {

                logger.warn("Missing required field: " + field);

                sendErrorResponse(ctx, 400, "Missing required field: " + field);

                return ;

            }

        }

    }

    /**
     * Validate port.
     *
     * @param ctx         the ctx
     * @param requestBody the request body
     * @param portField   the port field
     */
    public static void validatePort(RoutingContext ctx, JsonObject requestBody, String portField) {

        if (!requestBody.containsKey(portField) || requestBody.getValue(portField) == null) {

            logger.warn("Missing required field: " + portField);

            sendErrorResponse(ctx, 400, "Missing required field: " + portField);

        }

        else {

            try {

                int port = requestBody.getInteger(portField);

                if (port < 1 || port > 65535) {

                    logger.warn("Invalid port number: " + port);

                    sendErrorResponse(ctx, 400, "Invalid port number. It must be between 1 and 65535.");

                }

            }

            catch (ClassCastException e) {

                logger.warn("Port must be an integer: " + requestBody.getValue(portField));

                sendErrorResponse(ctx, 400, "Port must be a valid integer.");

            }
        }
    }


    /**
     * Validate ip address.
     *
     * @param ctx the ctx
     * @param ip  the ip
     */
    public static void validateIpAddress(RoutingContext ctx, String ip) {

        if (!isValidIPAddress(ip)) {

            logger.warn("Invalid IP address format: " + ip);

            sendErrorResponse(ctx, 400, "Invalid IP address format");

        }

    }

    /**
     * Validate json fields.
     *
     * @param ctx            the ctx
     * @param requestBody    the request body
     * @param jsonFieldName  the json field name
     * @param requiredFields the required fields
     */
    public static void validateJsonFields(RoutingContext ctx, JsonObject requestBody, String jsonFieldName, String... requiredFields) {

        if (!requestBody.containsKey(jsonFieldName) || requestBody.getValue(jsonFieldName) == null) {

            logger.warn("Missing JSON object: " + jsonFieldName);

            sendErrorResponse(ctx, 400, "Missing JSON object: " + jsonFieldName);

        }

        else {

            var jsonData = requestBody.getJsonObject(jsonFieldName);

            for (String field : requiredFields) {

                if (!jsonData.containsKey(field) || jsonData.getValue(field) == null) {

                    logger.warn("Missing required field in {}: {}"+ jsonFieldName+field);

                    sendErrorResponse(ctx, 400, "Missing required field in " + jsonFieldName + ": " + field);

                }

            }

        }


    }


    /**
     * Validate integer field.
     *
     * @param requestBody the request body
     * @param fieldName   the field name
     * @param ctx         the ctx
     */
    public static void validateIntegerField(JsonObject requestBody, String fieldName, RoutingContext ctx) {

        var value = requestBody.getInteger(fieldName);

        if (value == null || value <= 0) {

            sendErrorResponse(ctx, 400, fieldName + " should be a valid positive integer.");

            return;
        }

    }


    /**
     * Gets id and process.
     *
     * @param ctx          the ctx
     * @param processor    the processor
     * @param errorMessage the error message
     * @param paramNames   the param names
     */
    public static void getIdAndProcess(RoutingContext ctx, Consumer<Integer> processor, String errorMessage, String... paramNames) {

        for (String paramName : paramNames) {

            var idParam = ctx.pathParam(paramName);

            if (idParam == null || idParam.trim().isEmpty()) {

                logger.warn(paramName + " is missing");

                sendErrorResponse(ctx, 400, paramName + " is missing");

            } else if (!idParam.matches("\\d+")) {

                logger.warn(errorMessage + ": " + idParam);

                sendErrorResponse(ctx, 400, paramName + " is invalid. It should be greater than 0 and numeric.");

            } else {

                var id = Integer.parseInt(idParam);

                processor.accept(id);

            }
        }
    }


    /**
     * Is valid ip address boolean.
     *
     * @param ip the ip
     * @return the boolean
     */
    public static boolean isValidIPAddress(String ip) {

        return IPV4_PATTERN.matcher(ip).matches() || IPV6_PATTERN.matcher(ip).matches();

    }

    /**
     * Check availability boolean.
     *
     * @param ip   the ip
     * @param port the port
     * @return the boolean
     */
    public static boolean checkAvailability(String ip, int port) {
        try {
            // Run ping command
            var command = "ping -c 3 " + ip + " | awk '/packets transmitted/ {if ($(NF-4)== \"100%\") print \"false\"; else print \"true\"}'";

            var processBuilder = new ProcessBuilder("sh", "-c", command);

            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String output = reader.readLine();

            int exitCode = process.waitFor();

            if (exitCode != 0 || output == null || !output.trim().equals("true")) {

                 return false;

            }

            try (Socket socket = new Socket()) {

                socket.connect(new InetSocketAddress(ip, port), 3000);

                return true;

            } catch (Exception e) {

                System.err.println("Port " + port + " is not reachable on IP: " + ip);

                return false;
            }

        } catch (Exception e) {

            System.err.println("Error checking device availability: " + e.getMessage());

            return false;

        }
    }



}
