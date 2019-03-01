package com.example.nean.whitescreen;

import android.content.Context;
import android.hardware.input.InputManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

import java.io.IOException;

public class SecondaryActivity extends AppCompatActivity {

    private TextView mTextView;
    private MediaPlayer mMediaPlayer;
    private SurfaceView mSurfaceView;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.v(MainActivity.TAG, "Secondary activity text updated!");
            mTextView.setText(R.string.secondary_activity_text);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.secondary_activity_view);
        Log.v(MainActivity.TAG, "SecondaryActivity onCreate!");
        mTextView = this.findViewById(R.id.textview);
        initSurfaceView();
        mHandler.sendEmptyMessageDelayed(1001, 5 * 1000L);
    }

    @Override
    protected void onPostResume() {
        Log.v(MainActivity.TAG, "SecondaryActivity onPostResume!");
        super.onPostResume();
    }

    @Override
    protected void onResume() {
        Log.v(MainActivity.TAG, "SecondaryActivity onResume!");
        super.onResume();
    }

    @Override
    protected void onStart() {
        Log.v(MainActivity.TAG, "SecondaryActivity onStart!");
        super.onStart();
    }

    public void onButtonClick(View v) {
        int id = v.getId();
        switch(id) {
            case R.id.button:
                Log.v(MainActivity.TAG, "SecondaryActivity onButtonClicked!");
                mTextView.setText(R.string.secondary_activity_button_clicked);
                play();
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.v(MainActivity.TAG, "SecondaryActivity onKeyDown...keyCode:" + keyCodeToString(keyCode));
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.v(MainActivity.TAG, "SecondaryActivity onTouchEvent...event:" + event.toString());
        return super.onTouchEvent(event);
    }

    private String keyCodeToString(int keyCode) {
        String retVal = "";
        switch (keyCode) {
            case 1001:
                retVal = "Confirm";
                break;
            case 4:
                retVal = "Back";
                break;
            case 24:
                retVal = "VolumeUp";
                break;
            case 25:
                retVal = "VolumeDown";
                break;
            default:
                retVal = String.valueOf(keyCode);
                break;

        }
        return retVal;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus) {
            Log.v(MainActivity.TAG, "SecondaryActivity get Focus");
        } else {
            Log.v(MainActivity.TAG, "SecondaryActivity loses Focus");
        }
        super.onWindowFocusChanged(hasFocus);
    }

    public void initMediaPlayer() {
        Uri uriParse = Uri.parse("file:///storage/emulated/0/Dolphins_720.mp4");
        mMediaPlayer = new MediaPlayer();
        //mp.setSurface(surfaceTest.getSurface());
        mMediaPlayer.setDisplay(mSurfaceView.getHolder());
        mMediaPlayer.setLooping(true);
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            mMediaPlayer.setDataSource(this, uriParse);
            mMediaPlayer.prepare();
        } catch (IllegalArgumentException e) {
            Log.v(MainActivity.TAG, "IllegalArgumentException:", e);
        } catch (SecurityException e) {
            Log.v(MainActivity.TAG, "SecurityException:", e);
        } catch (IllegalStateException e) {
            Log.v(MainActivity.TAG, "IllegalStateException:", e);
        } catch (IOException e) {
            Log.v(MainActivity.TAG, "IOException:", e);
        }

        mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {

            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Log.v(MainActivity.TAG, "onError--->what is:" + what + ",extra is:" + extra);
                return false;
            }
        });

//		mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
//
//			@Override
//			public void onPrepared(MediaPlayer mp) {
//				Log.v(TAG, "MediaPlayer is prepared and is going to play!");
//				mp.start();
//			}
//		});
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp) {
                Log.v(MainActivity.TAG, "Video reaches the end....");
                mp.release();
            }
        });

        mMediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {

            @Override
            public void onBufferingUpdate(MediaPlayer mp, int percent) {
                Log.v(MainActivity.TAG, "onBufferingUpdate----->percent is:" + percent);
            }
        });
    }

    private void play() {
        try {
            Log.v(MainActivity.TAG, "MediaPlayer is playing:" + mMediaPlayer.isPlaying());
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
            } else {
                mMediaPlayer.start();
            }
        } catch (IllegalStateException e) {
            Log.e(MainActivity.TAG, "IllegalStateException occurs");
            e.printStackTrace();
        }
    }

    private void initSurfaceView() {
        mSurfaceView = this.findViewById(R.id.surfaceview);
        mSurfaceView.getHolder().setFixedSize(800, 480);
        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                initMediaPlayer();
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

            }
        });
    }

    @Override
    protected void onPause() {
        Log.v(MainActivity.TAG, "SecondaryActivity onPause!");
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.v(MainActivity.TAG, "SecondaryActivity onStop!");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.v(MainActivity.TAG, "SecondaryActivity onDestroy!");
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
        }
        super.onDestroy();
    }
}
