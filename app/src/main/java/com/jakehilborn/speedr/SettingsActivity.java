package com.jakehilborn.speedr;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.jakehilborn.speedr.utils.Prefs;

public class SettingsActivity extends AppCompatActivity {

    private EditText appIdField;
    private EditText appCodeField;
    private Toast emptyCredentials;
    private AppCompatButton hereMapsButton;
    private AppCompatButton openStreetMapsButton;
    private Spinner speedUnitSpinner;
    private GoogleApiClient googleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        appIdField = (EditText) findViewById(R.id.here_app_id);
        appIdField.setText(Prefs.getHereAppId(this));

        appCodeField = (EditText) findViewById(R.id.here_app_code);
        appCodeField.setText(Prefs.getHereAppCode(this));

        emptyCredentials = Toast.makeText(this, R.string.enter_here_maps_credentials_toast, Toast.LENGTH_LONG);

        openStreetMapsButton = (AppCompatButton) findViewById(R.id.open_street_maps_button);
        openStreetMapsButton.setSupportBackgroundTintList(ColorStateList.valueOf(getResources().getColor(
                Prefs.isUseHereMaps(this) ? R.color.materialGrey : R.color.colorAccent
        )));
        openStreetMapsButton.setOnClickListener(new View.OnClickListener() { //xml defined onClick for AppCompatButton crashes on Android 4.2
            public void onClick(View view) {
                limitProviderButtonHandler(false);
            }
        });
//
        hereMapsButton = (AppCompatButton) findViewById(R.id.here_maps_button);
        hereMapsButton.setSupportBackgroundTintList(ColorStateList.valueOf(getResources().getColor(
                Prefs.isUseHereMaps(this) ? R.color.colorAccent : R.color.materialGrey
        )));
        hereMapsButton.setOnClickListener(new View.OnClickListener() { //xml defined onClick for AppCompatButton crashes on Android 4.2
            public void onClick(View view) {
                limitProviderButtonHandler(true);
            }
        });

        speedUnitSpinner = (Spinner) findViewById(R.id.speed_unit);
        speedUnitSpinner.setSelection(Prefs.isUseKph(this) ? 1 : 0); //defaults to mph
    }

    @Override
    public void onPause() {
        Prefs.setHereAppId(this, appIdField.getText().toString().trim());
        Prefs.setHereAppCode(this, appCodeField.getText().toString().trim());
        Prefs.setUseKph(this, (speedUnitSpinner.getSelectedItemPosition() == 1)); //0 is mph, 1 is km/h
        if (emptyCredentials != null) emptyCredentials.cancel();
        super.onPause();
    }

    public void limitProviderButtonHandler(boolean isUseHereMaps) { //xml defined onClick not working on Android 4.2 so I'm using anonymous methods instead
        if (isUseHereMaps && (appIdField.getText().toString().isEmpty() || appCodeField.getText().toString().isEmpty())) {
            emptyCredentials.show();
            isUseHereMaps = false;
        }

        if (isUseHereMaps && !Prefs.isHereMapsTermsAccepted(this)) {
            showHereMapsTerms();
            isUseHereMaps = false;
        }

        Prefs.setUseHereMaps(this, isUseHereMaps);

        openStreetMapsButton.setSupportBackgroundTintList(ColorStateList.valueOf(getResources().getColor(
                isUseHereMaps ? R.color.materialGrey : R.color.colorAccent
        )));
        hereMapsButton.setSupportBackgroundTintList(ColorStateList.valueOf(getResources().getColor(
                isUseHereMaps ? R.color.colorAccent : R.color.materialGrey
        )));
    }

    private void showHereMapsTerms() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.here_maps_terms_content)
                .setCancelable(true)
                .setPositiveButton(R.string.accept_here_maps_terms_alert_button_text, new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        Prefs.setHereMapsTermsAccepted(SettingsActivity.this, true);
                        limitProviderButtonHandler(true); //Set limit provider now that terms have been accepted
                    }})
                .setNegativeButton(R.string.reject_here_maps_terms_alert_button_text, null);
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void launchWebpage(String uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        startActivity(intent);
    }

    public void openStreetMapsCoverageOnClick(View view) {
        if (ContextCompat.checkSelfPermission(SettingsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                        @Override
                        @SuppressWarnings("MissingPermission")
                        public void onConnected(Bundle bundle) {
                            String uri = "http://product.itoworld.com/map/124?lat=";
                            Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
                            if (lastLocation != null) {
                                uri = uri + lastLocation.getLatitude() + "&lon=" + lastLocation.getLongitude() + "&zoom=14";
                            }

                            launchWebpage(uri);
                            googleApiClient.disconnect();
                        }

                        @Override
                        public void onConnectionSuspended(int i) {}
                    })
                    .addApi(LocationServices.API)
                    .build();

            googleApiClient.connect();
        } else {
            launchWebpage("http://product.itoworld.com/map/124?lat=37.77557&lon=-100.44588&zoom=4"); //map of United States
        }
    }

    public void openStreetMapsDonateOnClick(View view) {
        launchWebpage("https://donate.openstreetmap.org");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.dev_info, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.dev_info:
                return devInfoOnClick();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private boolean devInfoOnClick() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.developed_by_jake_hilborn_dialog_title)
                .setCancelable(true)
                .setNeutralButton(R.string.github_link_text, new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        launchWebpage("https://github.com/jakehilborn");
                    }
                })
                .setNegativeButton(R.string.linkedin_link_text, new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        launchWebpage("https://www.linkedin.com/in/jakehilborn");
                    }
                })
                .setPositiveButton(R.string.email_link_text, new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        Intent intent = new Intent(Intent.ACTION_SENDTO);
                        intent.setData(Uri.parse("mailto: jakehilborn@gmail.com"));
                        startActivity(Intent.createChooser(intent, getString(R.string.email_speedr_developer_chooser_text)));
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
        return true;
    }

    public void versionOnClick(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.changelog_dialog_title)
                .setMessage(R.string.changelog_content)
                .setCancelable(true)
                .setNegativeButton(R.string.close_dialog_button, null);
        AlertDialog alert = builder.create();
        alert.show();
    }
}
