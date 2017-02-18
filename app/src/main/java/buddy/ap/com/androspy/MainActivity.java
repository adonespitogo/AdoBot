package buddy.ap.com.androspy;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
//import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.WindowManager;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        openSettings();
        finish();
    }

    private void openSettings() {
        startActivityForResult(new Intent(android.provider.Settings.ACTION_SETTINGS), 0);
    }
}
