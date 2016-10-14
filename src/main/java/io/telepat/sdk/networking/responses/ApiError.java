package io.telepat.sdk.networking.responses;

import java.io.IOException;
import java.lang.annotation.Annotation;

import io.telepat.sdk.Telepat;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Response;

/**
 * Created by ovidiuluca on 14/10/2016.
 */

public class ApiError {

    private int status;
    private String message;

    public int status() {
        return status;
    }

    public String message() {
        return message;
    }


    public static ApiError parseError(Response<?> response) {
        Converter<ResponseBody, ApiError> converter = Telepat.getInstance().retrofit.responseBodyConverter(ApiError.class, new Annotation[0]);

        ApiError error;

        try {
            error = converter.convert(response.errorBody());
        } catch (IOException e) {
            return new ApiError();
        }

        return error;
    }
}
