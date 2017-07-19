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
import java.util.ArrayList;
import android.graphics.Color;
import android.os.Build;
import android.util.Size;

import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraMetadata;
import android.support.annotation.NonNull;
import android.hardware.camera2.CameraCaptureSession;
import java.util.Arrays;
import java.util.concurrent.Semaphore;
import android.media.MediaRecorder;


public class CustomCameraActivityV2 extends BaseCustomActivity {
  private final String TAG = "CustomCameraActivityV2";
  // camera 2 variables
  private String mCameraId;
  private CameraDevice mCameraDevice;
  private CaptureRequest.Builder mPreviewRequestBuilder;
  private CameraCaptureSession mCaptureSession;
  private CaptureRequest mPreviewRequest;
  private Size selectedSize;
  private Integer mSensorOrientation;

  private CameraCaptureSession.CaptureCallback mCaptureCallback
    = new CameraCaptureSession.CaptureCallback() {
  };
  
  private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
    @Override
    public void onOpened(@NonNull CameraDevice cameraDevice) {
      // This method is called when the camera is opened.  We start camera preview here.
      mCameraDevice = cameraDevice;
      createCameraPreviewSession();
    }

    @Override
    public void onDisconnected(@NonNull CameraDevice cameraDevice) {
      cameraDevice.close();
      mCameraDevice = null;
    }

    @Override
    public void onError(@NonNull CameraDevice cameraDevice, int error) {
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

  @Override
  public void onPause() {
    super.onPause();
    isFlashOn = false;
    stopRecording(false);
    stopCamera();
  }


  private void selectCamera(int cameraView) {
    CameraManager manager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
    try {
      mCameraId = manager.getCameraIdList()[cameraView];
    } catch (CameraAccessException e) {
        e.printStackTrace();
    } catch (NullPointerException e) {
        e.printStackTrace();
    }
  }

  private void updatePreview() {
    if (null == mCameraDevice) {
      return;
    }

    try {
      mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
      mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
      if (isFlashOn) {
        mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);    
      }

      mPreviewRequest = mPreviewRequestBuilder.build();
      mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback, null);
    } catch (CameraAccessException e) {
        e.printStackTrace();
    }
  }

  protected void setPreviewSize(Size camSize) {
    Display display = getWindowManager().getDefaultDisplay();
    Point size = new Point();
    display.getSize(size);

    double height = ((double)camSize.getWidth()/ camSize.getHeight()) * size.x;
    mSurfaceHolder.setFixedSize(size.x, (int)height);
  }

  private void createCameraPreviewSession() {
    try {
      mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
      mPreviewRequestBuilder.addTarget(mSurfaceHolder.getSurface());

      mCameraDevice.createCaptureSession(Arrays.asList(mSurfaceHolder.getSurface()),
        new CameraCaptureSession.StateCallback() {
          @Override
          public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
            mCaptureSession = cameraCaptureSession;
            updatePreview();
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

  private void startCamera(int cameraView) {
    try {
      selectCamera(cameraView);
      CameraManager manager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
      try {
        CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraId);
        
        manager.openCamera(mCameraId, mStateCallback, null);

        StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        setOptimalResolution(map.getOutputSizes(MediaRecorder.class));
        mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);

        setPreviewSize(selectedSize);

      } catch (CameraAccessException e) {
        e.printStackTrace();
      }
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  public void startPreview(SurfaceHolder holder) {
    if (isBackCamera) {
      this.startCamera(0);  
    } else {
      this.startCamera(1);
    }
  }

  @Override
  public void surfaceDestroyed(SurfaceHolder holder) {
    stopCamera();
  }

  public void stopCamera() {
    if (null != mCaptureSession) {
      mCaptureSession.close();
      mCaptureSession = null;
    }
    if (null != mCameraDevice) {
      mCameraDevice.close();
      mCameraDevice = null;
    }

    if (null != mMediaRecorder) {
      mMediaRecorder.release();
      mMediaRecorder = null;
      recording = false;
    }
  }

  public void switchView() {
    isFlashOn = false;
    stopCamera();
    this.isBackCamera = !this.isBackCamera;
    startPreview(null);

    if(this.isBackCamera) {
      setFlashButtons(true, false);
    } else {
      setFlashButtons(false, false);
    }
  }

  public void stopRecording(boolean finished) {
    if (recording == true) {
      recording = false;
      stopRecordingButton.setVisibility(View.GONE);
      timerText.setText("3:00");
      timerText.setTextColor(Color.parseColor("#FFFFFF"));
      recordingDot.setVisibility(View.GONE);
      try{
        mMediaRecorder.stop();
      }catch(RuntimeException stopException){
        // media didn't record anything (only can happen on onResume)
      }
      currentCounter.cancel();
      refreshLibrary();
      mMediaRecorder = null;

      if (!finished) {
        startRecordingButton.setVisibility(View.VISIBLE);
        cancelRecordingButton.setVisibility(View.VISIBLE);
        if (hasFrontCamera()) {
          switchViewButton.setVisibility(View.VISIBLE);  
        }
      }
      
      if (finished) {
        stopCamera();
        Intent data = new Intent();
        data.putExtra("videoUrl", currentFileName.toString());
        setResult(RESULT_OK, data);
        finish();
      } else {
        File file = new File(currentFileName.toString());
        file.delete();
      }
    }
  }

  private void setOptimalResolution(Size[] choices) {
    if(selectedSize != null) {
      return;
    }

    for (Size size : choices) {
      if (size.getHeight() <= 480) {
        if(selectedSize == null) {
          selectedSize = size;
        } else if(size.getHeight() >= selectedSize.getHeight() && size.getWidth() < selectedSize.getWidth()) {
          selectedSize = size;
        }
        //preferred size width
        if(size.getWidth() == 640 && size.getHeight() == 480) {
          selectedSize = size;
          return;
        }
      }
    }
  }

  private void closePreviewSession() {
    if (mCaptureSession != null) {
      mCaptureSession.close();
      mCaptureSession = null;
    }
  }

  public void startRecording() {
    setFlashButtons(false, false);

    if (null == mCameraDevice) {
      return;
    }
    try {
      if (!hasEnoughtSpace()) {
        showToast("ERROR_STORAGE");
        return;
      }

      closePreviewSession();
      initRecorder();

      cancelRecordingButton.setVisibility(View.GONE);
      startRecordingButton.setVisibility(View.GONE);
      stopRecordingButton.setEnabled(false);
      stopRecordingButton.setVisibility(View.VISIBLE);
      switchViewButton.setVisibility(View.GONE);

      mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
      List<Surface> surfaces = new ArrayList<Surface>();
      surfaces.add(mSurfaceHolder.getSurface());
      mPreviewRequestBuilder.addTarget(mSurfaceHolder.getSurface());
      // Set up Surface for the MediaRecorder
      Surface recorderSurface = mMediaRecorder.getSurface();
      surfaces.add(recorderSurface);
      mPreviewRequestBuilder.addTarget(recorderSurface);

      mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
          mCaptureSession = cameraCaptureSession;
          updatePreview();
          recording = true;
          mMediaRecorder.start();
          startTimer();
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
          showToast("Failed");
        }
      }, null);
    } catch (CameraAccessException e) {
      showToast("ERROR_GENERAL");
    } catch (IOException e) {
      showToast("ERROR_GENERAL");
    }
  }

  public boolean hasFrontCamera() {
    CameraManager manager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
    try {
      return manager.getCameraIdList().length > 1;
    } catch (CameraAccessException e) {
      return false;
    }
  }

  public void switchFlash() {
    isFlashOn = !isFlashOn;
    stopCamera();
    this.startCamera(0);
    if (isFlashOn) {
      setFlashButtons(false, true);
    } else {
      setFlashButtons(true, false);
    }
  }

  public void cancelRecordingProcess() {
    stopRecording(false);
    stopCamera();
    Intent data = new Intent();
    setResult(RESULT_CANCELED, data);
    finish();
  }

  private void initRecorder() throws IOException {
    if (mMediaRecorder == null) {
      mMediaRecorder = new MediaRecorder();
    }

    mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
    mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
    mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4); 
    mMediaRecorder.setOutputFile(initFile().getAbsolutePath());
    mMediaRecorder.setVideoEncodingBitRate(REC_VIDEO_BITRATE);
    mMediaRecorder.setVideoFrameRate(REC_FPS);
    mMediaRecorder.setVideoSize(selectedSize.getWidth(), selectedSize.getHeight());
    mMediaRecorder.setAudioEncodingBitRate(REC_AUDIO_BITRATE);
    mMediaRecorder.setAudioSamplingRate(REC_AUDIO_SAMPLE_RATE);
    mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
    mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
    mMediaRecorder.setOrientationHint(getRecordingAngle());
    mMediaRecorder.setMaxDuration(REC_MAX_DURATION);

    try {
      mMediaRecorder.prepare();
    } catch (IllegalStateException e) {
      e.printStackTrace();
    }
  }

  private int getRecordingAngle() {
    int angle = 0;
    switch(rotation) {
      case Surface.ROTATION_0:
        angle = 0;
        break;
      case Surface.ROTATION_90:
        angle = 90;
        break;
      case Surface.ROTATION_180:
        angle = 180;
        break;
      case Surface.ROTATION_270:
        angle = 270;
        break;
    }
    if (!this.isBackCamera) {
      angle = (mSensorOrientation + angle) % 360;
    } else {
      angle = (mSensorOrientation - angle + 360) % 360;
    }
    return angle;
  }
}