package com.etchorner.tacarkit;

import android.app.Activity;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import com.etchorner.tacarkit.common.LowPassFilter;
import com.etchorner.tacarkit.common.Orientation;

import java.util.List;

public class SensorPack extends Activity implements SensorEventListener {
    private static final String TAG = "SensorPack";
//    private static final int MIN_TIME = 30 * 1000;
//    private static final int MIN_DISTANCE = 10;

    private static final float temp[] = new float[9]; // Temporary rotation matrix in Android format
    private static final float rotation[] = new float[9]; // Final rotation matrix in Android format
    private static final float grav[] = new float[3]; // Gravity (a.k.a accelerometer data)
    private static final float mag[] = new float[3]; // Magnetic

    static final float apr[] = new float[3]; //Azimuth, pitch, roll
    private static GeomagneticField gmf = null;
    private static float smooth[] = new float[3];
    private static SensorManager sensorMgr = null;
    private static List<Sensor> sensors = null;
    private static Sensor sensorGrav = null;
    private static Sensor sensorMag = null;
    private static LocationManager locationMgr = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();

//        float neg90rads = (float) Math.toRadians(-90);

//        // Counter-clockwise rotation at -90 degrees around the x-axis
//        // [ 1, 0, 0 ]
//        // [ 0, cos, -sin ]
//        // [ 0, sin, cos ]
//        xAxisRotation.set(1f, 0f, 0f,
//                0f, FloatMath.cos(neg90rads), -FloatMath.sin(neg90rads),
//                0f, FloatMath.sin(neg90rads), FloatMath.cos(neg90rads));
//
//        // Counter-clockwise rotation at -90 degrees around the y-axis
//        // [ cos,  0,   sin ]
//        // [ 0,    1,   0   ]
//        // [ -sin, 0,   cos ]
//        yAxisRotation.set(FloatMath.cos(neg90rads), 0f, FloatMath.sin(neg90rads),
//                0f, 1f, 0f,
//                -FloatMath.sin(neg90rads), 0f, FloatMath.cos(neg90rads));

        try {
            sensorMgr = (SensorManager) getSystemService(SENSOR_SERVICE);

            sensors = sensorMgr.getSensorList(Sensor.TYPE_ACCELEROMETER);
            if (sensors.size() > 0)
                sensorGrav = sensors.get(0);

            sensors = sensorMgr.getSensorList(Sensor.TYPE_MAGNETIC_FIELD);
            if (sensors.size() > 0)
                sensorMag = sensors.get(0);

            sensorMgr.registerListener(this, sensorGrav, SensorManager.SENSOR_DELAY_UI);
            sensorMgr.registerListener(this, sensorMag, SensorManager.SENSOR_DELAY_UI);

//            locationMgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
//            locationMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, MIN_DISTANCE, this);

//            try {
//
//                try {
//                    Location gps = locationMgr.getLastKnownLocation(LocationManager.GPS_PROVIDER);
//                    Location network = locationMgr.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
//                    if (gps != null) onLocationChanged(gps);
//                    else if (network != null) onLocationChanged(network);
//                    else onLocationChanged(ARData.hardFix);
//                } catch (Exception ex2) {
//                    onLocationChanged(ARData.hardFix);
//                }
//
//                gmf = new GeomagneticField((float) ARData.getCurrentLocation().getLatitude(),
//                        (float) ARData.getCurrentLocation().getLongitude(),
//                        (float) ARData.getCurrentLocation().getAltitude(),
//                        System.currentTimeMillis());
//
//                float dec = (float) Math.toRadians(-gmf.getDeclination());
//
//                synchronized (mageticNorthCompensation) {
//                    // Identity matrix
//                    // [ 1, 0, 0 ]
//                    // [ 0, 1, 0 ]
//                    // [ 0, 0, 1 ]
//                    mageticNorthCompensation.toIdentity();
//
//                    // Counter-clockwise rotation at negative declination around
//                    // the y-axis
//                    // note: declination of the horizontal component of the
//                    // magnetic field
//                    // from true north, in degrees (i.e. positive means the
//                    // magnetic
//                    // field is rotated east that much from true north).
//                    // note2: declination is the difference between true north
//                    // and magnetic north
//                    // [ cos, 0, sin ]
//                    // [ 0, 1, 0 ]
//                    // [ -sin, 0, cos ]
//                    mageticNorthCompensation.set(FloatMath.cos(dec), 0f, FloatMath.sin(dec),
//                            0f, 1f, 0f,
//                            -FloatMath.sin(dec), 0f, FloatMath.cos(dec));
//                }
//            } catch (Exception ex) {
//                ex.printStackTrace();
//            }
        } catch (Exception ex1) {
            try {
                if (sensorMgr != null) {
                    sensorMgr.unregisterListener(this, sensorGrav);
                    sensorMgr.unregisterListener(this, sensorMag);
                    sensorMgr = null;
                }
//                if (locationMgr != null) {
//                    locationMgr.removeUpdates(this);
//                    locationMgr = null;
//                }
            } catch (Exception ex2) {
                ex2.printStackTrace();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        try {
            try {
                sensorMgr.unregisterListener(this, sensorGrav);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            try {
                sensorMgr.unregisterListener(this, sensorMag);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            sensorMgr = null;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent evt) {

        if (evt.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            smooth = LowPassFilter.filter(0.5f, 1.0f, evt.values, grav);
            grav[0] = smooth[0];
            grav[1] = smooth[1];
            grav[2] = smooth[2];

            Orientation.calcOrientation(grav);

        } else if (evt.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {

            smooth = LowPassFilter.filter(2.0f, 4.0f, evt.values, mag);
            mag[0] = smooth[0];
            mag[1] = smooth[1];
            mag[2] = smooth[2];
        }

        // Get rotation matrix given the gravity and geomagnetic matrices
        SensorManager.getRotationMatrix(temp, null, grav, mag);
        SensorManager.remapCoordinateSystem(temp, SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_Z, rotation);

        //Get the azimuth, pitch, roll
        SensorManager.getOrientation(rotation, apr);
        float floatAzimuth = (float) Math.toDegrees(apr[0]);
        if (floatAzimuth < 0) floatAzimuth += 360;

        Log.d(TAG, floatAzimuth + ":" + apr[1] + ":" + apr[2]);
    }

    /**
     * Called when the accuracy of a sensor has changed.
     * <p>See {@link android.hardware.SensorManager SensorManager}
     * for details.
     *
     * @param sensor
     * @param accuracy The new accuracy of this sensor
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // empty method

    }
}
