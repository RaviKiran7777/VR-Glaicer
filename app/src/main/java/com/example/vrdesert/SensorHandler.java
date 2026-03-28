package com.example.vrdesert;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class SensorHandler implements SensorEventListener {

    private final SensorManager sensorManager;
    private final Sensor gyroscope;

    private float targetYaw = 0f, targetPitch = 0f;
    private float currentYaw = 0f, currentPitch = 0f;
    private float sensitivity = 1.5f; // Default: High responsiveness

    private long lastTime = 0;
    private static final float NS2S = 1.0f / 1000000000.0f;
    private static final float NOISE_THRESHOLD = 0.05f;
    private static final float RECENTER_SPEED = 8.0f;
    private static final float LERP_FACTOR = 0.15f;

    private long lastPitchMoveTimeNs = 0;
    private long misalignedStartTimeNs = 0;
    private static final long STILLNESS_DELAY_NS = 3500000000L;
    private static final long MAX_MISALIGN_DELAY_NS = 7500000000L;

    public SensorHandler(Context context) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        gyroscope = (sensorManager != null) ? sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) : null;
    }

    public void start() {
        if (gyroscope != null) sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME);
    }

    public void stop() {
        if (sensorManager != null) sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_GYROSCOPE) return;
        if (lastTime != 0) {
            float dT = (event.timestamp - lastTime) * NS2S;
            float yawRate = event.values[0];
            float pitchRate = event.values[1];

            if (Math.abs(pitchRate) > NOISE_THRESHOLD) lastPitchMoveTimeNs = event.timestamp;
            if (Math.abs(yawRate) < NOISE_THRESHOLD) yawRate = 0;
            if (Math.abs(pitchRate) < NOISE_THRESHOLD) pitchRate = 0;

            targetPitch += pitchRate * dT * (180f / (float)Math.PI) * sensitivity;
            targetYaw   += -yawRate  * dT * (180f / (float)Math.PI) * sensitivity;

            if (targetPitch > 45f) targetPitch = 45f;
            if (targetPitch < -45f) targetPitch = -45f;

            if (Math.abs(targetPitch) < 0.5f) {
                misalignedStartTimeNs = 0;
            } else if (misalignedStartTimeNs == 0) {
                misalignedStartTimeNs = event.timestamp;
            }

            boolean isStill = (event.timestamp - lastPitchMoveTimeNs) > STILLNESS_DELAY_NS;
            boolean isMisaligned = (misalignedStartTimeNs != 0) && ((event.timestamp - misalignedStartTimeNs) > MAX_MISALIGN_DELAY_NS);

            if (isStill || isMisaligned) {
                if (targetPitch > 0) targetPitch = Math.max(0, targetPitch - RECENTER_SPEED * dT);
                else if (targetPitch < 0) targetPitch = Math.min(0, targetPitch + RECENTER_SPEED * dT);
            }

            currentPitch += (targetPitch - currentPitch) * LERP_FACTOR;
            currentYaw   += (targetYaw   - currentYaw)   * LERP_FACTOR;
        }
        lastTime = event.timestamp;
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    public float getYaw()   { return currentYaw; }
    public float getPitch() { return currentPitch; }
    public void setSensitivity(float s) { this.sensitivity = s; }
    public float getSensitivity() { return sensitivity; }
}
