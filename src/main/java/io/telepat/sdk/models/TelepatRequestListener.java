package io.telepat.sdk.models;

import io.telepat.sdk.networking.responses.ApiError;

/**
 * Created by andrei on 11/8/15.
 */
public interface TelepatRequestListener {
    void onSuccess();
    void onError(ApiError error);
}
