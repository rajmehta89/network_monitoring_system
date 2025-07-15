package org.server.util;

import java.util.regex.Pattern;

/**
 * The type Constants.
 */
public class Constants {

    /**
     * The constant SUCCESS.
     */
    public static final String SUCCESS = "success";

    /**
     * The constant ERROR.
     */
    public static final String ERROR = "error";

    /**
     * The constant DATA.
     */
    public static final String DATA="data";

    /**
     * The constant RESULT.
     */
    public static final String RESULT="result";

    /**
     * The constant FAIL.
     */
    public static final String FAIL = "fail";

    /**
     * The constant ERRORS.
     */
    public static final String ERRORS="errors";

    /**
     * The constant PROFILES.
     */
    public static final String PROFILES="profiles";

    /**
     * The constant STATUS.
     */
    public static final String STATUS = "status";

    /**
     * The constant MESSAGE.
     */
    public static final String MESSAGE = "message";

    /**
     * The constant PROVISIONING.
     */
    public static final String PROVISIONING = "provisioning";

    /**
     * The constant REQUESTTYPE.
     */
    public static final String REQUESTTYPE = "RequestType";

    /**
     * The constant DISCOVERY.
     */
    public static final String DISCOVERY = "discovery";


    /**
     * The constant DISCOVERy.
     */
    public static final String DISCOVERy = "DISCOVERY";
    /**
     * The constant CONTENT_TYPE.
     */
    public static final String CONTENT_TYPE = "Content-Type";

    /**
     * The constant IS_ACTIVE.
     */
    public static final String IS_ACTIVE = "is_active";

    /**
     * The constant APPLICATION_JSON.
     */
    public static final String APPLICATION_JSON = "application/json";

    /**
     * The constant MEMORY_CHECK.
     */
    public static final String MEMORY_CHECK="memory_check";

    /**
     * The constant CPU_SPIKES.
     */
    public static final String CPU_SPIKES="cpu_spikes";

    /**
     * The constant TOP_CPU_SPIKES.
     */
    public static final String TOP_CPU_SPIKES="top_cpu_spikes";

    /**
     * The constant API_GET_CREDENTIAL_PROFILE.
     */
    public static final String API_GET_CREDENTIAL_PROFILE = "/credential-profiles/:id";

    /**
     * The constant API_GET_ALL_CREDENTIAL_PROFILES.
     */
    public static final String API_GET_ALL_CREDENTIAL_PROFILES = "/credential-profiles";

    /**
     * The constant API_CREATE_CREDENTIAL_PROFILE.
     */
    public static final String API_CREATE_CREDENTIAL_PROFILE = "/credential-profile";

    /**
     * The constant API_UPDATE_CREDENTIAL_PROFILE.
     */
    public static final String API_UPDATE_CREDENTIAL_PROFILE = "/credential-profiles/:id";

    /**
     * The constant API_DELETE_CREDENTIAL_PROFILE.
     */
    public static final String API_DELETE_CREDENTIAL_PROFILE = "/credential-profiles/:id";

    /**
     * The constant ACTION_GET_CREDENTIAL_PROFILE.
     */
    public static final String ACTION_GET_CREDENTIAL_PROFILE = "GET_CREDENTIAL_PROFILE";

    /**
     * The constant ACTION_GET_ALL_CREDENTIALS.
     */
    public static final String ACTION_GET_ALL_CREDENTIALS = "GET_ALL_CREDENTIALS";

    /**
     * The constant ACTION_CREATE_CREDENTIAL_PROFILE.
     */
    public static final String ACTION_CREATE_CREDENTIAL_PROFILE = "CREATE_CREDENTIAL_PROFILE";

    /**
     * The constant ACTION_UPDATE_CREDENTIAL_PROFILE.
     */
    public static final String ACTION_UPDATE_CREDENTIAL_PROFILE = "UPDATE_CREDENTIAL_PROFILE";

    /**
     * The constant ACTION_DELETE_CREDENTIAL_PROFILE.
     */
    public static final String ACTION_DELETE_CREDENTIAL_PROFILE = "DELETE_CREDENTIAL_PROFILE";

    /**
     * The constant API_GET_DISCOVERY_PROFILE.
     */
    public static final String API_GET_DISCOVERY_PROFILE = "/discovery-profiles/:id";

    /**
     * The constant API_GET_ALL_DISCOVERY_PROFILES.
     */
    public static final String API_GET_ALL_DISCOVERY_PROFILES = "/discovery-profiles";

    /**
     * The constant API_GET_DISCOVERY_RUN.
     */
    public static final String API_GET_DISCOVERY_RUN = "/discovery-run/:id";

    /**
     * The constant API_CREATE_DISCOVERY_PROFILE.
     */
    public static final String API_CREATE_DISCOVERY_PROFILE = "/discovery-profile";

    /**
     * The constant API_UPDATE_DISCOVERY_PROFILE.
     */
    public static final String API_UPDATE_DISCOVERY_PROFILE = "/discovery-profiles/:id";

    /**
     * The constant API_DELETE_DISCOVERY_PROFILE.
     */
    public static final String API_DELETE_DISCOVERY_PROFILE = "/discovery-profiles/:id";

    /**
     * The constant ACTION_GET_DISCOVERY_PROFILE.
     */
    public static final String ACTION_GET_DISCOVERY_PROFILE = "GET_DISCOVERY_PROFILE";

    /**
     * The constant ACTION_GET_ALL_DISCOVERYPROFILES.
     */
    public static final String ACTION_GET_ALL_DISCOVERYPROFILES = "GET_ALL_CREDENTIALS";

    /**
     * The constant ACTION_GET_DISCOVERY_RUN.
     */
    public static final String ACTION_GET_DISCOVERY_RUN = "GET_DISCOVERY_RUN";

    /**
     * The constant ACTION_CREATE_DISCOVERY_PROFILE.
     */
    public static final String ACTION_CREATE_DISCOVERY_PROFILE = "CREATE_DISCOVERY_PROFILE";

    /**
     * The constant ACTION_UPDATE_DISCOVERY_PROFILE.
     */
    public static final String ACTION_UPDATE_DISCOVERY_PROFILE = "UPDATE_DISCOVERY_PROFILE";

    /**
     * The constant ACTION_DELETE_DISCOVERY_PROFILE.
     */
    public static final String ACTION_DELETE_DISCOVERY_PROFILE = "DELETE_DISCOVERY_PROFILE";

    /**
     * The constant API_START_PROVISION.
     */
    public static final String API_START_PROVISION = "/provisioning/:id";

    /**
     * The constant DUPLICATION_ERROR_CODE.
     */
    public static  final String DUPLICATION_ERROR_CODE="23505";

    /**
     * The constant SQL_ERROR_CODE.
     */
    public static  final String SQL_ERROR_CODE="42601";

    /**
     * The constant FOREIGN_KEY_ERROR_CODE.
     */
    public static  final String FOREIGN_KEY_ERROR_CODE="23503";

    /**
     * The constant API_GET_PROVISIONED_DATA.
     */
    public static final String API_GET_PROVISIONED_DATA = "/provisioning/data/:monitor_id";

    /**
     * The constant API_DELETE_MONITOR.
     */
    public static final String API_DELETE_MONITOR = "/monitors/:monitor_id"; // Using {id} to specify a monitor

    /**
     * The constant ACTION_START_PROVISION.
     */
    public static final String ACTION_START_PROVISION = "START_PROVISION";

    /**
     * The constant ACTION_FETCH_PROVISIONED_DATA.
     */
    public static final String ACTION_FETCH_PROVISIONED_DATA = "FETCH_PROVISIONED_DATA";

    /**
     * The constant ACTION_DELETE_MONITOR.
     */
    public static final String ACTION_DELETE_MONITOR = "DELETE_MONITOR";

    /**
     * The constant PORT.
     */
    public static final String PORT="port";

    /**
     * The constant HEALTH.
     */
    public static  final String HEALTH="health";

    /**
     * The constant API_GET_MEMORY_CHECKS.
     */
    public static final String API_GET_MEMORY_CHECKS = "/system-monitor/memory-checks";

    /**
     * The constant API_GET_CPU_SPIKES.
     */
    public static final String API_GET_CPU_SPIKES = "/system-monitor/cpu-spikes";

    /**
     * The constant API_GET_TOP_CPU_SPIKES.
     */
    public static final String API_GET_TOP_CPU_SPIKES = "/system-monitor/top-cpu-spikes";

    /**
     * The constant ACTION_GET_MEMORY_CHECKS.
     */
    public static final String ACTION_GET_MEMORY_CHECKS = "GET_MEMORY_CHECKS";

    /**
     * The constant ACTION_GET_CPU_SPIKES.
     */
    public static final String ACTION_GET_CPU_SPIKES = "GET_CPU_SPIKES";

    /**
     * The constant ACTION_GET_TOP_CPU_SPIKES.
     */
    public static final String ACTION_GET_TOP_CPU_SPIKES = "GET_TOP_CPU_SPIKES";

    /**
     * The constant TABLE_SYSTEM_DATA.
     */
    public static final String TABLE_SYSTEM_DATA = "systemdata";

    /**
     * The constant TABLE_PROVISION_PROFILES.
     */
    public static final String TABLE_PROVISION_PROFILES = "provision";

    /**
     * The constant TABLE_CREDENTIAL_PROFILES.
     */
    public static final String TABLE_CREDENTIAL_PROFILES = "credentialprofiles";

    /**
     * The constant TABLE_DISCOVERY_PROFILES.
     */
    public static final String TABLE_DISCOVERY_PROFILES = "discoveryprofiles";

    /**
     * The constant CREDENTIAL_CONFIG.
     */
    public static final String CREDENTIAL_CONFIG = "credentialconfig";

    /**
     * The constant DISCOVERY_PROFILE_NAME.
     */
    public static final String DISCOVERY_PROFILE_NAME = "discovery_profile_name";

    /**
     * The constant DISCOVERY_PROFILE_ID.
     */
    public static final String DISCOVERY_PROFILE_ID="discovery_profile_id";

    /**
     * The constant SYSTEM_INFO.
     */
    public static final String SYSTEM_INFO = "system_info";  // Represents a single system info record

    /**
     * The constant IP.
     */
    public static final String IP = "ip";

    /**
     * The constant MONITOR_ID.
     */
    public static final String MONITOR_ID = "monitor_id";

    /**
     * The constant DISCOVERY_STATUS.
     */
    public static final String DISCOVERY_STATUS = "discovery_status";

    /**
     * The constant ID.
     */
    public static final String ID = "id";

    /**
     * The constant CREDENTIAL_PROFILE_ID.
     */
    public static final String CREDENTIAL_PROFILE_ID = "credential_profile_id";

    /**
     * The constant CREDENTIAL_PROFILE_NAME.
     */
    public static final String CREDENTIAL_PROFILE_NAME="credential_profile_name";

    /**
     * The constant CREATE.
     */
    public static final String CREATE="create";

    /**
     * The constant READ.
     */
    public static final String READ="read";

    /**
     * The constant UPDATE.
     */
    public static final String UPDATE="update";

    /**
     * The constant DELETE.
     */
    public static final String DELETE="delete";

    /**
     * The constant READALL.
     */
    public static final String READALL="readAll";

    /**
     * The constant USERNAME.
     */
    public static final String USERNAME="username";

    /**
     * The constant PASSWORD.
     */
    public static final String PASSWORD="password";

    /**
     * The constant SYSTEMTYPE.
     */
    public static final String SYSTEMTYPE="SystemType";

    /**
     * The constant SYSTEM_TYPE.
     */
    public static final String SYSTEM_TYPE="system_type";

    /**
     * The constant TIMETOPOLL.
     */
    public static final Long TIMETOPOLL=300L;

    /**
     * The constant SYSTEM_DATA_INSERT.
     */
    public static final String SYSTEM_DATA_INSERT="system.data.insert";

    /**
     * The constant POOL_SIZE.
     */
    public static final int POOL_SIZE = 10;

    /**
     * The constant MAX_WAIT_QUEUE_SIZE.
     */
    public static final int MAX_WAIT_QUEUE_SIZE = 1000;

    /**
     * The constant POOLED_IDLE_TIME.
     */
    public static final int POOLED_IDLE_TIME = 30000;

    /**
     * The constant DATABASE_PORT.
     */
    public static final int DATABASE_PORT=5432;

    /**
     * The constant DATABASENAME.
     */
    public static final String DATABASENAME="network_monitoring";

    /**
     * The constant DATABASEPASSWORD.
     */
    public static final String DATABASEPASSWORD="oT4OG1FYfmEdPBIgmEsYCM1PSHJhzTrO";


    public static final String DATABASEUSERNAME="network_monitoring_user";

    /**
     * The constant RECONNECTATTEMPT.
     */
    public static final int RECONNECTATTEMPT=5;

    /**
     * The constant RECONNECTINTERVALTIME.
     */
    public static final int RECONNECTINTERVALTIME=2000;

    /**
     * The constant DATABASEHOSTNAME.
     */
    public static final String DATABASEHOSTNAME="dpg-d1r2ilur433s739pj9fg-a.oregon-postgres.render.com";

    /**
     * The constant EVENT_BUS_SEND_TIMEOUT.
     */
    public static final Long EVENT_BUS_SEND_TIMEOUT = 60000L;

    /**
     * The constant DISCOVERY_EVENT_BUS_TIMEOUT.
     */
    public static final Long DISCOVERY_EVENT_BUS_TIMEOUT = 40000L;

    /**
     * The constant FETCHEDAT.
     */
    public static final String FETCHEDAT="fetched_at";

    /**
     * The constant REQUESTID.
     */
    public static final String REQUESTID="requestID";

    /**
     * The constant ZMQPOLLINGREQUESTTIMEOUT.
     */
    public static final Long ZMQPOLLINGREQUESTTIMEOUT=120000L;

    /**
     * The constant ZMQ_DISCOVERY_RUN_REQUEST.
     */
    public static final String ZMQ_DISCOVERY_RUN_REQUEST = "zmq.discovery.run.request";

    /**
     * The constant ZMQ_POLLING_REQUEST.
     */
    public static final String ZMQ_POLLING_REQUEST = "zmq.polling.request";

    /**
     * The constant ZMQ_BIND_ADDRESS.
     */
    public static final String ZMQ_BIND_ADDRESS = "tcp://127.0.0.1:5555";

    /**
     * The constant ZMQ_HEALTH_CHECK_TIMEOUT.
     */
    public static final int ZMQ_HEALTH_CHECK_TIMEOUT = 5000;


    /**
     * The constant SET_SEND_FIRST_MESSAGE_TIMEOUT.
     */
    public static final int SET_SEND_FIRST_MESSAGE_TIMEOUT = 500;

    /**
     * The constant ZMQ_CONNECT_ADDRESS.
     */
    public static final String ZMQ_CONNECT_ADDRESS="tcp://127.0.0.1:5556";

    /**
     * The constant CREDENTIAL.
     */
    public static final String CREDENTIAL="CREDENTIAL";

    /**
     * The constant MONITOR.
     */
    public static final String MONITOR="MONITOR";

    /**
     * The constant PROVISION.
     */
    public static final String PROVISION="PROVISION";


    /**
     * The constant IPV4_PATTERN.
     */
    public static final Pattern IPV4_PATTERN = Pattern.compile("""
        ^(25[0-5]|2[0-4][0-9]|1?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|1?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|1?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|1?[0-9][0-9]?)$
        """.trim());

    /**
     * The constant IPV6_PATTERN.
     */
    public static final Pattern IPV6_PATTERN = Pattern.compile("""
        ^(([0-9a-fA-F]{1,4}:){7,7}[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,7}:|([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|:((:[0-9a-fA-F]{1,4}){1,7}|:)|fe80:(:[0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]{1,}|::(ffff(:0{1,4}){0,1}:){0,1}((25[0-5]|(2[0-4]|1{0,1}[0-9])?[0-9])\\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9])?[0-9])|([0-9a-fA-F]{1,4}:){1,4}:((25[0-5]|(2[0-4]|1{0,1}[0-9])?[0-9])\\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9])?[0-9]))$
        """.trim());


}





