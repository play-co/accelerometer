package com.tealeaf.plugin.plugins;

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

public class AccelerometerPlugin implements IPlugin, SensorEventListener {

	// Sensors and Manager variables
	private SensorManager sensorManager;
	private Sensor accelerometerSensor;
	private Sensor gyroscopeSensor;
	private Sensor magneticSensor;

	//flags to indicate when the device should have sensors enabled
	//and if they are currently listening for events
	private boolean sensorsEnabled = false;
	private boolean sensorsListening = false;
	
	//flags to indicate which events should be generated
	private boolean deviceMotionEventsEnabled = false;
	private boolean deviceOrientationEventsEnabled = false;
	
	// store the latest rotation rates
	private float rotationRateAlpha = 0;
	private float rotationRateBeta = 0;
	private float rotationRateGamma = 0;

	// store the latest magnetic force reading
	private float[] magneticForce = new float[3];

	// store the average linear acceleration for filtering
	private float[] averageLinearAcceleration = new float[3];

	// filtering factor that controls how sensitive to noise the accelerometer
	// is
	private final float filteringFactor = 0.1f;

	// variable used to ignore the first accelerometer reading when computing
	// the average
	private boolean firstFilter = true;

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

	private class DeviceOrientationEvent extends Event {
		public float alpha;
		public float beta;
		public float gamma;
		public boolean absolute;
		public DeviceOrientationEvent(float alpha, float beta, float gamma) {
			super("deviceorientation");
			this.alpha = alpha;
			this.beta = beta;
			this.gamma = gamma;
			this.absolute = true;
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
			super("devicemotion");
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
	
	}

	public void onCreate(Activity activity, Bundle savedInstanceState) {
		Context context = activity;
		// initialize all sensors and the sensor manager
		sensorManager = (SensorManager) context
				.getSystemService(Context.SENSOR_SERVICE);
		accelerometerSensor = sensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		magneticSensor = sensorManager
				.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

		setDeviceMotionEventsEnabled(true);
	}

	// Register all needed sensor listeners
	public void registerListeners() {
		if (!sensorsListening && sensorsEnabled) {
			sensorManager.registerListener(this, accelerometerSensor,
					SensorManager.SENSOR_DELAY_GAME);
			sensorManager.registerListener(this, gyroscopeSensor,
					SensorManager.SENSOR_DELAY_GAME);
			sensorManager.registerListener(this, magneticSensor,
					SensorManager.SENSOR_DELAY_GAME);
			firstFilter = true;
			sensorsListening = true;
		}
	}

	// Unregister any listeners that are currently in use
	public void unregisterListeners() {
		if (sensorsListening) {
			sensorManager.unregisterListener(this);
			sensorsListening = false;
		}
	}
	
	//Enable sending of device motion events and any sensors required to generate them
	public void setDeviceMotionEventsEnabled(boolean enabled) {
		deviceMotionEventsEnabled = enabled;
		//if this is the first event type being enabled then enable the sensors
		if (enabled && !deviceOrientationEventsEnabled) {
			sensorsEnabled = true;
			registerListeners();
		//if this is the last event type enabled then disable the sensors
		} else if(!enabled && !deviceOrientationEventsEnabled) {
			sensorsEnabled = false;
			unregisterListeners();
		}
	}
	
	//Enable sending of device orientation events and any sensors required to generate them
	public void setDeviceOrientationEventsEnabled(boolean enabled) {
		deviceOrientationEventsEnabled = enabled;
		//if this is the first event type being enabled then enable the sensors
		if (enabled && !deviceMotionEventsEnabled) {
			sensorsEnabled = true;
			registerListeners();
		//if this is the last event type enabled then disable the sensors
		} else if(!enabled && !deviceMotionEventsEnabled) {
			sensorsEnabled = false;
			unregisterListeners();
		}
	}
	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		//No logic currently needed for accuracy changes
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		switch (event.sensor.getType()) {
		case Sensor.TYPE_ACCELEROMETER:
			//rotation matrix converting from phone coordinates to world coordinates
			float[] R = new float[9];
			//converts the magnetic vector to world coordinates
			float[] I = new float[9]; 
			//used to store the acceleration without gravity
			float[] accel = new float[3];
			//used to store the angles describing phone orientation
			float[] rAngles = new float[3]; //

			//if this is the first reading set the average to the current acceleration
			if (firstFilter) {
				for (int i = 0; i < 3; i++) {
					averageLinearAcceleration[i] = event.values[i];
				}
				firstFilter = false;
			} else {
				//otherwise update the new average acceleration and subtract it out of the
				//total acceleration to get the user acceleration
				for (int i = 0; i < 3; i++) {
					averageLinearAcceleration[i] = event.values[i]
							* filteringFactor + averageLinearAcceleration[i]
							* (1 - filteringFactor);
					accel[i] = event.values[i] - averageLinearAcceleration[i];
				}
			}
			//try to get the rotation matrices R and I based on the current average acceleration
			//and the magnetic force
			boolean gotOrientation = SensorManager.getRotationMatrix(R, I,
					averageLinearAcceleration, magneticForce);
			
			//if the orientation was correctly acquired add an event to the orientation queue
			//to be send to native on the next frame
			if(gotOrientation && deviceOrientationEventsEnabled) {
				SensorManager.getOrientation(R, rAngles);
				EventQueue.pushEvent(new DeviceOrientationEvent(rAngles[0], rAngles[1], rAngles[2]));
			}
			
			//add a new motion event to the queue so that is can be sent to native on the next frame
			if(deviceMotionEventsEnabled) {
				//lets send a motion event to javascript
				EventQueue.pushEvent(new DeviceMotionEvent(accel[0], accel[1], accel[2],
						event.values[0], event.values[1], event.values[2],
						rotationRateAlpha, rotationRateBeta, rotationRateGamma));
			}
			break;
		case Sensor.TYPE_GYROSCOPE:
			//store the latest gyroscope readings
			rotationRateAlpha = event.values[0];
			rotationRateBeta = event.values[1];
			rotationRateGamma = event.values[2];
			break;
		case Sensor.TYPE_MAGNETIC_FIELD:
			//store the current magnetic field vector
			magneticForce[0] = event.values[0];
			magneticForce[1] = event.values[1];
			magneticForce[2] = event.values[2];
			break;
		}
	}

	public void onResume() {
		registerListeners();
	}

	public void onStart() {
		unregisterListeners();
	}

	public void onPause() {
	
	}

	public void onStop() {
	}

	public void onDestroy() {
	}

	public void onNewIntent(Intent intent) {
	
	}

	public void setInstallReferrer(String referrer) {
	
	}

	public void onActivityResult(Integer request, Integer result, Intent data) {
	
	}

	public boolean consumeOnBackPressed() {
		return false;
	}

	public void onBackPressed() {
	
	}

}
