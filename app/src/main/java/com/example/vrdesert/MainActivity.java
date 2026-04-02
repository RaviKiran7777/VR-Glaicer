package com.example.vrdesert;

import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.format.Formatter;
import android.widget.TextView;
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

    private RemoteControlServer remoteServer;
    private TextView tvUrl;

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
            if (ttsReady && tts != null && !soundEngine.isMuted()) {
                tts.speak(factText, TextToSpeech.QUEUE_FLUSH, null, "fact");
            }
        });

        vrRenderer = new VRRenderer(sensorHandler, gazeInfoManager);
        vrRenderer.setSoundEngine(soundEngine);
        glSurfaceView.setRenderer(vrRenderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        // Display IP for remote control
        tvUrl = findViewById(R.id.tvUrl);
        updateIpDisplay();
    }

    private void updateIpDisplay() {
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        tvUrl.setText("Remote: http://" + ip + ":8080");
    }

    // --- Remote Control Callbacks ---

    public void onRemoteMove() {
        vrRenderer.moveForward();
    }

    public void onRemoteReset() {
        sensorHandler.resetView();
    }

    public void onRemoteClimateToggle() {
        boolean newMode = !vrRenderer.isPastMode();
        vrRenderer.setClimateMode(newMode);
    }

    public void onRemoteSensitivityCycle() {
        float cur = sensorHandler.getSensitivity();
        if (cur < 1.0f) sensorHandler.setSensitivity(1.5f);
        else if (cur < 2.0f) sensorHandler.setSensitivity(2.5f);
        else sensorHandler.setSensitivity(0.8f);
    }

    public void onRemoteSoundToggle() {
        boolean newMuteState = !soundEngine.isMuted();
        soundEngine.setMuted(newMuteState);
        if (newMuteState && tts != null) {
            tts.stop(); // Immediately stop current speech if muting
        }
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

        remoteServer = new RemoteControlServer(8080, this);
        remoteServer.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        glSurfaceView.onPause();
        sensorHandler.stop();
        soundEngine.stop();
        if (tts != null) tts.stop();

        if (remoteServer != null) {
            remoteServer.stopServer();
            remoteServer = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tts != null) { tts.shutdown(); tts = null; }
        soundEngine.stop();
    }
}
