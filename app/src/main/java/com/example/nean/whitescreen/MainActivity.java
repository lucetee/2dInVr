package com.example.nean.whitescreen;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "WhiteScreen";

    private static final int DISPLAY_TYPE_UNKNOWN = 0;
    private static final int DISPLAY_TYPE_BUILT_IN = 1;
    private static final int DISPLAY_TYPE_HDMI = 2;
    private static final int DISPLAY_TYPE_WIFI = 3;
    private static final int DISPLAY_TYPE_OVERLAY = 4;
    private static final int DISPLAY_TYPE_VIRTUAL = 5;

    private VirtualDisplay mVirtualDisplay;
    private ImageReader mVirtualDisplayImageReader;
    private SurfaceView mSurfaceView;
    private Surface mSurface;

    private DisplayManager mDisplayManager;
    private ImageReader.OnImageAvailableListener mImageReaderListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader imageReader) {
            Image image = imageReader.acquireLatestImage();
            Log.v(TAG, "onImageAvailable! image is null:" + (image == null));
            if(image != null) {
                int width = image.getWidth();
                int height = image.getHeight();
                final Image.Plane[] planes = image.getPlanes();
                final ByteBuffer buffer = planes[0].getBuffer();
                int pixelStride = planes[0].getPixelStride();
                int rowStride = planes[0].getRowStride();
                int rowPadding = rowStride - pixelStride * width;
                Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_4444);
                bitmap.copyPixelsFromBuffer(buffer);
                image.close();
                if (bitmap != null) {
                    saveImageToFile(bitmap);
                } else {
                    Log.w(TAG, "Bitmap is null");
                }
            }
        }
    };

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                Log.v(MainActivity.TAG, "MainActivity...ScreenOff");
                if (mVirtualDisplay != null) {
                    mVirtualDisplay.setSurface(null);//This operation causes virtualdisplay goes to OFF state
                }
            } else if (action.equals(Intent.ACTION_SCREEN_ON)) {
                Log.v(MainActivity.TAG, "MainActivity...ScreenOn");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        mDisplayManager = (DisplayManager) this.getSystemService(Context.DISPLAY_SERVICE);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        this.registerReceiver(mReceiver, filter);

        mSurfaceView = this.findViewById(R.id.surfaceview);
        final int screenWidth = this.getResources().getDisplayMetrics().widthPixels;
        final int screenHeight = this.getResources().getDisplayMetrics().heightPixels;
        mSurfaceView.getHolder().setFixedSize(screenWidth / 2, screenHeight / 2);
        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                Log.v(TAG, "MainActivity...surfaceCreated...");
                if (mVirtualDisplay != null) {
                    mVirtualDisplay.setSurface(surfaceHolder.getSurface());
                }
                if (mSurface != null) {
                    mSurface.release();
                }
                mSurface = surfaceHolder.getSurface();
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
                Log.v(TAG, "MainActivity...surfaceChanged...");
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                Log.v(TAG, "MainActivity...surfaceDestroyed...");
            }
        });

        //FloatActionButton on bottom|start position
        FloatingActionButton fabDisplayInfo = (FloatingActionButton) findViewById(R.id.fab_1);
        fabDisplayInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                StringBuffer displaysInfo = new StringBuffer();
                for(Display display : mDisplayManager.getDisplays()) {
                    try {
                        Class<?> sysClass = Class.forName("android.view.Display");
                        Method method = sysClass.getMethod("getType");
                        Object obj = method.invoke(display);
                        int displayType = Integer.valueOf(String.valueOf(obj)).intValue();
                        displaysInfo.append("DisplayId:" + display.getDisplayId() + " display type:" + displayType
                        + " display Name:" + display.getName() + "\n");
                    } catch (Exception e) {
                        Log.e(TAG, "Query hdmi display error!", e);
                    }
                }
                Snackbar.make(view, displaysInfo, Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        //FloatActionButton on start|top position
        FloatingActionButton fabHdmi = (FloatingActionButton) findViewById(R.id.fab_2);
        fabHdmi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
                launchSecondaryActivityOnHdmiDisplay();
            }
        });

        //FloatActionButton on end|bottom position
        FloatingActionButton fabVirtual = (FloatingActionButton) findViewById(R.id.fab);
        fabVirtual.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
                launchSecondaryActivityOnVirtualDisplay();
            }
        });
        fabVirtual.setVisibility(View.VISIBLE);
        toolbar.setVisibility(View.GONE);

        //ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        //android.util.Log.v(TAG, "Mobile:" + cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE) + ",Wifi:" + cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI));
        requestPermissions(new String[]{"android.permission.READ_EXTERNAL_STORAGE", "android.permission.READ_PHONE_STATE", "android.permission.WRITE_EXTERNAL_STORAGE"}, 1001);
    }

    private void launchSecondaryActivityOnVirtualDisplay() {
        try {
            if (mVirtualDisplayImageReader == null) {
                mVirtualDisplayImageReader = ImageReader.newInstance(2880, 1600, PixelFormat.RGBA_8888, 5);
                mVirtualDisplayImageReader.setOnImageAvailableListener(mImageReaderListener, null);
            }
            if (mVirtualDisplay == null) {
                int flags = DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC;
                flags |= 1 << 6;/*DisplayManager.VIRTUAL_DISPLAY_FLAG_SUPPORTS_TOUCH*/
                flags |= 1 << 7;/*DisplayManager.VIRTUAL_DISPLAY_FLAG_ROTATES_WITH_CONTENT*/
                flags |= 1 << 8;/*DisplayManager.VIRTUAL_DISPLAY_FLAG_DESTROY_CONTENT_ON_REMOVAL*/
                flags |= DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY;
                String uniqueId = "277f1a09-b88d-4d1e-8716-796f114d09cd";
                mVirtualDisplay = createVirtualDisplay("nean", 2880, 1600, 560, null, flags, uniqueId);
                if (mVirtualDisplay == null) {
                    Log.w(TAG, "Create virtual display with uniqueId failed!Create on with no uniqueId");
                    mVirtualDisplay = mDisplayManager.createVirtualDisplay("nean_test", 2880, 1600, 560,
                        /*mVirtualDisplayImageReader.getSurface()*/null, flags);
                }
                mVirtualDisplay.setSurface(mSurface);
            } else {
                Log.v(TAG, "VirtualDisplay:" + mVirtualDisplay.toString() + " exists...do not create again");
            }
        } catch (Exception e) {
            Log.v(TAG, "Exception when create virtual display!", e);
        }
        ActivityOptions options = ActivityOptions.makeBasic();
        int virtualDisplayId = -1;
        for (Display display : mDisplayManager.getDisplays()) {
            //TODO:Find virtual display with uniqueId rather than displayId
            if (getDisplayType(display) == DISPLAY_TYPE_VIRTUAL) {
                virtualDisplayId = display.getDisplayId();
                Log.v(TAG, "virtualDisplay Id:" + virtualDisplayId);
                break;
            }
        }
        if (virtualDisplayId != -1) {
            options.setLaunchDisplayId(virtualDisplayId);
        }
        Log.v(TAG, "Start sencondary activity on display:" + options.getLaunchDisplayId());
        //Intent secondIntent = new Intent(this, SecondaryActivity.class);
        Intent secondIntent = new Intent();
        String packageName = Utils.getProperty("nean.debug.multidisplay.packagename", "com.example.nean.whitescreen");
        String classname = Utils.getProperty("nean.debug.multidisplay.classname", "com.example.nean.whitescreen.SecondaryActivity");
        secondIntent.setClassName(packageName, classname);
        secondIntent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(secondIntent, options.toBundle());
    }

    private void launchSecondaryActivityOnHdmiDisplay() {
        ActivityOptions options = ActivityOptions.makeBasic();
        int hdmiDisplayId = -1;
        for (Display display : mDisplayManager.getDisplays()) {
            if (getDisplayType(display) == DISPLAY_TYPE_HDMI) {
                hdmiDisplayId = display.getDisplayId();
                Log.v(TAG, "hdmiDisplay Id:" + hdmiDisplayId);
            }
        }
        if (hdmiDisplayId != -1) {
            options.setLaunchDisplayId(hdmiDisplayId);
        }
        Log.v(TAG, "Start sencondary activity on display:" + options.getLaunchDisplayId());
        Intent secondIntent = new Intent(this, SecondaryActivity.class);
        secondIntent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(secondIntent, options.toBundle());
    }

    private int getDisplayType(Display display) {
        try {
            Class<?> sysClass = Class.forName("android.view.Display");
            Method method = sysClass.getMethod("getType");
            Object obj = method.invoke(display);
            int displayType = Integer.valueOf(String.valueOf(obj)).intValue();
            return displayType;
        } catch (Exception e) {
            Log.e(TAG, "getDisplayType error!", e);
        }
        return DISPLAY_TYPE_UNKNOWN;
    }

    private VirtualDisplay createVirtualDisplay(String displayName, int width, int height, int dpi, Surface surface, int flags, String uniqueId) {
        try {
            Class<?> sysClass = Class.forName("android.hardware.display.DisplayManager");
            Method method = sysClass.getMethod("createVirtualDisplay", MediaProjection.class, String.class, int.class, int.class, int.class, Surface.class, int.class, VirtualDisplay.Callback.class, Handler.class, String.class);
            Object obj = method.invoke(mDisplayManager, null, displayName, width, height, dpi, surface, flags, null, null, uniqueId);
            VirtualDisplay virtualDisplay = (VirtualDisplay)obj;
            return virtualDisplay;
        } catch (Exception e) {
            Log.e(TAG, "createVirtualDisplay error!", e);
        }
        return null;
    }

    private void saveImageToFile(Image image) {
        Log.v(TAG, "Save image to File!");
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        FileOutputStream output = null;
        try {
            //output = new FileOutputStream(new File(mDirectory, appendExtraZeroToFileName(timestamp, !TextUtils.isEmpty(mFileSuffix), mFileSuffix)));
            output = new FileOutputStream(new File("/storage/emulated/0/DCIM/pic-" + generateTimeStamp() + ".jpg"));
            output.write(bytes);
        } catch (IOException e) {
            Log.v(TAG, "Save to file exception!", e);
        } finally {
            image.close();
            if (null != output) {
                try {
                    output.close();
                } catch (IOException e) {
                    Log.e(TAG, "Close stream exception!", e);
                }
            }
        }
    }

    public static String saveImageToFile(Bitmap bmp) {
        File file = new File("/storage/emulated/0/DCIM/pic-" + generateTimeStamp() + ".jpg");
        try {
            FileOutputStream fos = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            Log.v(TAG, "Save bitmap file FileNotFoundException!", e);
        } catch (IOException e) {
            Log.v(TAG, "Save bitmap file IOException!", e);
        }
        return file.getAbsolutePath();
    }

    public static String generateTimeStamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS");
        String time = sdf.format(new Date(System.currentTimeMillis()));
        return time;
    }


//    @Override
//    public void onBackPressed() {
//        this.finish();
//    }

    @Override
	public void onRequestPermissionsResult(int requestCode,String[] permissions, int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		for(int i = 0; i < permissions.length; i++) {
		    Log.v(TAG, "permission:" + permissions[i] + " granted:" + grantResults[i]);
        }
		if (requestCode == 1001) {
			if (grantResults[2] == PackageManager.PERMISSION_GRANTED) {
			    Log.v(TAG, "Permission check ok!");
//				mCountDownLatch.countDown();
                //handleVirtualDisplay();
			} else {
				this.finish();
			}
		}
	}

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.v(TAG, "MainActivity..onKeyDown..keyCode:" + keyCodeToString(keyCode));
        //if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            //System.out.println("按下了back键 onKeyDown()");
        //    return false;
        //} else {
            return super.onKeyDown(keyCode, event);
        //}
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.v(MainActivity.TAG, "MainActivity onTouchEvent...event:" + event.toString());
        return super.onTouchEvent(event);
    }

    private String keyCodeToString(int keyCode) {
        String retVal;
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
            Log.v(TAG, "MainActivity get Focus");
        } else {
            Log.v(TAG, "MainActivity loses Focus");
        }
        super.onWindowFocusChanged(hasFocus);
    }

    @Override
    protected void onResume() {
        Log.v(TAG, "MainActivity.onResume...");
        super.onResume();
    }

    @Override
    protected void onPause() {
        Log.v(TAG, "MainActivity.onPause...");
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Log.v(TAG, "MainActivity.onDestroy...");
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
        }
        if (mVirtualDisplayImageReader != null) {
            mVirtualDisplayImageReader.setOnImageAvailableListener(null, null);
        }
        this.unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
