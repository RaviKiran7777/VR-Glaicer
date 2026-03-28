package com.example.vrdesert;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import java.util.List;
import java.util.Random;

import com.example.vrdesert.shapes.*;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class VRRenderer implements GLSurfaceView.Renderer {

    private final SensorHandler sensorHandler;
    private final GazeInfoManager gazeInfoManager;

    // Environment
    private IceGround iceGround;
    private GlacierWall glacierWall;
    private MeltWater meltWater;
    private SnowParticles snowParticles;
    private SplashParticles splashParticles;
    private CalvingManager calvingManager;
    private Cube iceChunkModel;

    // Animals
    private PolarBear polarBear;
    private Seal seal;
    private ArcticFox arcticFox;

    // UI
    private Crosshair crosshair;
    private TextOverlay infoOverlay;
    private TextOverlay titleOverlay;

    // Projection Matrices
    private final float[] leftProj = new float[16];
    private final float[] rightProj = new float[16];
    private final float[] uiProj = new float[16];
    private final float[] viewMat = new float[16];
    private final float[] vpMat = new float[16];
    private final float[] model = new float[16];
    private final float[] scratch = new float[16];

    // State
    private float camX = 0, camY = 1.6f, camZ = 5f;
    private int width, height;
    private static final float EYE_OFFSET = 0.035f;
    private boolean isPastMode = false;
    private String lastInfoText = "";

    // Time-based AI update
    private long lastAiTime = 0;

    private static final String vertexShader =
        "uniform mat4 uVPMatrix;" +
        "attribute vec4 vPosition;" +
        "attribute vec4 vColor;" +
        "varying vec4 _vColor;" +
        "varying float vDist;" +
        "void main() {" +
        "  gl_Position = uVPMatrix * vPosition;" +
        "  _vColor = vColor;" +
        "  vDist = gl_Position.z;" +
        "}";

    private static final String fragmentShader =
        "precision mediump float;" +
        "varying vec4 _vColor;" +
        "varying float vDist;" +
        "void main() {" +
        "  float fog = clamp((vDist - 25.0) / 60.0, 0.0, 1.0);" +
        "  vec4 fogCol = vec4(0.75, 0.87, 0.96, 1.0);" +
        "  gl_FragColor = mix(_vColor, fogCol, fog);" +
        "}";

    public VRRenderer(SensorHandler sh, GazeInfoManager gm) {
        this.sensorHandler = sh;
        this.gazeInfoManager = gm;
    }

    private SoundEngine soundEngine;
    public void setSoundEngine(SoundEngine se) {
        this.soundEngine = se;
        if (calvingManager != null) calvingManager.setSoundEngine(se);
    }

    public void moveForward() {
        float yawRad = (float) Math.toRadians(sensorHandler.getYaw());
        camX += (float) -Math.sin(yawRad) * 1.5f;
        camZ += (float) -Math.cos(yawRad) * 1.5f;
    }

    public void setClimateMode(boolean past) {
        this.isPastMode = past;
        calvingManager.setEnabled(!past);
    }

    public boolean isPastMode() { return isPastMode; }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        GLES20.glClearColor(0.72f, 0.85f, 0.96f, 1.0f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        int vs = loadShader(GLES20.GL_VERTEX_SHADER, vertexShader);
        int fs = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);
        shaderProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(shaderProgram, vs);
        GLES20.glAttachShader(shaderProgram, fs);
        GLES20.glLinkProgram(shaderProgram);

        // Environment
        iceGround    = new IceGround(shaderProgram);
        glacierWall  = new GlacierWall(shaderProgram, 42L);
        meltWater    = new MeltWater(50f, 20f);
        snowParticles = new SnowParticles();
        splashParticles = new SplashParticles();
        calvingManager = new CalvingManager(splashParticles, gazeInfoManager);
        if (soundEngine != null) calvingManager.setSoundEngine(soundEngine);

        iceChunkModel = new Cube(shaderProgram, 0.75f, 0.9f, 1.0f);

        // Animals — positioned on the ice
        polarBear = new PolarBear(shaderProgram, 8f, 0f, -10f);
        seal      = new Seal(shaderProgram, -7f, 0f, -8f);
        arcticFox = new ArcticFox(shaderProgram, 12f, 0f, -5f);

        // UI
        crosshair    = new Crosshair();
        infoOverlay  = new TextOverlay();
        titleOverlay = new TextOverlay();
        titleOverlay.updateText("VR Glacier Observation");

        // Register all educational gaze targets
        gazeInfoManager.registerTarget(InfoData.TARGET_GLACIER, 0f, 12f, -30f, 25f, InfoData.getFact(InfoData.TARGET_GLACIER));
        gazeInfoManager.registerTarget(InfoData.TARGET_WATER, 0f, 0f, -22f, 20f, InfoData.getFact(InfoData.TARGET_WATER));
        gazeInfoManager.registerTarget(InfoData.TARGET_BEAR, 8f, 1f, -10f, 3f, InfoData.getFact(InfoData.TARGET_BEAR));
        gazeInfoManager.registerTarget(InfoData.TARGET_SEAL, -7f, 0.5f, -8f, 3f, InfoData.getFact(InfoData.TARGET_SEAL));
        gazeInfoManager.registerTarget(InfoData.TARGET_FOX, 12f, 0.5f, -5f, 2f, InfoData.getFact(InfoData.TARGET_FOX));
    }

    private int shaderProgram;

    @Override
    public void onSurfaceChanged(GL10 unused, int w, int h) {
        this.width = w; this.height = h;
        GLES20.glViewport(0, 0, w, h);
        float ratio = (float)(w / 2) / h;
        Matrix.perspectiveM(leftProj,  0, 80f, ratio, 0.1f, 250f);
        Matrix.perspectiveM(rightProj, 0, 80f, ratio, 0.1f, 250f);
        Matrix.orthoM(uiProj, 0, 0, w / 2, h, 0, -1, 1);
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        // Deferred UI updates on GL thread
        String fact = gazeInfoManager.getCurrentFact();
        if (!fact.equals(lastInfoText)) {
            lastInfoText = fact;
            if (!fact.isEmpty()) infoOverlay.updateText(fact);
        }
        crosshair.setTargeting(gazeInfoManager.isTargeting());

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        float pitch = sensorHandler.getPitch();
        float yaw   = sensorHandler.getYaw();
        float yr = (float) Math.toRadians(yaw);
        float pr = (float) Math.toRadians(pitch);

        float fx = (float)(-Math.sin(yr) * Math.cos(pr));
        float fy = (float)  Math.sin(-pr);
        float fz = (float)(-Math.cos(yr) * Math.cos(pr));

        float tx = camX + fx, ty = camY + fy, tz = camZ + fz;

        // Gaze check
        gazeInfoManager.checkGaze(camX, camY, camZ, fx, fy, fz);

        // Snow fact when looking up
        if (pitch < -25f) {
            gazeInfoManager.showFact(InfoData.getFact(InfoData.TARGET_SNOW));
        }

        // Physics (once per frame)
        calvingManager.update();
        splashParticles.update();

        // Animal AI (once per frame)
        updateAnimals();

        // LEFT EYE
        GLES20.glViewport(0, 0, width / 2, height);
        float lx = (float) Math.cos(yr) * EYE_OFFSET;
        float lz = (float)-Math.sin(yr) * EYE_OFFSET;
        Matrix.setLookAtM(viewMat, 0, camX-lx, camY, camZ-lz, tx-lx, ty, tz-lz, 0, 1, 0);
        Matrix.multiplyMM(vpMat, 0, leftProj, 0, viewMat, 0);
        drawScene(vpMat);
        drawUI();

        // RIGHT EYE
        GLES20.glViewport(width / 2, 0, width / 2, height);
        float rx = (float)-Math.cos(yr) * EYE_OFFSET;
        float rz = (float) Math.sin(yr) * EYE_OFFSET;
        Matrix.setLookAtM(viewMat, 0, camX-rx, camY, camZ-rz, tx-rx, ty, tz-rz, 0, 1, 0);
        Matrix.multiplyMM(vpMat, 0, rightProj, 0, viewMat, 0);
        drawScene(vpMat);
        drawUI();
    }

    private void drawScene(float[] vp) {
        // 1. ICE GROUND
        Matrix.setIdentityM(model, 0);
        Matrix.multiplyMM(scratch, 0, vp, 0, model, 0);
        iceGround.draw(scratch);

        // 2. GLACIER WALL (Only show TODAY mode if NOT pastMode)
        if (!isPastMode) {
            Matrix.setIdentityM(model, 0);
            Matrix.multiplyMM(scratch, 0, vp, 0, model, 0);
            glacierWall.draw(scratch);
        }

        // 3. MELT WATER
        Matrix.setIdentityM(model, 0);
        Matrix.multiplyMM(scratch, 0, vp, 0, model, 0);
        meltWater.draw(scratch);

        // 4. SNOW PARTICLES
        snowParticles.draw(vp);

        // 5. CALVING CHUNKS
        for (CalvingManager.IceChunk chunk : calvingManager.getActiveChunks()) {
            Matrix.setIdentityM(model, 0);
            Matrix.translateM(model, 0, chunk.x, chunk.y, chunk.z);
            Matrix.rotateM(model, 0, chunk.rotX, 1, 0, 0);
            Matrix.rotateM(model, 0, chunk.rotY, 0, 1, 0);
            Matrix.rotateM(model, 0, chunk.rotZ, 0, 0, 1);
            Matrix.scaleM(model, 0, chunk.scale, chunk.scale * 0.7f, chunk.scale * 0.8f);
            Matrix.multiplyMM(scratch, 0, vp, 0, model, 0);
            iceChunkModel.draw(scratch);
        }

        // 6. SPLASH PARTICLES
        splashParticles.draw(vp);

        // 7. ANIMALS
        drawAnimal(vp, polarBear);
        drawAnimal(vp, seal);
        drawAnimal(vp, arcticFox);
    }

    private void drawAnimal(float[] vp, BaseShape animal) {
        Matrix.setIdentityM(model, 0);
        Matrix.translateM(model, 0, animal.worldX, animal.worldY, animal.worldZ);
        Matrix.rotateM(model, 0, animal.rotationY, 0, 1, 0);
        Matrix.multiplyMM(scratch, 0, vp, 0, model, 0);
        animal.draw(scratch);
    }

    private void drawUI() {
        crosshair.draw(uiProj);
        infoOverlay.draw(uiProj, height - 200f);
        titleOverlay.draw(uiProj, 100f);
    }

    private void updateAnimals() {
        long now = System.currentTimeMillis();
        if (lastAiTime == 0) { lastAiTime = now; return; }
        float dt = (now - lastAiTime) / 1000f;
        lastAiTime = now;

        polarBear.update(dt);
        seal.update(dt);
        arcticFox.update(dt);
    }

    public static int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }
}
