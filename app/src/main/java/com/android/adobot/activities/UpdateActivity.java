package com.android.adobot.activities;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;

import com.android.adobot.Constants;
import com.android.adobot.R;
import com.android.adobot.tasks.UpdateAppTask;

public class UpdateActivity extends BaseActivity {
    private Button btnUpdate;
    private File pkg;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pkg = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), Constants.UPDATE_PKG_FILE_NAME);
        if (!pkg.exists()) {
            Context context = getApplicationContext();
            CharSequence text = "Software is up to date.";
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
            finish();
        }
        setContentView(R.layout.activity_prompt_update);
        btnUpdate = (Button) findViewById(R.id.update_btn);
        btnUpdate.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                doUpdate();
            }
        });
    }

    private void doUpdate() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(pkg), "application/vnd.android.package-archive");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // without this flag android returned a intent error!
        getApplicationContext().startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        //delete file on exit to minimize traces
        super.onDestroy();
        if (pkg.exists())
            pkg.delete();
    }
}
