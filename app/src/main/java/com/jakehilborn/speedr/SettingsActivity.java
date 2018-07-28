package com.jakehilborn.speedr;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Spinner;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.jakehilborn.speedr.utils.Prefs;

public class SettingsActivity extends AppCompatActivity {

    private Spinner speedUnitSpinner;
    private SwitchCompat keepScreenOnSwitch;
    private TextView version;

    private GoogleApiClient googleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Crashlytics.log(Log.INFO, SettingsActivity.class.getSimpleName(), "onCreate()");
        setContentView(R.layout.activity_settings);

        speedUnitSpinner = (Spinner) findViewById(R.id.speed_unit);
        speedUnitSpinner.setSelection(Prefs.isUseKph(this) ? 1 : 0); //defaults to mph - 0 is mph, 1 is km/h

        keepScreenOnSwitch = (SwitchCompat) findViewById(R.id.screen_on_switch);
        keepScreenOnSwitch.setChecked(Prefs.isKeepScreenOn(this));

        version = (TextView) findViewById(R.id.version_button);
    }

    @Override
    protected void onStart() {
        Crashlytics.log(Log.INFO, SettingsActivity.class.getSimpleName(), "onStart()");
        String versionString = getString(R.string.version_text) + " " + BuildConfig.VERSION_NAME;
        version.setText(versionString);

        if (BuildConfig.VERSION_CODE < Prefs.getLatestVersion(this)) {
            findViewById(R.id.update_available_section).setVisibility(View.VISIBLE);
            findViewById(R.id.update_available).setVisibility(View.VISIBLE);
        }

        super.onStart();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.appbar_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.dev_info_appbar_button:
                devInfoOnClick();
                return true;
            case android.R.id.home:
                Crashlytics.log(Log.INFO, SettingsActivity.class.getSimpleName(), "AppBar Home pressed");
                return super.onOptionsItemSelected(item);
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void launchWebpage(String uri) {
        Crashlytics.log(Log.INFO, SettingsActivity.class.getSimpleName(), "launchWebpage()");
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        startActivity(intent);
    }

    public void openStreetMapCoverageOnClick(View view) {
        Crashlytics.log(Log.INFO, SettingsActivity.class.getSimpleName(), "openStreetMapCoverageOnClick()");

        final String mapOfUnitedStates = "http://product.itoworld.com/map/124?lat=37.77557&lon=-100.44588&zoom=4";

        if (ContextCompat.checkSelfPermission(SettingsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                        @Override
                        @SuppressWarnings("MissingPermission")
                        public void onConnected(Bundle bundle) {
                            Crashlytics.log(Log.INFO, SettingsActivity.class.getSimpleName(), "Coverage map with location");

                            String uri;
                            Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
                            if (lastLocation != null) {
                                uri = "http://product.itoworld.com/map/124?lat=" + lastLocation.getLatitude() + "&lon=" + lastLocation.getLongitude() + "&zoom=14";
                            } else {
                                uri = mapOfUnitedStates;
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
            Crashlytics.log(Log.INFO, SettingsActivity.class.getSimpleName(), "Coverage map without location");
            launchWebpage(mapOfUnitedStates);
        }

        Answers.getInstance().logCustom(new CustomEvent("Launched OpenStreetMap coverage"));
    }

    public void openStreetMapDonateOnClick(View view) {
        launchWebpage("https://donate.openstreetmap.org");
        Answers.getInstance().logCustom(new CustomEvent("Launched OpenStreetMap donate"));
    }

    public void keepScreenOnClick(View view) {
        Answers.getInstance().logCustom(new CustomEvent(keepScreenOnSwitch.isChecked() ? "Keep screen on enabled" : "Keep screen on enabled"));
    }

    public void privacyAndTermsOnClick(View view) {
        Crashlytics.log(Log.INFO, SettingsActivity.class.getSimpleName(), "privacyAndTermsOnClick()");

        String content = getString(R.string.privacy_policy_content);

        String localizedTerms = getString(R.string.speedr_terms_content);
        String englishTerms = "Speedr is for informational purposes only. Its function is to quantify how much time, or how little time, one saves when speeding in their car to help the user decide if speeding is worth the safety, monetary, and legal risks. Speeding is illegal and dangerous. By accepting these terms you absolve the Speedr developers, speed limit providers, and all other parties of any responsibility for accidents, legal consequences, and any and all other outcomes. The data presented by Speedr is not guaranteed to be accurate. Outdated/incorrect speed limit data and inaccurate GPS sensors may produce faulty data. Pay attention to the posted speed limits of roads as Speedr may not present accurate speed limits and pay attention to your vehicle's speedometer as Speedr may not present accurate current speed readings.";

        //These terms are important. Always show original in addition to localized terms since we can't rely on translators to correctly word this.
        if (!localizedTerms.equals(englishTerms)) {
            content += "<br><br>" + localizedTerms + "<br><br>" + englishTerms;
        } else {
            content += "<br><br>" + englishTerms;
        }

        ((TextView) new AlertDialog.Builder(this)
                .setTitle(R.string.privacy_policy_title)
                .setMessage(Html.fromHtml(content))
                .setCancelable(true)
                .setNegativeButton(R.string.close_dialog_button, null)
                .show()
                .findViewById(android.R.id.message)) //These 2 lines make the hyperlinks clickable
                .setMovementMethod(LinkMovementMethod.getInstance());

        Answers.getInstance().logCustom(new CustomEvent("Viewed privacy policy"));
    }

    private void devInfoOnClick() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.developed_by_jake_hilborn_dialog_title)
                .setCancelable(true)
                .setNeutralButton(R.string.github_link_text, new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        launchWebpage("https://github.com/jakehilborn/speedr");
                        Answers.getInstance().logCustom(new CustomEvent("Launched GitHub"));
                    }
                })
                .setNegativeButton(R.string.linkedin_link_text, new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        launchWebpage("https://www.linkedin.com/in/jakehilborn");
                        Answers.getInstance().logCustom(new CustomEvent("Launched LinkedIn"));
                    }
                })
                .setPositiveButton(R.string.email_link_text, new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        Intent intent = new Intent(Intent.ACTION_SENDTO);
                        intent.setData(Uri.parse("mailto: jakehilborn@gmail.com"));
                        startActivity(Intent.createChooser(intent, getString(R.string.email_speedr_developer_chooser_text)));
                    }
                })
                .show();

        Answers.getInstance().logCustom(new CustomEvent("Viewed developer info"));
    }

    public void versionOnClick(View view) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.changelog_dialog_title)
                .setMessage(R.string.changelog_content)
                .setCancelable(true)
                .setNegativeButton(R.string.close_dialog_button, null)
                .show();

        Answers.getInstance().logCustom(new CustomEvent("Viewed changelog"));
    }

    public void updateAvailableOnClick(View view) {
        Prefs.setUpdateAcknowledged(this, true);
        launchWebpage("https://jakehilborn.github.io/speedr");
        Answers.getInstance().logCustom(new CustomEvent("Settings update download"));
    }

    @Override
    public void onBackPressed() {
        Crashlytics.log(Log.INFO, SettingsActivity.class.getSimpleName(), "onBackPressed()");
        super.onBackPressed();
    }

    @Override
    public void onPause() {
        Crashlytics.log(Log.INFO, SettingsActivity.class.getSimpleName(), "onPause()");
        Prefs.setUseKph(this, (speedUnitSpinner.getSelectedItemPosition() == 1)); //0 is mph, 1 is km/h
        Prefs.setKeepScreenOn(this, keepScreenOnSwitch.isChecked());
        super.onPause();
    }
}
