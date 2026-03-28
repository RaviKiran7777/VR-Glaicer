package com.example.vrdesert;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.widget.Button;
import android.opengl.GLSurfaceView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private GLSurfaceView glSurfaceView;
    private VRRenderer vrRenderer;
    private SensorHandler sensorHandler;
    private GazeInfoManager gazeInfoManager;
    private SoundEngine soundEngine;
    private TextToSpeech tts;
    private boolean ttsReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        glSurfaceView = findViewById(R.id.glSurfaceView);
        glSurfaceView.setEGLContextClientVersion(2);

        sensorHandler   = new SensorHandler(this);
        gazeInfoManager = new GazeInfoManager();
        soundEngine     = new SoundEngine();

        // Text-to-Speech for reading educational facts aloud
        tts = new TextToSpeech(this, this);

        // When any fact triggers, speak it
        gazeInfoManager.setOnFactListener(factText -> {
            if (ttsReady && tts != null) {
                tts.speak(factText, TextToSpeech.QUEUE_FLUSH, null, "fact");
            }
        });

        vrRenderer = new VRRenderer(sensorHandler, gazeInfoManager);
        vrRenderer.setSoundEngine(soundEngine);
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
                sensorHandler.setSensitivity(1.5f);
                btnSens.setText("SENS: MED");
            } else if (cur < 2.0f) {
                sensorHandler.setSensitivity(2.5f);
                btnSens.setText("SENS: HIGH");
            } else {
                sensorHandler.setSensitivity(0.8f);
                btnSens.setText("SENS: LOW");
            }
        });
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(Locale.US);
            tts.setSpeechRate(0.85f); // Slightly slower for educational clarity
            ttsReady = true;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        glSurfaceView.onResume();
        sensorHandler.start();
        soundEngine.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        glSurfaceView.onPause();
        sensorHandler.stop();
        soundEngine.stop();
        if (tts != null) tts.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tts != null) { tts.shutdown(); tts = null; }
        soundEngine.stop();
    }
}
