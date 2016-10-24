package io.telepat.sdk.networking.responses;


/**
 * Created by Andrei Marinescu on 11/29/15.
 *
 */
public interface TelepatCountCallback {
    void onSuccess(int number, Double aggregationResult);
    void onFailure(String error);
}
