package com.example.stepcounterandroid;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity implements SensorEventListener {
	private boolean mInitialized; // used for initializing sensor only once
	private final float THRESHOLD_MOTION = (float) 5.0;
	private SensorManager mSensorManager;
	int stepsCount = 0;
	double mLastXValue = 0.0, mLastYValue = 0.0, mLastZValue = 0.0;
	private Sensor mLinearAccelerometer;

	final String STATUS_MESSAGE ="com.example.stepcounterandroid.STATUS_MESSAGE";
	private boolean isFlashOn = false;
	// final String p=Parameters.FLASH_MODE_OFF;
	private Camera camera;
	Parameters params;
	int noOfStepsToTake = 05;
	Button button;
	List<Sensor> listSensors = null;
	boolean stepTaken = false;
	EditText editNumberofSteps = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Initializing Camera Sensors
		PackageManager pm = this.getPackageManager();
		Log.i("system info", Boolean.toString(Build.MODEL.contains("Nexus")));
		if (Build.MODEL.contains("Nexus") || Build.MODEL.contains("SAMSUNG"))
			turnOnFlash();
		// if device support camera?
		if (!pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
			Log.e("err", "Device has no camera!");
			return;
		}
		// get the camera
		getCamera();
		button = (Button) findViewById(R.id.button_enable_sensors);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				editNumberofSteps = (EditText) findViewById(R.id.edit_no_of_steps);
				if(editNumberofSteps.getText().toString() != null && editNumberofSteps.getText().toString() != "" )
					noOfStepsToTake = Integer.parseInt(editNumberofSteps.getText().toString());
				else
					//Just setting default step value
					noOfStepsToTake=05;
				Log.i("info", "edit_enteredStepsValue:" + noOfStepsToTake);

				// Initialize Accelerometer sensor
				mInitialized = false;
				mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
				mLinearAccelerometer = mSensorManager
						.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
				startSensor();
				Log.i("info", "checking the sensors now");
				chkSensorList();
			}
		});

	}

	public void chkSensorList() {
		Log.i("info", "insideChkSensorList()");
		List<Sensor> listOfSensors = mSensorManager
				.getSensorList(Sensor.TYPE_ALL);
		for (int i = 0; i < listOfSensors.size(); i++) {

			Log.i("sensor " + i, listOfSensors.get(i).getName());
		}
	}

	// Get the camera
	private void getCamera() {
		if (camera == null) {
			try {
				camera = Camera.open();
				params = camera.getParameters();
			} catch (RuntimeException e) {
				Log.e("Camera Error. Failed to Open. Error: ", e.getMessage());
			}
		}
	}

	// Turning On flash
	private void turnOnFlash() {
		if (!isFlashOn) {
			if (camera == null || params == null) {
				Log.i("info", "camera== null or params=nul");
				return;
			}

			params = camera.getParameters();
			System.out.println(params.toString());
			Log.i("Parameters", params.toString());
			params.setFlashMode(Parameters.FLASH_MODE_TORCH);
			camera.setParameters(params);
			camera.startPreview();
			isFlashOn = true;
		}

	}

	// Turning Off flash
	private void turnOffFlash() {
		if (isFlashOn) {
			if (camera == null || params == null) {
				return;
			}

			params = camera.getParameters();
			params.setFlashMode(Parameters.FLASH_MODE_OFF);
			camera.setParameters(params);
			camera.stopPreview();
			isFlashOn = false;
		}
	}

	@Override
	protected void onStop() {
		super.onStop();

		// on stop release the camera
		if (camera != null) {
			camera.release();
			camera = null;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	private void startSensor() {
		Log.i("Sensor_App : ", "Starting Sensor");
		mSensorManager.registerListener(this, mLinearAccelerometer,
				SensorManager.SENSOR_DELAY_FASTEST);
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// event object contains values of acceleration, read those
		double x = event.values[0];
		double y = event.values[1];
		double z = event.values[2];

		final double alpha = 0.8; // constant for our filter below
		double[] gravity = { 0, 0, 0 };

		// Isolate the force of gravity with the low-pass filter.
		gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
		gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
		gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

		// Remove the gravity contribution with the high-pass filter.
		x = event.values[0] - gravity[0];
		y = event.values[1] - gravity[1];
		z = event.values[2] - gravity[2];

		if (!mInitialized) {
			// sensor is used for the first time, initialize the last read
			// values
			mLastXValue = x;
			mLastYValue = y;
			mLastZValue = z;
			mInitialized = true;
		} else {
			// sensor is already initialized, and we have previously read
			// values.
			// take difference of past and current values and decide which
			// axis acceleration was detected by comparing values

			double deltaX = Math.abs(mLastXValue - x);
			double deltaY = Math.abs(mLastYValue - y);
			double deltaZ = Math.abs(mLastZValue - z);

			/*
			 * //Just to see if program runs fine if (deltaX > 2) {
			 * System.out.println("deltaX: " + deltaX + "No Of Steps:" +
			 * String.valueOf(stepsCount)); }
			 * 
			 * if(deltaX >=THRESHOLD_MOTION || deltaY >= THRESHOLD_MOTION ||
			 * deltaZ >= THRESHOLD_MOTION) { stepTaken=true; } else
			 * stepTaken=false;
			 */
			if (deltaX < THRESHOLD_MOTION) {
				deltaX = (float) 0.0;
				stepTaken = false;
			} else
				stepTaken = true;

			if (deltaY < THRESHOLD_MOTION) {
				deltaY = (float) 0.0;
				stepTaken = false;
			} else
				stepTaken = true;

			if (deltaZ < THRESHOLD_MOTION) {
				deltaZ = (float) 0.0;
				stepTaken = false;
			} else
				stepTaken = true;

			if (stepTaken) {
				System.out.println("deltaX: " + deltaX + "deltaY: " + deltaY
						+ "deltaZ: " + deltaZ);
				// txt.setText("Y Step: " + String.valueOf(stepsCount));
				System.out.println("NoofSteps: " + String.valueOf(stepsCount));
				stepsCount = stepsCount + 1;
				if (stepsCount == noOfStepsToTake) {
					if (isFlashOn) {
						turnOffFlash();
						Toast.makeText(this, "Turning off flash!\nNo of steps: "+ noOfStepsToTake, Toast.LENGTH_LONG).show();
					} else {
						turnOnFlash();
						isFlashOn = true;
						Toast.makeText(this, "Turning on flash!\nNo of steps: "+ noOfStepsToTake, Toast.LENGTH_LONG).show();
					}
					stepsCount = 0;
				}
				stepTaken = false;
			}
			mLastXValue = x;
			mLastYValue = y;
			mLastZValue = z;

			if (deltaX > deltaY) {

			}
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}

	public void closeApp(View view) {
		finish();
		System.exit(0);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mSensorManager != null) {
			mSensorManager.registerListener(this, mLinearAccelerometer,
					SensorManager.SENSOR_DELAY_FASTEST);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mSensorManager != null) {
			mSensorManager.unregisterListener(this);
		}
	}
}
