package buddy.ap.com.androspy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetworkWatcher extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
//        here, check that the network connection is available. If yes, start your service. If not, stop your service.
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        if (info != null) {
            if (info.isConnected()) {
                //start service
//                Intent i = new Intent(context, MyService.class);
//                context.startService(i);
                System.out.print("Network Watcher running.............");
            }

        }
    }
}