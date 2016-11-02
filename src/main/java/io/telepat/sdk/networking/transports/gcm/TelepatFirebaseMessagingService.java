package io.telepat.sdk.networking.transports.gcm;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.telepat.sdk.Telepat;
import io.telepat.sdk.models.Channel;
import io.telepat.sdk.models.TransportNotification;
import io.telepat.sdk.utilities.TelepatLogger;

/**
 * Created by ovidiuluca on 01/11/2016.
 * 
 */

public class TelepatFirebaseMessagingService extends FirebaseMessagingService {
    private final Gson jsonParser = new Gson();
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        TelepatLogger.log("Received message is: " + remoteMessage.getData().get("data"));
        if (remoteMessage.getData() != null && remoteMessage.getData().size() > 0) {
            JsonObject jsonObject = jsonParser.fromJson(remoteMessage.getData().get("data"), JsonObject.class);
            try {
                JsonArray newObjects = (JsonArray) jsonObject.get("new");
                JsonArray updatedObjects = (JsonArray) jsonObject.get("updated");
                JsonArray deletedObjects = (JsonArray) jsonObject.get("deleted");
                if (newObjects != null)
                    prepareChannelNotification(newObjects, Channel.NotificationType.ObjectAdded);
                if (updatedObjects != null)
                    prepareChannelNotification(updatedObjects, Channel.NotificationType.ObjectUpdated);
                if (deletedObjects != null)
                    prepareChannelNotification(deletedObjects, Channel.NotificationType.ObjectDeleted);
            } catch (NullPointerException ex) {
                ex.printStackTrace();
            }
        }

    }

    /**
     * Builds TransportNotification objects and relays it to the relevant channels
     *
     * @param objects          A JsonArray of notifications
     * @param notificationType The type of notifications (added/updated/deleted)
     */
    private void prepareChannelNotification(JsonArray objects, Channel.NotificationType notificationType) {
        for (JsonElement notificationElement : objects) {
            if (notificationElement.isJsonObject()) {
                JsonObject notificationObject = (JsonObject) notificationElement;
                TransportNotification notification = new TransportNotification(notificationObject, notificationType);
                if (notificationObject.has("subscription")) {
                    String channelIdentifier = notificationObject.get("subscription").getAsString();
                    notifyChannel(channelIdentifier, notification);
                } else {
                    TelepatLogger.log("V2 notification format detected");
                    if (notificationObject.has("subscriptions")) {
                        JsonArray affectedChannels = notificationObject.getAsJsonArray("subscriptions");
                        for (JsonElement element : affectedChannels) {
                            if (element.isJsonPrimitive()) {
                                String channelIdentifier = element.getAsString();
                                notifyChannel(channelIdentifier, notification);
                            }
                        }
                    }
                }
            }
        }
    }

    private void notifyChannel(String channelIdentifier, TransportNotification notification) {
        if (channelIdentifier.endsWith(":context")) {
            Telepat.getInstance().fireContextUpdate(notification);
        } else {
            Channel channel = Telepat.getInstance().getSubscribedChannel(channelIdentifier);
            if (channel != null) channel.processNotification(notification);
            else {
                TelepatLogger.error("No local channel instance available");
                channel = new Channel(channelIdentifier);
                channel.processNotification(notification);
            }
        }
    }
}
