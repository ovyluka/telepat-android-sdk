package io.telepat.sdk.models;

/**
 * Created by andrei on 11/8/15.
 */
public interface TelepatRequestListener {
    void onSuccess();
    void onError(String error);
}
