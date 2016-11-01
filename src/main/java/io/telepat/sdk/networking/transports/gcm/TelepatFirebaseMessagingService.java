package io.telepat.sdk.networking.transports.gcm;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import io.telepat.sdk.utilities.TelepatLogger;

/**
 * Created by ovidiuluca on 01/11/2016.
 */

public class TelepatFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        TelepatLogger.log("Received message is: " + remoteMessage.getData());

        ComponentName comp = new ComponentName(getPackageName(), GcmIntentService.class.getName());
        // Start the service, keeping the device awake while it is launching.

//        startWakefulService(context, (intent.setComponent(comp)));
//        setResultCode(Activity.RESULT_OK);

        Intent intent = new Intent();
        intent.setComponent(comp);
        startService(intent);
    }
}
