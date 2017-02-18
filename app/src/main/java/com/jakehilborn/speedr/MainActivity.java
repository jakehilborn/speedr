package com.jakehilborn.speedr;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageButton;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.jakehilborn.speedr.utils.FormatTime;
import com.jakehilborn.speedr.utils.Prefs;

public class MainActivity extends AppCompatActivity implements MainService.Callback {

    private static final int BIND_IF_SERVICE_RUNNING = 0;
    private static final int REQUEST_LOCATION = 1;

    private MainService mainService;
    private GoogleApiClient googleApiClient;

    private boolean useHereMaps;
    private long firstLimitTime;
    private double curTimeDiff;

    private TextView timeSaved;

    private View totalTimeGroup;
    private TextView totalTime;
    private TextView totalTimeNoSpeed;
    private TextView percentFaster;

    private TextView speed;
    private TextView speedUnit;
    private TextView limit;
    private TextView limitUnit;

    private TextView pendingHereActivationNotice;
    private TextView internetDownNotice;

    private AppCompatImageButton reset;
    private AppCompatImageButton limitProviderLogo;
    private AppCompatImageButton missingOpenStreetMapLimit;

    private Toast noGPSPermissionToast;
    private Toast noNetworkToast;
    private Toast playServicesErrorToast;
    private Toast poweredByOpenStreetMapToast;
    private Toast poweredByHereMapsToast;

    private Handler totalTimeHandler;
    private Runnable totalTimeRunnable;
    private boolean totalTimeStartOrStop; //Let non-handler methods update the totalTime clock at first location update or service stop for immediate results
    private static final int TOTAL_TIME_REFRESH_FREQ = 1000; //milliseconds

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Crashlytics.log(Log.INFO, MainActivity.class.getSimpleName(), "onCreate()");
        setContentView(R.layout.activity_main);

        timeSaved = (TextView) findViewById(R.id.time_saved);

        totalTimeGroup = findViewById(R.id.total_time_group);
        totalTime = (TextView) findViewById(R.id.total_time);
        totalTimeNoSpeed = (TextView) findViewById(R.id.total_time_no_speed);
        percentFaster = (TextView) findViewById(R.id.percent_faster);

        speed = (TextView) findViewById(R.id.speed);
        speedUnit = (TextView) findViewById(R.id.speed_unit);
        limit = (TextView) findViewById(R.id.limit);
        limitUnit = (TextView) findViewById(R.id.limit_unit);
        pendingHereActivationNotice = (TextView) findViewById(R.id.pending_here_activation_notice);
        internetDownNotice = (TextView) findViewById(R.id.internet_down_notice);

        reset = (AppCompatImageButton) findViewById(R.id.reset_session);
        reset.setOnClickListener(new View.OnClickListener() { //xml defined onClick for AppCompatImageButton crashes on Android 4.2
            public void onClick(View view) {
                resetSessionOnClick(view);
            }
        });

        limitProviderLogo = (AppCompatImageButton) findViewById(R.id.limit_provider_logo);
        limitProviderLogo.setOnClickListener(new View.OnClickListener() { //xml defined onClick for AppCompatImageButton crashes on Android 4.2
            public void onClick(View view) {
                if (useHereMaps) {
                    poweredByHereMapsToast.show();
                } else {
                    poweredByOpenStreetMapToast.show();
                }
            }
        });

        missingOpenStreetMapLimit = (AppCompatImageButton) findViewById(R.id.missing_open_street_map_limit);
        missingOpenStreetMapLimit.setOnClickListener(new View.OnClickListener() { //xml defined onClick for AppCompatImageButton crashes on Android 4.2
            public void onClick(View view) {
                missingOpenStreetMapLimitOnClick();
            }
        });

        //Toasts declared here so that they are not reassigned. Allows for easy calls to toast.cancel() when stopping Activity
        noGPSPermissionToast = Toast.makeText(this, R.string.no_gps_permission_toast, Toast.LENGTH_LONG);
        noNetworkToast = Toast.makeText(this, R.string.no_network_toast, Toast.LENGTH_LONG);
        playServicesErrorToast = Toast.makeText(this, R.string.play_services_error_toast, Toast.LENGTH_LONG);
        poweredByOpenStreetMapToast = Toast.makeText(MainActivity.this, R.string.powered_by_open_street_map_toast, Toast.LENGTH_SHORT);
        poweredByHereMapsToast = Toast.makeText(MainActivity.this, R.string.powered_by_here_maps_toast, Toast.LENGTH_SHORT);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Crashlytics.log(Log.INFO, MainActivity.class.getSimpleName(), "onStart()");

        if (Prefs.isUseKph(this)) {
            speedUnit.setText(R.string.kmh);
            limitUnit.setText(R.string.kmh);
        } else {
            speedUnit.setText(R.string.mph);
            limitUnit.setText(R.string.mph);
        }

        useHereMaps = Prefs.isUseHereMaps(this);

        if (useHereMaps) {
            limitProviderLogo.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.here_maps_logo));
            missingOpenStreetMapLimit.setVisibility(View.INVISIBLE); //only applies to OpenStreetMap
        } else {
            limitProviderLogo.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.open_street_map_logo));
        }

        totalTimeStartOrStop = true;
        restoreSessionInUI();

        totalTimeHandler = new Handler();
        totalTimeRunnable = new Runnable() {
            @Override
            public void run() {
                refreshTotalTime(true);
                totalTimeHandler.postDelayed(this, TOTAL_TIME_REFRESH_FREQ);
            }
        };

        if (isMainServiceRunning()) {
            bindService(new Intent(this, MainService.class), mainServiceConn, BIND_IF_SERVICE_RUNNING);
            totalTimeHandler.postDelayed(totalTimeRunnable, TOTAL_TIME_REFRESH_FREQ);
        }
    }

    private boolean isMainServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)){
            if(MainService.class.getCanonicalName().equals(service.service.getClassName())) {
                Crashlytics.log(Log.INFO, MainActivity.class.getSimpleName(), "MainService running");
                return true;
            }
        }
        Crashlytics.log(Log.INFO, MainActivity.class.getSimpleName(), "MainService not running");
        return false;
    }

    private ServiceConnection mainServiceConn = new ServiceConnection() { //binder boilerplate
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Crashlytics.log(Log.INFO, MainActivity.class.getSimpleName(), "MainService connected");
            MainService.LocalBinder binder = (MainService.LocalBinder) service;
            mainService = binder.getService();
            mainService.setCallback(MainActivity.this);

            //Sets UI values on MainActivity onStart() if MainService was already running
            styleStartStopButton(true);
            updateUI(mainService.pollUIData());
        }

        @Override //Only called on service crashes, not called onDestroy or on unbindService
        public void onServiceDisconnected(ComponentName className) {
            Crashlytics.log(Log.INFO, MainActivity.class.getSimpleName(), "MainService disconnected");
            mainService = null;
        }
    };

    @Override
    public void onUIDataUpdate(UIData uiData) {
        updateUI(uiData);
    }

    private void updateUI(UIData uiData) {
        String formattedTime = FormatTime.nanosToLongHand(this, uiData.getTimeDiff());
        String stylizedTime = FormatTime.stylizedMainActivity(this, formattedTime);
        timeSaved.setText(Html.fromHtml(stylizedTime));

        firstLimitTime = uiData.getFirstLimitTime(); //store value in activity so totalTimeRunnable has access without location updates
        curTimeDiff = uiData.getTimeDiff(); //store value in activity so totalTimeRunnable has access without location updates
        refreshTotalTime(false);

        if (uiData.getLimit() == null || uiData.getLimit() == 0) {
            limit.setText("--");
            //If service is running and returns null limit for OpenStreetMap show badge about spotty coverage
            if (!useHereMaps && mainService != null) missingOpenStreetMapLimit.setVisibility(View.VISIBLE);
        } else {
            limit.setText(String.valueOf(uiData.getLimit()));
            missingOpenStreetMapLimit.setVisibility(View.INVISIBLE);
        }

        if (uiData.getSpeed() == null) {
            speed.setText("--");
        } else {
            speed.setText(String.valueOf(uiData.getSpeed()));
        }

        if (useHereMaps && mainService != null && Prefs.isPendingHereActivation(this)) {
            pendingHereActivationNotice.setVisibility(View.VISIBLE);
        } else {
            pendingHereActivationNotice.setVisibility(View.GONE);
        }

        if (uiData.isNetworkDown()) {
            internetDownNotice.setVisibility(View.VISIBLE);
        } else {
            internetDownNotice.setVisibility(View.GONE);
        }
    }

    private void refreshTotalTime(boolean viaHandler) {
        if (firstLimitTime == 0 && Prefs.getSessionTimeTotal(this) == 0) {
            totalTimeGroup.setVisibility(View.INVISIBLE);
            percentFaster.setVisibility(View.INVISIBLE);
            return;
        }

        double totalNanos = Prefs.getSessionTimeTotal(this);
        if (firstLimitTime != 0) { //User resuming session, first limit has not yet been received
            if (mainService != null && mainService.stopTime != 0) { //MainService stopping, use shared stop time to keep time values in sync
                totalNanos += (mainService.stopTime - firstLimitTime);
            }
            else {
                totalNanos += (System.nanoTime() - firstLimitTime);
            }
        }

        int percent = (int) Math.round((curTimeDiff / totalNanos) * 100);
        Spanned percentFasterText = Html.fromHtml("<b>" + percent + "%</b>  " + getString(R.string.percent_faster));
        percentFaster.setText(percentFasterText);

        //Only refresh time via handler so that it increments evenly second to second
        //Allow first refresh onLocationChange for immediate data at startup
        if (viaHandler || totalTimeStartOrStop) {
            totalTimeStartOrStop = false;
            String formattedTotalTime = FormatTime.nanosToShortHand(this, totalNanos);
            String stylizedTotalTime = FormatTime.stylizedMainActivity(this, formattedTotalTime);
            String formattedTotalTimeNoSpeed = FormatTime.nanosToShortHand(this, totalNanos + curTimeDiff);
            String stylizedTotalTimeNoSpeed = FormatTime.stylizedMainActivity(this, formattedTotalTimeNoSpeed);

            totalTime.setText(Html.fromHtml(stylizedTotalTime));
            totalTimeNoSpeed.setText(Html.fromHtml(stylizedTotalTimeNoSpeed));

            totalTimeGroup.setVisibility(View.VISIBLE);
            percentFaster.setVisibility(View.VISIBLE);
        }
    }

    private void restoreSessionInUI() {
        UIData uiData;
        if (mainService != null) {
            uiData = mainService.pollUIData();
        } else { //If service is not running then read the timeDiff from storage
            uiData = new UIData();
            uiData.setTimeDiff(Prefs.getSessionTimeDiff(this));
        }

        updateUI(uiData);

        if (!isMainServiceRunning() && (uiData.getTimeDiff() != 0 || Prefs.getSessionTimeTotal(this) != 0)) { //MainService may not be bound yet so explicitly check if running
            reset.setVisibility(View.VISIBLE);
        }
    }

    private void finalizeSessionInUI() {
        UIData uiData;
        if (mainService != null) {
            uiData = mainService.pollUIData();
        } else { //If MainService was unexpectedly terminated this else block provides null safety and displays most recent timeDiff
            uiData = new UIData();
            uiData.setTimeDiff(Prefs.getSessionTimeDiff(this));
        }

        uiData.setLimit(null);
        uiData.setSpeed(null);
        uiData.setNetworkDown(false);

        totalTimeStartOrStop = true;
        updateUI(uiData);

        //Only show during active sessions
        missingOpenStreetMapLimit.setVisibility(View.INVISIBLE);
        pendingHereActivationNotice.setVisibility(View.GONE);

        if (uiData.getTimeDiff() != 0 || firstLimitTime != 0 || Prefs.getSessionTimeTotal(this) != 0) {
            reset.setVisibility(View.VISIBLE);
        } else {
            reset.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.appbar_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.stats_appbar_button:
                statsButtonOnClick();
                return true;
            case R.id.settings_appbar_button:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void statsButtonOnClick() {
        final View dialogView = getLayoutInflater().inflate(R.layout.stats_dialog, null);

        //week
        String statsWeekTotalTime = FormatTime.nanosToShortHand(this, Prefs.getTimeTotalWeek(this));
        String statsWeekTotalTimeNoSpeed = FormatTime.nanosToShortHand(this, Prefs.getTimeTotalWeek(this) + Prefs.getTimeDiffWeek(this));
        String statsWeekTimeDiff = FormatTime.nanosToLongHand(this, Prefs.getTimeDiffWeek(this));

        Spanned statsWeekTotalTimeStylized = Html.fromHtml(FormatTime.stylizedStats(statsWeekTotalTime)
                + " - " + this.getString(R.string.stats_total_drive_time));
        Spanned statsWeekTotalTimeNoSpeedStylized = Html.fromHtml(FormatTime.stylizedStats(statsWeekTotalTimeNoSpeed)
                + " - " + getString(R.string.stats_time_if_you_did_not_seepd));
        Spanned statsWeekTimeDiffStylized = Html.fromHtml(FormatTime.stylizedStats(statsWeekTimeDiff)
                + " - " + this.getString(R.string.stats_time_difference));
        Spanned statsWeekRatioStylized = Html.fromHtml(this.getString(R.string.stats_percentage_sooner_start) + " <b>"
                + Math.round((Prefs.getTimeDiffWeek(this) / Prefs.getTimeTotalWeek(this)) * 100) + "%</b> "
                + this.getString(R.string.stats_percentage_sooner_end));

        ((TextView) dialogView.findViewById(R.id.stats_week_total_time)).setText(statsWeekTotalTimeStylized);
        ((TextView) dialogView.findViewById(R.id.stats_week_total_time_no_speed)).setText(statsWeekTotalTimeNoSpeedStylized);
        ((TextView) dialogView.findViewById(R.id.stats_week_time_diff)).setText(statsWeekTimeDiffStylized);
        ((TextView) dialogView.findViewById(R.id.stats_week_ratio)).setText(statsWeekRatioStylized);

        //month
        String statsMonthTotalTime = FormatTime.nanosToShortHand(this, Prefs.getTimeTotalMonth(this));
        String statsMonthTotalTimeNoSpeed = FormatTime.nanosToShortHand(this, Prefs.getTimeTotalMonth(this) + Prefs.getTimeDiffMonth(this));
        String statsMonthTimeDiff = FormatTime.nanosToLongHand(this, Prefs.getTimeDiffMonth(this));

        Spanned statsMonthTotalTimeStylized = Html.fromHtml(FormatTime.stylizedStats(statsMonthTotalTime)
                + " - " + this.getString(R.string.stats_total_drive_time));
        Spanned statsMonthTotalTimeNoSpeedStylized = Html.fromHtml(FormatTime.stylizedStats(statsMonthTotalTimeNoSpeed)
                + " - " + getString(R.string.stats_time_if_you_did_not_seepd));
        Spanned statsMonthTimeDiffStylized = Html.fromHtml(FormatTime.stylizedStats(statsMonthTimeDiff)
                + " - " + this.getString(R.string.stats_time_difference));
        Spanned statsMonthRatioStylized = Html.fromHtml(this.getString(R.string.stats_percentage_sooner_start) + " <b>"
                + Math.round((Prefs.getTimeDiffMonth(this) / Prefs.getTimeTotalMonth(this)) * 100) + "%</b> "
                + this.getString(R.string.stats_percentage_sooner_end));

        ((TextView) dialogView.findViewById(R.id.stats_month_total_time)).setText(statsMonthTotalTimeStylized);
        ((TextView) dialogView.findViewById(R.id.stats_month_total_time_no_speed)).setText(statsMonthTotalTimeNoSpeedStylized);
        ((TextView) dialogView.findViewById(R.id.stats_month_time_diff)).setText(statsMonthTimeDiffStylized);
        ((TextView) dialogView.findViewById(R.id.stats_month_ratio)).setText(statsMonthRatioStylized);

        //year
        String statsYearTotalTime = FormatTime.nanosToShortHand(this, Prefs.getTimeTotalYear(this));
        String statsYearTotalTimeNoSpeed = FormatTime.nanosToShortHand(this, Prefs.getTimeTotalYear(this) + Prefs.getTimeDiffYear(this));
        String statsYearTimeDiff = FormatTime.nanosToLongHand(this, Prefs.getTimeDiffYear(this));

        Spanned statsYearTotalTimeStylized = Html.fromHtml(FormatTime.stylizedStats(statsYearTotalTime)
                + " - " + this.getString(R.string.stats_total_drive_time));
        Spanned statsYearTotalTimeNoSpeedStylized = Html.fromHtml(FormatTime.stylizedStats(statsYearTotalTimeNoSpeed)
                + " - " + getString(R.string.stats_time_if_you_did_not_seepd));
        Spanned statsYearTimeDiffStylized = Html.fromHtml(FormatTime.stylizedStats(statsYearTimeDiff)
                + " - " + this.getString(R.string.stats_time_difference));
        Spanned statsYearRatioStylized = Html.fromHtml(this.getString(R.string.stats_percentage_sooner_start) + " <b>"
                + Math.round((Prefs.getTimeDiffYear(this) / Prefs.getTimeTotalYear(this)) * 100) + "%</b> "
                + this.getString(R.string.stats_percentage_sooner_end));

        ((TextView) dialogView.findViewById(R.id.stats_year_total_time)).setText(statsYearTotalTimeStylized);
        ((TextView) dialogView.findViewById(R.id.stats_year_total_time_no_speed)).setText(statsYearTotalTimeNoSpeedStylized);
        ((TextView) dialogView.findViewById(R.id.stats_year_time_diff)).setText(statsYearTimeDiffStylized);
        ((TextView) dialogView.findViewById(R.id.stats_year_ratio)).setText(statsYearRatioStylized);

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setTitle(R.string.stats_title)
                .setCancelable(true)
                .setPositiveButton(R.string.close_dialog_button, null)
                .setNeutralButton("clear week", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        Prefs.setTimeDiffWeek(MainActivity.this, 0D);
                        Prefs.setTimeTotalWeek(MainActivity.this, 0);
                        Prefs.setTimeDiffWeekNum(MainActivity.this, 0);
                    }
                })
                .show();

        Answers.getInstance().logCustom(new CustomEvent("Viewed stats"));
    }

    public void startStopButtonOnClick(View view) {
        if (mainService == null) {
            startMainService();
        } else {
            stopMainService();
        }
    }

    private void startMainService() {
        Crashlytics.log(Log.INFO, MainActivity.class.getSimpleName(), "MainService start chain");
        if (checkPlayServicesPrereq() && requestLocationPermission() && checkGPSPrereq() && checkNetworkPrereq()) {
            styleStartStopButton(true);
            reset.setVisibility(View.INVISIBLE);

            startService(new Intent(this, MainService.class));
            bindService(new Intent(this, MainService.class), mainServiceConn, BIND_AUTO_CREATE);
            totalTimeStartOrStop = true;
            totalTimeHandler.postDelayed(totalTimeRunnable, TOTAL_TIME_REFRESH_FREQ);

            Crashlytics.log(Log.INFO, MainActivity.class.getSimpleName(), "MainService started");
            Answers.getInstance().logCustom(new CustomEvent(useHereMaps ? "Using HERE" : "Using Overpass"));
            Answers.getInstance().logCustom(new CustomEvent(Prefs.isUseKph(this) ? "Using kph" : "Using mph"));
        } else {
            Crashlytics.log(Log.INFO, MainActivity.class.getSimpleName(), "MainService not started");
        }
    }

    private void stopMainService() {
        if (mainService != null) mainService.stopTime = System.nanoTime();
        Crashlytics.log(Log.INFO, MainActivity.class.getSimpleName(), "Stopping MainService");
        styleStartStopButton(false);
        finalizeSessionInUI();
        unbindService(mainServiceConn);
        stopService(new Intent(this, MainService.class));
        mainService = null;
        totalTimeHandler.removeCallbacks(totalTimeRunnable);
        showHereSuggestion();
    }

    private void styleStartStopButton(boolean start) {
        if (start) {
            //noinspection RedundantCast
            ((FloatingActionButton) findViewById(R.id.start_stop))
                    .setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.stopButton))); //Only solution I've found to be compatible with Android 4.2
            ((FloatingActionButton) findViewById(R.id.start_stop))
                    .setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pause));
        } else {
            //noinspection RedundantCast
            ((FloatingActionButton) findViewById(R.id.start_stop))
                    .setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.startButton)));
            ((FloatingActionButton) findViewById(R.id.start_stop))
                    .setImageDrawable(ContextCompat.getDrawable(this, R.drawable.car));
        }
    }

    private void showHereSuggestion() {
        if (useHereMaps || Prefs.isHereSuggestionAcknowledged(this)) return;

        Crashlytics.log(Log.INFO, MainActivity.class.getSimpleName(), "showHereSuggestion()");

        Snackbar snackbar = Snackbar
                .make(findViewById(R.id.start_stop), R.string.here_maps_suggestion_snackbar_text, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.dismiss_snackbar, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Prefs.setHereSuggestionAcknowledged(MainActivity.this, true);
                    }
                });

        View snackbarView = snackbar.getView();
        snackbarView.setBackgroundColor(getResources().getColor(R.color.darkGray));
        TextView textView = (TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text);
        textView.setMaxLines(5); //Override 2 line limit
        snackbar.show();
    }

    public void resetSessionOnClick(View view) {
        Crashlytics.log(Log.INFO, MainActivity.class.getSimpleName(), "resetSessionOnClick()");

        Prefs.setSessionTimeDiff(this, 0D);
        Prefs.setSessionTimeTotal(this, 0);

        UIData uiData = new UIData();
        uiData.setTimeDiff(0D);
        updateUI(uiData);

        reset.setVisibility(View.INVISIBLE);
    }

    private void missingOpenStreetMapLimitOnClick() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.missing_open_street_map_limit_dialog_title)
                .setMessage(R.string.missing_open_street_map_limit_dialog_content)
                .setCancelable(true)
                .setPositiveButton(R.string.settings_dialog_button, new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                    }
                })
                .show();

        Answers.getInstance().logCustom(new CustomEvent("Viewed missing OpenStreetMaps limit"));
    }

    private boolean requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
            Crashlytics.log(Log.INFO, MainActivity.class.getSimpleName(), "Request location permission");
            return false; //Short circuit startMainService() call, it will be recalled onRequestPermissionsResult()
        }
        Crashlytics.log(Log.INFO, MainActivity.class.getSimpleName(), "Location permission granted");
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) { //Success
            startMainService(); //Restart startMainService() chain
        } else {
            noGPSPermissionToast.show();
        }
    }

    private boolean checkGPSPrereq() {
        LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Crashlytics.log(Log.INFO, MainActivity.class.getSimpleName(), "GPS provider enabled");
            return true;
        }

        //Checking if GPS is enabled is spotty on some API levels (18) and some devices. Checking again in case of false negative.
        String networkList = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
        if (networkList != null && networkList.contains("gps")) {
            Crashlytics.log(Log.INFO, MainActivity.class.getSimpleName(), "GPS in network list");
            return true;
        }

        enableGPSViaPlayServices();
        Crashlytics.log(Log.INFO, MainActivity.class.getSimpleName(), "GPS is disabled");
        return false; //Short circuit startMainService() call, it will be recalled by enableGPSViaPlayServices()
    }

    private void showNoGPSAlert() {
        Crashlytics.log(Log.INFO, MainActivity.class.getSimpleName(), "showNoGPSAlert()");

        int messageRef = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) ?
                R.string.gps_disabled_message_4_4up : R.string.gps_disabled_message_4_3down;

        new AlertDialog.Builder(this)
                .setMessage(messageRef)
                .setCancelable(true)
                .setPositiveButton(R.string.go_to_location_settings_alert_button_text, new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .show();
    }

    private boolean checkNetworkPrereq() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnectedOrConnecting()) {
            noNetworkToast.show();
            Crashlytics.log(Log.ERROR, MainActivity.class.getSimpleName(), "Network unavailable");
            return false;
        }
        Crashlytics.log(Log.INFO, MainActivity.class.getSimpleName(), "Network available");
        return true;
    }

    private boolean checkPlayServicesPrereq() {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(this);
        if (result != ConnectionResult.SUCCESS) {
            if (googleAPI.isUserResolvableError(result)) {
                googleAPI.getErrorDialog(this, result, 0).show();

                Crashlytics.log(Log.INFO, MainActivity.class.getSimpleName(), "PlayServices update required");
                Answers.getInstance().logCustom(new CustomEvent("PlayServices update required"));
            } else {
                playServicesErrorToast.show();

                Crashlytics.log(Log.ERROR, MainActivity.class.getSimpleName(), "PlayServices incompatible");
                Answers.getInstance().logCustom(new CustomEvent("PlayServices incompatible"));
            }
            return false;
        }

        Crashlytics.log(Log.INFO, MainActivity.class.getSimpleName(), "PlayServices compatible");
        return true;
    }

    private void enableGPSViaPlayServices() {
        Crashlytics.log(Log.INFO, MainActivity.class.getSimpleName(), "enableGPSViaPlayServices()");

        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();

        LocationSettingsRequest locationSettingsRequest = new LocationSettingsRequest.Builder()
                .addLocationRequest(new LocationRequest().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY))
                .setAlwaysShow(true)
                .build();

        LocationServices.SettingsApi.checkLocationSettings(googleApiClient, locationSettingsRequest)
        .setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult result) {
                boolean showingLocationSettingsDialog = false;
                if (result.getStatus().getStatusCode() == LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {
                    try {
                        //Show location settings change dialog and check the result in onActivityResult()
                        result.getStatus().startResolutionForResult(MainActivity.this, REQUEST_LOCATION);
                        showingLocationSettingsDialog = true;
                        Crashlytics.log(Log.INFO, MainActivity.class.getSimpleName(), "Showing PlayServices GPS settings dialog");
                    } catch (Exception e) {
                        Crashlytics.log(Log.ERROR, MainActivity.class.getSimpleName(), "Error showing PlayServices GPS settings dialog");
                        Crashlytics.logException(e);
                    }
                }
                if (!showingLocationSettingsDialog) {
                    showNoGPSAlert(); //Ask user to manually enable GPS
                    googleApiClient.disconnect();
                }
            }
        });
    }

    @Override //Receives result of enableGPSViaPlayServices AlertDialog choice
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            //Check for the integer request code originally supplied to startResolutionForResult()
            case REQUEST_LOCATION:
                if (resultCode == Activity.RESULT_OK) {
                    Crashlytics.log(Log.INFO, MainActivity.class.getSimpleName(), "GPS enabled via PlayServices");
                    startMainService(); //Restart startMainService() chain
                } else {
                    Crashlytics.log(Log.INFO, MainActivity.class.getSimpleName(), "GPS not enabled via PlayServices");
                    showNoGPSAlert(); //Ask user to manually enable GPS
                }
                googleApiClient.disconnect();
        }
    }

    @Override
    public void onStop() {
        Crashlytics.log(Log.INFO, MainActivity.class.getSimpleName(), "onStop()");
        noGPSPermissionToast.cancel();
        noNetworkToast.cancel();
        playServicesErrorToast.cancel();
        poweredByOpenStreetMapToast.cancel();
        poweredByHereMapsToast.cancel();

        if (totalTimeHandler != null) totalTimeHandler.removeCallbacks(totalTimeRunnable);
        if (mainService != null) unbindService(mainServiceConn);
        if (googleApiClient != null) googleApiClient.disconnect();

        super.onStop();
    }
}
