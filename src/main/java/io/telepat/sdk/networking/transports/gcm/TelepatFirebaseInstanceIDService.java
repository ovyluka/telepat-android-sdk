package io.telepat.sdk.networking.transports.gcm;

import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import io.telepat.sdk.Telepat;
import io.telepat.sdk.utilities.TelepatConstants;
import io.telepat.sdk.utilities.TelepatLogger;
import io.telepat.sdk.utilities.TelepatUtilities;

import static io.telepat.sdk.networking.transports.gcm.GcmRegistrar.PROPERTY_REG_ID;

/**
 * Created by ovidiuluca on 01/11/2016.
 */

public class TelepatFirebaseInstanceIDService extends FirebaseInstanceIdService {

    private static final String PROPERTY_APP_VERSION = "appVersion";

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    // [START refresh_token]
    @Override
    public void onTokenRefresh() {
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        TelepatLogger.log("FCM Registration token is: " + refreshedToken);

        storeRegistrationId(refreshedToken);
        Telepat.getInstance().registerDevice(refreshedToken, true);
    }

    /**
     * Stores the registration ID and app versionCode in the application's
     *
     * @param regId registration ID
     */
    private void storeRegistrationId(String regId) {
        int appVersion = TelepatUtilities.getAppVersion(this);
        Log.i(TelepatConstants.TAG, "Saving regId on app version " + appVersion);
        Telepat.getInstance().getDBInstance().setOperationsData(PROPERTY_REG_ID, regId);
        Telepat.getInstance().getDBInstance().setOperationsData(PROPERTY_APP_VERSION, appVersion);
    }

}
