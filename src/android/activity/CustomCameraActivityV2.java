package com.cga;

import com.cga.FakeR;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;

import java.io.IOException;
import java.lang.Exception;
import android.util.Log;
import android.view.SurfaceView;
import android.view.SurfaceHolder;
import java.lang.RuntimeException;
import java.io.File;
import android.widget.Toast;
import android.os.Environment;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.view.View;
import android.view.Surface;
import java.util.Date;
import java.text.SimpleDateFormat;
import android.content.res.Configuration;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.content.Intent;
import android.os.CountDownTimer;
import android.widget.TextView;
import java.util.concurrent.TimeUnit;
import android.widget.ImageButton;
import android.view.OrientationEventListener;
import android.content.Context;
import android.view.Display;
import android.graphics.Point;
import java.lang.Math;
import java.util.List;
import android.graphics.Color;
import android.os.Build;

import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CameraDevice;
import android.support.annotation.NonNull;
import android.hardware.camera2.CameraCaptureSession;
import java.util.Arrays;
import java.util.concurrent.Semaphore;
import android.media.MediaRecorder;


public class CustomCameraActivityV2 extends BaseCustomActivity {
  private final String TAG = "CustomCameraActivityV2";
  // camera 2 variables
  private Semaphore mCameraOpenCloseLock = new Semaphore(1);
  private String mCameraId;
  private CameraDevice mCameraDevice;
  private CaptureRequest.Builder mPreviewRequestBuilder;
  private CameraCaptureSession mCaptureSession;
  private CaptureRequest mPreviewRequest;

  private CameraCaptureSession.CaptureCallback mCaptureCallback
    = new CameraCaptureSession.CaptureCallback() {
  };
  
  private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
    @Override
    public void onOpened(@NonNull CameraDevice cameraDevice) {
      // This method is called when the camera is opened.  We start camera preview here.
      mCameraOpenCloseLock.release();
      mCameraDevice = cameraDevice;
      createCameraPreviewSession();
    }

    @Override
    public void onDisconnected(@NonNull CameraDevice cameraDevice) {
      mCameraOpenCloseLock.release();
      cameraDevice.close();
      mCameraDevice = null;
    }

    @Override
    public void onError(@NonNull CameraDevice cameraDevice, int error) {
      mCameraOpenCloseLock.release();
      cameraDevice.close();
      mCameraDevice = null;
      CustomCameraActivityV2.this.finish();
    }
  };
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    init();
  }

  public void showToast(String key) {
    String tooltip = getIntent().getStringExtra(key);
    Toast.makeText(this, tooltip, Toast.LENGTH_LONG).show();
  }

  public void init() {
    super.init();
  }

  @Override
  public void onResume() {
    super.onResume();
    startPreview(null);
  }


  private void selectCamera(int cameraView, boolean isFlashOn) {
    CameraManager manager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
    try {
      mCameraId = manager.getCameraIdList()[cameraView];
    } catch (CameraAccessException e) {
        e.printStackTrace();
    } catch (NullPointerException e) {
        e.printStackTrace();
    }
  }

  private void createCameraPreviewSession() {
    try {
      mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
      mPreviewRequestBuilder.addTarget(mSurfaceHolder.getSurface());

      mCameraDevice.createCaptureSession(Arrays.asList(mSurfaceHolder.getSurface()),
        new CameraCaptureSession.StateCallback() {
          @Override
          public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
            if (null == mCameraDevice) {
                return;
            }

            mCaptureSession = cameraCaptureSession;
            try {
              mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                      CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
              mPreviewRequest = mPreviewRequestBuilder.build();
              mCaptureSession.setRepeatingRequest(mPreviewRequest,
                      mCaptureCallback, null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
          }

          @Override
          public void onConfigureFailed(
                  @NonNull CameraCaptureSession cameraCaptureSession) {
              showToast("Failed");
          }
        }, null
      );
    } catch(CameraAccessException e) {
      e.printStackTrace();
    }
  }

  private void startCamera(int cameraView, boolean isFlashOn) {
    try {
      selectCamera(cameraView, isFlashOn);
      CameraManager manager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
      try {
        if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
          throw new RuntimeException("Time out waiting to lock camera opening.");
        }
        manager.openCamera(mCameraId, mStateCallback, null);
      } catch (CameraAccessException e) {
        e.printStackTrace();
      } catch (InterruptedException e) {
        throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
      }
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  public void startPreview(SurfaceHolder holder) {
    if (isBackCamera) {
      this.startCamera(0, false);  
    } else {
      this.startCamera(1, false);
    }
  }

  @Override
  public void surfaceDestroyed(SurfaceHolder holder) {
    stopCamera();
  }

  public void stopCamera() {
    try {
      mCameraOpenCloseLock.acquire();
      if (null != mCaptureSession) {
        mCaptureSession.close();
        mCaptureSession = null;
      }
      if (null != mCameraDevice) {
        mCameraDevice.close();
        mCameraDevice = null;
      }
    } catch (InterruptedException e) {
      throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
    } finally {
      mCameraOpenCloseLock.release();
    }
  }

  public void switchView() {
    stopCamera();
    this.isBackCamera = !this.isBackCamera;
    startPreview(null);

    if(this.isBackCamera) {
      setFlashButtons(true, false);
    } else {
      setFlashButtons(false, false);
    }
  }

  public void startRecording() {

  }

  public void stopRecording(boolean finish) {

  }

  public void switchFlash() {

  }

  public void cancelRecordingProcess() {
    
  }

}