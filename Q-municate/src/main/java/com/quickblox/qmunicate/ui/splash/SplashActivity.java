package com.quickblox.qmunicate.ui.splash;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.facebook.Session;
import com.facebook.SessionState;
import com.quickblox.module.auth.model.QBProvider;
import com.quickblox.module.users.model.QBUser;
import com.quickblox.qmunicate.App;
import com.quickblox.qmunicate.R;
import com.quickblox.qmunicate.caching.DatabaseManager;
import com.quickblox.qmunicate.core.command.Command;
import com.quickblox.qmunicate.model.LoginType;
import com.quickblox.qmunicate.qb.commands.QBLoginCommand;
import com.quickblox.qmunicate.qb.commands.QBLoginRestWithSocialCommand;
import com.quickblox.qmunicate.service.QBServiceConsts;
import com.quickblox.qmunicate.ui.base.BaseActivity;
import com.quickblox.qmunicate.ui.landing.LandingActivity;
import com.quickblox.qmunicate.ui.main.MainActivity;
import com.quickblox.qmunicate.utils.FacebookHelper;
import com.quickblox.qmunicate.utils.PrefsHelper;

public class SplashActivity extends BaseActivity {

    private static final String TAG = SplashActivity.class.getSimpleName();
    private FacebookHelper facebookHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        canPerformLogout.set(false);
        addAction(QBServiceConsts.LOGIN_SUCCESS_ACTION, new LoginSuccessAction());

        facebookHelper = new FacebookHelper(this, savedInstanceState, new FacebookSessionStatusCallback());

        if (facebookHelper.isSessionOpened() && getLoginType() == LoginType.FACEBOOK) {
            return;
        }

        String userEmail = App.getInstance().getPrefsHelper().getPref(PrefsHelper.PREF_USER_EMAIL);
        String userPassword = App.getInstance().getPrefsHelper().getPref(PrefsHelper.PREF_USER_PASSWORD);

        boolean isEmailEntered = !TextUtils.isEmpty(userEmail);
        boolean isPasswordEntered = !TextUtils.isEmpty(userPassword);
        boolean isRememberMe = App.getInstance().getPrefsHelper().getPref(PrefsHelper.PREF_REMEMBER_ME,
                false);

        if (isRememberMe && isEmailEntered && isPasswordEntered) {
            login(userEmail, userPassword);
        } else {
            LandingActivity.start(SplashActivity.this);
            finish();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        facebookHelper.onActivityStart();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        facebookHelper.onSaveInstanceState(outState);
    }

    @Override
    public void onStop() {
        facebookHelper.onActivityStop();
        super.onStop();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        facebookHelper.onActivityResult(requestCode, resultCode, data);
    }

    private void login(String userEmail, String userPassword) {
        QBUser user = new QBUser(null, userPassword, userEmail);
        QBLoginCommand.start(this, user);
    }

    private LoginType getLoginType() {
        int defValue = LoginType.EMAIL.ordinal();
        int value = App.getInstance().getPrefsHelper().getPref(PrefsHelper.PREF_LOGIN_TYPE, defValue);
        return LoginType.values()[value];
    }

    private class FacebookSessionStatusCallback implements Session.StatusCallback {

        @Override
        public void call(Session session, SessionState state, Exception exception) {
            if (session.isOpened() && getLoginType() == LoginType.FACEBOOK) {
                QBLoginRestWithSocialCommand.start(SplashActivity.this, QBProvider.FACEBOOK,
                        session.getAccessToken(), null);
            }
        }
    }

    private class LoginSuccessAction implements Command {

        @Override
        public void execute(Bundle bundle) {
            QBUser user = (QBUser) bundle.getSerializable(QBServiceConsts.EXTRA_USER);
            App.getInstance().setUser(user);
            App.getInstance().getPrefsHelper().savePref(PrefsHelper.PREF_IMPORT_INITIALIZED, true);
            DatabaseManager.clearAllCache(SplashActivity.this);
            MainActivity.start(SplashActivity.this);
            finish();
        }
    }
}