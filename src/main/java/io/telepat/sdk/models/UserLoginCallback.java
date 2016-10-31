package io.telepat.sdk.models;

import java.util.HashMap;

import io.telepat.sdk.data.TelepatInternalDB;
import io.telepat.sdk.networking.OctopusRequestInterceptor;
import io.telepat.sdk.networking.responses.ApiError;
import io.telepat.sdk.networking.responses.GenericApiResponse;
import io.telepat.sdk.utilities.TelepatConstants;
import io.telepat.sdk.utilities.TelepatLogger;

/**
 * Created by Andrei on 11.02.2016.
 * Retrofit callback implementation for user login requests.
 */
public class UserLoginCallback extends TelepatCallback {

    private OctopusRequestInterceptor interceptor;
    private TelepatRequestListener loginListener;
    private TelepatInternalDB internalDB;

    public UserLoginCallback(OctopusRequestInterceptor interceptor,
                             TelepatInternalDB internalDB,
                             TelepatRequestListener loginListener) {
        this.interceptor = interceptor;
        this.internalDB = internalDB;
        this.loginListener = loginListener;
    }


    private void persistLoginData(HashMap<String, Object> loginData) {
//        String expiredToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1c2VybmFtZSI6Im92aWRpdS5sdWNhQGFwcHNjZW5kLmNvbSIsImlkIjoiODM3ZmM5NWItOGRmNi00OWIzLWFjOWQtYWRiODgxNWI3MzE4IiwiaWF0IjoxNDc2MTExNTkxLCJleHAiOjE0NzYxMTUxOTF9.5uHdM3qXzYk-dD87rVoqvV92rweomRQce1AdnAY0Dr8";
                internalDB.setOperationsData(TelepatConstants.JWT_KEY, loginData.get("token"));
                interceptor.setAuthorizationToken((String) loginData.get("token"));

        TelepatLogger.log("token::: " + interceptor.getAuthorizationToken());

        if(loginData.containsKey("user")) {
            internalDB.setOperationsData(TelepatConstants.CURRENT_USER_DATA, loginData.get("user"));
        }
    }

    @Override
    public void success(GenericApiResponse apiResponse) {
        persistLoginData(apiResponse.getContent());
        TelepatLogger.log("User logged in");
        if (loginListener != null)
            loginListener.onSuccess();
    }

    @Override
    public void failure(ApiError error) {
        if (error.status() == 409) {
            TelepatLogger.log("A facebook user with that fid already exists.");
        } else {
            TelepatLogger.log("User login failed.");
        }

        if (loginListener != null)
            loginListener.onError(error);
    }
}


