package com.quickblox.sample.chat.ui.activities;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.quickblox.core.QBCallback;
import com.quickblox.core.QBCallbackImpl;
import com.quickblox.core.result.Result;
import com.quickblox.module.chat.QBChatService;
import com.quickblox.module.chat.listeners.SessionListener;
import com.quickblox.module.chat.smack.SmackAndroid;
import com.quickblox.module.messages.QBMessages;
import com.quickblox.module.messages.model.QBEnvironment;
import com.quickblox.module.users.QBUsers;
import com.quickblox.module.users.model.QBUser;
import com.quickblox.sample.chat.App;
import com.quickblox.sample.chat.R;

public class LoginActivity extends Activity implements QBCallback, View.OnClickListener {

    private static final String TAG = LoginActivity.class.getSimpleName();
    private static final String DEFAULT_LOGIN = "pronane";
    private static final String DEFAULT_PASSWORD = "ced";
    private Button loginButton;
    private EditText loginEdit;
    private EditText passwordEdit;
    private ProgressDialog progressDialog;
    private String login;
    private String password;
    private QBUser user;
    private SmackAndroid smackAndroid;
    private Handler handler;
    

    public static final String EXTRA_MESSAGE = "message";
    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    /**
     * Substitute you own sender ID here. This is the project number you got
     * from the API Console, as described in "Getting Started."
     */
    String SENDER_ID = "362335065981";

    /**
     * Tag used on log messages.
     */
    
    private GoogleCloudMessaging gcm;
    private AtomicInteger msgId = new AtomicInteger();
    private Context context;

    private String regId;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        loginEdit = (EditText) findViewById(R.id.loginEdit);
        passwordEdit = (EditText) findViewById(R.id.passwordEdit);
        loginEdit.setText(DEFAULT_LOGIN);
        passwordEdit.setText(DEFAULT_PASSWORD);
        loginButton = (Button) findViewById(R.id.loginButton);
        loginButton.setOnClickListener(this);
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading");

        smackAndroid = SmackAndroid.init(this);
        handler = new Handler();
    }

    @Override
    protected void onDestroy() {
        smackAndroid.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onClick(View view) {
        login = loginEdit.getText().toString();
        password = passwordEdit.getText().toString();

        user = new QBUser(login, password);

        progressDialog.show();
        QBUsers.signIn(user, this);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent();
        setResult(RESULT_CANCELED, intent);
        finish();
    }

    @Override
    public void onComplete(Result result) {
        if (result.isSuccess()) {
            ((App)getApplication()).setQbUser(user);
            QBChatService.getInstance().loginWithUser(user, new SessionListener() {
                @Override
                public void onLoginSuccess() {
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                    }
                    Log.i(TAG, "success when login");

                    Intent intent = new Intent();
                    setResult(RESULT_OK, intent);
                    finish();
                }

                @Override
                public void onLoginError() {
                    Log.i(TAG, "error when login");
                }

                @Override
                public void onDisconnect() {
                    Log.i(TAG, "disconnect when login");
                }

                @Override
                public void onDisconnectOnError(Exception exc) {
                    Log.i(TAG, "disconnect error when login");
                }
            });
            // request registration ID
            /*
            The checkDevice() method verifies that the device supports GCM  
            and throws an exception if it does not (for instance, if it is 
            an emulator that does not contain the Google APIs). Similarly, 
            the checkManifest() method verifies that the application manifest
            contains meets all the requirements (this method is only necessary
            when you are developing the application; once the application is
            ready to be published, you can remove it).

            Once the sanity checks are done, the device calls 
            GCMRegsistrar.register() to register the device, passing the 
            SENDER_ID you got when you signed up for GCM. But since the 
            GCMRegistrar singleton keeps track of the registration ID upon the 
            arrival of registration intents, you can call 
            GCMRegistrar.getRegistrationId() first to check if the device is 
            already registered.
            */
            //
            //GoogleCloudMessaging.getInstance(context)
            context = getApplicationContext();

            // Check device for Play Services APK. If check succeeds, proceed with
            //  GCM registration.
            Log.d(TAG, "about to check google play services returned true!!!!!");
            if (checkPlayServices()) {
            	Log.d(TAG, "check google play services returned true");
                gcm = GoogleCloudMessaging.getInstance(this);
                regId = getRegistrationId(context);
                if (regId.isEmpty()) {
                	Log.d(TAG, "reg is empty about to register in background");
                    registerInBackground();
                }
                System.out.println("regId is " + regId);
                subscribeToPushNotifications(regId);
            }
       
        } else {
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setMessage("Error(s) occurred. Look into DDMS log for details, " +
                    "please. Errors: " + result.getErrors()).create().show();
        }
     	Log.d(TAG, "we are done");
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
            	Log.d(TAG, "this device is supported true");
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }
    /**
     * Gets the current registration ID for application on GCM service, if there is one.
     * <p>
     * If result is empty, the app needs to register.
     *
     * @return registration ID, or empty string if there is no existing
     *         registration ID.
     */
    private String getRegistrationId(Context context) {
        final SharedPreferences prefs = getGcmPreferences(context);
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            Log.i(TAG, "Registration not found.");
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            Log.i(TAG, "App version changed.");
            return "";
        }
        return registrationId;
    }
  
    //
	 // Subscribe to Push Notifications
	 public void subscribeToPushNotifications(String registrationID) {
	     String deviceId = ((TelephonyManager) getBaseContext().getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
	     Log.i(TAG, "In do in background about to subscribeToNotifications task" + deviceId);
	     Log.i(TAG, "In do in background about to subscribeToNotifications registrationID" + registrationID);
	     QBMessages.subscribeToPushNotificationsTask(registrationID, deviceId, QBEnvironment.DEVELOPMENT, new QBCallbackImpl() {
	         @Override
	         public void onComplete(Result result) {
	             if (result.isSuccess()) {
	                 Log.d("DEBUG", "subscribed to QuickBLOX!!!!!!!!!!!!!!!!!!!");
	             }
	         }
	     });
	 }

	 /**
	     * Registers the application with GCM servers asynchronously.
	     * <p>
	     * Stores the registration ID and the app versionCode in the application's
	     * shared preferences.
	     */
	    private void registerInBackground() {
	        new AsyncTask<Void, Void, String>() {
	            @Override
	            protected String doInBackground(Void... params) {
	                String msg = "";
	                try {
	                    if (gcm == null) {
	                    	Log.d(TAG, "GCM IS NULL so we are ABOUT TO REGISTER IT .");
	                        gcm = GoogleCloudMessaging.getInstance(context);
	                    }
	                    
	                    Log.d(TAG, "Calling gcm.register with sender_Id " + SENDER_ID);
	                    regId = gcm.register(SENDER_ID);
	                    msg = "Device registered, registration ID=" + regId;
	                    
	                    Log.d(TAG, "GCM IS NULL ABOUT TO REGISTER IT ." + msg);
	                    

	                    // You should send the registration ID to your server over HTTP, so it
	                    // can use GCM/HTTP or CCS to send messages to your app.
	                    sendRegistrationIdToBackend();

	                    // For this demo: we don't need to send it because the device will send
	                    // upstream messages to a server that echo back the message using the
	                    // 'from' address in the message.

	                    // Persist the regID - no need to register again.
	                    storeRegistrationId(context, regId);
	                } catch (IOException ex) {
	                    msg = "Error :" + ex.getMessage();
	                    // If there is an error, don't just keep trying to register.
	                    // Require the user to click a button again, or perform
	                    // exponential back-off.
	                }
	                return msg;
	            }
	        }.execute(null, null, null);
	    }

	    public void clickAButton(View view) {
	        // Do something that takes a while
	        Runnable runnable = new Runnable() {
	            @Override
	            public void run() {
	                handler.post(new Runnable() { // This thread runs in the UI
	                    @Override
	                    public void run() {
	                       // progress.setProgress("anything"); // Update the UI
	                    }
	                });
	            }
	        };
	        new Thread(runnable).start();
	    }
	    
	    /**
	     * Stores the registration ID and the app versionCode in the application's
	     * {@code SharedPreferences}.
	     *
	     * @param context application's context.
	     * @param regId registration ID
	     */
	    private void storeRegistrationId(Context context, String regId) {
	        final SharedPreferences prefs = getGcmPreferences(context);
	        int appVersion = getAppVersion(context);
	        Log.i(TAG, "Saving regId on app version " + appVersion);
	        SharedPreferences.Editor editor = prefs.edit();
	        editor.putString(PROPERTY_REG_ID, regId);
	        editor.putInt(PROPERTY_APP_VERSION, appVersion);
	        editor.commit();
	    }
	    
	 /**
	  * @return Application's version code from the {@code PackageManager}.
	  */
	 private static int getAppVersion(Context context) {
	     try {
	    	  Log.i(TAG, "Saving regId on app version context is " + context);
	         PackageInfo packageInfo = context.getPackageManager()
	                 .getPackageInfo(context.getPackageName(), 0);
	         return packageInfo.versionCode;
	     } catch (NameNotFoundException e) {
	         // should never happen
	         throw new RuntimeException("Could not get package name: " + e);
	     }
	 }
	
	 /**
	  * @return Application's {@code SharedPreferences}.
	  */
	 private SharedPreferences getGcmPreferences(Context context) {
	     // This sample app persists the registration ID in shared preferences, but
	     // how you store the regID in your app is up to you.
	     return getSharedPreferences(LoginActivity.class.getSimpleName(),
	             Context.MODE_PRIVATE);
	 }
	 /**
	  * Sends the registration ID to your server over HTTP, so it can use GCM/HTTP or CCS to send
	  * messages to your app. Not needed for this demo since the device sends upstream messages
	  * to a server that echoes back the message using the 'from' address in the message.
	  */
	 private void sendRegistrationIdToBackend() {
	   // Your implementation here.
	 }
    @Override
    public void onComplete(Result result, Object context) {
    }
}
