package org.server.messaging;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import org.server.util.UserProfileCacheManager;
import org.zeromq.ZMQ;

import java.util.UUID;

import static org.server.util.Constants.*;

/**
 * The type Request sender.
 */
public class RequestSender extends AbstractVerticle {

    private ZMQ.Socket zmqSocket;

    private final Logger logger;

    private final UserProfileCacheManager userProfileCacheManager;

    private String deploymentID;

    /**
     * Instantiates a new Request sender.
     */
    public RequestSender() {

        this.userProfileCacheManager = UserProfileCacheManager.getInstance();

        this.logger = LoggerFactory.getLogger(this.getClass());

    }

    /**
     * Starts the ZMQ handler verticle by initializing ZMQ components and registering event bus consumers.
     *
     * @param startPromise The promise to complete or fail when startup is finished
     */
    @Override
    public void start(Promise<Void> startPromise) {

        try{

        this.deploymentID = deploymentID();

        var zmqContext = ZmqContextManager.getInstance().getContext();

        zmqSocket = zmqContext.socket(ZMQ.PUSH);

        zmqSocket.bind(ZMQ_BIND_ADDRESS);

        registerEventBusConsumers();

        var msg=new JsonObject().put(REQUESTTYPE,HEALTH).encode();

        vertx.setTimer(SET_SEND_FIRST_MESSAGE_TIMEOUT, id -> {

            if (zmqSocket.send(msg, ZMQ.DONTWAIT)) {

               logger.info(String.format("Request sent: %s", msg));

                startPromise.complete();

            }

            else {

              logger.info(String.format("Request failed: %s", msg));

             }

        });

        }

        catch (Exception e) {

            logger.error("error to start message sender verticle", e);

            startPromise.fail(e);

        }

    }

    /**
     * Registers event bus consumers for ZMQ-related operations.
     */
    private void registerEventBusConsumers() {

        vertx.eventBus().<JsonObject>localConsumer(ZMQ_DISCOVERY_RUN_REQUEST, this::handleDiscoveryRun);

        vertx.eventBus().<JsonObject>localConsumer(ZMQ_POLLING_REQUEST, this::handlePollingRequest);

    }

    /**
     * Handles discovery run requests by forwarding them to ZMQ.
     */
    private void handleDiscoveryRun(Message<JsonObject> message) {

        handleZmqRequest(message);

    }

    /**
     * Handles polling requests by forwarding them to ZMQ.
     */
    private void handlePollingRequest(Message<JsonObject> message) {

        handleZmqRequest(message);

    }

    /**
     * Processes a ZMQ request by adding a unique request ID and sending it via ZMQ.
     */
    private void handleZmqRequest(Message<JsonObject> message) {

        var requestId = UUID.randomUUID().toString();

        message.body().put(REQUESTID, requestId);

        sendZmqMessage(requestId, message);

    }

    /**
     * Sends a ZMQ message and handles the response.
     */
    public void sendZmqMessage(String requestId, Message<JsonObject> message) {

        try {

            userProfileCacheManager.storeMessage(requestId, message);

            var sent = zmqSocket.send(message.body().toString(),ZMQ.NOBLOCK);

            if (sent) {

                logger.info("Message sent successfully for requestId: " + requestId);

            }

            else {

                logger.error("Failed to send message for requestId: " + requestId);

            }

        }

        catch (Exception e) {

            logger.error("Error while sending message for requestId: " + requestId, e);

        }

    }

    private void closeZmq() {

        if (zmqSocket != null) {

            zmqSocket.close();

        }

        logger.info("Socket closed successfully");

    }

    @Override
    public void stop() {

        ZmqContextManager.getInstance().close();

        closeZmq();

        logger.info("Stopped RequestSender");

    }

}
