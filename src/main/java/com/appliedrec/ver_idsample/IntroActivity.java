package com.appliedrec.ver_idsample;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.appliedrec.ver_id.VerID;
import com.appliedrec.ver_id.VerIDRegistrationIntent;
import com.appliedrec.ver_id.model.VerIDUser;
import com.appliedrec.ver_id.session.VerIDRegistrationSession;
import com.appliedrec.ver_id.session.VerIDRegistrationSessionSettings;
import com.appliedrec.ver_id.session.VerIDSessionResult;
import com.appliedrec.ver_id.ui.PageViewActivity;
import com.appliedrec.ver_id.ui.VerIDActivity;

public class IntroActivity extends PageViewActivity {

    private static final int REQUEST_CODE_REGISTER = 0;
    public static final String EXTRA_SHOW_REGISTRATION = "showRegistration";
    boolean showRegistration = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showRegistration = getIntent().getBooleanExtra(EXTRA_SHOW_REGISTRATION, true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.intro, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        int title;
        if (getViewPager().getCurrentItem() < getPageCount() - 1) {
            title = R.string.next;
        } else if (showRegistration) {
            title = R.string.register;
        } else {
            title = R.string.done;
        }
        menu.findItem(R.id.action_next).setTitle(title);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_next) {
            if (getViewPager().getCurrentItem() < getPageCount() - 1) {
                getViewPager().setCurrentItem(getViewPager().getCurrentItem() + 1, true);
            } else if (showRegistration) {
                register();
            } else {
                finish();
            }
            return true;
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_REGISTER && resultCode == RESULT_OK && data != null) {
            VerIDSessionResult result = data.getParcelableExtra(VerIDActivity.EXTRA_SESSION_RESULT);
            if (result.getIdentifiedUsers().length > 0) {
                VerIDUser user = result.getIdentifiedUsers()[0];
                Intent intent = new Intent(this, RegisteredUserActivity.class);
                intent.putExtra(RegisteredUserActivity.EXTRA_USER, user);
                startActivity(intent);
                finish();
            }
        }
    }

    private void register() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        VerID.LivenessDetection livenessDetection = VerID.LivenessDetection.NONE;
        if (preferences.getBoolean(getString(R.string.pref_key_enable_liveness_detection), true)) {
            livenessDetection = VerID.LivenessDetection.REGULAR;
        }
        VerIDRegistrationSessionSettings settings = new VerIDRegistrationSessionSettings(VerIDUser.DEFAULT_USER_ID, livenessDetection);
        settings.showGuide = true;
        settings.showResult = true;
        VerIDRegistrationIntent intent = new VerIDRegistrationIntent(this, settings);
        startActivityForResult(intent, REQUEST_CODE_REGISTER);
    }

    @Override
    protected int getPageCount() {
        return 3;
    }

    @Override
    public void onPageSelected(int position) {
        super.onPageSelected(position);
        invalidateOptionsMenu();
    }

    @Override
    protected Fragment createFragmentForPage(int page) {
        return IntroFragment.newInstance(page);
    }

    public static class IntroFragment extends Fragment {

        int[] imageResourceIds = new int[]{
                R.mipmap.guide_head_straight,
                R.mipmap.multiple_heads,
                R.mipmap.authentication
        };
        int[] titleResourceIds = new int[]{
                R.string.verid_person_sdk,
                R.string.one_registration,
                R.string.two_authentication
        };
        int[] textResourceIds = new int[]{
                R.string.verid_person_sdk_text,
                R.string.one_registration_text,
                R.string.two_authentication_text
        };

        public static IntroFragment newInstance(int index) {
            Bundle args = new Bundle();
            args.putInt("index", index);
            IntroFragment fragment = new IntroFragment();
            fragment.setArguments(args);
            return fragment;
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.intro_page_fragment, container, false);
            Bundle args = getArguments();
            int index = args.getInt("index", 0);
            ((ImageView)view.findViewById(R.id.imageView)).setImageResource(imageResourceIds[index]);
            ((TextView)view.findViewById(R.id.title)).setText(titleResourceIds[index]);
            ((TextView)view.findViewById(R.id.text)).setText(textResourceIds[index]);
            return view;
        }
    }
}
