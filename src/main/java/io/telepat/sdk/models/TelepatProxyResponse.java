package io.telepat.sdk.models;


import io.telepat.sdk.networking.responses.ApiError;
import okhttp3.Headers;

/**
 * Created by andrei on 3/21/16.
 *
 */
public interface TelepatProxyResponse {
    void onRequestFinished(String responseBody, Headers responseHeaders);
    void onTelepatError(ApiError error);
}
