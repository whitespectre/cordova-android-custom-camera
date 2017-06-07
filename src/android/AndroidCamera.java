package com.cga;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;

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
        this.callbackContext = callbackContext;
        if (action.equals("recordVideo")) {
            this.recordVideo(callbackContext, args);
            return true;
        }
        return false;
    }

    private void recordVideo(CallbackContext callbackContext, JSONArray args) throws JSONException {
        Intent intent = new Intent(cordova.getActivity(), CustomCameraActivity.class);
        intent.putExtra("LIBRARY_FOLDER", args.getString(0));
        intent.putExtra("CANCEL_TEXT", args.getString(1));
        intent.putExtra("TOOLTIP", args.getString(2));

        if (this.cordova != null) {
          this.cordova.startActivityForResult((CordovaPlugin) this, intent, 0);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
      if (resultCode == Activity.RESULT_OK && data != null) {
        this.callbackContext.success(data.getStringExtra("videoUrl"));
      } else {
        this.callbackContext.error("No video recorded");
      }
    }
}
