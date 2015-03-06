package com.tealeaf.plugin.plugins;

import com.tealeaf.TeaLeaf;
import com.tealeaf.plugin.IPlugin;
import com.tealeaf.event.Event;
import com.tealeaf.EventQueue;

import java.io.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import android.app.Activity;
import android.content.Intent;
import android.content.Context;
import android.util.Log;
import android.os.Bundle;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.WindowManager;
import android.view.Display;

public class AccelerometerPlugin implements IPlugin, SensorEventListener {

    private Context context;

    // Sensors and Manager variables
    private SensorManager sensorManager;
    private Sensor accelerometerSensor;
    private Sensor gyroscopeSensor;
    private Sensor magneticSensor;

    // flags to indicate when the device should have sensors enabled
    // and if they are currently listening for events
    private boolean sensorsEnabled = false;
    private boolean sensorsListening = false;

    // store the latest rotation rates
    private float rotationRateAlpha = 0;
    private float rotationRateBeta = 0;
    private float rotationRateGamma = 0;

    private float[] gravity = new float[3];

    // store the latest magnetic force reading
    private float[] magneticForce = new float[3];

    private static final float RAD_TO_DEGREES = (float) (180.0f / Math.PI);

    private class DeviceAcceleration {
        public float x;
        public float y;
        public float z;
        public DeviceAcceleration(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private class DeviceRotationRate {
        public float alpha;
        public float beta;
        public float gamma;
        public DeviceRotationRate(float alpha, float beta, float gamma) {
            this.alpha = alpha;
            this.beta = beta;
            this.gamma = gamma;
        }
    }

    private class DeviceMotionEvent extends Event {
        protected DeviceAcceleration acceleration;
        protected DeviceAcceleration accelerationIncludingGravity;
        protected DeviceRotationRate rotationRate;

        public DeviceMotionEvent(
                float accelerationX,
                float accelerationY,
                float accelerationZ,
                float accelerationIncludingGravityX,
                float accelerationIncludingGravityY,
                float accelerationIncludingGravityZ,
                float rotationRateAlpha,
                float rotationRateBeta,
                float rotationRateGamma) {
            super("DeviceMotionEvent");
            this.acceleration = new DeviceAcceleration(
                    accelerationX,
                    accelerationY,
                    accelerationZ);
            this.accelerationIncludingGravity = new DeviceAcceleration(
                    accelerationIncludingGravityX,
                    accelerationIncludingGravityY,
                    accelerationIncludingGravityZ);
            this.rotationRate = new DeviceRotationRate(
                    rotationRateAlpha,
                    rotationRateBeta,
                    rotationRateGamma);
        }
    }

    public AccelerometerPlugin() {
    }

    public void onCreateApplication(Context applicationContext) {
        context = applicationContext;
        initSensors();
    }

    public void onCreate(Activity activity, Bundle savedInstanceState) {
        Context context = activity;
        initSensors();
    }
    public void initSensors() {
        // initialize all sensors and the sensor manager
        sensorManager = (SensorManager) context
                .getSystemService(Context.SENSOR_SERVICE);
        accelerometerSensor = sensorManager
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        magneticSensor = sensorManager
                .getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

    }

    // Register all needed sensor listeners
    private void registerListeners() {
        if (!sensorsListening) {
            sensorManager.registerListener(this, accelerometerSensor,
                    SensorManager.SENSOR_DELAY_GAME);
            sensorManager.registerListener(this, gyroscopeSensor,
                    SensorManager.SENSOR_DELAY_GAME);
            sensorManager.registerListener(this, magneticSensor,
                    SensorManager.SENSOR_DELAY_GAME);
            sensorsListening = true;
        }
    }

    // Unregister any listeners that are currently in use
    private void unregisterListeners() {
        if (sensorsListening) {
            sensorManager.unregisterListener(this);
            sensorsListening = false;
        }
    }

    public void startEvents(String jsonData) {
        sensorsEnabled = true;
        registerListeners();
    }

    public void stopEvents(String jsonData) {
        sensorsEnabled = false;
        unregisterListeners();
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No logic currently needed for accuracy changes
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
        case Sensor.TYPE_ACCELEROMETER:
            final float alpha = 0.8f;

            // high-pass filter: http://developer.android.com/reference/android/hardware/SensorEvent.html#values
            gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
            gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
            gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

            EventQueue.pushEvent(new DeviceMotionEvent(
                    // acceleration
                    event.values[0] - gravity[0],
                    event.values[1] - gravity[1],
                    event.values[2] - gravity[2],

                    // acceleration including gravity
                    event.values[0], event.values[1], event.values[2],

                    // rotation
                    rotationRateAlpha, rotationRateBeta, rotationRateGamma));

            break;
        case Sensor.TYPE_GYROSCOPE:
            // store the latest gyroscope readings, convert to degrees per
            // second (from radians per second)
            rotationRateAlpha = event.values[0] * RAD_TO_DEGREES;
            rotationRateBeta = event.values[1] * RAD_TO_DEGREES;
            rotationRateGamma = event.values[2] * RAD_TO_DEGREES;
            break;
        case Sensor.TYPE_MAGNETIC_FIELD:
            // store the current magnetic field vector
            magneticForce[0] = event.values[0];
            magneticForce[1] = event.values[1];
            magneticForce[2] = event.values[2];
            break;
        }
    }

    public void onResume() {
        if (sensorsEnabled) {
            registerListeners();
        }
    }

    public void onStart() {}

    public void onPause() {
        unregisterListeners();
    }

    public void onStop() {}
    public void onDestroy() {}

    public void onNewIntent(Intent intent) {}

    public void setInstallReferrer(String referrer) {}

    public void onActivityResult(Integer request, Integer result, Intent data) {}

    public boolean consumeOnBackPressed() {
        return true;
    }

    public void onBackPressed() {}
}
