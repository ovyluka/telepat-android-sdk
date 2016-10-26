package io.telepat.sdk.networking.responses;

import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;

import static io.telepat.sdk.utilities.TelepatConstants.TAG;

/**
 * Created by Andrei Marinescu on 03.06.2015.
 * Response PoJo Class for User/Login
 */
public class GenericApiResponse {
    public int status;
    @SerializedName("content")
    public JsonElement content;

    public HashMap<String,Object> getContent() {

        HashMap<String, Object> map = new HashMap<>();
        JSONObject jObject = null;
        try {
            jObject = new JSONObject(content.toString());

            Iterator<?> keys = jObject.keys();

            while( keys.hasNext() ){
                String key = (String)keys.next();
                String value = jObject.getString(key);
                map.put(key, value);

            }
        } catch (JSONException e) {
            e.printStackTrace();
        }


        return map;
    }
}
