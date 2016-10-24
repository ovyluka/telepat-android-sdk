package io.telepat.sdk.networking;

import io.telepat.sdk.networking.responses.ApiError;
import io.telepat.sdk.networking.responses.GenericApiResponse;
import io.telepat.sdk.utilities.TelepatLogger;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by Andrei Marinescu on 22.06.2015.
 * Common Callback implementation for CRUD operations with OctopusApi
 */
public class CrudOperationsCallback implements Callback<GenericApiResponse> {
    private String opType;

    public CrudOperationsCallback(String opType) {
        this.opType = opType;
    }

    @Override
    public void onResponse(Call<GenericApiResponse> call, Response<GenericApiResponse> response) {
        if (response.isSuccessful()) {
//            if (response.body().content.get("status").equals("202"))
                TelepatLogger.log(opType + " successful: " + response.body().content.get("message"));
        } else {
            TelepatLogger.error(opType + " failed: " + ApiError.parseError(response).message());
        }
    }

    @Override
    public void onFailure(Call<GenericApiResponse> call, Throwable t) {
        TelepatLogger.log(opType + " failed: " + t.getMessage());
    }
}
