package io.telepat.sdk.networking;

import io.telepat.sdk.models.TelepatCallback;
import io.telepat.sdk.networking.responses.ApiError;
import io.telepat.sdk.networking.responses.GenericApiResponse;
import io.telepat.sdk.utilities.TelepatLogger;

/**
 * Created by Andrei Marinescu on 22.06.2015.
 * Common Callback implementation for CRUD operations with OctopusApi
 */
public class CrudOperationsCallback extends TelepatCallback {
    private String opType;

    public CrudOperationsCallback(String opType) {
        this.opType = opType;
    }

    @Override
    public void success(GenericApiResponse apiResponse) {
        if (apiResponse.status == 202)
            TelepatLogger.log(opType + " successful: " + apiResponse.content);
    }

    @Override
    public void failure(ApiError error) {
        TelepatLogger.error(opType + " failed: " + error.message());
    }

}
