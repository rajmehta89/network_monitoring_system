package org.server.messaging;

import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.server.util.UserProfileCacheManager;
import org.zeromq.ZMQ;
import static org.server.util.Constants.*;

/**
 * The type RequestReceiver using a separate thread for ZMQ processing.
 */
public class RequestReceiver {

    private final ZMQ.Socket zmqSocket;

    private final String BIND_ADDRESS = ZMQ_CONNECT_ADDRESS;

    private final Logger logger;

    private final UserProfileCacheManager userProfileCacheManager;

    private boolean running;

    private Thread zmqThread;

    private long timeoutID;

    private final Vertx vertx;

    /**
     * Instantiates a new RequestReceiver.
     */
    public RequestReceiver(Vertx vertx) {

        var zmqContext = ZmqContextManager.getInstance().getContext();

        this.vertx=vertx;

        this.userProfileCacheManager = UserProfileCacheManager.getInstance();

        this.logger = LoggerFactory.getLogger(this.getClass());

        this.zmqSocket = zmqContext.socket(ZMQ.PULL);

        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

    }

    /**
     * Starts the ZMQ processing in a separate thread.
     */
    public void start(Promise<Boolean>promise) {

        try {

            running = true;

            zmqThread = new Thread(() -> processMessages(promise));

            zmqThread.start();

            logger.info("RequestReceiver thread started.");

            timeoutID = vertx.setTimer(ZMQ_HEALTH_CHECK_TIMEOUT, id -> {

                logger.warn("Timeout reached without receiving a message");

                promise.fail("Timeout: No message received in 30 seconds");

            });

        } catch (Exception e) {

            logger.error("Error starting RequestReceiver thread: " + e.getMessage(), e);

            promise.fail(e);

        }

    }

    /**
     * Handles ZMQ message reception and processing.
     */
    private void processMessages(Promise<Boolean>promise) {

        zmqSocket.connect(BIND_ADDRESS);

        logger.info("ZMQ socket connected to: " + BIND_ADDRESS);

        while (running) {

            var requestObject = zmqSocket.recvStr(ZMQ.DONTWAIT);

            if (requestObject != null && !requestObject.trim().isEmpty()) {

                processMessage(requestObject,promise);

            }

        }

    }

    /**
     * Processes incoming ZMQ messages.
     */
    private void processMessage(String requestObject,Promise <Boolean> promise) {

        try {

            var response = new JsonObject(Json.decodeValue(requestObject, String.class));

            var clientID = response.getString(REQUESTID, "");

            if (response.getString(STATUS).equals("OK")) {

                logger.info("Response having successful and ready to take the messages" + clientID + " the response is " + response);

                vertx.cancelTimer(timeoutID);

                promise.complete(true);

                return;

            }

            var message = userProfileCacheManager.retrieveMessage(clientID);

            if (message != null) {

                var requestType = response.getString(REQUESTTYPE, "");

                if (PROVISIONING.equalsIgnoreCase(requestType)) {

                    handleProvisioningResponse(clientID, response);

                }

                else if (DISCOVERY.equalsIgnoreCase(requestType)) {

                    message.reply(response);

                }

            }

        }

        catch (Exception e) {

            logger.error("Error processing response: " + e.getMessage(), e);

        }

    }

    /**
     * Handles provisioning responses.
     */
    private void handleProvisioningResponse(String clientID, JsonObject responseJson) {

        var status = responseJson.getString(STATUS, "");

        var monitorId = responseJson.getInteger(MONITOR_ID, -1);

        var request = new JsonObject().put(MONITOR_ID, monitorId);

        if (SUCCESS.equalsIgnoreCase(status)) {

            request.put(SYSTEM_INFO, responseJson.getJsonObject(RESULT, new JsonObject())).put(FETCHEDAT, System.currentTimeMillis());

        }

        else {

            request.put(ERROR, responseJson.getJsonObject(ERRORS, new JsonObject())).put(FETCHEDAT, System.currentTimeMillis());

        }

        logger.info("Sending SystemData insert request for monitor_id: " + monitorId);

        vertx.eventBus().send(SYSTEM_DATA_INSERT, request);

    }

    /**
     * Stops the ZMQ processing thread and closes the socket.
     */
    public void stop() {

        if (!running) return;

        running = false;

        if (zmqThread != null) {

            try {

                zmqThread.join();

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();

            }

        }

        zmqSocket.close();

        logger.info("RequestReceiver stopped and resources closed.");

    }
}
