package com.cga;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.cga.CustomCameraActivity;

/**
 * This class echoes a string called from JavaScript.
 */
public class AndroidCamera extends CordovaPlugin {
    public static String TAG = "CordovaActivity";
    private CallbackContext callbackContext;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        Log.d(TAG, "Getting inside execute");
        this.callbackContext = callbackContext;
        if (action.equals("recordVideo")) {
            Log.d(TAG, "Executing function");
            this.recordVideo(callbackContext);
            return true;
        }
        return false;
    }

    private void recordVideo(CallbackContext callbackContext) {
        
        Intent intent = new Intent(cordova.getActivity(), CustomCameraActivity.class);
        if (this.cordova != null) {
          this.cordova.startActivityForResult((CordovaPlugin) this, intent, 0);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
      if (resultCode == Activity.RESULT_OK && data != null) {
        JSONArray res = new JSONArray();
        this.callbackContext.success(res);
      } else {
        this.callbackContext.error("No video recorded");
      }
    }
}
