package org.server.util;

import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The type User profile cache manager.
 */
public class UserProfileCacheManager {

    private static final UserProfileCacheManager INSTANCE = new UserProfileCacheManager();

    private final Map<Integer, JsonObject> credentialData = new ConcurrentHashMap<>();

    private final Map<Integer, JsonObject> monitoringData = new ConcurrentHashMap<>();

    private final Map<String, Message<JsonObject>> messageStore=new ConcurrentHashMap<>();
    
    private UserProfileCacheManager() {

    }
    
    /**
     * Gets instance.
     *
     * @return the instance
     */
    public static UserProfileCacheManager getInstance() {

        return INSTANCE;

    }

    /**
     * Store message.
     *
     * @param requestId the request id
     * @param message   the message
     */
    public void storeMessage(String requestId, Message<JsonObject> message) {

        messageStore.put(requestId, message);

    }


    /**
     * Retrieve message message.
     *
     * @param requestId the request id
     * @return the message
     */
    public Message<JsonObject> retrieveMessage(String requestId) {

        return messageStore.get(requestId);

    }


    /**
     * Remove message.
     *
     * @param requestId the request id
     */
    public void removeMessage(String requestId) {

        messageStore.remove(requestId);

    }

    /**
     * Gets credential data.
     *
     * @return the credential data
     */
    public Map<Integer, JsonObject> getCredentialData() {

        return credentialData;

    }

    /**
     * Gets monitoring data.
     *
     * @return the monitoring data
     */
    public Map<Integer, JsonObject> getMonitoringData() {

        return monitoringData;

    }

    /**
     * Update credential data.
     *
     * @param id      the id
     * @param newData the new data
     */
    public void updateCredentialData(int id, JsonObject newData) {

        credentialData.put(id, newData);

    }

    /**
     * Remove credential data.
     *
     * @param id the id
     */
    public void removeCredentialData(int id) {

        credentialData.remove(id);

    }

    /**
     * Update monitoring data.
     *
     * @param monitorId the monitor id
     * @param newData   the new data
     */
    public void updateMonitoringData(int monitorId, JsonObject newData) {

        monitoringData.merge(monitorId, newData, JsonObject::mergeIn);

    }


    /**
     * Remove monitoring data.
     *
     * @param monitorId the monitor id
     */
    public void removeMonitoringData(int monitorId) {

        monitoringData.remove(monitorId);

    }
}
