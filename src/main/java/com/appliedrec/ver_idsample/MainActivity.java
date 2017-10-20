package com.appliedrec.ver_idsample;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.appliedrec.ver_id.VerID;
import com.appliedrec.ver_id.VerIDAuthenticationIntent;
import com.appliedrec.ver_id.VerIDRegistrationIntent;
import com.appliedrec.ver_id.model.VerIDUser;
import com.appliedrec.ver_id.session.VerIDAuthenticationSessionSettings;
import com.appliedrec.ver_id.session.VerIDRegistrationSessionSettings;
import com.appliedrec.ver_id.session.VerIDSessionResult;
import com.appliedrec.ver_id.ui.VerIDActivity;
import com.appliedrec.ver_id.util.VerIDLogSubmitter;

public class MainActivity extends AppCompatActivity {

    static final int AUTHENTICATION_REQUEST_CODE = 1;
    static final int REGISTRATION_REQUEST_CODE = 2;

    private VerIDUser verIDUser;
    private Button authenticateButton;
    private Button registerButton;
    private View loadingIndicatorView;
    private ImageView profileImageView;
    private View scrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        loadingIndicatorView = findViewById(R.id.loading);
        registerButton = (Button) findViewById(R.id.register);
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                register();
            }
        });
        authenticateButton = (Button) findViewById(R.id.authenticate);
        authenticateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Authenticate if there is a registered user, otherwise register
                if (verIDUser != null) {
                    authenticate();
                }
            }
        });
        profileImageView = (ImageView) findViewById(R.id.profileImage);
        scrollView = findViewById(R.id.scrollView);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        VerID.shared.unload();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (VerID.shared.isLoaded()) {
            menu.findItem(R.id.action_reset_registration).setEnabled(verIDUser != null);
        } else {
            menu.findItem(R.id.action_reset_registration).setEnabled(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_reset_registration:
                if (verIDUser != null) {
                    try {
                        VerID.shared.deregisterUser(verIDUser.getUserId());
                        updateRegisteredUser();
                        invalidateOptionsMenu();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return true;
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ensure Ver-ID is loaded
        VerID.shared.load(this, new VerID.LoadCallback() {
            @Override
            public void onLoad() {
                if (!isDestroyed() && !isFinishing()) {
                    // Ver-ID is loaded, hide progress indicator and show buttons
                    updateRegisteredUser();
                    invalidateOptionsMenu();
                    loadingIndicatorView.setVisibility(View.GONE);
                    VerID.shared.setLogSubmitter(new VerIDLogSubmitter());
                    PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit()
                            .putBoolean(getResources().getString(R.string.pref_key_enable_logging), VerID.shared.getLogEnabled())
                            .putString(getResources().getString(R.string.pref_key_security_level), Integer.toString(VerID.shared.getSecurityLevel().ordinal()))
                            .apply();
                }
            }

            @Override
            public void onError(Exception e) {
                throw new RuntimeException(getString(R.string.verid_failed_to_load), e);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // To inspect the result of the session:
        if (resultCode == RESULT_OK && data != null && (requestCode == REGISTRATION_REQUEST_CODE || requestCode == AUTHENTICATION_REQUEST_CODE)) {
            VerIDSessionResult result = data.getParcelableExtra(VerIDActivity.EXTRA_SESSION_RESULT);
            // ...
        }
        if (requestCode == REGISTRATION_REQUEST_CODE) {
            updateRegisteredUser();
            invalidateOptionsMenu();
        }
    }

    private void updateRegisteredUser() {
        // Get the registered user from the database
        Uri profileImageUri = null;
        try {
            VerIDUser[] users = VerID.shared.getRegisteredVerIDUsers();
            if (users.length > 0) {
                verIDUser = users[0];
                profileImageUri = VerID.shared.getUserProfilePictureUri(verIDUser.getUserId());
            } else {
                verIDUser = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        profileImageView.setImageDrawable(null);
        if (profileImageUri != null) {
            profileImageView.setImageURI(profileImageUri);
            profileImageView.setVisibility(View.VISIBLE);
            scrollView.setVisibility(View.GONE);
        } else {
            profileImageView.setVisibility(View.GONE);
            scrollView.setVisibility(View.VISIBLE);
        }
        registerButton.setVisibility(View.VISIBLE);
        authenticateButton.setVisibility(View.VISIBLE);
        authenticateButton.setEnabled(verIDUser != null);
        if (verIDUser != null) {
            registerButton.setText(R.string.register_more_faces);
        } else {
            registerButton.setText(R.string.register);
        }
    }

    private void register() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        VerID.LivenessDetection livenessDetection = VerID.LivenessDetection.NONE;
        if (preferences.getBoolean(getString(R.string.enable_liveness_detection), true)) {
            livenessDetection = VerID.LivenessDetection.REGULAR;
        }
        // If your application requires an extra level of confidence on liveness detection set the livenessDetection parameter to VerID.LivenessDetection.STRICT.
        // This negatively affects the user experience at registration.
        register(livenessDetection);
    }

    private void register(VerID.LivenessDetection livenessDetection) {
        VerIDRegistrationSessionSettings settings = new VerIDRegistrationSessionSettings(VerIDUser.DEFAULT_USER_ID, livenessDetection);
        // Setting showGuide to false will prevent the activity from displaying a guide on how to register
        settings.showGuide = true;
        // Setting showResult to false will prevent the activity from displaying a result at the end of the session
        settings.showResult = true;
        Intent intent = new VerIDRegistrationIntent(this, settings);
        startActivityForResult(intent, REGISTRATION_REQUEST_CODE);
    }

    private void authenticate() {
        // Authenticate using face
        if (verIDUser == null) {
            return;
        }
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        VerID.LivenessDetection livenessDetection = VerID.LivenessDetection.NONE;
        if (preferences.getBoolean(getString(R.string.pref_key_enable_liveness_detection), true)) {
            livenessDetection = VerID.LivenessDetection.REGULAR;
        }
        // If your application requires an extra level of confidence on liveness detection set the livenessDetection parameter to VerID.LivenessDetection.STRICT.
        // Note that strict liveness detection requires the user to also be registered with the STRICT level of liveness detection. This negatively affects the user experience.
        VerIDAuthenticationSessionSettings settings = new VerIDAuthenticationSessionSettings(verIDUser.getUserId(), livenessDetection);
        // Setting showGuide to false will prevent the activity from displaying a guide on how to authenticate
        settings.showGuide = true;
        // Setting showResult to false will prevent the activity from displaying a result at the end of the session
        settings.showResult = true;
        try {
            if (VerID.shared.canUserAuthenticateWithSettings(verIDUser.getUserId(), settings)) {
                Intent intent = new VerIDAuthenticationIntent(this, settings);
                startActivityForResult(intent, AUTHENTICATION_REQUEST_CODE);
            } else {
                new AlertDialog.Builder(this)
                        .setMessage(R.string.unable_to_authenticate_with_settings)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                register(VerID.LivenessDetection.STRICT);
                            }
                        })
                        .create()
                        .show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
