package com.jakehilborn.speedr;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
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

//MainService may run on the main thread shared by the UI. The only long running operations MainService does are network
//calls. These calls are all async so there is no blocking. Using Service instead of IntentService for simplicity sake.
public class MainService extends Service {

    private static final int NOTIFICATION_ID = 1;
    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;

    private LocationListener locationListener;
    private GoogleApiClient googleApiClient; //make this public if there are errors

    private Stats stats;
    private LimitTool limitTool;

    Callback callback;

    //Binder for MainActivity to poll data from MainService
    private final IBinder binder = new LocalBinder();
    public class LocalBinder extends Binder {
        MainService getService() {
            return MainService.this;
        }
    }

    public IBinder onBind(Intent intent) {
        return binder;
    }

    //Callback for MainService to push data to MainActivity
    public interface Callback {
        void onStatsUpdate(int test);
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    Handler handler = new Handler(Looper.getMainLooper());

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
                    public void onConnected(Bundle bundle) {
                        LocationRequest locationRequest = new LocationRequest();
                        locationRequest.setInterval(1000); //Request GPS location every 1 second
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

        return START_STICKY;
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
            currentLimit = stats.getLimit() == null ? -1 : UnitUtils.msToKphRoundToFive(stats.getLimit());
        } else { //mph
            currentSpeed = UnitUtils.msToMph(stats.getSpeed());
            currentLimit = stats.getLimit() == null ? -1 : UnitUtils.msToMphRoundToFive(stats.getLimit());
        }

        Double timeDiff = UnitUtils.nanoToSeconds(stats.getTimeDiff());

        updateNotification(currentSpeed, currentLimit, timeDiff);
        updateActivity();
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

    private void updateActivity() {
        handler.post(new Runnable() {
            public void run() {
                callback.onStatsUpdate(0);
            }
        });
    }

    @Override
    public void onDestroy() {
        Crashlytics.log(Log.INFO, "MainService", "onDestroy() called");
        notificationManager.cancel(NOTIFICATION_ID);

        limitTool.destroy();

        if (googleApiClient != null) googleApiClient.disconnect();

        super.onDestroy();
    }
}

