package com.cga;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.Manifest;

import com.cga.CustomCameraActivity;

import org.apache.cordova.PermissionHelper;
import org.apache.cordova.PluginResult;
import android.content.pm.PackageManager;
import android.os.Build;

/**
 * This class echoes a string called from JavaScript.
 */
public class AndroidCamera extends CordovaPlugin {
    public static String TAG = "CordovaActivity";
    public static final int REQUEST_PERMISSIONS = 2;
    public static final int PERMISSION_DENIED_ERROR = 20;
    private CallbackContext callbackContext;
    private JSONArray args;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
      this.callbackContext = callbackContext;
      this.args = args;

      if (action.equals("recordVideo")) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          PermissionHelper.requestPermissions(
            this, 
            REQUEST_PERMISSIONS, 
            new String[] {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO});
        } else {
          this.recordVideo();
        }

        return true;
      }        
     
      return false;
    }

    public void onRequestPermissionResult(int requestCode, String[] permissions,
                                          int[] grantResults) throws JSONException {
      for (int r : grantResults) {
        if (r == PackageManager.PERMISSION_DENIED) {
            this.callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, PERMISSION_DENIED_ERROR));
            return;
        }
      }
      this.recordVideo();
    }

    private void recordVideo() throws JSONException {
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
