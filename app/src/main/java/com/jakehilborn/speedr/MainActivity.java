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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.jakehilborn.speedr.utils.Prefs;
import com.jakehilborn.speedr.utils.UnitUtils;

public class MainActivity extends AppCompatActivity implements MainService.Callback {

    private static final int BIND_IF_SERVICE_RUNNING = 0;
    private static final int REQUEST_LOCATION = 1;

    private MainService mainService;
    private GoogleApiClient googleApiClient;

    private boolean useHereMaps;

    private TextView timeDiffH;
    private TextView timeDiffHSymbol;
    private TextView timeDiffM;
    private TextView timeDiffMSymbol;
    private TextView timeDiffS;
    private TextView timeDiffS10th;
    private TextView speed;
    private TextView speedUnit;
    private TextView limit;
    private TextView limitUnit;
    private TextView pendingHereActivationNotice;

    private AppCompatImageButton reset;
    private AppCompatImageButton limitProviderLogo;
    private AppCompatImageButton missingOpenStreetMapLimit;

    private Toast noGPSPermissionToast;
    private Toast noNetworkToast;
    private Toast playServicesErrorToast;
    private Toast poweredByOpenStreetMapToast;
    private Toast poweredByHereMapsToast;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        timeDiffH = (TextView) findViewById(R.id.time_diff_h);
        timeDiffHSymbol = (TextView) findViewById(R.id.time_diff_h_symbol);
        timeDiffM = (TextView) findViewById(R.id.time_diff_m);
        timeDiffMSymbol = (TextView) findViewById(R.id.time_diff_m_symbol);
        timeDiffS = (TextView) findViewById(R.id.time_diff_s);
        timeDiffS10th = (TextView) findViewById(R.id.time_diff_s10th);
        speed = (TextView) findViewById(R.id.speed);
        speedUnit = (TextView) findViewById(R.id.speed_unit);
        limit = (TextView) findViewById(R.id.limit);
        limitUnit = (TextView) findViewById(R.id.limit_unit);
        pendingHereActivationNotice = (TextView) findViewById(R.id.pending_here_activation_notice);

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

        restoreSessionInUI();

        if (isMainServiceRunning()) {
            bindService(new Intent(this, MainService.class), mainServiceConn, BIND_IF_SERVICE_RUNNING);
        }
    }

    private boolean isMainServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)){
            if("com.jakehilborn.speedr.MainService".equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private ServiceConnection mainServiceConn = new ServiceConnection() { //binder boilerplate
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            MainService.LocalBinder binder = (MainService.LocalBinder) service;
            mainService = binder.getService();
            mainService.setCallback(MainActivity.this);

            //Sets UI values on MainActivity onStart() if MainService was already running
            styleStartStopButton(true);
            setStatsInUI(mainService.pollStats());
        }

        @Override //Only called on service crashes, not called onDestroy or on unbindService
        public void onServiceDisconnected(ComponentName className) {
            mainService = null;
        }
    };

    @Override
    public void onStatsUpdate(Stats stats) {
        setStatsInUI(stats);
    }

    private void setStatsInUI(Stats stats) {
        if (stats.getTimeDiff() != null) {
            timeDiffS10th.setText(String.valueOf(UnitUtils.nanosTo10thsModuloSeconds(stats.getTimeDiff())));
            timeDiffS.setText(String.valueOf(UnitUtils.nanosToSecondsModuloMinutes(stats.getTimeDiff())));

            if (stats.getTimeDiff() >= UnitUtils.NANO_ONE_MINUTE) {
                timeDiffM.setText(String.valueOf(UnitUtils.nanosToMinutesModuloHours(stats.getTimeDiff())));
                timeDiffM.setVisibility(View.VISIBLE);
                timeDiffMSymbol.setVisibility(View.VISIBLE);
            } else {
                timeDiffM.setVisibility(View.GONE); //GONE used instead of INVISIBLE so that this view is not rendered which lets timeDiff center correctly
                timeDiffMSymbol.setVisibility(View.GONE);
            }
            if (stats.getTimeDiff() >= UnitUtils.NANO_ONE_HOUR) {
                timeDiffH.setText(String.valueOf(UnitUtils.nanosToHoursModuloMinutes(stats.getTimeDiff())));
                timeDiffH.setVisibility(View.VISIBLE);
                timeDiffHSymbol.setVisibility(View.VISIBLE);
            } else {
                timeDiffH.setVisibility(View.GONE);
                timeDiffHSymbol.setVisibility(View.GONE);
            }
        }

        if (stats.getLimit() == null || stats.getLimit() == 0) {
            limit.setText("--");
            //If service is running and returns null limit for OpenStreetMap show badge about spotty coverage
            if (!useHereMaps && mainService != null) missingOpenStreetMapLimit.setVisibility(View.VISIBLE);
        } else {
            limit.setText(String.valueOf(stats.getLimit()));
            missingOpenStreetMapLimit.setVisibility(View.INVISIBLE);
        }

        if (stats.getSpeed() == null) {
            speed.setText("--");
        } else {
            speed.setText(String.valueOf(stats.getSpeed()));
        }

        if (useHereMaps && mainService != null && Prefs.isPendingHereActivation(this)) {
            pendingHereActivationNotice.setVisibility(View.VISIBLE);
        } else {
            pendingHereActivationNotice.setVisibility(View.GONE);
        }
    }

    private void restoreSessionInUI() { //Restores timeDiff from the completed MainService session and reset button if necessary
        Stats stats;
        if (mainService != null) {
            stats = mainService.pollStats();
        } else { //If service is not running then read the timeDiff from storage
            stats = new Stats();
            stats.setTimeDiff(Prefs.getSessionTimeDiff(this));
        }

        setStatsInUI(stats);

        if (stats.getTimeDiff() != 0 && !isMainServiceRunning()) { //MainService may not be bound yet so explicitly check if running
            reset.setVisibility(View.VISIBLE);
        }
    }

    private void finalizeSessionInUI() {
        Stats stats;
        if (mainService != null) {
            stats = mainService.pollStats();
        } else { //If MainService was unexpectedly terminated this else block provides null safety and displays most recent timeDiff
            stats = new Stats();
            stats.setTimeDiff(Prefs.getSessionTimeDiff(this));
        }

        stats.setLimit(null);
        stats.setSpeed(null);
        setStatsInUI(stats);

        //Only show during active sessions
        missingOpenStreetMapLimit.setVisibility(View.INVISIBLE);
        pendingHereActivationNotice.setVisibility(View.GONE);

        if (stats.getTimeDiff() != 0) {
            reset.setVisibility(View.VISIBLE);
        } else {
            reset.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //item ID is not checked since the only menu button is the settings cog
        startActivity(new Intent(this, SettingsActivity.class));
        return true;
    }

    public void startStopButtonOnClick(View view) {
        if (mainService == null) {
            startMainService();
        } else {
            stopMainService();
        }
    }

    private void startMainService() {
        if (checkPlayServicesPrereq() && requestLocationPermission() && checkGPSPrereq() && checkNetworkPrereq()) {
            styleStartStopButton(true);
            reset.setVisibility(View.INVISIBLE);

            startService(new Intent(this, MainService.class));
            bindService(new Intent(this, MainService.class), mainServiceConn, BIND_AUTO_CREATE);
        }
    }

    private void stopMainService() {
        styleStartStopButton(false);
        finalizeSessionInUI();
        unbindService(mainServiceConn);
        stopService(new Intent(this, MainService.class));
        mainService = null;
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
        Stats stats = new Stats();
        stats.setTimeDiff(0D);
        setStatsInUI(stats);

        reset.setVisibility(View.INVISIBLE);

        Prefs.setSessionTimeDiff(this, 0D);
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
    }

    private boolean requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
            return false; //Short circuit startMainService() call, it will be recalled onRequestPermissionsResult()
        }
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
            return true;
        }

        //Checking if GPS is enabled is spotty on some API levels (18) and some devices. Checking again in case of false negative.
        String networkList = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
        if (networkList != null && networkList.contains("gps")) {
            return true;
        }

        enableGPSViaPlayServices();
        return false; //Short circuit startMainService() call, it will be recalled by enableGPSViaPlayServices()
    }

    private void showNoGPSAlert() {
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
            return false;
        }
        return true;
    }

    private boolean checkPlayServicesPrereq() {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(this);
        if (result != ConnectionResult.SUCCESS) {
            if (googleAPI.isUserResolvableError(result)) {
                googleAPI.getErrorDialog(this, result, 0).show();
            } else {
                playServicesErrorToast.show();
            }
            return false;
        }
        return true;
    }

    private void enableGPSViaPlayServices() {
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
                    } catch (Exception e) {
                        //Do nothing
                    }
                }
                if (!showingLocationSettingsDialog) {
                    showNoGPSAlert(); //Ask user to manually enable GPS
                    googleApiClient.disconnect();
                }
            }
        });
    }

    @Override //Needed to receive result of enableGPSViaPlayServices AlertDialog choice
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            //Check for the integer request code originally supplied to startResolutionForResult()
            case REQUEST_LOCATION:
                if (resultCode == Activity.RESULT_OK) {
                    startMainService(); //Restart startMainService() chain
                } else {
                    showNoGPSAlert(); //Ask user to manually enable GPS
                }
                googleApiClient.disconnect();
        }
    }

    @Override
    public void onStop() {
        noGPSPermissionToast.cancel();
        noNetworkToast.cancel();
        playServicesErrorToast.cancel();
        poweredByOpenStreetMapToast.cancel();
        poweredByHereMapsToast.cancel();

        if (mainService != null) unbindService(mainServiceConn);
        if (googleApiClient != null) googleApiClient.disconnect();

        super.onStop();
    }
}
