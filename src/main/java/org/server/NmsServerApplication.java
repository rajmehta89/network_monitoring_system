package org.server;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;

import org.server.api.RestApiServer;
import org.server.database.DatabaseConnectionManager;
import org.server.database.DatabaseRepository;
import org.server.messaging.RequestReceiver;
import org.server.messaging.RequestSender;
import org.server.scheduledJobs.PollingScheduler;
import org.server.util.UserProfileCacheManager;

import java.io.File;

import static org.server.util.Constants.*;

/**
 * The type NMS server application.
 */
public class NmsServerApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(NmsServerApplication.class);

    /**
     * Entry point of the application.
     *
     * @param args the input arguments
     */
    public static void main(String[] args) {

        Vertx vertx = Vertx.vertx();

        // Graceful shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutting down gracefully...");
            vertx.close();
        }));

        var dbManager = new DatabaseConnectionManager(vertx);
        var userCache = UserProfileCacheManager.getInstance();

        startGoPlugin().onSuccess(plugin -> {
            LOGGER.info("Go Plugin started successfully!");

            fetchCredentialProfiles(dbManager, userCache).onSuccess(v -> {
                LOGGER.info("Credential profiles loaded into cache.");

                fetchMonitoringData(dbManager, userCache).onSuccess(v2 -> {
                    LOGGER.info("Monitoring data loaded into cache.");

                    vertx.deployVerticle(new RestApiServer()).onSuccess(httpRes -> {
                        LOGGER.info("RestApiServer deployed successfully.");

                        vertx.deployVerticle(new DatabaseRepository(dbManager)).onSuccess(dbRes -> {
                            LOGGER.info("DatabaseRepository deployed successfully.");

                            vertx.deployVerticle(new RequestSender()).onSuccess(sender -> {
                                LOGGER.info("RequestSender deployed successfully.");

                                Promise<Boolean> zmqPromise = Promise.promise();
                                RequestReceiver receiver = new RequestReceiver(vertx);
                                receiver.start(zmqPromise);

                                zmqPromise.future().onSuccess(zmqRes -> {
                                    LOGGER.info("RequestReceiver deployed successfully.");

                                    vertx.deployVerticle(new PollingScheduler(dbManager)).onSuccess(pollRes -> {
                                        LOGGER.info("PollingScheduler deployed successfully.");
                                        LOGGER.info("✅ All verticles deployed successfully.");
                                    }).onFailure(err -> logAndClose(vertx, "PollingScheduler", err));

                                }).onFailure(err -> logAndClose(vertx, "RequestReceiver", err));

                            }).onFailure(err -> logAndClose(vertx, "RequestSender", err));

                        }).onFailure(err -> logAndClose(vertx, "DatabaseRepository", err));

                    }).onFailure(err -> logAndClose(vertx, "RestApiServer", err));

                }).onFailure(err -> logAndClose(vertx, "MonitoringData", err));

            }).onFailure(err -> logAndClose(vertx, "CredentialProfiles", err));

        }).onFailure(err -> logAndClose(vertx, "GoPlugin", err));

        // Block the main thread so the JVM doesn't exit
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            LOGGER.error("Main thread interrupted: ", e);
            Thread.currentThread().interrupt();
        }
    }

    private static void logAndClose(Vertx vertx, String context, Throwable err) {
        LOGGER.error("❌ Failed in " + context + ": " + err.getMessage(), err);
        vertx.close();
    }

    private static Future<Void> fetchCredentialProfiles(DatabaseConnectionManager db, UserProfileCacheManager cache) {
        Promise<Void> promise = Promise.promise();
        String query = "SELECT id, credentialconfig FROM credentialprofiles";

        db.getPool().query(query).execute().onSuccess(rows -> {
            if (rows != null && rows.size() > 0) {
                for (Row row : rows) {
                    cache.updateCredentialData(row.getInteger(ID), row.getJsonObject(CREDENTIAL_CONFIG));
                }
            }
            LOGGER.info("Fetched credential profiles.");
            promise.complete();
        }).onFailure(err -> {
            LOGGER.error("Failed to fetch credential profiles: " + err.getMessage(), err);
            promise.fail(err);
        });

        return promise.future();
    }

    private static Future<Void> fetchMonitoringData(DatabaseConnectionManager db, UserProfileCacheManager cache) {
        Promise<Void> promise = Promise.promise();
        String query = "SELECT monitor_id, credential_id, ip , port FROM provision";

        db.getPool().query(query).execute().onSuccess(rows -> {
            if (rows != null && rows.size() > 0) {
                for (Row row : rows) {
                    JsonObject monitoringData = new JsonObject()
                            .put(IP, row.getString(IP))
                            .put(PORT, row.getInteger(PORT))
                            .put(CREDENTIAL_PROFILE_ID, row.getInteger("credential_id"));

                    cache.updateMonitoringData(row.getInteger(MONITOR_ID), monitoringData);
                }
            }
            LOGGER.info("Fetched monitoring data.");
            promise.complete();
        }).onFailure(err -> {
            LOGGER.error("Failed to fetch monitoring data: " + err.getMessage(), err);
            promise.fail(err);
        });

        return promise.future();
    }

    private static Future<Boolean> startGoPlugin() {
        Promise<Boolean> promise = Promise.promise();

        try {
            new ProcessBuilder("pkill", "-f", "pluginengine").start().waitFor();
        } catch (Exception ignored) {
            // pluginengine may not be running; ignore
        }

        var projectDir = System.getProperty("user.dir");
        var goPlugin = new File(projectDir + "/go_executable/pluginengine");

        if (!goPlugin.exists() || !goPlugin.canExecute()) {
            promise.fail("Go plugin not found or not executable: " + goPlugin.getAbsolutePath());
            return promise.future();
        }

        try {
            var processBuilder = new ProcessBuilder(goPlugin.getAbsolutePath());
            var goProcess = processBuilder.start();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (goProcess.isAlive()) {
                    goProcess.destroy();
                }
            }));

            promise.complete(true);
        } catch (Exception e) {
            promise.fail(e);
        }

        return promise.future();
    }
}
