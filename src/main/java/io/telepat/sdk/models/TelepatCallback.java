package io.telepat.sdk.models;

import io.telepat.sdk.Telepat;
import io.telepat.sdk.networking.responses.ApiError;
import io.telepat.sdk.networking.responses.GenericApiResponse;
import io.telepat.sdk.utilities.TelepatConstants;
import io.telepat.sdk.utilities.TelepatLogger;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by ovidiuluca on 14/10/2016.
 */

public abstract class TelepatCallback implements Callback<GenericApiResponse> {

    /**
     * isRefreshTokenCalled is used to make one single request for refreshToken
     * */
    private boolean isRefreshTokenCalled = false;

    public abstract void success(GenericApiResponse apiResponse);

    public abstract void failure();

    @Override
    public void onResponse(Call<GenericApiResponse> call, Response<GenericApiResponse> response) {
        if (response.isSuccessful()) {
            success(response.body());
        } else {
            ApiError apiError = ApiError.parseError(response);
            if (apiError.code().equals(TelepatConstants.CODE_TOKEN_EXPIRED)) {
                final Call<GenericApiResponse> newCall = call.clone();
                if(!isRefreshTokenCalled) {
                    isRefreshTokenCalled = true;
                    Telepat.getInstance().refreshToken(new TelepatRequestListener() {
                        @Override
                        public void onSuccess() {
                            newCall.enqueue(TelepatCallback.this);
                        }

                        @Override
                        public void onError(String error) {
                            failure();
                        }
                    });
                }
            }else{
                failure();
            }
        }
    }

    @Override
    public void onFailure(Call<GenericApiResponse> call, Throwable t) {
        TelepatLogger.error(t.getMessage());
    }
}
