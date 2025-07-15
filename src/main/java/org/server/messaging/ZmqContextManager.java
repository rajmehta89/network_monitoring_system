package org.server.messaging;

import org.zeromq.ZMQ;

/**
 * Singleton class for managing the ZMQ context.
 */
public class ZmqContextManager {

    private static final ZmqContextManager INSTANCE = new ZmqContextManager();

    private final ZMQ.Context zmqContext;

    /**
     * Private constructor to initialize the ZMQ context.
     */
    private ZmqContextManager() {

        this.zmqContext = ZMQ.context(1);

    }

    /**
     * Returns the singleton instance of ZmqContextManager.
     */
    public static ZmqContextManager getInstance() {

        return INSTANCE;

    }

    /**
     * Retrieves the ZMQ context.
     */
    public ZMQ.Context getContext() {

        return zmqContext;

    }

    /**
     * Closes the ZMQ context.
     */
    public void close() {

        if (zmqContext != null) {

            zmqContext.close();

        }

    }
}
