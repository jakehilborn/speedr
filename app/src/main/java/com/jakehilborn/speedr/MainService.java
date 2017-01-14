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

    private LocationListener locationListener;
    private GoogleApiClient googleApiClient;

    private static final int NOTIFICATION_ID = 1;
    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;

    private StatsCalculator statsCalculator;
    private LimitTool limitTool;

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
    Callback callback;
    Handler handler = new Handler(Looper.getMainLooper());

    public interface Callback {
        void onStatsUpdate(Stats stats);
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Crashlytics.log(Log.INFO, "MainService", "onCreate() called");

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_s)
                .setContentTitle("Speed data not yet available")
                .setContentText("Are GPS and Internet connections enabled?")
                .setContentIntent(pendingIntent)
                .setOngoing(true);

        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
        startForeground(NOTIFICATION_ID, notificationBuilder.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Crashlytics.log(Log.INFO, null, "MainService starting");

        limitTool = new LimitTool();
        statsCalculator = new StatsCalculator();
        statsCalculator.setTimeDiff(Prefs.getSessionTimeDiff(this));

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
                        locationRequest.setFastestInterval(500);
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
        statsCalculator.setLocation(location);
        statsCalculator.calcTimeDiff();

        if (statsCalculator.isLimitStale()) {
            limitTool.fetchLimit(this, location.getLatitude(), location.getLongitude(), statsCalculator);
        }

        Stats stats = buildStats();
        updateMainActivity(stats);
        updateNotification(stats);
    }

    private Stats buildStats() {
        Stats stats = new Stats();
        if (Prefs.isUseKph(this)) {
            stats.setUseKph(true);
            stats.setSpeed(UnitUtils.msToKph(statsCalculator.getSpeed()));
            stats.setLimit(UnitUtils.msToKphRoundToFive(statsCalculator.getLimit()));
        } else { //mph
            stats.setUseKph(false);
            stats.setSpeed(UnitUtils.msToMph(statsCalculator.getSpeed()));
            stats.setLimit(UnitUtils.msToMphRoundToFive(statsCalculator.getLimit()));
        }
        stats.setTimeDiff(statsCalculator.getTimeDiff());

        return stats;
    }

    public Stats pollStats() {
        return buildStats();
    }

    private void updateMainActivity(final Stats stats) {
        handler.post(new Runnable() {
            public void run() {
                callback.onStatsUpdate(stats);
            }
        });
    }

    private void updateNotification(Stats stats) {
        String timeDiff = formatTimeDiff(stats.getTimeDiff());

        String speed = "  "; //Padding to keep prevent values from shifting too much in notification
        if (stats.getSpeed() == null) {
            speed = "  0";
        } else {
            if (stats.getSpeed() >= 10) speed = " ";
            if (stats.getSpeed() >= 100) speed = ""; //Assumes currentSpeed won't exceed 999
            speed += stats.getSpeed();
        }

        String limit = "  ";
        if (stats.getLimit() == null || stats.getLimit() == 0) {
            limit = " --";
        } else {
            if (stats.getLimit() >= 10) limit = " ";
            if (stats.getLimit() >= 100) limit = ""; //Assumes currentLimit won't exceed 999
            limit += stats.getLimit();
        }

        notificationBuilder
                .setContentTitle("Time:  " + timeDiff)
                .setContentText("Speed Limit: " + limit + "   |   Speed: " + speed);

        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }

    private String formatTimeDiff(Double timeDiff) {
        StringBuilder timeDiffString = new StringBuilder(
                UnitUtils.nanosToSecondsModuloMinutes(timeDiff) + "." +
                UnitUtils.nanosTo10thsModuloSeconds(timeDiff) + "s"
        ); //always show seconds

        if (timeDiff >= UnitUtils.NANO_ONE_MINUTE) {
            timeDiffString.insert(0, UnitUtils.nanosToMinutesModuloHours(timeDiff) + "m  ");
        }
        if (timeDiff >= UnitUtils.NANO_ONE_HOUR) {
            timeDiffString.insert(0, UnitUtils.nanosToHoursModuloMinutes(timeDiff) + "h  ");
        }

        return timeDiffString.toString();
    }

    @Override
    public void onDestroy() {
        Crashlytics.log(Log.INFO, "MainService", "onDestroy() called");
        Prefs.setSessionTimeDiff(this, statsCalculator.getTimeDiff());
        notificationManager.cancel(NOTIFICATION_ID);
        limitTool.destroy();
        if (googleApiClient != null) googleApiClient.disconnect();
        super.onDestroy();
    }
}

