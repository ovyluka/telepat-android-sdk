package io.telepat.sdk;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import io.telepat.BuildConfig;
import io.telepat.sdk.data.TelepatInternalDB;
import io.telepat.sdk.data.TelepatSnappyDb;
import io.telepat.sdk.models.Channel;
import io.telepat.sdk.models.ContextUpdateListener;
import io.telepat.sdk.models.OnChannelEventListener;
import io.telepat.sdk.models.TelepatContext;
import io.telepat.sdk.models.TelepatProxyRequest;
import io.telepat.sdk.models.TelepatProxyResponse;
import io.telepat.sdk.models.TelepatRequestListener;
import io.telepat.sdk.models.TransportNotification;
import io.telepat.sdk.models.UserCreateListener;
import io.telepat.sdk.models.UserLoginCallback;
import io.telepat.sdk.models.UserUpdatePatch;
import io.telepat.sdk.networking.OctopusApi;
import io.telepat.sdk.networking.OctopusRequestInterceptor;
import io.telepat.sdk.networking.requests.RegisterDeviceRequest;
import io.telepat.sdk.networking.requests.RegisterFacebookUserRequest;
import io.telepat.sdk.networking.requests.RegisterTwitterUserRequest;
import io.telepat.sdk.networking.responses.ContextsApiResponse;
import io.telepat.sdk.networking.responses.GenericApiResponse;
import io.telepat.sdk.networking.responses.TelepatCountCallback;
import io.telepat.sdk.networking.transports.gcm.GcmRegistrar;
import io.telepat.sdk.utilities.TelepatConstants;
import io.telepat.sdk.utilities.TelepatLogger;
import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.android.AndroidLog;
import retrofit.client.Response;

/**
 * Created by Andrei Marinescu, catalinivan on 10/03/15.
 *
 * Telepat Main Orchestrator
 */
public final class Telepat
{
	/**
	 * Telepat singleton instance
	 */
	private static Telepat mInstance;
	/**
	 * Reference to the application context
	 */
	private        Context                        mContext;
	/**
	 * References to the currently available Telepat contexts
	 */
	private        HashMap<String, TelepatContext> mServerContexts;
	/**
	 * Reference to a Telepat Sync API client
	 */
	private OctopusApi apiClient;
	/**
	 * Retrofit RequestInterceptor implementation for injecting the proper authentication headers
	 */
	private OctopusRequestInterceptor requestInterceptor;
	/**
	 * Internal storage reference
	 */
	private TelepatInternalDB internalDB;
	/**
	 * Locally registered Channel instances
	 */
	private HashMap<String, Channel> subscriptions = new HashMap<>();
	/**
	 * Context update listener array
	 */
	private CopyOnWriteArrayList<ContextUpdateListener> contextUpdateListeners = new CopyOnWriteArrayList<>();
	/**
	 * Unique device identifier
	 */
	private String localUdid;

	/**
	 * Configured Telepat Application ID
	 */
	private String appId;

	private Telepat() {	}

	/**
	 *
	 * @return Returns a reference to the singleton instance
	 */
	public static Telepat getInstance()
	{
		if (mInstance == null)	{
			mInstance = new Telepat();
		}

		return mInstance;
	}

	/**
	 * Get access to an instance controlling the internal storage DB
	 * @return An instance of a class implementing <code>TelepatInternalDB</code>
	 */
	public TelepatInternalDB getDBInstance() {
		return internalDB;
	}

    /**
     * Get access to an Retrofit instance that is able to communicate with the Telepat Sync API
     * @return An <code>OctopusApi</code> instance
     */
	public OctopusApi getAPIInstance() { return apiClient; }

	@SuppressWarnings("unused")
	public void emptyDB(Context ctx) {
		if(internalDB == null) {
			internalDB = new TelepatSnappyDb(ctx);
		}
		internalDB.empty();
	}

	public void initialize(Context context,
						   final String telepatEndpoint,
						   final String clientApiKey,
						   final String clientAppId,
						   String senderId) {
		mContext = context.getApplicationContext();
		internalDB = new TelepatSnappyDb(context);
		appId = clientAppId;
		TelepatConstants.GCM_SENDER_ID = senderId;
		initHTTPClient(telepatEndpoint, clientApiKey, clientAppId);
		new GcmRegistrar(mContext).initGcmRegistration();

		String JWTtoken = (String) internalDB.getOperationsData(TelepatConstants.JWT_KEY, null, String.class);
		if(JWTtoken != null) {
			requestInterceptor.setAuthorizationToken(JWTtoken);
			refreshToken(null);
		}

		requestContexts();
//		String registrationId = (String) Telepat.getInstance()
//				.getDBInstance()
//				.getOperationsData(GcmRegistrar.PROPERTY_REG_ID, "", String.class);
		TelepatLogger.log("Initialized Telepat Android SDK version " + BuildConfig.VERSION_NAME);
	}

    /**
     * Close the current Telepat instance. You should reinitialize the Telepat SDK before doing
     * additional work.
     */
	@SuppressWarnings("unused")
	public void destroy() {
		internalDB.close();
	}

    /**
     * Configures the OctopusApi instance with relevant credentials
     * @param clientApiKey A string containing a Telepat client API key
     * @param clientAppId A string containing the corresponding Telepat application ID
     */
	private void initHTTPClient(String telepatEndpoint, String clientApiKey, final String clientAppId) {
		requestInterceptor = new OctopusRequestInterceptor(clientApiKey, clientAppId);

		RestAdapter.Builder rBuilder = new RestAdapter.Builder()
				.setEndpoint(telepatEndpoint)
				.setRequestInterceptor(requestInterceptor);
		if(TelepatConstants.RETROFIT_DEBUG_ENABLED)
			rBuilder.setLogLevel(RestAdapter.LogLevel.FULL)
					.setLog(new AndroidLog(TelepatConstants.TAG));

		RestAdapter restAdapter = rBuilder.build();
		apiClient = restAdapter.create(OctopusApi.class);
	}

    /**
     * Send the Telepat Sync API a device registration request
     * @param regId A GCM token for the current device
     * @param shouldUpdateBackend If true, an update should be sent to the Telepat cloud instance
     *                            regardless of the state of the token (new/already sent)
     */
	public void registerDevice(String regId, boolean shouldUpdateBackend)
	{
		String udid = (String) internalDB.getOperationsData(TelepatConstants.UDID_KEY,
															"",
															String.class);
		if(!udid.isEmpty() && !shouldUpdateBackend) {
			for (ContextUpdateListener listener : Telepat.this.contextUpdateListeners) {
				listener.deviceRegisterSuccess();
			}
			return;
		}

		if(udid.isEmpty()) {
			RegisterDeviceRequest request = new RegisterDeviceRequest(regId);
			apiClient.registerDevice(request.getParams(), new Callback<GenericApiResponse>() {
				@Override
				public void success(GenericApiResponse octopusResponse,
									retrofit.client.Response response) {
					TelepatLogger.log("Register device success");
					if (octopusResponse.status == 200 && octopusResponse.content.get("identifier") != null) {
						requestInterceptor.setUdid((String)octopusResponse.content.get("identifier"));
						internalDB.setOperationsData(TelepatConstants.UDID_KEY,
								octopusResponse.content.get("identifier"));
						for (ContextUpdateListener listener : Telepat.this.contextUpdateListeners) {
							listener.deviceRegisterSuccess();
						}
						TelepatLogger.log("Received Telepat UDID: " + octopusResponse.content.get("identifier"));
					}
				}

				@Override
				public void failure(RetrofitError error) {
					TelepatLogger.log("Register device failure.");

				}
			});
		} //else {
			//TODO send update
		//}
	}

    /**
     * Retrieve the currently active contexts for the current Telepat application
     */
	private void requestContexts()
	{
		apiClient.updateContexts(new Callback<ContextsApiResponse>() {
			@Override
			public void success(ContextsApiResponse contextMap,
								retrofit.client.Response response) {
				updateContexts(contextMap);
			}

			@Override
			public void failure(RetrofitError error) {
				//TODO: bubble errors to the User level
				if (error != null && error.getResponse() != null) {
					if (error.getResponse().getStatus() == 404) {
						apiClient.updateContextsCompat(new Callback<ContextsApiResponse>() {
							@Override
							public void success(ContextsApiResponse contextsApiResponse, Response response) {
								updateContexts(contextsApiResponse);
							}

							@Override
							public void failure(RetrofitError error) {
								TelepatLogger.log("Failed to get contexts" + error.getMessage());
							}
						});
					} else {
						TelepatLogger.log("Failed to get contexts" + error.getMessage());
					}
				} else {
					TelepatLogger.log("Failed to get contexts");
				}
			}
		});
	}

	private void updateContexts(ContextsApiResponse contextMap) {
		if (contextMap == null) return;
		if (mServerContexts == null) mServerContexts = new HashMap<>();
		for (TelepatContext ctx : contextMap.content)
			mServerContexts.put(ctx.getId(), ctx);
		for (ContextUpdateListener listener : Telepat.this.contextUpdateListeners) {
			listener.contextInitializeSuccess();
		}
		TelepatLogger.log("Retrieved " + contextMap.content.size() + " contexts");
	}

	/**
	 * Send a Telepat Sync API call for registering a user with the Facebook auth provider
	 * @param fbToken A Facebook OAUTH token
	 */
	@Deprecated
	@SuppressWarnings("unused")
	public void register(final String fbToken, final TelepatRequestListener loginListener) {
		registerFacebookUser(fbToken, loginListener);
	}

	/**
	 * Send a Telepat Sync API call for registering a user with the Facebook auth provider
	 * @param fbToken A Facebook OAUTH token
	 */
	@SuppressWarnings("unused")
	public void registerFacebookUser(final String fbToken, final TelepatRequestListener loginListener)
	{
		internalDB.setOperationsData(TelepatConstants.FB_TOKEN_KEY, fbToken);
		apiClient.registerUserFacebook(new RegisterFacebookUserRequest(fbToken).getParams(), new Callback<Map<String, String>>() {
			@Override
			public void success(Map<String, String> userRegisterResponse, retrofit.client.Response response) {
				apiClient.loginFacebook(
						new RegisterFacebookUserRequest(fbToken).getParams(),
						new UserLoginCallback(requestInterceptor, internalDB, loginListener)
				);

			}

			@Override
			public void failure(RetrofitError error) {
				if (error.getResponse().getStatus() == 409) {
					apiClient.loginFacebook(
							new RegisterFacebookUserRequest(fbToken).getParams(),
							new UserLoginCallback(requestInterceptor, internalDB, loginListener)
					);
				} else {
					TelepatLogger.error("user register failed");
					loginListener.onError(error);
				}
			}
		});
	}

	/**
	 * Send a Telepat Sync API call for registering a user with the Twitter auth provider
	 */
	@SuppressWarnings("unused")
	public void registerTwitterUser(final String oauthToken, final String oauthTokenSecret, final TelepatRequestListener loginListener)
	{
		internalDB.setOperationsData(TelepatConstants.TWITTER_TOKEN_KEY, oauthToken);
		internalDB.setOperationsData(TelepatConstants.TWITTER_SECRET_TOKEN_KEY, oauthTokenSecret);
		apiClient.registerUserTwitter(new RegisterTwitterUserRequest(oauthToken, oauthTokenSecret).getParams(), new Callback<Map<String, String>>() {
			@Override
			public void success(Map<String, String> userRegisterResponse, retrofit.client.Response response) {
				TelepatLogger.log("User registered");
				apiClient.loginTwitter(
						new RegisterTwitterUserRequest(oauthToken, oauthTokenSecret).getParams(),
						new UserLoginCallback(requestInterceptor, internalDB, loginListener)
				);
			}

			@Override
			public void failure(RetrofitError error) {
				if (error.getResponse().getStatus() == 409) {
					apiClient.loginTwitter(
							new RegisterTwitterUserRequest(oauthToken, oauthTokenSecret).getParams(),
							new UserLoginCallback(requestInterceptor, internalDB, loginListener)
					);
				} else {
					TelepatLogger.error("User register failed");
				}
			}
		});
	}

	/**
	 * Submit a set of user credentials for registering a new user with the Email/Password auth provider
	 * @param email The username of the Telepat user
	 * @param password A cleartext password to be associated
	 * @param name The displayable name of the user
	 * @param callbackUrl An optional deep link for redirecting the user back into the app after confirming his email address
	 * @param listener A callback for success and error events
	 */
	@SuppressWarnings("unused")
	public void createUser(final String email, final String password, final String name, final String callbackUrl, final HashMap<String, String> additionalMetadata, final UserCreateListener listener) {
		if(email!=null && password!=null && name!=null) {
			HashMap<String, String> userHash = new HashMap<>();
			userHash.put("username", email);
			userHash.put("email", email);
			userHash.put("password", password);
			userHash.put("name", name);

			if(additionalMetadata != null) {
				userHash.putAll(additionalMetadata);
			}

			if(callbackUrl != null && !callbackUrl.isEmpty()) {
				userHash.put("callbackUrl", callbackUrl);
			}

			apiClient.registerUserEmailPass(userHash, new Callback<Map<String, String>>() {
				@Override
				public void success(Map<String, String> stringStringMap, Response response) {
					listener.onUserCreateSuccess();
				}

				@Override
				public void failure(RetrofitError error) {
					listener.onUserCreateFailure(error);
				}
			});
		}
	}

	@SuppressWarnings("unused")
	public void createUser(final String email, final String password, final String name, final HashMap<String, String> additionalMetadata, final UserCreateListener listener) {
		createUser(email, password, name, null, additionalMetadata, listener);
}

	@SuppressWarnings("unused")
	public void createUser(final String email, final String password, final String name, final UserCreateListener listener) {
		createUser(email, password, name, null, listener);
	}

	/**
	 * Refreshes the JWT authorization token. Tokens expire 60 minutes after they are issued,
	 * call this in order to get a fresh one without doing login again.
	 * @param requestListener a listener for the result state of the operation
	 */
	public void refreshToken(final TelepatRequestListener requestListener) {
		apiClient.refreshToken(
				new UserLoginCallback(
						requestInterceptor,
						internalDB,
						requestListener
				)
		);
	}

	@SuppressWarnings("unused")
	public void loginWithUsername(final String email,
								  final String password,
								  final TelepatRequestListener listener) {
		if(email != null && password != null) {
			HashMap<String, String> userHash = new HashMap<>();
			userHash.put("username", email);
			userHash.put("password", password);
			apiClient.loginEmailAndPassword(
					userHash,
					new UserLoginCallback(requestInterceptor, internalDB, listener)
			);
		}
	}

	@SuppressWarnings("unused")
	public void loginWithFacebook(final String fbToken, final String existingUsername, final TelepatRequestListener loginListener) {
		apiClient.loginFacebook(
				new RegisterFacebookUserRequest(fbToken, existingUsername).getParams(),
				new UserLoginCallback(requestInterceptor, internalDB, loginListener)
		);
	}

	@SuppressWarnings("unused")
	public void loginWithFacebook(final String fbToken, final TelepatRequestListener loginListener) {
		loginWithFacebook(fbToken, null, loginListener);
	}

	@SuppressWarnings("unused")
	public void loginWithTwitter(final String oauthToken,
								 final String oauthTokenSecret,
								 final TelepatRequestListener loginListener) {
		apiClient.loginTwitter(
				new RegisterTwitterUserRequest(oauthToken, oauthTokenSecret).getParams(),
				new UserLoginCallback(requestInterceptor, internalDB, loginListener));
	}

    /**
     * Send a Telepat Sync API call for logging out the current user. The method will return null if there is no currently logged in user.
     */
	@SuppressWarnings("unused")
	public void logout()
	{
		apiClient.logout(new Callback<HashMap<String, Object>>() {
			@Override
			public void success(HashMap<String, Object> userLogoutResponse, retrofit.client.Response response) {
				TelepatLogger.log("Logout successful");
				requestInterceptor.setAuthorizationToken(null);
			}

			@Override
			public void failure(RetrofitError error) {
				TelepatLogger.error("user logout failed - " + error.getMessage());
			}
		});
	}

	/**
	 * Get information about the currently logged in user
	 * @return a HashMap of the user data.
	 */
	@SuppressWarnings("unused")
	public Map<String, Object> getLoggedInUserInfo() {
		Object userData = internalDB.getOperationsData(TelepatConstants.CURRENT_USER_DATA, null, HashMap.class);
		if(userData instanceof HashMap) {
			//noinspection unchecked
			return (HashMap<String, Object>) userData;
		} else {
			TelepatLogger.error("Not a hashmap");
			return null;
		}
	}

	/**
	 * Request a password reset email
	 */
	@SuppressWarnings("unused")
	public void requestPasswordResetEmail(String username, String callbackUrl, final TelepatRequestListener listener) {
		HashMap<String, String> requestBody = new HashMap<>();
		requestBody.put("username", username);
		requestBody.put("type", "android");

		if(callbackUrl != null && !callbackUrl.isEmpty()) {
			requestBody.put("callbackUrl", callbackUrl);
		}

		apiClient.requestPasswordReset(requestBody, new Callback<HashMap<String, String>>() {
			@Override
			public void success(HashMap<String, String> genericApiResponse, Response response) {
				TelepatLogger.log("Reset email sent");
				listener.onSuccess();
			}

			@Override
			public void failure(RetrofitError error) {
				TelepatLogger.log("Reset request failed");
				listener.onError(error);
			}
		});
	}

	@SuppressWarnings("unused")
	public void requestPasswordResetEmail(String username, final TelepatRequestListener listener) {
		requestPasswordResetEmail(username, null, listener);
	}

	/**
	 * Commit password change request
	 * @param userId the user ID
	 * @param token the token received via the reset email / deep-link
	 * @param newPassword
	 * @param listener
	 */
	@SuppressWarnings("unused")
	public void resetPassword(String userId, String token, String newPassword, final TelepatRequestListener listener) {
		HashMap<String, String> requestBody = new HashMap<>();
		requestBody.put("user_id", userId);
		requestBody.put("token", token);
		requestBody.put("password", newPassword);
		apiClient.resetPassword(requestBody, new Callback<HashMap<String, String>>() {
			@Override
			public void success(HashMap<String, String> genericApiResponse, Response response) {
				TelepatLogger.log("Password was reset");
				listener.onSuccess();
			}

			@Override
			public void failure(RetrofitError error) {
				TelepatLogger.log("Password reset failed");
				listener.onError(error);
			}
		});
	}

	@SuppressWarnings("unused")
	public void getUserMetadata(Callback<GenericApiResponse> callback) {
		apiClient.getUserMetadata(callback);
	}

	@SuppressWarnings("unused")
	public void updateUserMetadata(List<UserUpdatePatch> patches, String metadataId, final TelepatRequestListener listener) {
		HashMap<String, Object> requestBody = new HashMap<>();
		ArrayList<HashMap<String, Object>> jsonPatches = new ArrayList<>();
		for(UserUpdatePatch patch : patches) {
			HashMap<String, Object> jsonPatch = new HashMap<>();
			jsonPatch.put("op", "replace");
			jsonPatch.put("path", "user_metadata/"+metadataId+"/"+patch.getFieldName());
			jsonPatch.put("value", patch.getFieldValue());
			jsonPatches.add(jsonPatch);
		}
		requestBody.put("patches", jsonPatches);
		apiClient.updateUserMetadata(requestBody, new Callback<HashMap<String, Object>>() {
			@Override
			public void success(HashMap<String, Object> response, Response response2) {
				listener.onSuccess();
			}

			@Override
			public void failure(RetrofitError error) {
				listener.onError(error);
			}
		});
	}

	@SuppressWarnings("unused")
	public void updateUserMetadata(UserUpdatePatch patch, String metadataId, final TelepatRequestListener listener) {
		ArrayList<UserUpdatePatch> patches = new ArrayList<>();
		patches.add(patch);
		updateUserMetadata(patches, metadataId, listener);
	}

    /**
     * Create a new subscription to a Telepat channel
     * @param context The context ID where the desired objects live in
     * @param modelName The model name of the desired objects
     * @param listener An object implementing OnChannelEventListener. All channel events will be sent
     *                 to this object.
     * @param type The desired Java class of the objects that will be emitted in this channel (should
     *             extend the TelepatBaseModel class)
     * @return a <code>Channel</code> object with the specified characteristics
     */
	@SuppressWarnings("unused")
	public Channel subscribe(TelepatContext context, String modelName, OnChannelEventListener listener, Class type) {
		Channel channel = new Channel.Builder().
                setContext(context).
                setModelName(modelName).
                setChannelEventListener(listener).
                setObjectType(type).
                build();
//		subscriptions.put(channel.getSubscriptionIdentifier(), channel);
		channel.subscribe();
		return channel;
	}

	/**
	 * Create a new subscription to a Telepat channel
	 * @param context The context ID where the desired objects live in
	 * @param modelName The model name of the desired objects
	 * @param objectId The ID of an object to subscribe to. Can be null if not used.
	 * @param userId The user ID to filter results by. Can be null if not used.
	 * @param parentModelName The parent model name to filter results by. Can be null if not used.
	 * @param parentId The parent object ID to filter results by. Can be null if not used.
	 * @param filters A HashMap of filters. See http://docs.telepat.io/api.html#api-Object-ObjectSubscribe for details. Can be null if not used.
	 * @param type The desired Java class of the objects that will be emitted in this channel (should
	 *             extend the TelepatBaseModel class)
	 * @param listener An object implementing OnChannelEventListener. All channel events will be sent
	 *                 to this object.
	 * @return a <code>Channel</code> object with the specified characteristics
	 */
	@Deprecated
	@SuppressWarnings("unused")
	public Channel subscribe(TelepatContext context,
							 String modelName,
							 String objectId,
							 String userId,
							 String parentModelName,
							 String parentId,
							 HashMap<String, Object> filters,
							 Class type,
							 OnChannelEventListener listener) {
		Channel channel = new Channel.Builder()
				.setContext(context)
				.setModelName(modelName)
				.setUserFilter(userId)
				.setSingleObjectIdFilter(objectId)
				.setParentFilter(parentModelName, parentId)
				.setFilters(filters)
				.setObjectType(type)
				.setChannelEventListener(listener)
				.build();
		channel.subscribe();
		return channel;
	}

	/**
	 * Create a new subscription to a Telepat channel
	 * @param channel A channel object that covers all the desired characteristics. See Channel.Builder for ways to create a channel object.
	 */
	@SuppressWarnings("unused")
	public void subscribe(Channel channel) {
		channel.subscribe();
	}

	@SuppressWarnings("unused")
	public void count(TelepatContext context,
					  String modelName,
					  String objectId,
					  String userId,
					  String parentModelName,
					  String parentId,
					  HashMap<String, Object> filters,
					  Channel.AggregationType aggregationType,
					  String aggregationField,
					  final TelepatCountCallback callback) {

		Channel channel = new Channel.Builder()
				.setContext(context)
				.setModelName(modelName)
				.setUserFilter(userId)
				.setSingleObjectIdFilter(objectId)
				.setParentFilter(parentModelName, parentId)
				.setFilters(filters)
				.build();

		channel.count(callback, aggregationType, aggregationField);

	}

    /**
     * Get a Map of all curently active contexts for the Telepat Application
     * @return A Map instance containing TelepatContext objects stored by their ID
     */
	public Map<String, TelepatContext> getContexts() { return mServerContexts; }

    /**
     * Remove a locally registered subscription of a Telepat Channel object (this does not send any
     * notifications to the Telepat Sync API
     * @param mChannel The channel instance
     */
    @SuppressWarnings("unused")
	public void removeSubscription(Channel mChannel) {
        mChannel.unsubscribe();
        subscriptions.remove(mChannel.getSubscriptionIdentifier());
    }

    /**
     * Locally register an active subscription to a Telepat Channel with the Telepat SDK instance
     * (new channel objects register themselves automatically)
     * @param mChannel The channel object to be registered
     */
    public void registerSubscription(Channel mChannel) {
        subscriptions.put(mChannel.getSubscriptionIdentifier(), mChannel);
    }

    /**
     * Get the <code>Channel</code> instance of a locally registered channel.
     * @param channelIdentifier A properly formatted string of the channel identifier.
     * @return the <code>Channel</code> instance
     */
	public Channel getSubscribedChannel(String channelIdentifier) {
		return subscriptions.get(channelIdentifier);
	}

    /**
     * Get a unique device identifier. Used internally for detecting already registered devices
     * @return A String containing the UDID
     */
	public String getDeviceLocalIdentifier() {
		if(localUdid!=null) return localUdid;
		String androidId = android.provider.Settings.
                                    System.getString(mContext.getContentResolver(),
                                                     android.provider.Settings.Secure.ANDROID_ID);

		localUdid = (String) internalDB.getOperationsData(TelepatConstants.LOCAL_UDID_KEY,
                                                          androidId,
                                                          String.class);

		return localUdid;
	}

    /**
     * Set the unique device identifier sent to the Telepat cloud. This method should be used as
     * early as possible, before registering the device with the Sync API.
     * @param udid the desired UDID
     */
	@SuppressWarnings("unused")
	public void setDeviceLocalIdentifier(String udid) {
		internalDB.setOperationsData(TelepatConstants.LOCAL_UDID_KEY, udid);
	}

	public void registerContextUpdateListener(ContextUpdateListener listener) {
		this.contextUpdateListeners.add(listener);
	}

	@SuppressWarnings("unused")
	public void removeContextUpdateListener(ContextUpdateListener listener) {
		this.contextUpdateListeners.remove(listener);
	}

	@SuppressWarnings("unused")
	public void updateUser(final ArrayList<UserUpdatePatch> userChanges, String userId, final TelepatRequestListener listener) {
		HashMap<String, Object> requestBody = new HashMap<>();
		ArrayList<HashMap<String, Object>> jsonPatches = new ArrayList<>();
		for(UserUpdatePatch patch : userChanges) {
			HashMap<String, Object> jsonPatch = new HashMap<>();
			jsonPatch.put("op", "replace");
			jsonPatch.put("path", "user/"+userId+"/"+patch.getFieldName());
			jsonPatch.put("value", patch.getFieldValue());
			jsonPatches.add(jsonPatch);
		}
		requestBody.put("patches", jsonPatches);
		apiClient.updateUser(requestBody, new Callback<HashMap<String, String>>() {
			@Override
			public void success(HashMap<String, String> genericApiResponse, Response response) {
				TelepatLogger.log("User update successful");
				if (getLoggedInUserInfo() != null) {
					HashMap<String, Object> userData = new HashMap<>(getLoggedInUserInfo());
					for (UserUpdatePatch patch : userChanges) {
						userData.put(patch.getFieldName(), patch.getFieldValue());
					}
					internalDB.setOperationsData(TelepatConstants.CURRENT_USER_DATA, userData);
				}
				listener.onSuccess();
			}

			@Override
			public void failure(RetrofitError error) {
				TelepatLogger.log("User update failed");
				listener.onError(error);
			}
		});
	}

	/**
	 * Method for notifying listeners of updates to context objects. Used internally by transports
	 * for firing notification updated.
	 * @param notification a TransportNotification instance that encapsulates changes made to a context
	 */
	public void fireContextUpdate(TransportNotification notification) {
		if(mServerContexts == null ) return;
		Gson gson = new Gson();
		TelepatContext ctx;
		String contextId;
		switch (notification.getNotificationType()) {
			case ObjectAdded:
				ctx = gson.fromJson(notification.getNotificationValue(), TelepatContext.class);
				mServerContexts.put(ctx.getId(), ctx);
				for (ContextUpdateListener listener : this.contextUpdateListeners) {
					listener.contextAdded(ctx);
				}
				break;
			case ObjectUpdated:
				String[] pathSegments = notification.getNotificationPath().getAsString().replace("context/", "").split("/");
				contextId = pathSegments[0];
				String fieldName = pathSegments[1];
				if(mServerContexts.containsKey(contextId)) {
					ctx = mServerContexts.get(contextId);
					switch (fieldName) {
						case "meta":
							Type type = new TypeToken<HashMap<String, Object>>(){}.getType();
							HashMap<String, Object> meta = gson.fromJson(notification.getNotificationValue().toString(), type);
							ctx.getMeta().putAll(meta);
							break;
						case "name":
							ctx.setName(notification.getNotificationValue().getAsString());
					}
					mServerContexts.put(ctx.getId(), ctx);
					for (ContextUpdateListener listener : this.contextUpdateListeners) {
						listener.contextUpdated(ctx);
					}
				}
				break;
			case ObjectDeleted:
				contextId = notification.getNotificationPath().getAsString().replace("context/","");
				if(mServerContexts.containsKey(contextId)) {
					ctx = mServerContexts.get(contextId);
					mServerContexts.remove(contextId);
					for (ContextUpdateListener listener : this.contextUpdateListeners) {
						listener.contextEnded(ctx);
					}
				}
				break;
		}
	}

	@SuppressWarnings("unused")
	public void sendProxiedRequest(TelepatProxyRequest request, final TelepatProxyResponse callback) {
		apiClient.proxy(request, new Callback<Response>() {
			@Override
			public void success(Response response, Response response2) {
				//Try to get response body
				BufferedReader reader = null;
				StringBuilder sb = new StringBuilder();
				try {

					reader = new BufferedReader(new InputStreamReader(response.getBody().in()));
					String line;

					try {
						while ((line = reader.readLine()) != null) {
							sb.append(line);
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}

				String responseBody = sb.toString();
				if(callback!=null) {
					callback.onRequestFinished(responseBody, response.getHeaders());
				}
			}

			@Override
			public void failure(RetrofitError error) {
				if(callback!=null) {
					callback.onTelepatError(error);
				}
			}
		});
	}

	@SuppressWarnings("unused")
	public void sendEmail(List<String> recipients, String from, String fromName, String subject, String body, Callback<GenericApiResponse> callback) {
		HashMap<String, Object> requestBody = new HashMap<>();
		if(recipients==null || from==null || body==null) {
			callback.failure(null);
			return;
		}
		ArrayList<String> recipientsList = new ArrayList<>(recipients);
		requestBody.put("recipients", recipientsList);
		requestBody.put("from", from);
		if(fromName!=null) {
			requestBody.put("from_name", fromName);
		}
		if(subject!=null) {
			requestBody.put("subject", subject);
		}
		requestBody.put("body", body);
		apiClient.sendEmail(requestBody, callback);
	}

	@SuppressWarnings("unused")
	public void me(Callback<GenericApiResponse> callback) {
		getAPIInstance().me(callback);
	}

	@SuppressWarnings("unused")
	public void get(String userId, Callback<GenericApiResponse> callback) {
		getAPIInstance().get(userId, callback);
	}

	public String getAppId() {
		return appId;
	}

	@SuppressWarnings("unused")
	public void appendToIndexedList(Map<String, String> objectToIndex, String listName, String indexedPropertyName, Callback<GenericApiResponse> callback) {
		HashMap<String, Object> appendRequestBody = new HashMap<>();
		appendRequestBody.put("listName", listName);
		appendRequestBody.put("indexedProperty", indexedPropertyName);
		appendRequestBody.put("memberObject", objectToIndex);
		getAPIInstance().appendToIndexedList(appendRequestBody, callback);
	}

	@SuppressWarnings("unused")
	public void objectsExistsInIndex(List<String> memberPropertyValues, String listName, String indexedPropertyName, Callback<GenericApiResponse> callback) {
		HashMap<String, Object> requestBody = new HashMap<>();
		requestBody.put("listName", listName);
		requestBody.put("indexedProperty", indexedPropertyName);
		requestBody.put("members", memberPropertyValues);
		getAPIInstance().checkIndexedListMembers(requestBody, callback);
	}

	@SuppressWarnings("unused")
	public void deleteIndexedList(String listName, Callback<GenericApiResponse> callback) {
		HashMap<String, String> requestBody = new HashMap<>();
		requestBody.put("listName", listName);
		getAPIInstance().deleteIndexedList(requestBody, callback);
	}

	@SuppressWarnings("unused")
	public void removeMemberFromIndexedList(String memberPropertyValue, String listName, String indexedPropertyName, Callback<GenericApiResponse> callback) {
		HashMap<String, String> requestBody = new HashMap<>();
		requestBody.put("listName", listName);
		requestBody.put("indexedProperty", indexedPropertyName);
		requestBody.put("member", memberPropertyValue);
		getAPIInstance().removeFromIndexedList(requestBody, callback);
	}
}
