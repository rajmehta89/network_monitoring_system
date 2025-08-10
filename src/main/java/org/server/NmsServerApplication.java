package org.server;

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

public class NmsServerApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(NmsServerApplication.class);

    public static void main(String[] args) {

        LOGGER.info("▶ NMS Server Application starting...");

        var vertx = Vertx.vertx();

        LOGGER.info("✔ Vert.x instance created.");

        var databaseConnectionManager = new DatabaseConnectionManager(vertx);

        LOGGER.info("✔ DatabaseConnectionManager initialized.");

        var userProfileCacheManager = UserProfileCacheManager.getInstance();

        LOGGER.info("✔ UserProfileCacheManager retrieved.");

//        startGoPlugin().onSuccess(plugin -> {

            LOGGER.info("✔ Go Plugin started successfully!");

            fetchCredentialProfiles(databaseConnectionManager, userProfileCacheManager).onSuccess(v -> {

                LOGGER.info("✔ Credential profiles loaded into cache.");

                fetchMonitoringData(databaseConnectionManager, userProfileCacheManager).onSuccess(v2 -> {

                    LOGGER.info("✔ Monitoring data loaded into cache.");

                    vertx.deployVerticle(new RestApiServer()).onSuccess(httpRes -> {

                        LOGGER.info("✔ RestApiServer deployed successfully!");

                        vertx.deployVerticle(new DatabaseRepository(databaseConnectionManager)).onSuccess(databaseRes -> {

                            LOGGER.info("✔ DatabaseRepository Verticle deployed successfully!");

                            vertx.deployVerticle(new RequestSender()).onSuccess(req -> {

                                LOGGER.info("✔ RequestSender deployed successfully!");

                                Promise<Boolean> promise = Promise.promise();

                                var requestReceiver = new RequestReceiver(vertx);

                                LOGGER.info("▶ Starting RequestReceiver...");

                                requestReceiver.start(promise);

                                promise.future().onSuccess(zmqRes -> {

                                    LOGGER.info("✔ RequestReceiver (ZMQ) started successfully!");

                                    vertx.deployVerticle(new PollingScheduler(databaseConnectionManager)).onSuccess(pollingRes -> {

                                        LOGGER.info("✔ PollingScheduler deployed successfully!");

                                        LOGGER.info("🎯 NMS Server Application started successfully! All verticles deployed.");

                                    }).onFailure(err -> {
                                        LOGGER.error("❌ Failed to deploy PollingScheduler", err);
                                        vertx.close();
                                    });

                                }).onFailure(err -> {
                                    LOGGER.error("❌ Failed to start RequestReceiver (ZMQ)", err);
                                    vertx.close();
                                });

                            }).onFailure(err -> {
                                LOGGER.error("❌ Failed to deploy RequestSender", err);
                                vertx.close();
                            });

                        }).onFailure(err -> {
                            LOGGER.error("❌ Failed to deploy DatabaseRepository", err);
                            vertx.close();
                        });

                    }).onFailure(err -> {
                        LOGGER.error("❌ Failed to deploy RestApiServer", err);
                        vertx.close();
                    });

                }).onFailure(err -> {
                    LOGGER.error("❌ Failed to load monitoring data", err);
                    vertx.close();
                });

            }).onFailure(err -> {
                LOGGER.error("❌ Failed to load credential profiles", err);
                vertx.close();
            });

//        }).onFailure(err -> {
//            LOGGER.error("❌ Failed to start Go Plugin", err);
//            vertx.close();
//        });
    }

    private static Future<Void> fetchCredentialProfiles(DatabaseConnectionManager databaseConnectionManager,
                                                        UserProfileCacheManager userProfileCacheManager) {

        Promise<Void> promise = Promise.promise();

        LOGGER.info("▶ Fetching credential profiles from database...");

        String query = "SELECT id, credentialconfig FROM credentialprofiles";

        databaseConnectionManager.getPool().query(query).execute().onSuccess(rows -> {

            if (rows != null && rows.size() != 0) {
                for (Row row : rows) {
                    userProfileCacheManager.updateCredentialData(
                            row.getInteger(ID), row.getJsonObject(CREDENTIAL_CONFIG)
                    );
                }
            }

            LOGGER.info("✔ Credential profiles fetched successfully. Count: " + (rows != null ? rows.size() : 0));

            promise.complete();

        }).onFailure(err -> {

            LOGGER.error("❌ Failed to fetch credential profiles", err);

            promise.fail(err);
        });

        return promise.future();
    }

    private static Future<Void> fetchMonitoringData(DatabaseConnectionManager databaseConnectionManager,
                                                    UserProfileCacheManager userProfileCacheManager) {

        Promise<Void> promise = Promise.promise();

        LOGGER.info("▶ Fetching monitoring data from database...");

        String query = "SELECT monitor_id, credential_id, ip , port FROM provision";

        databaseConnectionManager.getPool().query(query).execute().onSuccess(rows -> {

            if (rows != null && rows.size() != 0) {
                for (Row row : rows) {
                    userProfileCacheManager.updateMonitoringData(
                            row.getInteger(MONITOR_ID),
                            new JsonObject()
                                    .put(IP, row.getString(IP))
                                    .put(PORT, row.getInteger(PORT))
                                    .put(CREDENTIAL_PROFILE_ID, row.getInteger("credential_id"))
                    );
                }
            }

            LOGGER.info("✔ Monitoring data fetched successfully. Count: " + (rows != null ? rows.size() : 0));

            promise.complete();

        }).onFailure(err -> {

            LOGGER.error("❌ Failed to fetch monitoring data", err);

            promise.fail(err);
        });

        return promise.future();
    }

//    private static Future<Boolean> startGoPlugin() {
//        Promise<Boolean> promise = Promise.promise();
//        LOGGER.info("▶ Attempting to start Go Plugin...");
//
//        String projectDir = System.getProperty("user.dir");
//        File goPlugin = new File(projectDir + "/go_executable/pluginengine");
//
//        if (!goPlugin.exists()) {
//            LOGGER.error("❌ Go plugin file not found: " + goPlugin.getAbsolutePath());
//            promise.fail("Go plugin file not found: " + goPlugin.getAbsolutePath());
//            return promise.future();
//        }
//        if (!goPlugin.canExecute()) {
//            LOGGER.error("❌ Go plugin is not executable. Please check Dockerfile chmod.");
//            promise.fail("Go plugin is not executable: " + goPlugin.getAbsolutePath());
//            return promise.future();
//        }
//
//        // Try to kill stale processes, but if this fails it's not fatal
//        try {
//            new ProcessBuilder("pkill", "-f", "pluginengine").start().waitFor();
//        } catch (Exception e) {
//            LOGGER.warn("No old Go process to kill or failed to kill: " + e.getMessage());
//        }
//
//        try {
//            ProcessBuilder builder = new ProcessBuilder(goPlugin.getAbsolutePath());
//            builder.redirectErrorStream(true);
//
//            Process goProcess = builder.start();
//
//            // Optionally: wait a short time and check if it exits immediately
//            boolean exited = goProcess.waitFor(2, java.util.concurrent.TimeUnit.SECONDS);
//            int exitCode = -999;
//
//            if (exited) {
//                exitCode = goProcess.exitValue();
//                LOGGER.error("❌ Go plugin exited immediately with exit code " + exitCode);
//                // Optionally read and report the output
//                String output = new String(goProcess.getInputStream().readAllBytes());
//                LOGGER.error("Output: " + output);
//                promise.fail("Go plugin process exited too quickly with code " + exitCode + ". Output: " + output);
//                return promise.future();
//            }
//
//            // Start a background thread to read output (optional)
//            new Thread(() -> {
//                try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(goProcess.getInputStream()))) {
//                    String line;
//                    while ((line = reader.readLine()) != null) {
//                        LOGGER.info("[GoPlugin] " + line);
//                    }
//                } catch (Exception ex) {
//                    LOGGER.error("Error reading plugin output", ex);
//                }
//            }).start();
//
//            // Shutdown hook as before
//            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//                LOGGER.info("Shutting down Go plugin process...");
//                if (goProcess.isAlive()) goProcess.destroy();
//            }));
//
//            LOGGER.info("✔ Go plugin process launched and alive.");
//            promise.complete(true);
//
//        } catch (Exception e) {
//            LOGGER.error("Failed to launch Go plugin process: ", e);
//            promise.fail("Failed to launch Go plugin: " + e.getMessage());
//        }
//        return promise.future();
//    }

}
