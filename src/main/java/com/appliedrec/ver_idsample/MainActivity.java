package com.appliedrec.ver_idsample;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.appliedrec.ver_id.VerID;
import com.appliedrec.ver_id.VerIDAuthenticationIntent;
import com.appliedrec.ver_id.VerIDRegistrationIntent;
import com.appliedrec.ver_id.loaders.VerIDLoaderResponse;
import com.appliedrec.ver_id.loaders.VerIDUserPictureUriLoader;
import com.appliedrec.ver_id.loaders.VerIDUsersLoader;
import com.appliedrec.ver_id.model.VerIDUser;
import com.appliedrec.ver_id.session.VerIDAuthenticationSessionSettings;
import com.appliedrec.ver_id.session.VerIDRegistrationSessionSettings;
import com.appliedrec.ver_id.session.VerIDSessionResult;
import com.appliedrec.ver_id.ui.VerIDActivity;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<VerIDLoaderResponse> {

    static final int AUTHENTICATION_REQUEST_CODE = 1;
    static final int REGISTRATION_REQUEST_CODE = 2;
    private static final int USER_LOADER_ID = 1;
    private static final int USER_PICTURE_LOADER_ID = 2;

    private VerIDUser verIDUser;
    private Uri profileImageUri = null;
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
        loadRegisteredUser();
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
                        loadRegisteredUser();
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // To inspect the result of the session:
        if (resultCode == RESULT_OK && data != null && (requestCode == REGISTRATION_REQUEST_CODE || requestCode == AUTHENTICATION_REQUEST_CODE)) {
            VerIDSessionResult result = data.getParcelableExtra(VerIDActivity.EXTRA_SESSION_RESULT);
            // See documentation at
            // https://appliedrecognition.github.io/Ver-ID-Android-Sample/com.appliedrec.ver_id.session.VerIDSessionResult.html
        }
        if (requestCode == REGISTRATION_REQUEST_CODE) {
            loadRegisteredUser();
        }
    }

    /**
     * Use a loader to retrieve the registered user in the background
     * This task may take a while if Ver-ID has not yet finished loading
     */
    private void loadRegisteredUser() {
        getSupportLoaderManager().initLoader(USER_LOADER_ID, null, this).forceLoad();
    }

    /**
     * Show register and authenticate buttons if Ver-ID is loaded
     */
    private void updateButtonVisibility() {
        int buttonVisibility = VerID.shared.isLoaded() ? View.VISIBLE : View.GONE;
        int loadingIndicatorVisibility = VerID.shared.isLoaded() ? View.GONE : View.VISIBLE;
        registerButton.setVisibility(buttonVisibility);
        authenticateButton.setVisibility(buttonVisibility);
        loadingIndicatorView.setVisibility(loadingIndicatorVisibility);
    }

    /**
     * Update UI showing a user profile picture if available
     */
    private void updateRegisteredUser() {
        profileImageView.setImageDrawable(null);
        if (profileImageUri != null) {
            profileImageView.setImageURI(profileImageUri);
            profileImageView.setVisibility(View.VISIBLE);
            scrollView.setVisibility(View.GONE);
        } else {
            profileImageView.setVisibility(View.GONE);
            scrollView.setVisibility(View.VISIBLE);
        }
        authenticateButton.setEnabled(verIDUser != null);
        if (verIDUser != null) {
            registerButton.setText(R.string.register_more_faces);
        } else {
            registerButton.setText(R.string.register);
        }
    }

    /**
     * Start a registration session with liveness detection settings loaded from preferences
     */
    private void register() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        VerID.LivenessDetection livenessDetection = VerID.LivenessDetection.NONE;
        if (preferences.getBoolean(getString(R.string.pref_key_enable_liveness_detection), true)) {
            livenessDetection = VerID.LivenessDetection.REGULAR;
        }
        // If your application requires an extra level of confidence on liveness detection set the livenessDetection parameter to VerID.LivenessDetection.STRICT.
        // This negatively affects the user experience at registration.
        register(livenessDetection);
    }

    /**
     * Start a registration session
     * @param livenessDetection Requested liveness detection setting
     */
    private void register(VerID.LivenessDetection livenessDetection) {
        VerIDRegistrationSessionSettings settings = new VerIDRegistrationSessionSettings(VerIDUser.DEFAULT_USER_ID, livenessDetection);
        // Setting showGuide to false will prevent the activity from displaying a guide on how to register
        settings.showGuide = true;
        // Setting showResult to false will prevent the activity from displaying a result at the end of the session
        settings.showResult = true;
        Intent intent = new VerIDRegistrationIntent(this, settings);
        startActivityForResult(intent, REGISTRATION_REQUEST_CODE);
    }

    /**
     * Start a face authentication session
     */
    private void authenticate() {
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
        if (livenessDetection != VerID.LivenessDetection.NONE) {
            // This setting dictates how many poses the user will be required to move her/his head to to ensure liveness
            // The higher the count the more confident we can be of a live face at the expense of usability
            // Note that 1 is added to the setting to include the initial mandatory straight pose
            settings.requiredNumberOfSegments = Integer.parseInt(preferences.getString(getString(R.string.pref_key_required_pose_count), "1")) + 1;
        }
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

    // User data loader

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        switch (id) {
            case USER_LOADER_ID:
                // Loads registered users
                return new VerIDUsersLoader(this);
            case USER_PICTURE_LOADER_ID:
                // Loads the profile picture of the given registered user
                return new VerIDUserPictureUriLoader(this, verIDUser.getUserId());
            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader loader, VerIDLoaderResponse data) {
        if (data != null && data.getException() != null && data.getException() instanceof IllegalStateException) {
            // Unable to load Ver-ID
            throw new RuntimeException(getString(R.string.verid_failed_to_load), ((VerIDLoaderResponse)data).getException());
        }
        switch (loader.getId()) {
            case USER_LOADER_ID:
                profileImageUri = null;
                verIDUser = null;
                if (data.getException() == null && data.getResult() != null) {
                    // Update preferences
                    PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit()
                            .putString(getResources().getString(R.string.pref_key_security_level), Integer.toString(VerID.shared.getSecurityLevel().ordinal()))
                            .apply();
                    VerIDUser[] users = (VerIDUser[]) data.getResult();
                    if (users.length > 0) {
                        verIDUser = users[0];
                        // Load the profile picture
                        getSupportLoaderManager().initLoader(USER_PICTURE_LOADER_ID, null, this).forceLoad();
                        return;
                    }
                }
                break;
            case USER_PICTURE_LOADER_ID:
                if (data.getException() == null && data.getResult() != null) {
                    profileImageUri = (Uri) data.getResult();
                }
                break;
        }
        updateRegisteredUser();
        invalidateOptionsMenu();
        updateButtonVisibility();
    }

    @Override
    public void onLoaderReset(Loader loader) {
        verIDUser = null;
        profileImageUri = null;
        updateRegisteredUser();
        invalidateOptionsMenu();
        updateButtonVisibility();
    }
}
