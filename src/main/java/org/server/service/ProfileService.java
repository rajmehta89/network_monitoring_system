package org.server.service;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import org.server.util.UserProfileCacheManager;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.server.util.Constants.*;
import static org.server.util.Validation.checkAvailability;

/**
 * The type Profile service.
 */
public class ProfileService {

    private final Pool pool;

    private final Logger logger = LoggerFactory.getLogger(ProfileService.class);

    private final String tableName;

    private final Map<String, String> columnMappings;

    private final UserProfileCacheManager userProfileCacheManager;

    private final Vertx vertx;

    private final EventBus eventBus;

    /**
     * Instantiates a new Profile service.
     *
     * @param pool           the pool
     * @param tableName      the table name
     * @param columnMappings the column mappings
     * @param vertx          the vertx
     */
    public ProfileService(Pool pool, String tableName, Map<String, String> columnMappings, Vertx vertx) {

        this.pool = pool;

        this.tableName = tableName;

        this.columnMappings = columnMappings;

        this.vertx = vertx;

        this.eventBus = vertx.eventBus();

        this.userProfileCacheManager = UserProfileCacheManager.getInstance();

    }

    /**
     * Generalized method to execute database operations in a blocking manner
     *
     * @param operation The database operation to execute
     * @param <T>       The type of result returned by the operation
     * @return A Future containing the result of the operation
     */
    private <T> Future<T> executeDbOperation(Function<Pool, Future<T>> operation) {

        Promise<T> promise = Promise.promise();

        vertx.executeBlocking(p -> {

            operation.apply(pool).onComplete(ar -> {

                if (ar.succeeded()) {

                    p.complete(ar.result());

                } else {

                    p.fail(ar.cause());

                }

            });


        }).onComplete(ar -> {

            if (ar.succeeded()) {

                promise.complete((T) ar.result());

            } else {

                promise.fail(ar.cause());

            }

        });

        return promise.future();

    }

    /**
     * Convert a RowSet to a JsonArray
     *
     * @param rows The RowSet to convert
     * @return A JsonArray containing the rows
     */
    private JsonArray rowSetToJsonArray(RowSet<Row> rows) {

        var dataArray = new JsonArray();

        for (Row row : rows) {

            var dataObject = new JsonObject();

            for (int i = 0; i < row.size(); i++) {

                if (CREDENTIAL_CONFIG.equals(row.getColumnName(i)) && row.getValue(i) instanceof String) {

                    dataObject.put(row.getColumnName(i), new JsonObject((String) row.getValue(i)));

                } else {

                    dataObject.put(row.getColumnName(i), row.getValue(i));
                }

            }

            dataArray.add(dataObject);

        }

        return dataArray;

    }

    /**
     * Gets memory check.
     *
     * @param response the response
     * @return the memory check
     */
    public Future<JsonObject> getMemoryCheck(JsonObject response) {

        Promise<JsonObject> promise = Promise.promise();

        var sql = """
                    SELECT id,
                           monitor_id,
                           system_info->>'SystemName' AS system_name,
                           (system_info->>'SystemMemoryFreeBytes')::bigint AS memory_free_bytes,
                           (system_info->>'SystemMemoryCommittedBytes')::bigint AS committed_memory_bytes,
                           (system_info->>'SystemMemoryInstalledBytes')::bigint AS total_memory_bytes,
                           fetched_at
                    FROM systemdata
                    WHERE (system_info->>'SystemMemoryFreeBytes')::bigint < 500 * 1024 * 1024
                      AND (system_info->>'SystemMemoryCommittedBytes')::bigint > (system_info->>'SystemMemoryInstalledBytes')::bigint * 0.9;
                """;

        executeDbOperation(pool -> pool.preparedQuery(sql).execute()).onComplete(ar -> {

            if (ar.succeeded()) {

                var dataArray = rowSetToJsonArray(ar.result());

                promise.complete(response.put(STATUS, SUCCESS).put(DATA, dataArray));

            } else {

                logger.error("Error fetching memory data: " + ar.cause().getMessage(), ar.cause());

                promise.fail("Error fetching memory data: " + ar.cause().getMessage());

            }

        });

        return promise.future();

    }

    /**
     * Gets cpu spikes.
     *
     * @param response the response
     * @return the cpu spikes
     */
    public Future<JsonObject> getCPUSpikes(JsonObject response) {

        Promise<JsonObject> promise = Promise.promise();

        var sql = """
                WITH cpu_usage_data AS (
                    SELECT 
                        id,
                        monitor_id,
                        system_info->>'SystemName' AS system_name,
                        system_info->>'SystemCPUType' AS cpu_type,
                        (system_info->>'SystemCPUPercent')::numeric AS current_cpu_usage,
                        (system_info->>'SystemCPUUserPercent')::numeric AS user_cpu_usage,
                        LAG((system_info->>'SystemCPUPercent')::numeric) 
                            OVER (PARTITION BY monitor_id ORDER BY fetched_at) AS previous_cpu_usage,
                        (system_info->>'SystemThreads')::int AS running_threads,
                        (system_info->>'SystemNetworkTCPConnections')::int AS tcp_connections,
                        (system_info->>'SystemProcessorQueueLength')::int AS processor_queue_length,
                        (system_info->>'SystemContextSwitchesPerSec')::int AS context_switches_per_sec,
                        (system_info->>'SystemDiskFreePercent')::numeric AS disk_free_percent,
                        (system_info->>'SystemDiskUsedPercent')::numeric AS disk_used_percent,
                        (system_info->>'SystemMemoryFreeBytes')::bigint AS memory_free_bytes,
                        (system_info->>'SystemMemoryUsedBytes')::bigint AS memory_used_bytes,
                        fetched_at
                    FROM systemdata
                )
                SELECT *
                FROM cpu_usage_data
                WHERE current_cpu_usage - COALESCE(previous_cpu_usage, 0) > 40
                ORDER BY fetched_at DESC;
                """;

        executeDbOperation(pool -> pool.preparedQuery(sql).execute()).onComplete(ar -> {

            if (ar.succeeded()) {

                var dataArray = rowSetToJsonArray(ar.result());

                promise.complete(response.put(STATUS, SUCCESS).put(DATA, dataArray));

            } else {

                logger.error("Error fetching CPU spike data: " + ar.cause().getMessage(), ar.cause());

                promise.fail("Error fetching CPU spike data: " + ar.cause().getMessage());

            }

        });

        return promise.future();

    }

    /**
     * Gets top cpu spikes.
     *
     * @param response the response
     * @return the top cpu spikes
     */
    public Future<JsonObject> getTopCPUSpikes(JsonObject response) {

        Promise<JsonObject> promise = Promise.promise();

        var sql = """
                WITH cpu_spike_data AS (
                    SELECT 
                        monitor_id,
                        system_info->>'SystemName' AS system_name,
                        COALESCE((system_info->>'SystemCPUPercent')::numeric, 0) AS current_cpu_usage,
                        COALESCE(LAG((system_info->>'SystemCPUPercent')::numeric) 
                                 OVER (PARTITION BY monitor_id ORDER BY fetched_at), 0) AS previous_cpu_usage,
                        fetched_at
                    FROM systemdata
                    WHERE system_info ? 'SystemCPUPercent'
                )
                SELECT 
                    system_name, 
                    current_cpu_usage, 
                    previous_cpu_usage, 
                    (current_cpu_usage - previous_cpu_usage) AS cpu_spike, 
                    fetched_at
                FROM cpu_spike_data
                ORDER BY cpu_spike DESC
                LIMIT 5;
                """;

        executeDbOperation(pool -> pool.preparedQuery(sql).execute()).onComplete(ar -> {

            if (ar.succeeded()) {

                var dataArray = rowSetToJsonArray(ar.result());

                promise.complete(response.put(STATUS, SUCCESS).put(DATA, dataArray));

            } else {

                logger.error("Error fetching top CPU spikes: " + ar.cause().getMessage(), ar.cause());

                promise.fail("Error fetching top CPU spikes: " + ar.cause().getMessage());

            }

        });

        return promise.future();

    }

    /**
     * Insert system data future.
     *
     * @param request the request
     * @return the future
     */
    public Future<JsonObject> insertSystemData(JsonObject request) {

        Promise<JsonObject> promise = Promise.promise();

        var monitorId = request.getInteger(MONITOR_ID);

        var systemInfo = request.getJsonObject(SYSTEM_INFO);

        var fetchedAt = request.getLong(FETCHEDAT);

        var errorInfo = request.getJsonObject(ERROR);

        var dataToInsert = (systemInfo != null && !systemInfo.isEmpty()) ? systemInfo : (errorInfo != null && !errorInfo.isEmpty()) ? errorInfo : null;

        var sql = "INSERT INTO SystemData (monitor_id, system_info, fetched_at) VALUES ($1, $2, $3)";

        executeDbOperation(pool -> pool.preparedQuery(sql).execute(Tuple.of(monitorId, dataToInsert, fetchedAt))).onComplete(ar -> {

            if (ar.succeeded()) {

                var rows = ar.result();

                if (rows.rowCount() > 0) {

                    logger.info("System data inserted successfully for monitor_id: " + monitorId);

                    request.put(STATUS, SUCCESS).put(MESSAGE, "System data inserted successfully for monitor_id: " + monitorId);

                    request.remove(SYSTEM_INFO);

                    request.remove(ERROR);

                    promise.complete(request);

                } else {

                    logger.warn("No data inserted for monitor_id: " + monitorId);

                    promise.fail("No data inserted for monitor_id: " + monitorId);

                }

            } else {

                logger.error("Failed to insert system data for monitor_id: " + monitorId + ": " + ar.cause().getMessage(), ar.cause());

                promise.fail("Failed to insert system data: " + ar.cause().getMessage());

            }

        });

        return promise.future();

    }

    /**
     * Create profile future.
     *
     * @param profileData the profile data
     * @return the future
     */
    public Future<JsonObject> createProfile(JsonObject profileData) {

        Promise<JsonObject> promise = Promise.promise();

        List<String> columnNames = new ArrayList<>();

        List<String> placeholders = new ArrayList<>();

        var params = buildInsertParams(profileData, columnNames, placeholders);

        var query = """
                INSERT INTO %s (%s) 
                VALUES (%s) 
                RETURNING id
                """.formatted(tableName, String.join(", ", columnNames), String.join(", ", placeholders));

        pool.preparedQuery(query)
                .execute(params)
                .onSuccess(rows -> {

                    if (rows.rowCount() == 0) return;

                    var row = rows.iterator().next();

                    var profileId = row.getInteger(ID);

                    profileData.put(ID, profileId);

                    logger.info("Profile created successfully with ID:" + profileId);

                    if (profileData.containsKey(CREDENTIAL_PROFILE_NAME) || profileData.containsKey(CREDENTIAL_CONFIG)) {

                        userProfileCacheManager.updateCredentialData(profileId, profileData.getJsonObject(CREDENTIAL_CONFIG));

                    } else {

                        logger.info("Skipping storage in credentialProfiles as it does not contain required fields.");

                    }

                    var response = new JsonObject().put(STATUS, SUCCESS).put(MESSAGE, "Profile created successfully").put(ID, profileId);

                    promise.complete(response);

                })
                .onFailure(err -> {

                    logger.error("Error creating profile", err);

                    promise.fail(err);

                });

        return promise.future();

    }


    private Tuple buildInsertParams(JsonObject profileData, List<String> columnNames, List<String> placeholders) {

        var params = Tuple.tuple();

        var index = 1;

        for (Map.Entry<String, String> entry : columnMappings.entrySet()) {

            var key = entry.getKey();

            var dbColumn = entry.getValue();

            var value = profileData.getValue(key);

            if (!(DISCOVERY_STATUS.equals(key) && value == null)) {

                columnNames.add(dbColumn);

                placeholders.add("$" + index++);

                params.addValue(value);

            }

        }

        return params;

    }

    /**
     * Gets all profiles.
     *
     * @return the all profiles
     */
    public Future<JsonArray> getAllProfiles() {

        Promise<JsonArray> promise = Promise.promise();

        var sql = "SELECT * FROM " + tableName;

        executeDbOperation(pool -> pool.preparedQuery(sql).execute()).onComplete(ar -> {

            if (ar.succeeded()) {

                 var rows = ar.result();

                 var profiles = rowSetToJsonArray(rows);

                 promise.complete(profiles); // Return the profiles as a JsonArray

            } else {

                logger.error("Error fetching profiles: " + ar.cause().getMessage(), ar.cause());

                promise.fail("Error fetching profiles: " + ar.cause().getMessage());

            }

        });

        return promise.future();

    }


    /**
     * Gets profile by id.
     *
     * @param response the response
     * @return the profile by id
     */
    public Future<JsonObject> getProfileById(JsonObject response) {

        Promise<JsonObject> promise = Promise.promise();

        var id = response.getInteger(ID);

        var sql = "SELECT * FROM %s WHERE id = $1".formatted(tableName);

        executeDbOperation(pool -> pool.preparedQuery(sql).execute(Tuple.of(id))).onComplete(ar -> {

            if (ar.succeeded()) {

                var rows = ar.result();

                if (!rows.iterator().hasNext()) {

                    logger.warn("Profile not found in DB for ID: " + id);

                    promise.fail("Profile not found for ID: " + id);

                } else {

                    var row = rows.iterator().next();

                    response.remove(ID);

                    response.put(STATUS, SUCCESS);

                    for (int i = 0; i < row.size(); i++) {

                        response.put(row.getColumnName(i), row.getValue(i));

                    }

                    logger.info("Profile found for id and get successfully" + id);

                    promise.complete(response.put(MESSAGE, "Profile found successfully"));

                }

            } else {

                logger.error("Error fetching profile for ID: " + id + ": " + ar.cause().getMessage(), ar.cause());

                promise.fail("Error fetching profile: " + ar.cause().getMessage());

            }

        });

        return promise.future();
    }

    /**
     * Update profile future.
     *
     * @param response the response
     * @return the future
     */
    public Future<JsonObject> updateProfile(JsonObject response) {

        Promise<JsonObject> promise = Promise.promise();

        var id = response.getInteger(ID);

        executeDbOperation(pool -> pool.preparedQuery("SELECT * FROM " + tableName + " WHERE id = $1").execute(Tuple.of(id)))
                .onComplete(ar -> {

                    if (ar.succeeded()) {

                        var rows = ar.result();

                        if (rows.rowCount() == 0) {

                            logger.error("Profile not found for ID: " + id);

                            promise.fail("Profile not found for ID: " + id);

                        } else {

                            var existingRow = rows.iterator().next();

                            var updates = new StringBuilder();

                            List<Object> values = new ArrayList<>();

                            var index = new AtomicInteger(1);

                            var changesDetected = false;

                            for (Map.Entry<String, String> entry : columnMappings.entrySet()) {

                                var key = entry.getKey();

                                var dbColumn = entry.getValue();

                                var oldValue = existingRow.getValue(dbColumn);

                                var newValue = response.getValue(key);

                                if (!Objects.equals(newValue, oldValue)) {

                                    if (updates.length() > 0) updates.append(", ");

                                    updates.append(dbColumn).append(" = $").append(index.getAndIncrement());

                                    values.add(newValue);

                                    changesDetected = true;

                                }

                            }

                            if (changesDetected) {

                                values.add(id);

                                var sql = "UPDATE %s SET %s WHERE id = $%d RETURNING *".formatted(tableName, updates, index.get());

                                executeDbOperation(p -> p.preparedQuery(sql).execute(Tuple.from(values)))
                                        .onComplete(updateAr -> {

                                            if (updateAr.succeeded()) {

                                                var updatedRows = updateAr.result();

                                                var updatedRow = updatedRows.iterator().next();

                                                columnMappings.forEach((jsonKey, dbColumn) -> response.put(jsonKey, updatedRow.getValue(dbColumn)));

                                                if (response.containsKey(CREDENTIAL_CONFIG)) {

                                                    userProfileCacheManager.updateCredentialData(id, response.getJsonObject(CREDENTIAL_CONFIG));

                                                }

                                                logger.info("Profile updated successfully for ID: " + id);

                                                response.clear();

                                                response.put(STATUS, SUCCESS).put(MESSAGE, "Profile updated successfully for ID: " + id).put(ID, id);

                                                promise.complete(response);

                                            } else {

                                                logger.error("Error updating profile for ID: " + id + ": " + updateAr.cause().getMessage(), updateAr.cause());

                                                promise.fail("Error updating profile: " + updateAr.cause().getMessage());

                                            }
                                        });
                            } else {

                                logger.warn("No new changes detected for ID: " + id);

                                promise.fail("No new changes detected; the new data is the same as old data");

                            }

                        }

                    } else {

                        logger.error("Error checking profile for ID: " + id + ": " + ar.cause().getMessage(), ar.cause());

                        promise.fail("Error checking profile: " + ar.cause().getMessage());

                    }

                });

        return promise.future();

    }

    /**
     * Delete profile future.
     *
     * @param response the response
     * @return the future
     */
    public Future<JsonObject> deleteProfile(JsonObject response) {

        Promise<JsonObject> promise = Promise.promise();

        var id = response.getInteger(ID);

        checkProvisionStatus(id).onComplete(checkAr -> {

            if (checkAr.succeeded()) {

                var isActive = checkAr.result();

                if (isActive) {

                    logger.warn("Deletion failed: Profile is linked to an active provision.");

                    promise.fail("Deletion failed: Profile is linked to an active provision.");

                } else {

                    var sql = "DELETE FROM %s WHERE id = $1".formatted(tableName);

                    executeDbOperation(pool -> pool.preparedQuery(sql).execute(Tuple.of(id))).onComplete(deleteAr -> {
                        if (deleteAr.succeeded()) {

                            var rows = deleteAr.result();

                            if (rows.rowCount() > 0) {

                                if (TABLE_CREDENTIAL_PROFILES.equalsIgnoreCase(tableName)) {

                                    userProfileCacheManager.removeCredentialData(id);

                                }

                                logger.info("Profile deleted successfully for ID: " + id);

                                response.put(STATUS, SUCCESS).put(MESSAGE, "Profile deleted successfully for ID: " + id);

                                promise.complete(response);

                            } else {

                                logger.warn("Deletion failed: Profile not found for ID: " + id);

                                promise.fail("Deletion failed: Profile not found for ID: " + id);

                            }

                        } else {

                            logger.error("Error deleting profile for ID: " + id + ": " + deleteAr.cause().getMessage(), deleteAr.cause());

                            promise.fail("Error deleting profile: " + deleteAr.cause().getMessage());

                        }
                    });
                }
            } else {

                logger.error("Error checking provision status: " + checkAr.cause().getMessage(), checkAr.cause());

                promise.fail("Error checking provision status: " + checkAr.cause().getMessage());

            }
        });

        return promise.future();
    }

    private Future<Boolean> checkProvisionStatus(int id) {

        Promise<Boolean> promise = Promise.promise();

        var checkProvisionSql = "SELECT is_active FROM Provision WHERE credential_id = $1";

        executeDbOperation(pool -> pool.preparedQuery(checkProvisionSql).execute(Tuple.of(id))).onComplete(ar -> {

            if (ar.succeeded()) {

                var rows = ar.result();

                promise.complete(rows.rowCount() > 0 && rows.iterator().next().getBoolean("is_active"));

            } else {

                promise.fail(ar.cause().getMessage());

            }

        });

        return promise.future();

    }

    /**
     * Delete monitor future.
     *
     * @param response the response
     * @return the future
     */
    public Future<JsonObject> deleteMonitor(JsonObject response) {

        Promise<JsonObject> promise = Promise.promise();

        var monitorId = response.getInteger(MONITOR_ID);

        executeDbOperation(pool -> pool.preparedQuery("SELECT is_active FROM provision WHERE monitor_id = $1")
                .execute(Tuple.of(monitorId)))
                .onComplete(checkAr -> {
                    if (checkAr.succeeded()) {

                        var rows = checkAr.result();

                        if (rows.iterator().hasNext()) {

                            var isActive = rows.iterator().next().getBoolean(IS_ACTIVE);

                            if (isActive) {

                                var updateSql = "UPDATE provision SET is_active = false WHERE monitor_id = $1";

                                executeDbOperation(pool -> pool.preparedQuery(updateSql).execute(Tuple.of(monitorId)))
                                        .onComplete(updateAr -> {

                                            if (updateAr.succeeded()) {

                                                var updateRows = updateAr.result();

                                                if (updateRows.rowCount() > 0) {

                                                    userProfileCacheManager.removeMonitoringData(monitorId);

                                                    logger.info("Monitor deleted successfully for monitor_id: " + monitorId);

                                                    response.put(STATUS, SUCCESS).put(MONITOR_ID, monitorId).put("message", "Monitor deleted successfully.");

                                                    promise.complete(response);

                                                } else {

                                                    logger.warn("Failed to delete monitor.");

                                                    promise.fail("Failed to delete monitor.");

                                                }
                                            } else {

                                                logger.error("Error deleting monitor: " + updateAr.cause().getMessage(), updateAr.cause());

                                                promise.fail("Error deleting monitor: " + updateAr.cause().getMessage());

                                            }
                                        });

                            } else {

                                logger.warn("Monitor ID " + monitorId + " is already inactive.");

                                promise.fail("Monitor is already deleted.");

                            }

                        } else {

                            logger.warn("Monitor ID " + monitorId + " not found in the database.");

                            promise.fail("Monitor ID not found.");

                        }
                    } else {

                        logger.error("Error checking monitor: " + checkAr.cause().getMessage(), checkAr.cause());

                        promise.fail("Error checking monitor: " + checkAr.cause().getMessage());

                    }
                });

        return promise.future();
    }


    /**
     * Gets provision device data.
     *
     * @param response the response
     * @return the provision device data
     */
    public Future<JsonObject> getProvisionDeviceData(JsonObject response) {

        Promise<JsonObject> promise = Promise.promise();

        var monitorId = response.getInteger(MONITOR_ID);

        var selectSql = "SELECT system_info, fetched_at FROM systemdata WHERE monitor_id = $1";

        executeDbOperation(pool -> pool.preparedQuery(selectSql).execute(Tuple.of(monitorId))).onComplete(ar -> {

            if (ar.succeeded()) {

                var result = ar.result();

                var systemInfoArray = new JsonArray();

                if (result.rowCount() > 0) {

                    for (Row row : result) {

                        var systemInfo = row.getJsonObject(SYSTEM_INFO);

                        var formattedTimestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(row.getLong(FETCHEDAT)));

                        systemInfo.put(FETCHEDAT, formattedTimestamp);

                        systemInfoArray.add(systemInfo);

                    }

                    response.put(STATUS, SUCCESS).put(SYSTEM_INFO, systemInfoArray);

                } else {

                    response.put(STATUS, FAIL).put(ERROR, "No system info found for monitor_id: " + monitorId);

                }

                promise.complete(response);

            } else {

                logger.error("Database query failed for monitor ID: " + monitorId + ": " + ar.cause().getMessage(), ar.cause());

                promise.fail("Database query failed: " + ar.cause().getMessage());

            }

        });

        return promise.future();

    }

    /**
     * Provision device future.
     *
     * @param response the response
     * @return the future
     */
    public Future<JsonObject> provisionDevice(JsonObject response) {

        Promise<JsonObject> promise = Promise.promise();

        var discoveryProfileId = response.getInteger(DISCOVERY_PROFILE_ID);


        var fetchSql = """
                SELECT credential_profile_id, ip, port, discovery_status
                FROM %s
                WHERE id = $1
                """.formatted(TABLE_DISCOVERY_PROFILES);

        executeDbOperation(pool -> pool.preparedQuery(fetchSql).execute(Tuple.of(discoveryProfileId))).onComplete(fetchAr -> {

            if (fetchAr.succeeded()) {

                var rows = fetchAr.result();

                if (!rows.iterator().hasNext()) {

                    logger.warn("Profile ID " + discoveryProfileId + " not found");

                    response.put(STATUS, FAIL).put(MESSAGE, "Profile ID not found.");

                    promise.complete(response);

                } else {

                    var row = rows.iterator().next();

                    var discoveryStatus = row.getBoolean(DISCOVERY_STATUS);

                    if (discoveryStatus == null || discoveryStatus != true) {

                        logger.warn("Provisioning not allowed. Discovery status is " + discoveryStatus);

                        response.put(STATUS, FAIL).put(MESSAGE, "Discovery status is not eligible for provisioning.");

                        promise.complete(response);

                    } else {

                        var credentialId = row.getInteger(CREDENTIAL_PROFILE_ID);

                        var ip = row.getString(IP);

                        var port = row.getInteger(PORT);

                        var checkExistingSql = "SELECT monitor_id FROM Provision WHERE ip = $1 AND is_active = false";

                        executeDbOperation(pool -> pool.preparedQuery(checkExistingSql).execute(Tuple.of(ip))).onComplete(checkAr -> {

                            if (checkAr.succeeded()) {

                                var existingRows = checkAr.result();

                                if (existingRows.rowCount() > 0) {

                                    var existingMonitorId = existingRows.iterator().next().getInteger(MONITOR_ID);

                                    var updateSql = "UPDATE Provision SET is_active = true WHERE monitor_id = $1 RETURNING monitor_id";

                                    executeDbOperation(pool -> pool.preparedQuery(updateSql).execute(Tuple.of(existingMonitorId))).onComplete(updateAr -> {

                                        if (updateAr.succeeded()) {

                                            logger.info("Reactivated existing provision for monitor_id: " + existingMonitorId);

                                            userProfileCacheManager.updateMonitoringData(existingMonitorId, new JsonObject().put(IP, ip).put(PORT, port).put(CREDENTIAL_PROFILE_ID, credentialId));

                                            response.put(STATUS, SUCCESS).put(MONITOR_ID, existingMonitorId).put(CREDENTIAL_PROFILE_ID, credentialId).put(IP, ip).put(MESSAGE, "Provision reactivated successfully");

                                            promise.complete(response);

                                        } else {

                                            logger.error("Error reactivating provision: " + updateAr.cause().getMessage(), updateAr.cause());

                                            promise.fail("Error reactivating provision: " + updateAr.cause().getMessage());

                                        }

                                    });

                                } else {

                                    var insertProvisionSql = "INSERT INTO Provision (credential_id, ip, port) VALUES ($1, $2, $3) RETURNING monitor_id;";

                                    executeDbOperation(pool -> pool.preparedQuery(insertProvisionSql).execute(Tuple.of(credentialId, ip, port))).onComplete(insertAr -> {

                                        if (insertAr.succeeded()) {

                                            var monitorId = insertAr.result().iterator().next().getInteger(MONITOR_ID);

                                            userProfileCacheManager.updateMonitoringData(monitorId, new JsonObject().put(IP, ip).put(CREDENTIAL_PROFILE_ID, credentialId).put(PORT, port));

                                            response.put(STATUS, SUCCESS).put(MONITOR_ID, monitorId).put(CREDENTIAL_PROFILE_ID, credentialId).put(IP, ip).put(MESSAGE, "Provision entry created successfully");

                                            promise.complete(response);

                                        } else {

                                            logger.error("Error creating provision: " + insertAr.cause().getMessage(), insertAr.cause());

                                            promise.fail("Error creating provision: " + insertAr.cause().getMessage());

                                        }

                                    });

                                }

                            } else {

                                logger.error("Error checking existing provisions: " + checkAr.cause().getMessage(), checkAr.cause());

                                promise.fail("Error checking existing provisions: " + checkAr.cause().getMessage());

                            }

                        });

                    }

                }

            } else {

                logger.error("Error fetching discovery profile: " + fetchAr.cause().getMessage(), fetchAr.cause());

                promise.fail("Error fetching discovery profile: " + fetchAr.cause().getMessage());

            }

        });

        return promise.future();

    }

    /**
     * Update discovery status future.
     *
     * @param discoveryProfileID the discovery profile id
     * @param discoveryStatus    the discovery status
     * @return the future
     */
    public Future<JsonObject> updateDiscoveryStatus(Integer discoveryProfileID, Boolean discoveryStatus) {

        Promise<JsonObject> promise = Promise.promise();

        var response = new JsonObject();

        var sql = "UPDATE " + TABLE_DISCOVERY_PROFILES + " SET " + DISCOVERY_STATUS + " = $1 WHERE " + ID + " = $2";

        executeDbOperation(pool -> pool.preparedQuery(sql).execute(Tuple.of(discoveryStatus, discoveryProfileID))).onComplete(ar -> {

            if (ar.succeeded()) {

                var rows = ar.result();

                if (rows.rowCount() > 0) {

                    logger.info("Discovery status updated successfully for profile ID: " + discoveryProfileID);

                    promise.complete(response.put(STATUS, SUCCESS).put(MESSAGE, "Discovery status updated successfully for profile ID: " + discoveryProfileID));

                } else {

                    logger.warn("Update failed: Discovery profile not found for ID: " + discoveryProfileID);

                    promise.fail("Update failed: Discovery profile not found");

                }
            } else {

                logger.error("Error updating discovery status for profile ID: " + discoveryProfileID + ": " + ar.cause().getMessage(), ar.cause());

                promise.fail("Error updating discovery status: " + ar.cause().getMessage());

            }

        });

        return promise.future();

    }

    /**
     * Run discovery future.
     *
     * @param request the request
     * @return the future
     */
    public Future<JsonObject> runDiscovery(JsonObject request) {

        Promise<JsonObject> promise = Promise.promise();

        var response = new JsonObject();

        var discoveryProfileID = request.getInteger(ID);

        if (discoveryProfileID == null) {

            logger.error("Discovery profile ID is missing");

            promise.fail("Discovery profile ID is missing");

        }

        else {

            var sql = """
                    SELECT dp.discovery_profile_name, dp.ip, dp.port,
                           cp.credential_profile_name, cp.credentialconfig::jsonb
                    FROM discoveryprofiles dp
                    JOIN credentialprofiles cp ON dp.credential_profile_id = cp.id
                    WHERE dp.id = $1
                    """;

            executeDbOperation(pool -> pool.preparedQuery(sql).execute(Tuple.of(discoveryProfileID))).onComplete(fetchAr -> {
                if (fetchAr.succeeded()) {

                    var rows = fetchAr.result();

                    if (!rows.iterator().hasNext()) {

                        logger.warn("Profile ID " + discoveryProfileID + " not found");

                        promise.fail("Profile ID not found");

                    } else {

                        var row = rows.iterator().next();

                        var ipAddress = row.getString(IP);

                        var port = row.getInteger(PORT);

                        var credentialConfig = row.getJsonObject(CREDENTIAL_CONFIG);

                        var username = credentialConfig.getString(USERNAME, "");

                        var password = credentialConfig.getString(PASSWORD, "");

                        var systemType = credentialConfig.getString(SYSTEM_TYPE, "");

                        logger.info("Fetched profile data for ID: " + discoveryProfileID);

                        vertx.executeBlocking(p -> {

                            var isAvailable = checkAvailability(ipAddress, port);

                            p.complete(isAvailable);

                        }).onComplete(availAr -> {

                            if (availAr.succeeded()) {

                                var isAvailable = (Boolean) availAr.result();

                                if (!isAvailable) {

                                    var errorMessage = "Device is not reachable at IP: " + ipAddress + " port: " + port + "...";

                                    logger.error(errorMessage);

                                    promise.fail(errorMessage);

                                } else {

                                    logger.info("Device is reachable at IP: " + ipAddress + " on Port: " + port);

                                    response.put(IP, ipAddress).put(PORT, port).put(PASSWORD, password).put(USERNAME, username).put(SYSTEMTYPE, systemType).put(REQUESTTYPE, DISCOVERY);

                                    eventBus.<JsonObject>request(ZMQ_DISCOVERY_RUN_REQUEST, response, new DeliveryOptions().setSendTimeout(DISCOVERY_EVENT_BUS_TIMEOUT)).onComplete(replyAr -> {

                                        if (replyAr.succeeded()) {

                                            var replyJson = replyAr.result().body();

                                            var status = replyJson.getString(STATUS, FAIL);

                                            if (SUCCESS.equalsIgnoreCase(status)) {

                                                updateDiscoveryStatus(discoveryProfileID, true).onComplete(updateAr -> {

                                                    if (updateAr.succeeded()) {

                                                        var filteredResponse = new JsonObject().put(REQUESTTYPE, response.getString(REQUESTTYPE)).put(STATUS, SUCCESS).put(RESULT, replyJson.getJsonObject(RESULT));

                                                        logger.info("Discovery completed for profile ID: " + discoveryProfileID);

                                                        promise.complete(filteredResponse);

                                                    } else {

                                                        logger.warn("Discovery succeeded but status update failed: " + updateAr.cause().getMessage());

                                                        promise.complete(replyJson);

                                                    }

                                                });

                                            } else {

                                                logger.warn("ZMQ discovery failed for profile ID: " + discoveryProfileID + " with status: " + status);

                                                promise.complete(replyJson);

                                            }

                                        } else {

                                            logger.error("Event bus request failed: " + replyAr.cause().getMessage(), replyAr.cause());

                                            promise.fail("Event bus request failed: " + replyAr.cause().getMessage());

                                        }
                                    });
                                }
                            } else {

                                logger.error("Error checking device availability: " + availAr.cause().getMessage(), availAr.cause());

                                promise.fail("Error checking device availability: " + availAr.cause().getMessage());

                            }

                        });

                    }

                } else {

                    logger.error("Error fetching discovery profile: " + fetchAr.cause().getMessage(), fetchAr.cause());

                    promise.fail("Error fetching discovery profile: " + fetchAr.cause().getMessage());

                }
            });
        }

        return promise.future();

    }

}