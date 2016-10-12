package io.telepat.sdk.models;

import java.util.HashMap;

import io.telepat.sdk.data.TelepatInternalDB;
import io.telepat.sdk.networking.OctopusRequestInterceptor;
import io.telepat.sdk.networking.responses.GenericApiResponse;
import io.telepat.sdk.utilities.TelepatConstants;
import io.telepat.sdk.utilities.TelepatLogger;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by Andrei on 11.02.2016.
 * Retrofit callback implementation for user login requests.
 */
public class UserLoginCallback implements Callback<GenericApiResponse> {

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
        if(loginData.containsKey("token")) {
            internalDB.setOperationsData(TelepatConstants.JWT_KEY, loginData.get("token"));
//            internalDB.setOperationsData(TelepatConstants.JWT_TIMESTAMP_KEY, System.currentTimeMillis());
            interceptor.setAuthorizationToken((String) loginData.get("token"));
        }

        if(loginData.containsKey("user")) {
            internalDB.setOperationsData(TelepatConstants.CURRENT_USER_DATA, loginData.get("user"));
        }
    }

    @Override
    public void onResponse(Call<GenericApiResponse> call, Response<GenericApiResponse> response) {
        persistLoginData(response.body().content);
        TelepatLogger.log("User logged in");
        if(loginListener != null)
            loginListener.onSuccess();
    }

    @Override
    public void onFailure(Call<GenericApiResponse> call, Throwable t) {
        // TODO: 11/10/2016 parse error
//        if(t!=null && error.getResponse()!=null && error.getResponse().getStatus()==409) {
//            TelepatLogger.log("A facebook user with that fid already exists.");
//        } else {
//            TelepatLogger.log("User login failed.");
//        }
//        if(loginListener != null)
//            loginListener.onError(error);
    }
}


