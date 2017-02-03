package com.jakehilborn.speedr;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.jakehilborn.speedr.utils.Prefs;

public class SettingsActivity extends AppCompatActivity {

    private int APP_ID_LENGTH = 20;
    private int APP_CODE_LENGTH = 22;

    private EditText appIdField;
    private EditText appCodeField;
    private Toast emptyCredentials;
    private Toast shortCredentials;
    private AppCompatButton hereMapsButton;
    private AppCompatButton openStreetMapButton;
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
        shortCredentials = Toast.makeText(this, R.string.short_here_maps_credentials_toast, Toast.LENGTH_LONG);

        openStreetMapButton = (AppCompatButton) findViewById(R.id.open_street_map_button);
        openStreetMapButton.setSupportBackgroundTintList(ColorStateList.valueOf(getResources().getColor(
                Prefs.isUseHereMaps(this) ? R.color.unselectedButtonGray : R.color.colorAccent
        )));
        openStreetMapButton.setOnClickListener(new View.OnClickListener() { //xml defined onClick for AppCompatButton crashes on Android 4.2
            public void onClick(View view) {
                limitProviderSelectorHandler(false);
            }
        });

        hereMapsButton = (AppCompatButton) findViewById(R.id.here_maps_button);
        hereMapsButton.setSupportBackgroundTintList(ColorStateList.valueOf(getResources().getColor(
                Prefs.isUseHereMaps(this) ? R.color.colorAccent : R.color.unselectedButtonGray
        )));
        hereMapsButton.setOnClickListener(new View.OnClickListener() { //xml defined onClick for AppCompatButton crashes on Android 4.2
            public void onClick(View view) {
                limitProviderSelectorHandler(true);
            }
        });

        speedUnitSpinner = (Spinner) findViewById(R.id.speed_unit);
        speedUnitSpinner.setSelection(Prefs.isUseKph(this) ? 1 : 0); //defaults to mph - 0 is mph, 1 is km/h
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
                devInfoOnClick();
                return true;
            case android.R.id.home:
                if (newHereCredentials()) {
                    return true;
                } else {
                    return super.onOptionsItemSelected(item);
                }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void limitProviderSelectorHandler(boolean isUseHereMaps) {
        if (isUseHereMaps && (appIdField.getText().toString().trim().isEmpty() || appCodeField.getText().toString().trim().isEmpty())) {
            emptyCredentials.show();
            isUseHereMaps = false;
        }

        if (isUseHereMaps && !Prefs.isHereMapsTermsAccepted(this)) {
            showHereMapsTerms();
            isUseHereMaps = false;
        }

        if (isUseHereMaps && (appIdField.getText().toString().trim().length() < APP_ID_LENGTH ||
                appCodeField.getText().toString().trim().length() < APP_CODE_LENGTH)) {
            shortCredentials.show(); //Show warning but leave HERE Maps enabled
        }

        saveHereCredsIfChanged();
        Prefs.setUseHereMaps(this, isUseHereMaps);

        openStreetMapButton.setSupportBackgroundTintList(ColorStateList.valueOf(getResources().getColor(
                isUseHereMaps ? R.color.unselectedButtonGray : R.color.colorAccent
        )));
        hereMapsButton.setSupportBackgroundTintList(ColorStateList.valueOf(getResources().getColor(
                isUseHereMaps ? R.color.colorAccent : R.color.unselectedButtonGray
        )));
    }

    //Detect if user input HERE credentials but did not click the HERE MAPS button, activate HERE for them.
    private boolean newHereCredentials() {
        if (appIdField.getText().toString().trim().length() < APP_ID_LENGTH) {
            return false;
        }
        if (appCodeField.getText().toString().trim().length() < APP_CODE_LENGTH) {
            return false;
        }
        if (appIdField.getText().toString().trim().equals(Prefs.getHereAppId(this)) &&
                appCodeField.getText().toString().trim().equals(Prefs.getHereAppCode(this))) {
            return false;
        }
        if (Prefs.isUseHereMaps(this)) {
            return false;
        }

        limitProviderSelectorHandler(true);
        Toast.makeText(this, R.string.enabled_here_maps_toast, Toast.LENGTH_LONG).show();

        Answers.getInstance().logCustom(new CustomEvent("Auto enabled HERE"));

        return true;
    }

    private void saveHereCredsIfChanged() {
        if (appIdField.getText().toString().trim().equals(Prefs.getHereAppId(this)) &&
                appCodeField.getText().toString().trim().equals(Prefs.getHereAppCode(this))) {
            return;
        }

        Prefs.setHereAppId(this, appIdField.getText().toString().trim());
        Prefs.setHereAppCode(this, appCodeField.getText().toString().trim());
        Prefs.setTimeOfHereCreds(this, System.currentTimeMillis());
    }

    private void showHereMapsTerms() {
        WebView webView = new WebView(this);
        webView.loadUrl(getString(R.string.here_maps_terms_url));

        new AlertDialog.Builder(this)
                .setView(webView)
                .setCancelable(true)
                .setPositiveButton(R.string.accept_here_maps_terms_alert_button_text, new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        Prefs.setHereMapsTermsAccepted(SettingsActivity.this, true);
                        limitProviderSelectorHandler(true); //Set limit provider now that terms have been accepted
                        Answers.getInstance().logCustom(new CustomEvent("Enabled HERE"));
                    }})
                .setNegativeButton(R.string.reject_here_maps_terms_alert_button_text, null)
                .show();
    }

    private void launchWebpage(String uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        startActivity(intent);
    }

    public void hereMapsCreateAccountOnClick(View view) {
        final View dialogView = getLayoutInflater().inflate(R.layout.here_account_dialog, null);

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .setPositiveButton(R.string.here_maps_create_account_dialog_button, new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        launchWebpage("https://developer.here.com/plans?create=Public_Free_Plan_Monthly&keepState=true&step=account");
                        Answers.getInstance().logCustom(new CustomEvent("Launched create account"));
                    }
                })
                .setNeutralButton(R.string.help_dialog_button, new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        showHereMapsCreateAccountHelpDialog();
                    }
                })
                .show();

        Handler handler = new Handler();

        int delay = 250;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                dialogView.findViewById(R.id.here_tutorial_step_1_text).setVisibility(View.VISIBLE);
            }
        }, delay);

        delay += 500;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                dialogView.findViewById(R.id.here_tutorial_step_1_image).setVisibility(View.VISIBLE);
            }
        }, delay);

        delay += 1600;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                dialogView.findViewById(R.id.here_tutorial_step_2_text).setVisibility(View.VISIBLE);
            }
        }, delay);

        delay += 500;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                dialogView.findViewById(R.id.here_tutorial_step_2_image).setVisibility(View.VISIBLE);
            }
        }, delay);
    }

    public void showHereMapsCreateAccountHelpDialog() {
        final View dialogView = getLayoutInflater().inflate(R.layout.here_account_help_dialog, null);

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .setPositiveButton(R.string.close_dialog_button, null)
                .show();

        Handler handler = new Handler();

        int delay = 250;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                dialogView.findViewById(R.id.here_tutorial_help_1_text).setVisibility(View.VISIBLE);
            }
        }, delay);

        delay += 500;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                dialogView.findViewById(R.id.here_tutorial_help_1_image).setVisibility(View.VISIBLE);
            }
        }, delay);

        delay += 1600;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                dialogView.findViewById(R.id.here_tutorial_help_2_text).setVisibility(View.VISIBLE);
            }
        }, delay);

        delay += 500;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                dialogView.findViewById(R.id.here_tutorial_help_2_image).setVisibility(View.VISIBLE);
            }
        }, delay);

        Answers.getInstance().logCustom(new CustomEvent("Viewed HERE help dialog"));
    }

    public void openStreetMapCoverageOnClick(View view) {
        final String mapOfUnitedStates = "http://product.itoworld.com/map/124?lat=37.77557&lon=-100.44588&zoom=4";

        if (ContextCompat.checkSelfPermission(SettingsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                        @Override
                        @SuppressWarnings("MissingPermission")
                        public void onConnected(Bundle bundle) {
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
            launchWebpage(mapOfUnitedStates);
        }

        Answers.getInstance().logCustom(new CustomEvent("Launched OpenStreetMap coverage"));
    }

    public void openStreetMapDonateOnClick(View view) {
        launchWebpage("https://donate.openstreetmap.org");
        Answers.getInstance().logCustom(new CustomEvent("Launched OpenStreetMap donate"));
    }

    public void privacyAndTermsOnClick(View view) {
        String content = getString(R.string.privacy_policy_content).replace("HERE_TERMS_PLACEHOLDER", getString(R.string.here_maps_terms_url));

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
                        launchWebpage("https://github.com/jakehilborn");
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

    @Override
    public void onBackPressed() {
        if (!newHereCredentials()) {
            super.onBackPressed();
        }
    }

    @Override
    public void onPause() {
        saveHereCredsIfChanged();
        Prefs.setUseKph(this, (speedUnitSpinner.getSelectedItemPosition() == 1)); //0 is mph, 1 is km/h
        emptyCredentials.cancel();
        shortCredentials.cancel();
        super.onPause();
    }
}
