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
 * The type Nms server application.
 */
public class NmsServerApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(NmsServerApplication.class);

    /**
     * The entry point of application.
     *
     * @param args the input arguments
     */
    public static void main(String[] args) {


        var vertx = Vertx.vertx();

        var databaseConnectionManager = new DatabaseConnectionManager(vertx);

        var userProfileCacheManager = UserProfileCacheManager.getInstance();

        startGoPlugin().onSuccess(plugin -> {

            LOGGER.info("Go Plugin started successfully!");

            fetchCredentialProfiles(databaseConnectionManager, userProfileCacheManager).onSuccess(v -> {

                        LOGGER.info("Credential profiles loaded into cache.");

                        fetchMonitoringData(databaseConnectionManager, userProfileCacheManager).onSuccess(v2 -> {

                                    LOGGER.info("Monitoring data loaded into cache.");

                                    vertx.deployVerticle(new RestApiServer()).onSuccess(httpRes -> {

                                                LOGGER.info("HttpServerVerticle deployed successfully!");

                                                vertx.deployVerticle(new DatabaseRepository(databaseConnectionManager)).onSuccess(databaseRes -> {

                                                            LOGGER.info("DatabaseVerticle deployed successfully!");

                                                            vertx.deployVerticle(new RequestSender()).onSuccess(req -> {

                                                                    LOGGER.info("RequestReceiver deployed successfully!");

                                                                        Promise<Boolean> promise = Promise.promise();

                                                                        var requestReceiver = new RequestReceiver(vertx);

                                                                        requestReceiver.start(promise);

                                                                        promise.future().onSuccess(zmqRes -> {

                                                                            LOGGER.info("ZmqVerticle deployed successfully!");

                                                                            vertx.deployVerticle(new PollingScheduler(databaseConnectionManager)).onSuccess(pollingRes -> {

                                                                                LOGGER.info("PollingVerticle deployed successfully!");

                                                                                LOGGER.info("All verticles deployed successfully!");

                                                                            }).onFailure(err -> {LOGGER.error("Failed to deploy PollingVerticle: " + err.getMessage());vertx.close();});

                                                                        })
                                                                        .onFailure(err ->{ LOGGER.error("Failed to deploy ZmqVerticle: " + err.getMessage()); vertx.close();});

                                                            }).onFailure(err ->{LOGGER.error("Failed to deploy ZmqVerticle: " + err.getMessage()); vertx.close();});

                                                        })
                                                        .onFailure(err -> {LOGGER.error("Failed to deploy DatabaseVerticle: " + err.getMessage()); vertx.close();});
                                            })
                                            .onFailure(err ->{ LOGGER.error("Failed to deploy HttpServerVerticle: " + err.getMessage()); vertx.close();});
                                })
                                .onFailure(err -> {LOGGER.error("Failed to load monitoring data: " + err.getMessage()); vertx.close();});
                    })
                    .onFailure(err -> {LOGGER.error("Failed to load credential profiles: " + err.getMessage()); vertx.close();});

        }).onFailure(err ->{ LOGGER.error("Failed to start Go Plugin: " + err.getMessage()); vertx.close();});


    }


    /**
     * Fetches credential profiles from the database and updates the user profile cache.
     *
     * @param databaseConnectionManager The database connection manager.
     * @param userProfileCacheManager   The user profile cache manager.
     * @return A future that completes when credential profiles are fetched and updated.
     */
    private static Future<Void> fetchCredentialProfiles(DatabaseConnectionManager databaseConnectionManager, UserProfileCacheManager userProfileCacheManager) {

        Promise<Void> promise = Promise.promise();

        var query = "SELECT id, credentialconfig FROM credentialprofiles";

        databaseConnectionManager.getPool().query(query).execute().onSuccess(rows -> {

                    if (rows != null && rows.size() != 0) {

                        for (Row row : rows) {

                            userProfileCacheManager.updateCredentialData(row.getInteger(ID), row.getJsonObject(CREDENTIAL_CONFIG));

                        }

                    }

                    LOGGER.info("Fetched credential profile successfully from the database.");

                    promise.complete();

                })
                .onFailure(err -> {

                    LOGGER.error("Failed to fetch credential profiles: " + err.getMessage(), err);

                    promise.fail(err);

                });

        return promise.future();

    }

    /**
     * Fetches monitoring data from the database and updates the user profile cache.
     *
     * @param databaseConnectionManager The database connection manager.
     * @param userProfileCacheManager   The user profile cache manager.
     * @return A future that completes when monitoring data is fetched and updated.
     */
    private static Future<Void> fetchMonitoringData(DatabaseConnectionManager databaseConnectionManager, UserProfileCacheManager userProfileCacheManager) {

        Promise<Void> promise = Promise.promise();

        var query = "SELECT monitor_id, credential_id, ip , port FROM provision";

        databaseConnectionManager.getPool().query(query).execute().onSuccess(rows -> {

                    if (rows != null && rows.size() != 0) {

                        for (Row row : rows) {

                            userProfileCacheManager.updateMonitoringData(row.getInteger(MONITOR_ID), new JsonObject().put(IP, row.getString(IP)).put(PORT, row.getInteger(PORT)).put(CREDENTIAL_PROFILE_ID, row.getInteger("credential_id")));

                        }

                    }

                    LOGGER.info("Fetched monitoring data successfully from the database.");

                    promise.complete();

                })
                .onFailure(err -> {

                    LOGGER.error("Failed to fetch monitoring data: " + err.getMessage(), err);

                    promise.fail(err);

                });

        return promise.future();

    }


    /**
     * Runs the Go plugin and ensures it starts successfully.
     * Kills any existing Go process before starting a new one.
     *
     * @return A future that completes with true if the Go plugin starts successfully, otherwise fails.
     */
    private static Future<Boolean> startGoPlugin() {

        Promise<Boolean> promise = Promise.promise();

        try {

             var killProcessBuilder = new ProcessBuilder("pkill", "-f", "pluginengine");

             killProcessBuilder.start().waitFor();

        } catch (Exception e) {

            promise.fail(e);

            return promise.future();

        }

        var projectDir = System.getProperty("user.dir");

        var goPlugin = new File(projectDir + "/go_executable/pluginengine");

        if (!goPlugin.exists() || !goPlugin.canExecute()) {

            promise.fail("Go plugin file not found or not accessible: " + goPlugin.getAbsolutePath());

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
