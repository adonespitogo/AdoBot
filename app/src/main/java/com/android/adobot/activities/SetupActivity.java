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
import com.android.adobot.Constants;
import com.android.adobot.R;

/**
 * Created by adones on 2/26/17.
 */

public class SetupActivity extends BaseActivity {

    private static final String TAG = "SetupActivity";

    SharedPreferences prefs;
    EditText editTextUrl;
    Button btnSetUrl;
    String url = "";
    AppCompatActivity activity = this;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);
        prefs = this.getSharedPreferences("com.android.adobot", Context.MODE_PRIVATE);
        url = prefs.getString("serverUrl", Constants.DEVELOPMENT_SERVER);

        editTextUrl = (EditText) findViewById(R.id.edit_text_server_url);
        if (url != null) {
            if (!(url.equals(Constants.DEVELOPMENT_SERVER) || url.equals(""))) {
                // server is already set
                done();
                return;
            } else {
                editTextUrl.setText(url);
            }
        }

        TextView instruction = (TextView) findViewById(R.id.text_instruction);
        instruction.setText("Set your server address. Make sure it has \"http://\" or \"https://\" in front of the domain name or IP address and has NO slash \"/\" at the end of the URL.\n\n" +
                "Examples:\n\nhttps://adobot.herokuapp.com\nhttp://123.123.12.123");

        btnSetUrl = (Button) findViewById(R.id.btn_set_server);
        btnSetUrl.setOnClickListener(setUrlClickListener);
    }

    private View.OnClickListener setUrlClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            url = editTextUrl.getText().toString();
            String reviewText = "Confirm your server address: \n\n" + url;
            new AlertDialog.Builder(activity)
                    .setTitle("Server Setup")
                    .setMessage(reviewText)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int whichButton) {
                            setServerUrl(url);
                            Toast.makeText(SetupActivity.this, "AdoBot server set to \n" + url, Toast.LENGTH_LONG).show();
                        }
                    })
                    .setNegativeButton(android.R.string.no, null).show();
        }
    };

    private void setServerUrl(String url) {
        prefs.edit().putString("serverUrl", url).commit();
        if (!hasPermissions()) {
            requestPermissions();
        } else {
            done();
        }
    }

    private void done() {
        startClient();
        if (!BuildConfig.DEBUG) hideApp();
        finish();
    }
}
