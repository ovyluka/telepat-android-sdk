package io.telepat.sdk.networking.transports.gcm;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.json.JSONException;

import io.telepat.sdk.Telepat;
import io.telepat.sdk.models.Channel;
import io.telepat.sdk.models.TransportNotification;
import io.telepat.sdk.utilities.TelepatLogger;
import io.telepat.sdk.utilities.TelepatUtilities;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by ovidiuluca on 01/11/2016.
 * 
 */

public class TelepatFirebaseMessagingService extends FirebaseMessagingService {
    private final Gson jsonParser = new Gson();
    @Override
    public void onMessageReceived(final RemoteMessage remoteMessage) {

        if (remoteMessage.getData().get("data") != null && remoteMessage.getData().size() > 0) {
            TelepatLogger.log("Received message is: " + remoteMessage.getData().get("data"));
            parseMessage(remoteMessage.getData().get("data"));
        } else if (remoteMessage.getData().get("url") != null) {
            TelepatLogger.log("Received message is: " + remoteMessage.getData().get("url"));

            Telepat.getInstance().downloadFileFromUrl(remoteMessage.getData().get("url"), new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        try {
                            String message = TelepatUtilities.convertStreamToString(response.body().byteStream());
                            JsonObject jsonObject = jsonParser.fromJson(message, JsonObject.class);
                            JsonObject data = (JsonObject) jsonObject.get("data");
                            parseMessage(data.toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    } else {
                        TelepatLogger.log("File download failed.");
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    t.printStackTrace();
                }

            });
        }

    }

    private void parseMessage(String message) {
        JsonObject jsonObject = jsonParser.fromJson(message, JsonObject.class);
        JsonArray newObjects = (JsonArray) jsonObject.get("new");
        JsonArray updatedObjects = (JsonArray) jsonObject.get("updated");
        JsonArray deletedObjects = (JsonArray) jsonObject.get("deleted");
        if (newObjects != null)
            prepareChannelNotification(newObjects, Channel.NotificationType.ObjectAdded);
        if (updatedObjects != null)
            prepareChannelNotification(updatedObjects, Channel.NotificationType.ObjectUpdated);
        if (deletedObjects != null)
            prepareChannelNotification(deletedObjects, Channel.NotificationType.ObjectDeleted);
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
