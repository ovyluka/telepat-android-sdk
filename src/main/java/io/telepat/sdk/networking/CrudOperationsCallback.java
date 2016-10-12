package io.telepat.sdk.networking;

import java.util.HashMap;

import io.telepat.sdk.utilities.TelepatLogger;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by Andrei Marinescu on 22.06.2015.
 * Common Callback implementation for CRUD operations with OctopusApi
 */
public class CrudOperationsCallback implements Callback<HashMap<String, String>> {
    private String opType;

    public CrudOperationsCallback(String opType) {
        this.opType = opType;
    }

    @Override
    public void onResponse(Call<HashMap<String, String>> call, Response<HashMap<String, String>> response) {
        if (response.body().get("status").equals("202"))
            TelepatLogger.log(opType + " successful: " + response.body().get("message"));
        else
            TelepatLogger.error(opType + " failed: " + response.body().get("message"));
    }

    @Override
    public void onFailure(Call<HashMap<String, String>> call, Throwable t) {
        TelepatLogger.log(opType + " failed: " + t.getMessage());
    }
}
