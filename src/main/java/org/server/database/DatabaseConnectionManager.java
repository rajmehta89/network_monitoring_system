package org.server.database;

import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;

import static org.server.util.Constants.*;

/**
 * The type Database connection manager.
 */
public class DatabaseConnectionManager {

    private final Pool pool;

    private final Logger Logger = LoggerFactory.getLogger(DatabaseConnectionManager.class);

    /**
     * Instantiates a new Database connection manager.
     *
     * @param vertx the vertx
     */
    public DatabaseConnectionManager(Vertx vertx) {

        var connectOptions = new PgConnectOptions().setHost(DATABASEHOSTNAME).setPort(DATABASE_PORT).setDatabase(DATABASENAME).setUser(USERNAME).setPassword(DATABASEPASSWORD).setReconnectAttempts(RECONNECTATTEMPT).setReconnectInterval(RECONNECTINTERVALTIME);

        var poolOptions = new PoolOptions().setMaxSize(POOL_SIZE).setMaxWaitQueueSize(MAX_WAIT_QUEUE_SIZE).setIdleTimeout(POOLED_IDLE_TIME);

        this.pool = Pool.pool(vertx, connectOptions, poolOptions);

        Logger.info("Database connected successfully and pool created successfully");

        createTables();

    }


    /**
     * Gets pool.
     *
     * @return the pool
     */
    public Pool getPool() {

        return pool;

    }


    /**
     * Ensures that the necessary tables are created in the database.
     * This method will attempt to create the tables if they do not already exist.
     * It uses a SQL script to define the table structures and indexes.
     * Logs the success or failure of the table creation process.
     */
    private void createTables() {

        var sql = """
                    CREATE TABLE IF NOT EXISTS CredentialProfiles (
                        id SERIAL PRIMARY KEY,
                        credential_profile_name VARCHAR(255) NOT NULL UNIQUE,
                        CredentialConfig JSONB NOT NULL
                    );
                
                    CREATE TABLE IF NOT EXISTS DiscoveryProfiles (
                        id SERIAL PRIMARY KEY,
                        discovery_profile_name VARCHAR(255) NOT NULL UNIQUE,
                        credential_profile_id INT NOT NULL,
                        ip VARCHAR(45) NOT NULL,
                        port INT NOT NULL,
                        discovery_status BOOLEAN NOT NULL DEFAULT true,
                        FOREIGN KEY (credential_profile_id) REFERENCES CredentialProfiles(id) ON DELETE RESTRICT
                    );
                
                    CREATE INDEX IF NOT EXISTS idx_discovery_credential ON DiscoveryProfiles(credential_profile_id);
                   
                    CREATE TABLE IF NOT EXISTS Provision (
                        monitor_id SERIAL PRIMARY KEY,
                        credential_id INT NOT NULL,
                        ip VARCHAR(45) NOT NULL,
                        port INT NOT NULL,
                        is_active BOOLEAN NOT NULL DEFAULT TRUE
                    );
                
                    CREATE INDEX IF NOT EXISTS idx_provision_active_ip ON provision (ip) WHERE is_active;
                  
                    CREATE TABLE IF NOT EXISTS SystemData (
                        id SERIAL PRIMARY KEY,
                        monitor_id INT NOT NULL,
                        system_info JSONB NOT NULL,
                        fetched_at BIGINT DEFAULT (EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000)
                    );
                
                
                    CREATE INDEX  IF NOT EXISTS idx_systemdata_system_info ON SystemData USING GIN(system_info);
                    CREATE INDEX  IF NOT EXISTS idx_systemdata_memory_free ON SystemData (((system_info->>'SystemMemoryFreeBytes')::bigint));
                    CREATE INDEX  IF NOT EXISTS idx_systemdata_memory_committed ON SystemData (((system_info->>'SystemMemoryCommittedBytes')::bigint));
                    CREATE INDEX  IF NOT EXISTS idx_systemdata_memory_installed ON SystemData (((system_info->>'SystemMemoryInstalledBytes')::bigint));
                    CREATE INDEX  IF NOT EXISTS idx_systemdata_cpu_percent ON SystemData (((system_info->>'SystemCPUPercent')::numeric));
                    CREATE INDEX  IF NOT EXISTS idx_systemdata_cpu_user_percent ON SystemData (((system_info->>'SystemCPUUserPercent')::numeric));
                    CREATE INDEX  IF NOT EXISTS idx_systemdata_threads ON SystemData (((system_info->>'SystemThreads')::int));
                    CREATE INDEX  IF NOT EXISTS idx_systemdata_tcp_connections ON SystemData (((system_info->>'SystemNetworkTCPConnections')::int));
                    CREATE INDEX  IF NOT EXISTS idx_systemdata_processor_queue ON SystemData (((system_info->>'SystemProcessorQueueLength')::int));
                    CREATE INDEX  IF NOT EXISTS idx_systemdata_context_switches ON SystemData (((system_info->>'SystemContextSwitchesPerSec')::int));
                    CREATE INDEX  IF NOT EXISTS idx_systemdata_disk_free ON SystemData (((system_info->>'SystemDiskFreePercent')::numeric));
                    CREATE INDEX  IF NOT EXISTS idx_systemdata_disk_used ON SystemData (((system_info->>'SystemDiskUsedPercent')::numeric));
                
                """;

        pool.getConnection(ar -> {

            if (ar.succeeded()) {

                var connection = ar.result();

                connection.query(sql).execute(res -> {

                    if (res.succeeded()) {

                        Logger.info("Tables created successfully");

                    } else {

                        Logger.error("Failed to create tables", res.cause());

                    }

                    connection.close();

                });

            } else {

                Logger.error("Failed to obtain database connection", ar.cause());

            }

        });

    }

}
