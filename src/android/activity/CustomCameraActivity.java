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

public class CustomCameraActivity extends Activity implements SurfaceHolder.Callback {
  private FakeR fakeR;
  private MediaRecorder mediaRecorder;
  private boolean initSuccessfull;
  private boolean recording = false;
  private int currentOrientation;
  private Uri currentFileName;
  private ImageButton switchViewButton;
  private ImageButton switchFlashButton;
  private ImageButton flashOffButton;
  private ImageButton startRecordingButton;
  private ImageButton stopRecordingButton;
  private int currentView = 0;
  private TextView timerText;
  private CountDownTimer currentCounter;
  private View recordingDot;
  private boolean isFlashOn = false;
  private boolean isBackCamera = true;
  private OrientationListener orientationListener;
  private int rotation;

  Camera camera;
  SurfaceView surfaceView;
  SurfaceHolder surfaceHolder;  
  private final String TAG = "CustomCameraActivity";


  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    fakeR = new FakeR(this);
    setContentView(fakeR.getId("layout", "custom_camera"));

    recordingDot = (View)findViewById(fakeR.getId("id", "recordingDot"));

    surfaceView = (SurfaceView)findViewById(fakeR.getId("id", "surfaceView"));
    surfaceHolder = surfaceView.getHolder();
    surfaceHolder.addCallback(this);
    surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    
    this.startCamera(0, false);

    //orientation listener
    orientationListener = new OrientationListener(this);

    //init buttons
    timerText = (TextView) findViewById(fakeR.getId("id", "recordingTimer"));
    switchViewButton = (ImageButton) findViewById(fakeR.getId("id", "switchViewButton"));
    switchViewButton.setOnClickListener( new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!recording) {
              switchView(); 
            }
        }
    });

    flashOffButton = (ImageButton) findViewById(fakeR.getId("id", "flashOffButton"));
    flashOffButton.setOnClickListener( new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!recording) {
              switchFlash(); 
            }
        }
    });

    switchFlashButton = (ImageButton) findViewById(fakeR.getId("id", "flashButton"));
    switchFlashButton.setOnClickListener( new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!recording) {
              switchFlash(); 
            }
        }
    });

    
    stopRecordingButton = (ImageButton) findViewById(fakeR.getId("id", "stopRecordingButton"));
    stopRecordingButton.setOnClickListener( new OnClickListener() {
      @Override
      public void onClick(View v) {
        stopRecording(false);
      }
    });

    startRecordingButton = (ImageButton) findViewById(fakeR.getId("id", "recordingButton"));
    startRecordingButton.setOnClickListener( new OnClickListener() {
      @Override
      public void onClick(View v) {            
        setFlashButtons(false, false);
        try {
            initRecorder(surfaceHolder.getSurface());
        } catch (IOException e) {
            e.printStackTrace();
        }
        mediaRecorder.start();
        startTimer();
        recording = true;
        // show buttons
        startRecordingButton.setVisibility(View.GONE);
        stopRecordingButton.setVisibility(View.VISIBLE);         
      }
    });

  }

  @Override
  public void onStart() {
    orientationListener.enable();
    super.onStart();
  }

  @Override 
  protected void onStop() {
    orientationListener.disable();
    super.onStop();
  }

  public void stopRecording(boolean deleteFile) {
    if(recording == true) {
      timerText.setText("3:00");
      recordingDot.setVisibility(View.GONE);
      setFlashButtons(true, false);
      switchViewButton.setVisibility(View.VISIBLE);
      mediaRecorder.stop();
      currentCounter.cancel();
      refreshLibrary();
      recording = false;
      mediaRecorder = null;
      // show buttons
      stopRecordingButton.setVisibility(View.GONE);
      startRecordingButton.setVisibility(View.VISIBLE);
    }
  }

  @Override
  public void onPause() {
    super.onPause();
    stopRecording(true);
    camera.stopPreview();
    setFlashButtons(true, false);
  }

  @Override
  public void onResume() {
    super.onResume();
    this.startCamera(0, false);
    startPreview(surfaceView.getHolder());
  }

  private void setFlashButtons(boolean flashOn, boolean flashOff) {
    if(flashOn) {
      switchFlashButton.setVisibility(View.VISIBLE);      
    } else {
      switchFlashButton.setVisibility(View.GONE);
    }

    if(flashOff) {
      flashOffButton.setVisibility(View.VISIBLE);
    } else {
      flashOffButton.setVisibility(View.GONE);
    }
  }

  private void switchView() {
    stopCamera();
    if(this.isBackCamera) {
      this.startCamera(1, false);
      setFlashButtons(false, false);
    } else {
      this.startCamera(0, false);
      setFlashButtons(true, false);
    }
    this.isBackCamera = !this.isBackCamera;
    startPreview(surfaceView.getHolder());
  }

  public void switchFlash() {
    stopCamera();
    this.startCamera(0, !isFlashOn);
    startPreview(surfaceView.getHolder());
    if(isFlashOn) {
      setFlashButtons(false, true);
    } else {
      setFlashButtons(true, false);
    }
  }

  public void startTimer() {
    currentCounter = new CountDownTimer(180000, 500) {
       public void onTick(long millisUntilFinished) {

          if (recordingDot.getVisibility() == View.VISIBLE) {
            recordingDot.setVisibility(View.INVISIBLE);
          } else {
            recordingDot.setVisibility(View.VISIBLE);
          }

          String timeFormat = String.format("%d:%02d", 
            TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) -  
            TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millisUntilFinished)),
            TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) - 
            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished))); 
           timerText.setText(timeFormat);
       }

       public void onFinish() {
          stopRecordingButton.performClick();
       }
    };
    currentCounter.start();
  }

  private void setCameraOrientation() {
    int rotation = getWindowManager().getDefaultDisplay().getRotation();
    camera.stopPreview();
    switch(rotation) {
    case Surface.ROTATION_180:
        camera.setDisplayOrientation(90);
        break;
    case Surface.ROTATION_270:
        // right rotate
        camera.setDisplayOrientation(180);
        break;
    case  Surface.ROTATION_0:
        // left portrait
        camera.setDisplayOrientation(90);
        break;
    case Surface.ROTATION_90:
        // left rotate
        camera.setDisplayOrientation(0);
        break;
    }
    camera.startPreview();
  }

  private void startCamera(int cameraView, boolean isFlashOn)
  {
      this.isFlashOn = isFlashOn;
      try{
          camera = Camera.open(cameraView);
      }catch(RuntimeException e){
        return;
      }
      Camera.Parameters param = camera.getParameters();
      if (isFlashOn) {
        param.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);  
      }
      if (param.getSupportedFocusModes().contains(
          Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
        param.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
      }
      setCameraOrientation();
      camera.setParameters(param);
      
  }

  public void startPreview(SurfaceHolder holder) {
    try {
          camera.stopPreview();
    } catch (Exception e){}

    try {
        camera.setPreviewDisplay(holder);
        camera.startPreview();
    } catch (IOException e) {}
  }

  @Override
  public void surfaceCreated(SurfaceHolder holder) {
    startPreview(holder);   
  }

  private void stopCamera()
  {
      camera.stopPreview();
      camera.release();
  }

  public void surfaceChanged(SurfaceHolder holder, int arg1, int arg2, int arg3) {
      startPreview(holder);
  }


  public void surfaceDestroyed(SurfaceHolder holder) {
    if(mediaRecorder != null) {
      mediaRecorder.stop();
      File file = new File(currentFileName.toString());
      file.delete();
    }
    camera.stopPreview();
    camera.release();
  }

  private File initFile() {
      File dir = new File(Environment.getExternalStorageDirectory(), "My Challenge Tracker");
      File file = null;

      if (!dir.exists() && !dir.mkdirs()) {
          file = null;
      } else {
          file = new File(dir.getAbsolutePath(), new SimpleDateFormat(
                  "'IMG_'yyyyMMddHHmmss'.mp4'").format(new Date()));
          currentFileName = Uri.fromFile(file);
      }
      return file;
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    setCameraOrientation();
  }

  private void refreshLibrary() {
    Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,currentFileName);
    sendBroadcast(mediaScanIntent);
  }


  private void initRecorder(Surface surface) throws IOException {
      // It is very important to unlock the camera before doing setCamera
      // or it will results in a black preview
      camera.unlock();

      if (mediaRecorder == null) {
        mediaRecorder = new MediaRecorder();
      }

      mediaRecorder.setPreviewDisplay(surface);
      mediaRecorder.setCamera(camera);
      mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
      mediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
      mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4); 
      mediaRecorder.setOutputFile(this.initFile().getAbsolutePath());

      // No limit. Don't forget to check the space on disk.
      mediaRecorder.setMaxDuration(180000);
      mediaRecorder.setVideoFrameRate(30);
      mediaRecorder.setVideoSize(640, 480);
      mediaRecorder.setVideoEncodingBitRate(3000000);
      mediaRecorder.setAudioEncodingBitRate(8000);

      mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);
      mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
      // 270 para portrait frontal
      // 180 para landscape frontal
      mediaRecorder.setOrientationHint(getRecordingAngle());

      try {
          mediaRecorder.prepare();
      } catch (IllegalStateException e) {
          e.printStackTrace();
      }

      initSuccessfull = true;
  }

  private int getRecordingAngle() {
    int angle = 0;
    //int rotation = getWindowManager().getDefaultDisplay().getRotation();
    switch(rotation) {
    case Surface.ROTATION_180:
        angle = 90;
        break;
    case Surface.ROTATION_270:
        angle = 180;
        break;
    case Surface.ROTATION_0:
        angle = 90;
        if (!this.isBackCamera) {
          angle = 270;
        }
        break;
    case Surface.ROTATION_90:
        angle = 0;
        break;
    }
    return angle;
  }

  private class OrientationListener extends OrientationEventListener{
    public OrientationListener(Context context) { super(context); }

    @Override public void onOrientationChanged(int orientation) {
        if( (orientation < 35 || orientation > 325) && rotation != Surface.ROTATION_0){ // PORTRAIT
            rotation = Surface.ROTATION_0;
            Log.d("orientation", "***************************** ROTATION_O");
        }
        else if( orientation > 145 && orientation < 215 && rotation != Surface.ROTATION_180){ // REVERSE PORTRAIT
            rotation = Surface.ROTATION_180;
            Log.d("orientation", "***************************** ROTATION_180");
        }
        else if(orientation > 55 && orientation < 125 && rotation != Surface.ROTATION_270){ // REVERSE LANDSCAPE
            rotation = Surface.ROTATION_270;
            Log.d("orientation", "***************************** ROTATION_270");
        }
        else if(orientation > 235 && orientation < 305 && rotation != Surface.ROTATION_90){ //LANDSCAPE
            rotation = Surface.ROTATION_90;
            Log.d("orientation", "***************************** ROTATION_90");
        }
    }
  }
}