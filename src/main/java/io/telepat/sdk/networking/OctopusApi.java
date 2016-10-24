package io.telepat.sdk.networking;

import com.google.gson.JsonElement;

import java.util.HashMap;
import java.util.Map;

import io.telepat.sdk.models.TelepatProxyRequest;
import io.telepat.sdk.networking.responses.ContextsApiResponse;
import io.telepat.sdk.networking.responses.GenericApiResponse;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * Created by Andrei Marinescu on 02.06.2015.
 * Octopus API Retrofit Interface
 */
@SuppressWarnings("JavaDoc")
public interface OctopusApi {
    /**
     * Method for sending a device registration request
     * @param body
     * @return call
     */
    @POST("/device/register")
    Call<GenericApiResponse> registerDevice(@Body Map<String, Object> body);

    /**
     * Method for retrieving all active contexts
     * @return call
     */
    @POST("/context/all")
    Call<ContextsApiResponse> updateContexts();

    @Deprecated
    @GET("/context/all")
    Call<ContextsApiResponse> updateContextsCompat();

    /**
     * Method for sending a register request using the Facebook auth provider
     * @param body
     */
    @POST("/user/register-facebook")
    Call<GenericApiResponse> registerUserFacebook(@Body Map<String, String> body);

    /**
     * Method for sending a register request using the Email/Password auth provider
     * @param body
     */
    @POST("/user/register-username")
    Call<GenericApiResponse> registerUserEmailPass(@Body Map<String, String> body);

    /**
     * Method for sending a register request using the Twitter auth provider
     * @param body
     */
    @POST("/user/register-twitter")
    Call<GenericApiResponse> registerUserTwitter(@Body Map<String, String> body);

    /**
     * Refresh the current JWT token
     */
    @GET("/user/refresh_token")
    Call<GenericApiResponse> refreshToken();

    /**
     * Method for sending a login request using the Facebook auth provider
     * @param body
     */
    @POST("/user/login-facebook")
    Call<GenericApiResponse> loginFacebook(@Body Map<String, String> body);

    /**
     * Method for sending a login request using the Twitter auth provider
     * @param body
     */
    @POST("/user/login-twitter")
    Call<GenericApiResponse> loginTwitter(@Body Map<String, String> body);

    /**
     * Method for sending a login request using the Email/Password auth provider
     * @param body
     */
    @POST("/user/login_password")
    Call<GenericApiResponse> loginEmailAndPassword(@Body Map<String, String> body);

    /**
     * Method for sending a logout request
     */
    @GET("/user/logout")
    Call<GenericApiResponse> logout();

    /**
     * Method for requesting a password reset email
     * @param body
     */
    @POST("/user/request_password_reset")
    Call<GenericApiResponse> requestPasswordReset(@Body Map<String, String> body);

    /**
     * Method for changing a user authentication password
     * @param body
     */
    @POST("/user/password_reset")
    Call<GenericApiResponse> resetPassword(@Body Map<String, String> body);

    @POST("/user/update")
    Call<GenericApiResponse> updateUser(@Body Map<String, Object> body);

    @GET("/user/metadata")
    Call<GenericApiResponse> getUserMetadata();

    @POST("/user/update_metadata")
    Call<GenericApiResponse> updateUserMetadata(@Body Map<String, Object> body);

    /**
     * Method for sending a subscribe request
     * @param body
     */
    @POST("/object/subscribe")
    Call<GenericApiResponse> subscribe(@Body Map<String, Object> body);

    @POST("/object/count")
    Call<GenericApiResponse> count(@Body Map<String, Object> body);

    /**
     * Method for sending an unsubscribe request
     * @param body
     */
    @POST("/object/unsubscribe")
    Call<GenericApiResponse> unsubscribe(@Body Map<String, Object> body);

    /**
     * Method for sending an object creation request
     * @param body
     */
    @POST("/object/create")
    Call<GenericApiResponse> create(@Body Map<String, Object> body);

    /**
     * Method for sending an object update request
     * @param body
     */
    @POST("/object/update")
    Call<GenericApiResponse> update(@Body Map<String, Object> body);

    /**
     * Method for sending an object delete request
     * @param body
     */
    @POST("/object/delete")
    Call<GenericApiResponse> delete(@Body Map<String, Object> body);

    @POST("/proxy")
    Call<Response> proxy(@Body TelepatProxyRequest request);

    @GET("/user/me")
    Call<GenericApiResponse> me();

    @GET("/user/get")
    Call<GenericApiResponse> get(@Query("user_id") String userId);

    @POST("/email")
    Call<GenericApiResponse> sendEmail(@Body Map<String, Object> body);

    @POST("/til/append")
    Call<GenericApiResponse> appendToIndexedList(@Body Map<String, Object> body);

    @POST("/til/get")
    Call<GenericApiResponse> checkIndexedListMembers(@Body Map<String, Object> body);

    @POST("/til/removeList")
    Call<GenericApiResponse> deleteIndexedList(@Body Map<String, String> body);

    @POST("/til/removeMember")
    Call<GenericApiResponse> removeFromIndexedList(@Body Map<String, String> body);
}
