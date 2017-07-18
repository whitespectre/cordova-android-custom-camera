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

public abstract class BaseCustomActivity extends Activity implements SurfaceHolder.Callback {
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
  // recording
  protected MediaRecorder mMediaRecorder;
  protected boolean isBackCamera = false;
  protected Uri currentFileName;

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

  abstract void stopCamera();
  abstract void stopRecording(boolean finished);
  abstract void startRecording();
  abstract void switchView();
  abstract void startPreview(SurfaceHolder holder);
  abstract void switchFlash();
  abstract void cancelRecordingProcess();

  public void surfaceCreated(SurfaceHolder holder) {
    startPreview(holder);
  }

  public void surfaceChanged(SurfaceHolder holder, int arg1, int arg2, int arg3) {
    startPreview(holder);
  }

  public void init() {
    fakeR = new FakeR(this);
    setContentView(fakeR.getId("layout", "custom_camera"));

    mSurfaceView = (SurfaceView)findViewById(fakeR.getId("id", "surfaceView"));
    mSurfaceHolder = mSurfaceView.getHolder();
    mSurfaceHolder.addCallback(this);
    mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

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
}

