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
import com.jakehilborn.speedr.utils.FormatTime;
import com.jakehilborn.speedr.utils.Prefs;
import com.jakehilborn.speedr.utils.UnitUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.GregorianCalendar;

//MainService may run on the main thread shared by the UI. The only long running operations MainService does are network
//calls. These calls are all async so there is no blocking. Using Service instead of IntentService for simplicity sake.
public class MainService extends Service implements StatsCalculator.Callback {

    private LocationListener locationListener;
    private GoogleApiClient googleApiClient;

    private static final int NOTIFICATION_ID = 1;
    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;

    private StatsCalculator statsCalculator;
    private LimitFetcher limitFetcher;

    public long stopTime;

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
    private Callback callback;
    private Handler handler = new Handler(Looper.getMainLooper());

    public interface Callback {
        void onUIDataUpdate(UIData uiData);
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Crashlytics.log(Log.INFO, MainService.class.getSimpleName(), "onCreate()");

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle(getString(R.string.notification_init_title))
                .setContentText(getString(R.string.notification_init_text))
                .setContentIntent(pendingIntent)
                .setOngoing(true);

        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
        startForeground(NOTIFICATION_ID, notificationBuilder.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Crashlytics.log(Log.INFO, MainService.class.getSimpleName(), "onStartCommand()");

        statsCalculator = new StatsCalculator();
        statsCalculator.setCallback(this);
        statsCalculator.setTimeDiff(Prefs.getSessionTimeDiff(this));
        limitFetcher = new LimitFetcher(statsCalculator);

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
                        Crashlytics.log(Log.INFO, MainService.class.getSimpleName(), "Location connecting");
                        LocationRequest locationRequest = new LocationRequest();
                        locationRequest.setInterval(1000); //Request GPS location every 1 second
                        locationRequest.setFastestInterval(500);
                        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, locationListener);
                        Crashlytics.log(Log.INFO, MainService.class.getSimpleName(), "Location connected");
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                        if (googleApiClient != null && googleApiClient.isConnected()) { //Remove pending updates, let GoogleAPIClient auto reconnect to restart updates
                            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, locationListener).setResultCallback(new ResultCallback<Status>() {
                                @Override
                                public void onResult(@NonNull Status status) {
                                    Crashlytics.log(Log.INFO, MainService.class.getSimpleName(), "Location suspended");
                                }
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
        statsCalculator.setLocation(location);
        statsCalculator.calcTimeDiff();

        boolean forceFetch = false;
        if (statsCalculator.isLimitStale() || forceFetch) {
            limitFetcher.fetchLimit(this, location.getLatitude(), location.getLongitude());

            if (statsCalculator.isNetworkCheckStale()) {
                checkNetworkDown();
            }
        }

        UIData uiData = buildUIData();
        updateMainActivity(uiData);
        updateNotification(uiData);
    }

    @Override
    public void handleNetworkUpdate() {
        UIData uiData = buildUIData();
        updateMainActivity(uiData);
        updateNotification(uiData);
    }

    private UIData buildUIData() {
        UIData uiData = new UIData();
        if (Prefs.isUseKph(this)) {
            uiData.setSpeed(UnitUtils.msToKph(statsCalculator.getSpeed()));
            uiData.setLimit(UnitUtils.msToKphRoundToFive(statsCalculator.getLimit()));
        } else { //mph
            uiData.setSpeed(UnitUtils.msToMph(statsCalculator.getSpeed()));
            uiData.setLimit(UnitUtils.msToMphRoundToFive(statsCalculator.getLimit()));
        }
        uiData.setTimeDiff(statsCalculator.getTimeDiff());
        uiData.setFirstLimitTime(statsCalculator.getFirstLimitTime());
        uiData.setNetworkDown(statsCalculator.isNetworkDown());

        return uiData;
    }

    public UIData pollUIData() {
        return buildUIData();
    }

    private void updateMainActivity(final UIData uiData) {
        handler.post(new Runnable() {
            public void run() {
                if (callback != null) callback.onUIDataUpdate(uiData);
            }
        });
    }

    private void updateNotification(UIData uiData) {
        String timeDiff = FormatTime.nanosToLongHand(this, uiData.getTimeDiff());

        String speed = "  "; //Padding to prevent values from shifting too much in notification
        if (uiData.getSpeed() == null) {
            speed = "  0";
        } else {
            if (uiData.getSpeed() >= 10) speed = " ";
            if (uiData.getSpeed() >= 100) speed = ""; //Assumes currentSpeed won't exceed 999
            speed += uiData.getSpeed();
        }

        String limit = "  ";
        if (uiData.getLimit() == null || uiData.getLimit() == 0) {
            limit = " --";
        } else {
            if (uiData.getLimit() >= 10) limit = " ";
            if (uiData.getLimit() >= 100) limit = ""; //Assumes currentLimit won't exceed 999
            limit += uiData.getLimit();
        }

        notificationBuilder
                .setContentTitle(getString(R.string.notification_time) + ":  " + timeDiff)
                .setContentText(getString(R.string.notification_speed_limit) + ": " + limit + "   |   " + getString(R.string.notification_speed) + ": " + speed);

        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }

    private void checkNetworkDown() {
        new Thread() {
            public void run() {
                boolean isNetworkDown;
                try {
                    HttpURLConnection conn = (HttpURLConnection) new URL("https://google.com").openConnection(); //ping
                    conn.setConnectTimeout(5000);
                    conn.connect();
                    isNetworkDown = conn.getResponseCode() != HttpURLConnection.HTTP_OK;
                } catch (IOException e) {
                    isNetworkDown = true;
                }

                if (isNetworkDown) {
                    Crashlytics.log(Log.INFO, MainService.class.getSimpleName(), "network down");
                } else {
                    Crashlytics.log(Log.INFO, MainService.class.getSimpleName(), "network up");
                }

                statsCalculator.setNetworkDown(isNetworkDown);
                handleNetworkUpdate();
            }
        }.start();
    }

    private void persistTimeDiff(Double timeDiff, long firstLimitTime) {
        long totalTime;
        if (firstLimitTime == 0) {
            totalTime = 0; //User stopped MainService before 1st speed limit was received
        } else if (stopTime == 0) {
            totalTime = System.nanoTime() - firstLimitTime; //Service shutting down despite user not clicking the stop button in MainActivity
        } else {
            totalTime = stopTime - firstLimitTime;
        }

        GregorianCalendar now = new GregorianCalendar();

        Double sessionTimeDiffDelta = timeDiff - Prefs.getSessionTimeDiff(this);

        Prefs.setSessionTimeDiff(this, timeDiff);
        Prefs.setSessionTimeTotal(this, totalTime + Prefs.getSessionTimeTotal(this));

        //redundant cast to int to suppress false positive IDE error
        if (Prefs.getTimeDiffWeekNum(this) != (int) now.get(Calendar.WEEK_OF_YEAR)) {
            Prefs.setTimeDiffWeekNum(this, now.get(Calendar.WEEK_OF_YEAR));
            Prefs.setTimeDiffWeek(this, sessionTimeDiffDelta);
            Prefs.setTimeTotalWeek(this, totalTime);
        } else {
            Prefs.setTimeDiffWeek(this, sessionTimeDiffDelta + Prefs.getTimeDiffWeek(this));
            Prefs.setTimeTotalWeek(this, totalTime + Prefs.getTimeTotalWeek(this));
        }

        //Month is zero-indexed. Adding 1 since Prefs returns '0' as the default value if it has not yet been set
        if (Prefs.getTimeDiffMonthNum(this) != (int) now.get(Calendar.MONTH) + 1) {
            Prefs.setTimeDiffMonthNum(this, now.get(Calendar.MONTH) + 1);
            Prefs.setTimeDiffMonth(this, sessionTimeDiffDelta);
            Prefs.setTimeTotalMonth(this, totalTime);
        } else {
            Prefs.setTimeDiffMonth(this, sessionTimeDiffDelta + Prefs.getTimeDiffMonth(this));
            Prefs.setTimeTotalMonth(this, totalTime + Prefs.getTimeTotalMonth(this));
        }

        if (Prefs.getTimeDiffYearNum(this) != (int) now.get(Calendar.YEAR)) {
            Prefs.setTimeDiffYearNum(this, now.get(Calendar.YEAR));
            Prefs.setTimeDiffYear(this, sessionTimeDiffDelta);
            Prefs.setTimeTotalYear(this, totalTime);
        } else {
            Prefs.setTimeDiffYear(this, sessionTimeDiffDelta + Prefs.getTimeDiffYear(this));
            Prefs.setTimeTotalYear(this, totalTime + Prefs.getTimeTotalYear(this));
        }
    }

    @Override
    public void onDestroy() {
        Crashlytics.log(Log.INFO, MainService.class.getSimpleName(), "onDestroy()");
        persistTimeDiff(statsCalculator.getTimeDiff(), statsCalculator.getFirstLimitTime());
        notificationManager.cancel(NOTIFICATION_ID);
        limitFetcher.destroy(this);
        if (googleApiClient != null) googleApiClient.disconnect();
        super.onDestroy();
    }
}
