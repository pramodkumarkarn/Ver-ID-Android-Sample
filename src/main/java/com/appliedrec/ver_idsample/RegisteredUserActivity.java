package com.appliedrec.ver_idsample;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import com.appliedrec.ver_id.VerID;
import com.appliedrec.ver_id.VerIDAuthenticationIntent;
import com.appliedrec.ver_id.VerIDRegistrationIntent;
import com.appliedrec.ver_id.model.VerIDUser;
import com.appliedrec.ver_id.session.VerIDAuthenticationSessionSettings;
import com.appliedrec.ver_id.session.VerIDRegistrationSessionSettings;
import com.appliedrec.ver_id.session.VerIDSessionResult;
import com.appliedrec.ver_id.ui.VerIDActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class RegisteredUserActivity extends AppCompatActivity {

    public static final String EXTRA_USER = "com.appliedrec.verid.user";
    private static final int AUTHENTICATION_REQUEST_CODE = 0;
    private static final int REGISTRATION_REQUEST_CODE = 1;

    VerIDUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registered_user);
        if (getIntent() != null) {
            user = getIntent().getParcelableExtra(EXTRA_USER);
            if (user != null) {
                loadProfilePicture();
            }
        }
        findViewById(R.id.removeButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                unregisterUser();
            }
        });
        findViewById(R.id.authenticate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                authenticate();
            }
        });
        findViewById(R.id.register).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerMoreFaces();
            }
        });
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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.registered_user, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_about:
                showIntro();
                return true;
            case R.id.action_settings:
                showSettings();
                return true;
        }
        return false;
    }

    private void loadProfilePicture() {
        final ImageView profileImageView = (ImageView) findViewById(R.id.profileImage);
        profileImageView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                profileImageView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                final int width = profileImageView.getWidth();
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        Bitmap colourBitmap = VerID.shared.getUserProfilePicture(user.getUserId());
                        if (colourBitmap != null) {
                            byte[] grayscale = VerID.shared.getPlatformUtils().bitmapToGrayscale(colourBitmap, ExifInterface.ORIENTATION_NORMAL);
                            Bitmap grayscaleBitmap;
                            if (grayscale != null) {
                                grayscaleBitmap = VerID.shared.getPlatformUtils().grayscaleToBitmap(grayscale, colourBitmap.getWidth(), colourBitmap.getHeight());
                            } else {
                                grayscaleBitmap = colourBitmap;
                            }
                            if (grayscaleBitmap != null) {
                                int size = Math.min(grayscaleBitmap.getWidth(), grayscaleBitmap.getHeight());
                                int x = (int) ((double) grayscaleBitmap.getWidth() / 2.0 - (double) size / 2.0);
                                int y = (int) ((double) grayscaleBitmap.getHeight() / 2.0 - (double) size / 2.0);
                                grayscaleBitmap = Bitmap.createBitmap(grayscaleBitmap, x, y, size, size);
                                grayscaleBitmap = Bitmap.createScaledBitmap(grayscaleBitmap, width, width, true);
                                final RoundedBitmapDrawable roundedBitmapDrawable = RoundedBitmapDrawableFactory.create(getResources(), grayscaleBitmap);
                                roundedBitmapDrawable.setCornerRadius((float) width / 2f);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        profileImageView.setImageDrawable(roundedBitmapDrawable);
                                    }
                                });
                            }
                        }
                    }
                });
            }
        });
    }

    private void showIntro() {
        Intent intent = new Intent(this, IntroActivity.class);
        intent.putExtra(IntroActivity.EXTRA_SHOW_REGISTRATION, false);
        startActivity(intent);
    }

    private void showSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    private void unregisterUser() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_unregister)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.unregister, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            VerID.shared.deregisterUser(user.getUserId());
                            Intent intent = new Intent(RegisteredUserActivity.this, IntroActivity.class);
                            startActivity(intent);
                            finish();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                })
                .create()
                .show();
    }

    private void authenticate() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        VerID.LivenessDetection livenessDetection = VerID.LivenessDetection.NONE;
        if (preferences.getBoolean(getString(R.string.pref_key_enable_liveness_detection), true)) {
            livenessDetection = VerID.LivenessDetection.REGULAR;
        }
        // If your application requires an extra level of confidence on liveness detection set the livenessDetection parameter to VerID.LivenessDetection.STRICT.
        // Note that strict liveness detection requires the user to also be registered with the STRICT level of liveness detection. This negatively affects the user experience.
        VerIDAuthenticationSessionSettings settings = new VerIDAuthenticationSessionSettings(user.getUserId(), livenessDetection);
        // This setting dictates how many poses the user will be required to move her/his head to to ensure liveness
        // The higher the count the more confident we can be of a live face at the expense of usability
        // Note that 1 is added to the setting to include the initial mandatory straight pose
        settings.numberOfResultsToCollect = Integer.parseInt(preferences.getString(getString(R.string.pref_key_required_pose_count), "1")) + 1;
        if (settings.numberOfResultsToCollect == 1) {
            // Turn off liveness detection if only one pose is requested
            settings.livenessDetection = VerID.LivenessDetection.NONE;
        }
        // Setting showGuide to false will prevent the activity from displaying a guide on how to authenticate
        settings.showGuide = true;
        // Setting showResult to false will prevent the activity from displaying a result at the end of the session
        settings.showResult = true;
        settings.videoURL = Uri.parse(new File(getFilesDir(), "video.mp4").getPath());
        try {
            if (VerID.shared.canUserAuthenticateWithSettings(user.getUserId(), settings)) {
                Intent intent = new VerIDAuthenticationIntent(this, settings);
                startActivityForResult(intent, AUTHENTICATION_REQUEST_CODE);
            } else {
                new AlertDialog.Builder(this)
                        .setMessage(R.string.unable_to_authenticate_with_settings)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                registerMoreFaces(VerID.LivenessDetection.STRICT);
                            }
                        })
                        .create()
                        .show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void registerMoreFaces() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        VerID.LivenessDetection livenessDetection = VerID.LivenessDetection.NONE;
        if (preferences.getBoolean(getString(R.string.pref_key_enable_liveness_detection), true)) {
            livenessDetection = VerID.LivenessDetection.REGULAR;
        }
        // If your application requires an extra level of confidence on liveness detection set the livenessDetection parameter to VerID.LivenessDetection.STRICT.
        // This negatively affects the user experience at registration.
        registerMoreFaces(livenessDetection);
    }

    private void registerMoreFaces(VerID.LivenessDetection livenessDetection) {
        VerIDRegistrationSessionSettings settings = new VerIDRegistrationSessionSettings(VerIDUser.DEFAULT_USER_ID, livenessDetection);
        // Setting showGuide to false will prevent the activity from displaying a guide on how to register
        settings.showGuide = true;
        // Setting showResult to false will prevent the activity from displaying a result at the end of the session
        settings.showResult = true;
        settings.appendIfUserExists = true;
        Intent intent = new VerIDRegistrationIntent(this, settings);
        startActivityForResult(intent, REGISTRATION_REQUEST_CODE);
    }
}
