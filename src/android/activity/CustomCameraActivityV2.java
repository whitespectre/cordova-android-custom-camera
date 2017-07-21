package com.cga;

import com.cga.AutoFitTextureView;
import com.cga.FakeR;
import android.app.Activity;
import android.os.Bundle;
import java.io.IOException;
import java.lang.Exception;
import android.util.Log;
import java.util.Arrays;
import android.view.SurfaceHolder;
import java.lang.RuntimeException;
import java.io.File;
import android.view.View;
import android.view.Surface;
import android.content.Intent;
import android.content.Context;
import android.view.Display;
import java.util.List;
import java.util.ArrayList;
import android.graphics.Color;
import android.util.Size;
import android.graphics.SurfaceTexture;
import android.util.SizeF;
import android.os.HandlerThread;
import android.os.Handler;
import android.view.TextureView;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraMetadata;
import android.support.annotation.NonNull;
import android.hardware.camera2.CameraCaptureSession;
import android.media.MediaRecorder;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CustomCameraActivityV2 extends BaseCustomActivity {
  private final String TAG = "CustomCameraActivityV2";
  private AutoFitTextureView mTextureView;
  // camera 2 variables
  private String mCameraId;
  private CameraDevice mCameraDevice;
  private CaptureRequest.Builder mPreviewRequestBuilder;
  private CameraCaptureSession mCaptureSession;
  private CaptureRequest mPreviewRequest;
  private Size selectedSize;
  private Integer mSensorOrientation;
  // threading for perf
  private HandlerThread mBackgroundThread;
  private Handler mBackgroundHandler;
  private Semaphore mCameraOpenCloseLock = new Semaphore(1);
  // container of video preview
  private TextureView.SurfaceTextureListener mSurfaceTextureListener
          = new TextureView.SurfaceTextureListener() {
      @Override
      public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture,
                                            int width, int height) {
        startPreview(width, height);
      }

      @Override
      public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture,
                                              int width, int height) {}
      
      @Override
      public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {}

      @Override
      public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        stopCamera();
        return true;
      }
  };
  
  private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
    @Override
    public void onOpened(@NonNull CameraDevice cameraDevice) {
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

  private void startBackgroundThread() {
    mBackgroundThread = new HandlerThread("CameraBackground");
    mBackgroundThread.start();
    mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
  }

  private void stopBackgroundThread() {
    mBackgroundThread.quitSafely();
    try {
      mBackgroundThread.join();
      mBackgroundThread = null;
      mBackgroundHandler = null;
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    init();
  }

  public void init() {
    super.init();
    // with camera 2 we will be using AutoFixTextureView to preserve aspect ratio
    mTextureView = (AutoFitTextureView) findViewById(fakeR.getId("id", "texture"));
    mTextureView.setVisibility(View.VISIBLE);
  }

  @Override
  public void onResume() {
    super.onResume();
    startBackgroundThread();
    // where camera gets init
    if (mTextureView.isAvailable()) {
        startPreview(mTextureView.getWidth(), mTextureView.getHeight());
      } else {
        mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
      }
    
  }

  @Override
  public void onPause() {
    super.onPause();
    isFlashOn = false;
    stopRecording(false);
    stopCamera();
    stopBackgroundThread();
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

  private void updatePreview(CameraCaptureSession.CaptureCallback callback) {
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
      mCaptureSession.setRepeatingRequest(mPreviewRequest, callback, mBackgroundHandler);
    } catch (CameraAccessException e) {
        e.printStackTrace();
    }
  }

  private void createCameraPreviewSession() {
    try {
      mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
      
      SurfaceTexture texture = mTextureView.getSurfaceTexture();
      texture.setDefaultBufferSize(selectedSize.getWidth(), selectedSize.getHeight());
      Surface previewSurface = new Surface(texture); 
      mPreviewRequestBuilder.addTarget(previewSurface);

      mCameraDevice.createCaptureSession(Arrays.asList(previewSurface),
        new CameraCaptureSession.StateCallback() {
          @Override
          public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
            mCaptureSession = cameraCaptureSession;
            updatePreview(null);
          }

          @Override
          public void onConfigureFailed(
                  @NonNull CameraCaptureSession cameraCaptureSession) {
              showToast("Failed");
          }
        }, mBackgroundHandler);
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
        if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
          throw new RuntimeException("Time out waiting to lock camera opening.");
        }
        manager.openCamera(mCameraId, mStateCallback, null);

        StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        Size selectedSize = chooseOptimalResolution(map.getOutputSizes(MediaRecorder.class));
        mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        mTextureView.setAspectRatio(selectedSize.getHeight(), selectedSize.getWidth());
      } catch (CameraAccessException e) {
        Intent data = new Intent();
        e.printStackTrace();
        reportError();
      }
    } catch(Exception e) {
      e.printStackTrace();
      reportError();
    }
  }

  protected void startPreview(SurfaceHolder holder) {
    if (isBackCamera) {
      this.startCamera(0);  
    } else {
      this.startCamera(1);
    }
  }

  public void startPreview(int width, int height) {
    if (isBackCamera) {
      this.startCamera(0);  
    } else {
      this.startCamera(1);
    }
  }

  protected void stopCamera() {
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

      if (null != mMediaRecorder) {
        mMediaRecorder.release();
        mMediaRecorder = null;
        recording = false;
      }
    } catch (InterruptedException e) {
      throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
    } finally {
      mCameraOpenCloseLock.release();
    }
  }

  protected void switchView() {
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

  protected void stopRecording(boolean finished) {
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

  private Size chooseOptimalResolution(Size[] choices) {
    if(selectedSize != null) {
      return selectedSize;
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
          return selectedSize;
        }
      }
    }
    return selectedSize;
  }

  private void closePreviewSession() {
    if (mCaptureSession != null) {
      mCaptureSession.close();
      mCaptureSession = null;
    }
  }

  protected void startRecording() {
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

      SurfaceTexture texture = mTextureView.getSurfaceTexture();
      texture.setDefaultBufferSize(selectedSize.getWidth(), selectedSize.getHeight());

      List<Surface> surfaces = new ArrayList<Surface>();
      
      Surface previewSurface = new Surface(texture);
      surfaces.add(previewSurface);
      mPreviewRequestBuilder.addTarget(previewSurface);

      // Set up Surface for the MediaRecorder
      Surface recorderSurface = mMediaRecorder.getSurface();
      surfaces.add(recorderSurface);
      mPreviewRequestBuilder.addTarget(recorderSurface);

      mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
          mCaptureSession = cameraCaptureSession;
          updatePreview(null);

          CustomCameraActivityV2.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
              recording = true;
              mMediaRecorder.start();
              startTimer();
            }
          });
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
          showToast("ERROR_GENERAL");
        }
      }, mBackgroundHandler);
    } catch (CameraAccessException e) {
      showToast("ERROR_GENERAL");
    } catch (IOException e) {
      showToast("ERROR_GENERAL");
    }
  }

  protected boolean hasFrontCamera() {
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

  protected void cancelRecordingProcess() {
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