package org.server.scheduledJobs;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Pool;
import org.server.database.DatabaseConnectionManager;
import org.server.util.UserProfileCacheManager;

import java.util.Map;

import static org.server.util.Constants.*;

/**
 * The type Polling scheduler.
 */
public class PollingScheduler extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(PollingScheduler.class);

    private final Pool pool;

    private final UserProfileCacheManager userProfileCacheManager;

    private Map<Integer, JsonObject> monitoringData;

    private Map<Integer, JsonObject> credentialData;

    private long timerId; // Store the timer ID for cancellation


    /**
     * Handles periodic polling of provisioned monitoring profiles by:
     * - Retrieving monitoring and credential data from cache
     * - Validating profile configurations
     * - Sending ZMQ requests for each valid profile
     */
    public PollingScheduler(DatabaseConnectionManager databaseConnectionManager) {

        this.pool = databaseConnectionManager.getPool();

        this.userProfileCacheManager = UserProfileCacheManager.getInstance();

    }


    /**
     * Starts the periodic polling process when verticle is deployed.
     * Sets up a timer to fetch profiles at fixed intervals.
     *
     * @param startPromise Promise to complete when startup finishes
     */
    @Override
    public void start(Promise<Void> startPromise) {

        timerId = vertx.setPeriodic(TIMETOPOLL, id -> fetchProvisionedProfiles());

        startPromise.complete();

    }

    /**
     * Retrieves and processes all provisioned monitoring profiles.
     * For each valid profile:
     * 1. Validates required fields exist
     * 2. Combines monitoring data with credentials
     * 3. Sends ZMQ polling request
     */
    private void fetchProvisionedProfiles() {

        monitoringData = userProfileCacheManager.getMonitoringData();

        credentialData = userProfileCacheManager.getCredentialData();

        for (Map.Entry<Integer, JsonObject> monitorEntry : monitoringData.entrySet()) {

            var monitorID = monitorEntry.getKey();

            var monitordata = monitorEntry.getValue();

            var credentialConfig = credentialData.get(monitordata.getInteger(CREDENTIAL_PROFILE_ID));

            var responseObject = new JsonObject()
                    .put(REQUESTTYPE, PROVISIONING)
                    .put(IP, monitordata.getString(IP, "0.0.0.0"))
                    .put(PORT, monitordata.getInteger(PORT, 5985))
                    .put(USERNAME, credentialConfig.getString(USERNAME, "default_user"))
                    .put(PASSWORD, credentialConfig.getString(PASSWORD, "default_password"))
                    .put(SYSTEMTYPE, credentialConfig.getString(SYSTEM_TYPE, "unknown"))
                    .put(MONITOR_ID, monitorID);

            vertx.eventBus().<JsonObject>send(ZMQ_POLLING_REQUEST, responseObject, new DeliveryOptions().setSendTimeout(ZMQPOLLINGREQUESTTIMEOUT));

        }
    }


    /**
     * Clean shutdown handler for the verticle.
     *
     * @param stopPromise Promise to complete when shutdown finishes
     */

    @Override
    public void stop(Promise<Void> stopPromise) {

        if (timerId != 0) {

            vertx.cancelTimer(timerId);

            LOGGER.info("PollingScheduler timer cancelled.");

        }

        stopPromise.complete();

    }

}
