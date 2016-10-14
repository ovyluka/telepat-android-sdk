package io.telepat.sdk.models;

import io.telepat.sdk.Telepat;
import io.telepat.sdk.networking.responses.ApiError;
import io.telepat.sdk.networking.responses.GenericApiResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by ovidiuluca on 14/10/2016.
 */

public abstract class TelepatCallback implements Callback<GenericApiResponse> {

    public abstract void success();

    public abstract void failure();

    @Override
    public void onResponse(Call<GenericApiResponse> call, Response<GenericApiResponse> response) {
        if (response.isSuccessful()) {
            success();
        } else {
            ApiError apiError = ApiError.parseError(response);
            if (apiError.status() == 500) {
                final Call<GenericApiResponse> newCall = call.clone();
                Telepat.getInstance().refreshToken(new TelepatRequestListener() {
                    @Override
                    public void onSuccess() {
                        newCall.enqueue(TelepatCallback.this);
                        Telepat.getInstance().isDebugMode = false;
                    }

                    @Override
                    public void onError(Throwable error) {
                        failure();
                    }
                });
            }
        }
    }

    @Override
    public void onFailure(Call<GenericApiResponse> call, Throwable t) {
        failure();
    }
}
