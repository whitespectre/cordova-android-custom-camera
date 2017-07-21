package com.cga;

import com.cga.FakeR;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;

import java.io.IOException;
import java.lang.Exception;
import android.util.Log;
import android.hardware.Camera;
import android.view.SurfaceView;
import android.view.SurfaceHolder;
import java.lang.RuntimeException;
import android.media.MediaRecorder;
import java.io.File;
import android.view.View;
import android.view.Surface;
import android.content.res.Configuration;
import android.content.pm.ActivityInfo;
import android.content.Intent;
import android.view.Display;
import android.graphics.Point;
import java.util.List;
import android.media.CamcorderProfile;
import android.graphics.Color;
import android.os.Build;


public class CustomCameraActivity extends BaseCustomActivity implements SurfaceHolder.Callback {
  
  private Camera.Size selectedSize = null;
  Camera camera;
  
  private final String TAG = "CustomCameraActivity";

  protected boolean hasFrontCamera() {
    return camera.getNumberOfCameras() > 1;
  }

  public boolean isSensorRotated(boolean isBackCamera) {
    if (isBackCamera) {
      return false;
    }

    String deviceModel = Build.MODEL.toLowerCase();
    if (deviceModel.contains("nexus 6p")) {
      return true;
    }
    return false;
  }

  public boolean isBackSensorRotated(boolean isBackCamera) {
    if (!isBackCamera) {
      return false;
    }

    String deviceModel = Build.MODEL.toLowerCase();
    if (deviceModel.contains("nexus 5x")) {
      return true;
    }
    return false;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    init();
  }

  public void init() {
    super.init();
    mSurfaceView.setVisibility(View.VISIBLE);
    mSurfaceHolder.addCallback(this);
  }


  protected void cancelRecordingProcess() {
    stopRecording(false);
    stopCamera();
    Intent data = new Intent();
    setResult(RESULT_CANCELED, data);
    finish();
  }

  protected void startRecording() {
    try {
      if (!hasEnoughtSpace()) {
        showToast("ERROR_STORAGE");
        return;
      }
      initRecorder(mSurfaceHolder.getSurface());
      setFlashButtons(false, false);
    } catch (IOException e) {
      showToast("ERROR_GENERAL");
      return;
    }
    mMediaRecorder.start();
    startTimer();
    recording = true;

    cancelRecordingButton.setVisibility(View.GONE);
    startRecordingButton.setVisibility(View.GONE);
    stopRecordingButton.setEnabled(false);
    stopRecordingButton.setVisibility(View.VISIBLE);
    switchViewButton.setVisibility(View.GONE);
  }

  protected void stopRecording(boolean finished) {
    if (recording == true) {
      recording = false;
      stopRecordingButton.setVisibility(View.GONE);
      timerText.setText("3:00");
      timerText.setTextColor(Color.parseColor("#FFFFFF"));
      recordingDot.setVisibility(View.GONE);
      setFlashButtons(true, false);
      if (hasFrontCamera()) {
        switchViewButton.setVisibility(View.VISIBLE);
      }
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

  @Override
  public void onPause() {
    super.onPause();
    stopRecording(false);
    if(camera != null ) {
      camera.stopPreview();
      if(isBackCamera) {
        setFlashButtons(true, false);  
      }
    }
  }


  @Override
  public void onResume() {
    super.onResume();
    if (isBackCamera) {
      this.startCamera(0, false);  
    } else {
      this.startCamera(1, false);
    }
    
    startPreview(mSurfaceView.getHolder());
  }

  public void surfaceChanged(SurfaceHolder holder, int arg1, int arg2, int arg3) {
      startPreview(holder);
  }

  public void surfaceDestroyed(SurfaceHolder holder) {
    if (mMediaRecorder != null) {
      mMediaRecorder.stop();
      File file = new File(currentFileName.toString());
      file.delete();
    }
    if(camera != null) {
      camera.stopPreview();
      camera.release();  
    }
  }

  protected void switchView() {
    stopCamera();
    if (this.isBackCamera) {
      this.isBackCamera = !this.isBackCamera;
      this.startCamera(1, false);
      setFlashButtons(false, false);
    } else {
      this.isBackCamera = !this.isBackCamera;
      this.startCamera(0, false);
      setFlashButtons(true, false);
    }
    
    startPreview(mSurfaceView.getHolder());
  }

  public void switchFlash() {
    stopCamera();
    this.startCamera(0, !isFlashOn);
    startPreview(mSurfaceView.getHolder());
    if (isFlashOn) {
      setFlashButtons(false, true);
    } else {
      setFlashButtons(true, false);
    }
  }

  private void setCameraOrientation() {
    int rotation = getWindowManager().getDefaultDisplay().getRotation();
    if (isSensorRotated(this.isBackCamera) ||
      isBackSensorRotated(this.isBackCamera)) {
      camera.setDisplayOrientation(270);
    } else {
      camera.setDisplayOrientation(90);
    }
    camera.startPreview();
  }

  private void startCamera(int cameraView, boolean isFlashOn)
  {
    this.isFlashOn = isFlashOn;
    try{
        camera = Camera.open(cameraView);
        setOptimalResolution();
    }catch(RuntimeException e){
      showToast("ERROR_GENERAL");
      Intent data = new Intent();
      setResult(5, data);
      finish();
      return;
    }
    Camera.Parameters param = camera.getParameters();
    param.setRecordingHint(true);
    if (isFlashOn) {
      param.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);  
    }
    if (param.getSupportedFocusModes().contains(
        Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
      param.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
    }
    
    setCameraOrientation();
    camera.setParameters(param);

    setPreviewSize(param.getPreviewSize());
  }

  protected void startPreview(SurfaceHolder holder) {
    if(camera == null) {
      return;
    }
    
    try {
      camera.stopPreview();
    } catch (Exception e){}

    try {
      camera.setPreviewDisplay(holder);
      camera.startPreview();
    } catch (IOException e) {}
  }

  protected void setPreviewSize(Camera.Size camSize) {
    Display display = getWindowManager().getDefaultDisplay();
    Point size = new Point();
    display.getSize(size);

    double height = ((double)camSize.width/ camSize.height) * size.x;
    mSurfaceView.getHolder().setFixedSize(size.x, (int)height);
  }

  @Override
  public void surfaceCreated(SurfaceHolder holder) {
    startPreview(holder);   
  }

  protected void stopCamera()
  {
    camera.stopPreview();
    camera.setPreviewCallback(null);
    camera.release();
    camera = null;
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    setCameraOrientation();
  }

  private void setOptimalResolution() {
    if(selectedSize != null) {
      return;
    }

    Camera.Parameters param = camera.getParameters();
    List<Camera.Size> listSizes = param.getSupportedVideoSizes();

    for (Camera.Size size : listSizes) {
      if (size.height <= 480) {
        if(selectedSize == null) {
          selectedSize = size;
        } else if(size.height >= selectedSize.height && size.width < selectedSize.width) {
          selectedSize = size;
        }
        //preferred size width
        if(size.width == 640 && size.height == 480) {
          selectedSize = size;
          return;
        }
      }
    }
  }

  private void initRecorder(Surface surface) throws IOException {
    camera.unlock();

    if (mMediaRecorder == null) {
      mMediaRecorder = new MediaRecorder();
    }

    mMediaRecorder.setPreviewDisplay(surface);
    mMediaRecorder.setCamera(camera);
    mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
    mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
    mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4); 
    mMediaRecorder.setOutputFile(this.initFile().getAbsolutePath());
    mMediaRecorder.setMaxDuration(REC_MAX_DURATION);
    mMediaRecorder.setVideoFrameRate(REC_FPS);
    mMediaRecorder.setVideoSize(selectedSize.width, selectedSize.height);
    mMediaRecorder.setVideoEncodingBitRate(REC_VIDEO_BITRATE);
    mMediaRecorder.setAudioEncodingBitRate(REC_AUDIO_BITRATE);
    mMediaRecorder.setAudioSamplingRate(REC_AUDIO_SAMPLE_RATE); 
    mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
    mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
    mMediaRecorder.setOrientationHint(getRecordingAngle());

    try {
      mMediaRecorder.prepare();
    } catch (IllegalStateException e) {
      e.printStackTrace();
    }
  }

  private int getRecordingAngle() {
    int angle = 0;
    switch(rotation) {
    case Surface.ROTATION_180:
      angle = 90;
      if (isSensorRotated(this.isBackCamera) ||
        isBackSensorRotated(this.isBackCamera)) {
        angle = 270;
      }
      break;
    case Surface.ROTATION_270:
      angle = 180;
      if (isSensorRotated(this.isBackCamera) || 
        isBackSensorRotated(this.isBackCamera)) {
        angle = 0;
      }
      break;
    case Surface.ROTATION_0:
      angle = 90;
      if (!this.isBackCamera) {
        if(isSensorRotated(false)) {
          angle = 90;  
        } else {
          angle = 270;
        }
      } else if (isBackSensorRotated(this.isBackCamera)) {
        angle = 270;
      }
      break;
    case Surface.ROTATION_90:
      angle = 0;
      if (isBackSensorRotated(this.isBackCamera) ||
        isSensorRotated(this.isBackCamera)) {
        angle = 180;
      }
      break;
    }
    return angle;
  }
}