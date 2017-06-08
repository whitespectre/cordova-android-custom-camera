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
import android.view.Display;
import android.graphics.Point;
import java.lang.Math;
import java.util.List;

public class CustomCameraActivity extends Activity implements SurfaceHolder.Callback {
  private FakeR fakeR;
  private MediaRecorder mediaRecorder;
  private boolean recording = false;
  private Uri currentFileName;
  private ImageButton switchViewButton;
  private ImageButton switchFlashButton;
  private ImageButton flashOffButton;
  private ImageButton startRecordingButton;
  private ImageButton stopRecordingButton;
  private Button cancelRecordingButton;
  private TextView timerText;
  private CountDownTimer currentCounter;
  private View recordingDot;
  private boolean isFlashOn = false;
  private boolean isBackCamera = false;
  private OrientationListener orientationListener;
  private int rotation;
  
  Camera camera;
  SurfaceView surfaceView;
  SurfaceHolder surfaceHolder;  
  private final String TAG = "CustomCameraActivity";

  public boolean hasFrontCamera() {
    return camera.getNumberOfCameras() > 1;
  }

  public void showToast(String key) {
    String tooltip = getIntent().getStringExtra(key);
    Toast.makeText(this, tooltip, Toast.LENGTH_LONG).show();
  }

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
        stopRecording(true);
      }
    });

    startRecordingButton = (ImageButton) findViewById(fakeR.getId("id", "recordingButton"));
    startRecordingButton.setOnClickListener( new OnClickListener() {
      @Override
      public void onClick(View v) {            
        startRecording();
      }
    });

    String paramCancelText = getIntent().getStringExtra("CANCEL_TEXT");
    cancelRecordingButton = (Button) findViewById(fakeR.getId("id", "cancelRecording"));
    cancelRecordingButton.setText(paramCancelText);
    cancelRecordingButton.setOnClickListener( new OnClickListener() {
      @Override
      public void onClick(View v) {            
        cancelRecordingProcess();
      }
    });

    // init camera
    showToast("TOOLTIP");
    if (!hasFrontCamera()) {
      this.startCamera(0, false);
      isBackCamera = true;
      switchViewButton.setVisibility(View.GONE);
    } else {
      this.startCamera(1, false);
      isBackCamera = false;
      setFlashButtons(false, false);
    }
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

  public void cancelRecordingProcess() {
    stopRecording(false);
    camera.stopPreview();
    Intent data = new Intent();
    setResult(RESULT_CANCELED, data);
    finish();
  }

  public void startRecording() {
    try {
      if (!hasEnoughtSpace()) {
        showToast("ERROR_STORAGE");
        return;
      }
      initRecorder(surfaceHolder.getSurface());
      setFlashButtons(false, false);
    } catch (IOException e) {
      showToast("ERROR_GENERAL");
      return;
    }
    mediaRecorder.start();
    startTimer();
    recording = true;

    cancelRecordingButton.setVisibility(View.GONE);
    startRecordingButton.setVisibility(View.GONE);
    stopRecordingButton.setVisibility(View.VISIBLE);
    switchViewButton.setVisibility(View.GONE);
  }

  public void stopRecording(boolean finished) {
    if (recording == true) {
      timerText.setText("3:00");
      recordingDot.setVisibility(View.GONE);
      setFlashButtons(true, false);
      if (hasFrontCamera()) {
        switchViewButton.setVisibility(View.VISIBLE);
      }
      mediaRecorder.stop();
      currentCounter.cancel();
      refreshLibrary();
      recording = false;
      mediaRecorder = null;
      // show buttons
      stopRecordingButton.setVisibility(View.GONE);
      startRecordingButton.setVisibility(View.VISIBLE);
      cancelRecordingButton.setVisibility(View.VISIBLE);

      if (finished) {
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
    if (flashOn) {
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

  public void surfaceChanged(SurfaceHolder holder, int arg1, int arg2, int arg3) {
      startPreview(holder);
  }

  public void surfaceDestroyed(SurfaceHolder holder) {
    if (mediaRecorder != null) {
      mediaRecorder.stop();
      File file = new File(currentFileName.toString());
      file.delete();
    }
    camera.stopPreview();
    camera.release();
  }

  private void switchView() {
    stopCamera();
    if (this.isBackCamera) {
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
    if (isFlashOn) {
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
    camera.setDisplayOrientation(90);
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
    param.setRecordingHint(true);
    if (isFlashOn) {
      param.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);  
    }
    if (param.getSupportedFocusModes().contains(
        Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
      param.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
    }
    setPreviewSize();
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

  protected void setPreviewSize() {
    Camera.Parameters param =  camera.getParameters();
    Camera.Size previewSize = param.getPictureSize();
    Display display = getWindowManager().getDefaultDisplay();
    Point size = new Point();
    display.getSize(size);

    double height = ((double)previewSize.width/previewSize.height) * size.x;
    surfaceView.getHolder().setFixedSize(size.x, (int)height);
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

  private File initFile() {
    String libraryFolder = getIntent().getStringExtra("LIBRARY_FOLDER");
    File dir = new File(Environment.getExternalStorageDirectory(), libraryFolder);
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

  private boolean hasEnoughtSpace() throws IOException {
    String libraryFolder = getIntent().getStringExtra("LIBRARY_FOLDER");
    File dir = new File(Environment.getExternalStorageDirectory(), libraryFolder);
    File file = null;

    if (!dir.exists() && !dir.mkdirs()) {
      file = null;
    } else {
      file = new File(dir.getAbsolutePath(), "tempFile");
      if(!file.exists()) {
        file.createNewFile();
      }
      return file.getFreeSpace() > 62914560;
    }

    return false;
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
    mediaRecorder.setVideoFrameRate(60);
    mediaRecorder.setVideoSize(720, 480);
    mediaRecorder.setVideoEncodingBitRate(3000000);
    mediaRecorder.setAudioEncodingBitRate(8000);

    mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);
    mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
    mediaRecorder.setOrientationHint(getRecordingAngle());

    try {
      mediaRecorder.prepare();
    } catch (IllegalStateException e) {
      e.printStackTrace();
    }
  }

  private int getRecordingAngle() {
    int angle = 0;
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
      if( (orientation < 35 || orientation > 325) && rotation != Surface.ROTATION_0){
        rotation = Surface.ROTATION_0;
      } else if( orientation > 145 && orientation < 215 && rotation != Surface.ROTATION_180){
        rotation = Surface.ROTATION_180;
      } else if(orientation > 55 && orientation < 125 && rotation != Surface.ROTATION_270){
        rotation = Surface.ROTATION_270;
      } else if(orientation > 235 && orientation < 305 && rotation != Surface.ROTATION_90){
        rotation = Surface.ROTATION_90;
      }
    }
  }
}