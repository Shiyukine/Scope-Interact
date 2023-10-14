package com.shiyukine.scopeinteract;

import static android.util.Half.EPSILON;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Looper;
import android.os.MessageQueue;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.text.DecimalFormat;

public class Graph extends AppCompatActivity {

    //left, top, right, bottom
    static int[] margin = {-1, -1, -1, -1};
    static int sensivity = 0;
    public static boolean isSet = false;
    public static Socket socket;
    public static DatagramSocket socketU;
    public static InetAddress address;
    public static byte[] buf;
    public static PrintWriter output = null;
    public static boolean isMove = false;
    public static boolean keyPressed = false;
    public static boolean errorShowed = false;
    public static Graph g;
    public static boolean isUsb = true;

    @SuppressLint({"SourceLockedOrientationActivity", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            errorShowed = false;
            super.onCreate(savedInstanceState);
            setContentView(R.layout.tgraphic);
            //change screen
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
            if(!MainActivity.revert) setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            else setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
            //for new api versions.
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            decorView.setSystemUiVisibility(uiOptions);
            //setting pressure
            ((SeekBar)findViewById(R.id.pressbar)).setProgress(sensivity);
            ((SeekBar)findViewById(R.id.pressbar)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                    sensivity = progress;
                    MainActivity.setSetting("sensivity", progress);
                    new SendInfo().execute("sensivity:" + sensivity + "|");
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
            g = this;
            //settings tgraph
            ViewGroup vg2 = findViewById(R.id.gl_main);
            for (int i = 0; i < vg2.getChildCount(); i++) {
                View v = vg2.getChildAt(i);
                if (v.getTag() != null && ((String) v.getTag()).contains("key-")) {
                    if (!isSet) v.setOnTouchListener(touchkey);
                }
            }
            sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            linearSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
            if(!isSet) {
                //send info when activity loaded
                MessageQueue.IdleHandler handler = new MessageQueue.IdleHandler() {
                    @Override
                    public boolean queueIdle() {
                        new SendInfo().execute("ready:" + MainActivity.verCode + ";" + Graph.sensivity + "|", g);
                        return false;
                    }
                };
                Looper.myQueue().addIdleHandler(handler);
            }

        } catch (Exception e) {
            Log.e("Error", e.getMessage(), e);
        }
    }

    // Create a constant to convert nanoseconds to seconds.
    private static final float NS2S = 1.0f / 1000000000.0f;

    // Create a constant to convert nanoseconds to milliseconds.
    private static final float NS2MS = 1.0f / 1000000.0f;
    private final float[] deltaRotationVector = new float[4];
    private double timestamp;
    private double timestamp2;

    private SensorManager sensorManager;
    private Sensor sensor;
    private Sensor linearSensor;
    private float rotationCurrent;

    DecimalFormat decForm = new DecimalFormat("#.###");

    private final SensorEventListener sensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            // This timestep's delta rotation to be multiplied by the current rotation
            // after computing it from the gyro sample data.
            if (timestamp != 0) {
                final double dT = (event.timestamp - timestamp) * NS2S;
                // Axis of the rotation sample, not normalized yet.
                float axisX = event.values[0];
                float axisY = event.values[1];
                float axisZ = event.values[2];

                // Calculate the angular speed of the sample
                float omegaMagnitude = (float) sqrt(axisX*axisX + axisY*axisY + axisZ*axisZ);

                // Normalize the rotation vector if it's big enough to get the axis
                // (that is, EPSILON should represent your maximum allowable margin of error)
                if (omegaMagnitude > EPSILON) {
                    axisX /= omegaMagnitude;
                    axisY /= omegaMagnitude;
                    axisZ /= omegaMagnitude;
                }

                // Integrate around this axis with the angular speed by the timestep
                // in order to get a delta rotation from this sample over the timestep
                // We will convert this axis-angle representation of the delta rotation
                // into a quaternion before turning it into the rotation matrix.
                double thetaOverTwo = omegaMagnitude * dT / 2.0f;
                float sinThetaOverTwo = (float) sin(thetaOverTwo);
                float cosThetaOverTwo = (float) cos(thetaOverTwo);
                deltaRotationVector[0] = sinThetaOverTwo * axisX;
                deltaRotationVector[1] = sinThetaOverTwo * axisY;
                deltaRotationVector[2] = sinThetaOverTwo * axisZ;
                deltaRotationVector[3] = cosThetaOverTwo;
                if(isMove) {
                    new SendInfo().execute("move:" + axisX + ";" + axisY + ";" + axisZ + "|");
                }
            }
            timestamp = event.timestamp;
            float[] deltaRotationMatrix = new float[9];
            SensorManager.getRotationMatrixFromVector(deltaRotationMatrix, deltaRotationVector);
            // User code should concatenate the delta rotation we computed with the current rotation
            // in order to get the updated rotation.
            //rotationCurrent = rotationCurrent * deltaRotationMatrix;
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
        }
    };

    private final SensorEventListener linearSensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if(timestamp2 != 0) {
                double dT = (event.timestamp - timestamp2) * NS2MS;
                float axisX = event.values[0];
                float axisY = event.values[1];
                float axisZ = event.values[2];
                float omegaMagnitude = (float) sqrt(axisX*axisX + axisY*axisY + axisZ*axisZ);
                //Log.e("gfgdfsgdsg6", omegaMagnitude + "");
                if (isMove) {
                    new SendInfo().execute("linear:" + axisX + ";" + axisY + ";" + axisZ + ";" + dT + "|");
                }
            }
            timestamp2 = event.timestamp;
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(sensorListener);
        sensorManager.unregisterListener(linearSensorListener);
    }

    public void onBackPressed() {
        super.onBackPressed();
        try {
            sensorManager.unregisterListener(sensorListener);
            sensorManager.unregisterListener(linearSensorListener);
            if(socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    int sampling = SensorManager.SENSOR_DELAY_GAME;

    public void onResume() {
        //for new api versions.
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);
        super.onResume();
        if(MainActivity.histEv) sampling = SensorManager.SENSOR_DELAY_GAME;
        else sampling = SensorManager.SENSOR_DELAY_UI;
        sensorManager.registerListener(sensorListener, sensor, sampling);
        sensorManager.registerListener(linearSensorListener, linearSensor, sampling);
    }

    View.OnTouchListener touchkey =  new View.OnTouchListener() {
        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            try {
                if (!isSet) {
                    ImageButton ib = (ImageButton) view;
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            new SendInfo().execute(ib.getTag() + "-kdown|", g);
                            if(((String)view.getTag()).contains("key-2")) isMove = true;
                            keyPressed = true;
                            return false;

                        case MotionEvent.ACTION_UP:
                            new SendInfo().execute(ib.getTag() + "-kup|", g);
                            if(((String)view.getTag()).contains("key-2")) isMove = false;
                            keyPressed = false;
                            return false;
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
                Log.e("Error", "E" + e.getMessage());
            }
            return false;
        }
    };

    int firstPres = -1;
    RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(0,0);

    public void set_click(View view) {
        if(!isSet) {
            if (findViewById(R.id.setg).getVisibility() == View.VISIBLE)
                findViewById(R.id.setg).setVisibility(View.GONE);
            else findViewById(R.id.setg).setVisibility(View.VISIBLE);
        }
    }
}
