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
import android.view.View.OnClickListener;
import android.widget.Button;
import android.view.View;
import android.view.Surface;
import java.util.Date;
import java.text.SimpleDateFormat;
import android.net.Uri;
import android.content.Intent;
import android.os.CountDownTimer;
import android.widget.TextView;
import java.util.concurrent.TimeUnit;
import android.widget.ImageButton;
import android.content.Context;
import android.graphics.Color;
import android.media.MediaRecorder;
import android.os.Environment;
import android.view.OrientationEventListener;
import android.widget.Toast;

public abstract class BaseCustomActivity extends Activity {
  protected FakeR fakeR;
  protected boolean recording = false;
  // UI Buttons
  protected ImageButton switchViewButton;
  protected ImageButton switchFlashButton;
  protected ImageButton flashOffButton;
  protected ImageButton startRecordingButton;
  protected ImageButton stopRecordingButton;
  protected Button cancelRecordingButton;
  protected TextView timerText;
  protected SurfaceView mSurfaceView;
  protected SurfaceHolder mSurfaceHolder;
  protected View recordingDot;
  // recording
  protected MediaRecorder mMediaRecorder;
  protected boolean isBackCamera = false;
  protected Uri currentFileName;
  protected CountDownTimer currentCounter;
  protected int rotation;
  protected boolean isFlashOn = false;
  // recording params
  protected final int REC_MAX_DURATION = 181000;
  protected final int REC_FPS = 30;
  protected final int REC_VIDEO_BITRATE = 1700000;
  protected final int REC_AUDIO_BITRATE = 98000;
  protected final int REC_AUDIO_SAMPLE_RATE = 44100;
  // orientation tracking
  private OrientationListener orientationListener;

  protected void setFlashButtons(boolean flashOn, boolean flashOff) {
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

  protected abstract void stopCamera();
  protected abstract void stopRecording(boolean finished);
  protected abstract void startRecording();
  protected abstract void switchView();
  protected abstract void startPreview(SurfaceHolder holder);
  protected abstract void switchFlash();
  protected abstract void cancelRecordingProcess();
  protected abstract boolean hasFrontCamera();

  public void showToast(String key) {
    String tooltip = getIntent().getStringExtra(key);
    Toast.makeText(this, tooltip, Toast.LENGTH_LONG).show();
  }

  public void init() {
    fakeR = new FakeR(this);
    setContentView(fakeR.getId("layout", "custom_camera"));

    //orientation listener
    orientationListener = new OrientationListener(this);

    mSurfaceView = (SurfaceView)findViewById(fakeR.getId("id", "surfaceView"));
    mSurfaceHolder = mSurfaceView.getHolder();
    mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

    recordingDot = (View)findViewById(fakeR.getId("id", "recordingDot"));
    timerText = (TextView) findViewById(fakeR.getId("id", "recordingTimer"));
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

    String paramCancelText = getIntent().getStringExtra("CANCEL_TEXT");
    cancelRecordingButton = (Button) findViewById(fakeR.getId("id", "cancelRecording"));
    cancelRecordingButton.setText(paramCancelText);
    cancelRecordingButton.setOnClickListener( new OnClickListener() {
      @Override
      public void onClick(View v) {            
        cancelRecordingProcess();
      }
    });

    showToast("TOOLTIP");
    if (!hasFrontCamera()) {
      isBackCamera = true;
      switchViewButton.setVisibility(View.GONE);
    } else {
      isBackCamera = false;
      setFlashButtons(false, false);
    }
  }

  protected boolean hasEnoughtSpace() throws IOException {
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

  protected void refreshLibrary() {
    Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,currentFileName);
    sendBroadcast(mediaScanIntent);
  }

  protected File initFile() {
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

  public void startTimer() {
    currentCounter = new CountDownTimer(REC_MAX_DURATION, 500) {
      public void onTick(long millisUntilFinished) {
        if (millisUntilFinished > 170000 && millisUntilFinished < 177000 ) {
          stopRecordingButton.setEnabled(true);
        }

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
        if(millisUntilFinished <= 10000) {
          timerText.setTextColor(Color.parseColor("#FF0000"));
        }
     }

      public void onFinish() {
        stopRecordingButton.performClick();
      }
    };
    currentCounter.start();
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
}

