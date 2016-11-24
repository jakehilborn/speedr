package com.jakehilborn.speedr;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.jakehilborn.speedr.utils.Prefs;
import com.jakehilborn.speedr.utils.UnitUtils;

public class MainService extends Service {

    public static final String BROADCAST = MainService.class.getName() + "Broadcast";
    private static final int NOTIFICATION_ID = 1;
    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;

    private LocationListener locationListener;
    private GoogleApiClient googleApiClient; //make this public if there are errors

    private Stats stats;
    private LimitTool limitTool;

    public IBinder onBind(Intent intent) {
        return null; //Using LocalBroadcasts instead of callbacks so no need to bind to MainActivity
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Crashlytics.log(Log.INFO, "MainService", "onCreate() called");

        stats = new Stats();
        limitTool = new LimitTool();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0); //Maybe better values than 0

        notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_s)
                .setContentTitle("Time difference: ")
                .setContentText("Speed data not yet available, are GPS and internet connections enabled?")
                .setContentIntent(pendingIntent)
                .setOngoing(true);

        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());

        startForeground(NOTIFICATION_ID, notificationBuilder.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Crashlytics.log(Log.INFO, null, "MainService starting");
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(final Location location) {
                handleLocationChange(location);
            }
        };

        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    @SuppressWarnings("MissingPermission") //Location permission is granted before the MainService is started
                    public void onConnected(@Nullable Bundle bundle) {
                        LocationRequest locationRequest = new LocationRequest();
                        locationRequest.setInterval(1000);
                        locationRequest.setFastestInterval(0);
                        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, locationListener);
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                        if (googleApiClient != null && googleApiClient.isConnected()) { //Remove pending updates, let GoogleAPIClient auto reconnect to restart updates
                            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, locationListener).setResultCallback(new ResultCallback<Status>() {
                                @Override
                                public void onResult(@NonNull Status status) {}
                            });
                        }
                    }
                })
                .addApi(LocationServices.API)
                .build();

        googleApiClient.connect();

        return super.onStartCommand(intent, flags, startId);
    }

    private void handleLocationChange(Location location) {
        Crashlytics.log(Log.INFO, "MainService", "handling location change");
        stats.setLocation(location);
        stats.calcTimeDiff();

        if (stats.isLimitStale()) {
            limitTool.fetchLimit(this, location.getLatitude(), location.getLongitude(), stats);
        }

        int currentSpeed;
        int currentLimit;
        if (Prefs.isUseKph(this)) {
            currentSpeed = UnitUtils.msToKph(stats.getSpeed());
            currentLimit = stats.getLimit() == null ? Constants.NO_VALUE : UnitUtils.msToKphRoundToFive(stats.getLimit());
        } else { //mph
            currentSpeed = UnitUtils.msToMph(stats.getSpeed());
            currentLimit = stats.getLimit() == null ? Constants.NO_VALUE : UnitUtils.msToMphRoundToFive(stats.getLimit());
        }

        Double timeDiff = UnitUtils.nanoToSeconds(stats.getTimeDiff());

        updateNotification(currentSpeed, currentLimit, timeDiff);
        updateActivity(currentSpeed, currentLimit, timeDiff);
    }

    private void updateNotification(int currentSpeed, int currentLimit, Double timeDiff) {
        String speedPad = "  ";
        if (currentSpeed >= 10) speedPad = " ";
        if (currentSpeed >= 100) speedPad = ""; //Assumes currentSpeed won't exceed 999
        String limitPad = "  ";
        if (currentLimit >= 10) limitPad = " ";
        if (currentLimit >= 100) limitPad = ""; //Assumes currentLimit won't exceed 999

        notificationBuilder
                .setContentTitle("Time difference: " + timeDiff)
                .setContentText("Speed: " + speedPad + currentSpeed + "   |   SpeedLimit: " + limitPad + currentLimit);

        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }

    private void updateActivity(int currentSpeed, int currentLimit, Double timeDiff) {
        Intent intent = new Intent(BROADCAST);
        intent.putExtra(Constants.CURRENT_SPEED, currentSpeed);
        intent.putExtra(Constants.CURRENT_LIMIT, currentLimit);
        intent.putExtra(Constants.TIME_DIFF, timeDiff);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void notifyMainActivityThatServiceStopped() {
        Intent intent = new Intent(BROADCAST);
        intent.putExtra(Constants.SERVICE_STOPPED, true);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        Crashlytics.log(Log.INFO, "MainService", "onDestroy() called");
        notificationManager.cancel(NOTIFICATION_ID);
        notifyMainActivityThatServiceStopped();

        limitTool.destroy();

        if (googleApiClient != null) googleApiClient.disconnect();

        super.onDestroy();
    }
}
