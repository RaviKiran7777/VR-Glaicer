package com.example.vrdesert;

import android.os.Bundle;
import android.widget.Button;
import android.opengl.GLSurfaceView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private GLSurfaceView glSurfaceView;
    private VRRenderer vrRenderer;
    private SensorHandler sensorHandler;
    private GazeInfoManager gazeInfoManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        glSurfaceView = findViewById(R.id.glSurfaceView);
        glSurfaceView.setEGLContextClientVersion(2);

        sensorHandler   = new SensorHandler(this);
        gazeInfoManager = new GazeInfoManager();

        vrRenderer = new VRRenderer(sensorHandler, gazeInfoManager);
        glSurfaceView.setRenderer(vrRenderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        // MOVE button
        Button btnMove = findViewById(R.id.btnMove);
        btnMove.setOnClickListener(v -> vrRenderer.moveForward());

        // Climate comparison toggle
        Button btnClimate = findViewById(R.id.btnClimate);
        btnClimate.setOnClickListener(v -> {
            boolean newMode = !vrRenderer.isPastMode();
            vrRenderer.setClimateMode(newMode);
            btnClimate.setText(newMode ? "TODAY" : "50 YEARS AGO");
        });

        // Sensitivity toggle: LOW → MED → HIGH cycle
        Button btnSens = findViewById(R.id.btnSensitivity);
        btnSens.setOnClickListener(v -> {
            float cur = sensorHandler.getSensitivity();
            if (cur < 1.0f) {
                // Was LOW → go to MED
                sensorHandler.setSensitivity(1.5f);
                btnSens.setText("SENS: MED");
            } else if (cur < 2.0f) {
                // Was MED → go to HIGH
                sensorHandler.setSensitivity(2.5f);
                btnSens.setText("SENS: HIGH");
            } else {
                // Was HIGH → go to LOW
                sensorHandler.setSensitivity(0.8f);
                btnSens.setText("SENS: LOW");
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        glSurfaceView.onResume();
        sensorHandler.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        glSurfaceView.onPause();
        sensorHandler.stop();
    }
}
