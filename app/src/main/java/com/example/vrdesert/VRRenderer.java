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
    private GlacierMountain glacierMountain;
    private IceSpires iceSpires;
    private IceArches iceArches;
    private CloudSystem cloudSystem;
    private MeltWater meltWater;
    private SnowParticles snowParticles;
    private SplashParticles splashParticles;
    private CalvingManager calvingManager;
    private Cube iceChunkModel;
    private IceFloe iceFloe;

    // Animals
    private PolarBear polarBear;
    private Seal seal;
    private ArcticFox arcticFox;
    
    // New Animal Instances for Populating the Scene
    private PolarBear polarBearCub;
    private Seal seal2;
    private Seal seal3;
    private ArcticFox arcticFox2;

    // Birds and Antarctic animals
    private Penguin penguin1;
    private Penguin penguin2;
    private Penguin penguin3;
    private SnowyOwl snowyOwl;

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

    private static final String vertexShader =
        "uniform mat4 uMVPMatrix;" +
        "attribute vec4 vPosition;" +
        "attribute vec4 vColor;" +
        "varying vec4 _vColor;" +
        "void main() {" +
        "  gl_Position = uMVPMatrix * vPosition;" +
        "  _vColor = vColor;" +
        "}";

    private static final String fragmentShader =
        "precision mediump float;" +
        "varying vec4 _vColor;" +
        "void main() {" +
        "  gl_FragColor = _vColor;" +
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
        glacierMountain = new GlacierMountain(shaderProgram);
        iceSpires       = new IceSpires(shaderProgram);
        iceArches       = new IceArches(shaderProgram);
        cloudSystem     = new CloudSystem(0); // Uses its own specialized shader

        meltWater    = new MeltWater(50f, 20f);
        snowParticles = new SnowParticles();
        splashParticles = new SplashParticles();
        calvingManager = new CalvingManager(splashParticles, gazeInfoManager);
        if (soundEngine != null) calvingManager.setSoundEngine(soundEngine);

        iceChunkModel = new Cube(shaderProgram, 0.75f, 0.9f, 1.0f);
        iceFloe       = new IceFloe(shaderProgram, 5.0f);

        // Animals — Brought closer for better visibility (z > -25)
        polarBear = new PolarBear(shaderProgram, 12f, 0.1f, -10f);
        seal      = new Seal(shaderProgram, -10f, 0.1f, -14f);
        arcticFox = new ArcticFox(shaderProgram, 16f, 0.1f, -12f);
        
        // New Animals — Midground placement
        polarBearCub = new PolarBear(shaderProgram, 15f, 0.1f, -9f);
        seal2        = new Seal(shaderProgram, -18f, 0.1f, -16f);
        seal3        = new Seal(shaderProgram, 20f, 0.1f, -18f);
        arcticFox2   = new ArcticFox(shaderProgram, -22f, 0.1f, -20f);

        // New Antarctic and Arctic Species
        penguin1 = new Penguin(shaderProgram, 0f, 0.1f, -15f);
        penguin2 = new Penguin(shaderProgram, 3f, 0.1f, -14f);
        penguin3 = new Penguin(shaderProgram, -3f, 0.1f, -16f);
        snowyOwl = new SnowyOwl(shaderProgram, -18f, 8f, -22f); // On a closer spire

        // UI
        crosshair    = new Crosshair();
        infoOverlay  = new TextOverlay();
        titleOverlay = new TextOverlay();
        titleOverlay.updateText("VR Glacier Observation");

        // Register all educational gaze targets
        gazeInfoManager.registerTarget(InfoData.TARGET_GLACIER, 0f, 12f, -25f, 30f, InfoData.getFact(InfoData.TARGET_GLACIER));
        gazeInfoManager.registerTarget(InfoData.TARGET_WATER,   0f, 0f, -22f, 25f, InfoData.getFact(InfoData.TARGET_WATER));
        
        // WildLife (All brought closer)
        gazeInfoManager.registerTarget(InfoData.TARGET_BEAR, 12f, 1f, -10f, 6f, InfoData.getFact(InfoData.TARGET_BEAR));
        gazeInfoManager.registerTarget(InfoData.TARGET_SEAL, -10f, 1f, -14f, 5f, InfoData.getFact(InfoData.TARGET_SEAL));
        gazeInfoManager.registerTarget(InfoData.TARGET_FOX,  16f, 1f, -12f, 4f, InfoData.getFact(InfoData.TARGET_FOX));

        gazeInfoManager.registerTarget(100, 15f, 1f, -9f, 4f, InfoData.getFact(InfoData.TARGET_BEAR));
        gazeInfoManager.registerTarget(101, -18f, 1f, -16f, 5f, InfoData.getFact(InfoData.TARGET_SEAL));
        gazeInfoManager.registerTarget(102, 20f, 1f, -18f, 5f, InfoData.getFact(InfoData.TARGET_SEAL));
        gazeInfoManager.registerTarget(103, -22f, 1f, -20f, 4f, InfoData.getFact(InfoData.TARGET_FOX));

        // New Species Gaze Targets
        gazeInfoManager.registerTarget(200, 0f, 1f, -15f, 4f, InfoData.getFact(InfoData.TARGET_PENGUIN));
        gazeInfoManager.registerTarget(201, 3f, 1f, -14f, 4f, InfoData.getFact(InfoData.TARGET_PENGUIN));
        gazeInfoManager.registerTarget(202, -3f, 1f, -16f, 4f, InfoData.getFact(InfoData.TARGET_PENGUIN));
        gazeInfoManager.registerTarget(203, -18f, 8.5f, -22f, 3f, InfoData.getFact(InfoData.TARGET_OWL));
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
        if (pitch < -25f) gazeInfoManager.showFact(InfoData.getFact(InfoData.TARGET_SNOW));

        // Physics
        calvingManager.update();

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
        // 1. OPAQUE ENVIRONMENT
        Matrix.setIdentityM(model, 0);
        Matrix.multiplyMM(scratch, 0, vp, 0, model, 0);
        iceGround.draw(scratch);

        // Render 360-degree environment
        Matrix.setIdentityM(model, 0);
        Matrix.multiplyMM(scratch, 0, vp, 0, model, 0);
        glacierMountain.draw(scratch); // Back
        iceSpires.draw(scratch);       // Left
        iceArches.draw(scratch);       // Right
        
        float globalTime = System.currentTimeMillis() / 1000f;
        cloudSystem.draw(scratch, globalTime);     // Top

        // Main Glacier Wall (Front)
        Matrix.setIdentityM(model, 0);
        Matrix.translateM(model, 0, 0, 0, -25f);
        if (isPastMode) {
            Matrix.scaleM(model, 0, 1.0f, 1.8f, 1.0f);
        }
        Matrix.multiplyMM(scratch, 0, vp, 0, model, 0);
        glacierWall.draw(scratch);

        // 2. ICE FLOES (Opaque platforms for animals)
        // Floe for Seal 2
        Matrix.setIdentityM(model, 0);
        Matrix.translateM(model, 0, -18f, 0, -16f);
        Matrix.scaleM(model, 0, 0.8f, 1.0f, 0.8f);
        Matrix.multiplyMM(scratch, 0, vp, 0, model, 0);
        iceFloe.draw(scratch);

        // Floe for Polar Bear Family
        Matrix.setIdentityM(model, 0);
        Matrix.translateM(model, 0, 13.5f, 0, -9.5f);
        Matrix.scaleM(model, 0, 1.2f, 1.0f, 1.2f);
        Matrix.multiplyMM(scratch, 0, vp, 0, model, 0);
        iceFloe.draw(scratch);

        // Floe for Seal 3
        Matrix.setIdentityM(model, 0);
        Matrix.translateM(model, 0, 20f, 0, -18f);
        Matrix.scaleM(model, 0, 0.8f, 1.0f, 0.8f);
        Matrix.multiplyMM(scratch, 0, vp, 0, model, 0);
        iceFloe.draw(scratch);

        // Floe for Penguin Colony
        Matrix.setIdentityM(model, 0);
        Matrix.translateM(model, 0, 0f, 0, -15f);
        Matrix.scaleM(model, 0, 1.5f, 1.0f, 1.5f);
        Matrix.multiplyMM(scratch, 0, vp, 0, model, 0);
        iceFloe.draw(scratch);

        // 3. ANIMALS
        // Original Polar Bear
        Matrix.setIdentityM(model, 0);
        Matrix.translateM(model, 0, polarBear.worldX, polarBear.worldY, polarBear.worldZ);
        Matrix.multiplyMM(scratch, 0, vp, 0, model, 0);
        polarBear.draw(scratch);

        // Original Seal
        Matrix.setIdentityM(model, 0);
        Matrix.translateM(model, 0, seal.worldX, seal.worldY, seal.worldZ);
        Matrix.multiplyMM(scratch, 0, vp, 0, model, 0);
        seal.draw(scratch);

        // Original Arctic Fox
        Matrix.setIdentityM(model, 0);
        Matrix.translateM(model, 0, arcticFox.worldX, arcticFox.worldY, arcticFox.worldZ);
        Matrix.multiplyMM(scratch, 0, vp, 0, model, 0);
        arcticFox.draw(scratch);
        
        // New Polar Bear Cub (smaller)
        Matrix.setIdentityM(model, 0);
        Matrix.translateM(model, 0, polarBearCub.worldX, polarBearCub.worldY, polarBearCub.worldZ);
        Matrix.scaleM(model, 0, 0.5f, 0.5f, 0.5f);
        Matrix.multiplyMM(scratch, 0, vp, 0, model, 0);
        polarBearCub.draw(scratch);

        // New Seals
        Matrix.setIdentityM(model, 0);
        Matrix.translateM(model, 0, seal2.worldX, seal2.worldY, seal2.worldZ);
        Matrix.multiplyMM(scratch, 0, vp, 0, model, 0);
        seal2.draw(scratch);

        Matrix.setIdentityM(model, 0);
        Matrix.translateM(model, 0, seal3.worldX, seal3.worldY, seal3.worldZ);
        Matrix.multiplyMM(scratch, 0, vp, 0, model, 0);
        seal3.draw(scratch);

        // New Species
        Matrix.setIdentityM(model, 0);
        Matrix.translateM(model, 0, penguin1.worldX, penguin1.worldY, penguin1.worldZ);
        Matrix.multiplyMM(scratch, 0, vp, 0, model, 0);
        penguin1.draw(scratch);

        Matrix.setIdentityM(model, 0);
        Matrix.translateM(model, 0, penguin2.worldX, penguin2.worldY, penguin2.worldZ);
        Matrix.multiplyMM(scratch, 0, vp, 0, model, 0);
        penguin2.draw(scratch);

        Matrix.setIdentityM(model, 0);
        Matrix.translateM(model, 0, penguin3.worldX, penguin3.worldY, penguin3.worldZ);
        Matrix.multiplyMM(scratch, 0, vp, 0, model, 0);
        penguin3.draw(scratch);

        Matrix.setIdentityM(model, 0);
        Matrix.translateM(model, 0, snowyOwl.worldX, snowyOwl.worldY, snowyOwl.worldZ);
        Matrix.multiplyMM(scratch, 0, vp, 0, model, 0);
        snowyOwl.draw(scratch);

        // 3. TRANSPARENT / ANIMATED ENVIRONMENT (Draw LAST)
        // Static meltwater base
        Matrix.setIdentityM(model, 0);
        Matrix.translateM(model, 0, 0, 0, -22f); // Align with wall base
        Matrix.multiplyMM(scratch, 0, vp, 0, model, 0);
        meltWater.draw(scratch);

        // Calving chunks
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

        // Snow and Splashes
        snowParticles.updateAndDraw(vp, camX, camZ);
        splashParticles.updateAndDraw(vp);
    }

    private void drawUI() {
        // 1. CROSSHAIR (Center of UI Viewport)
        Matrix.setIdentityM(model, 0);
        Matrix.translateM(model, 0, width / 4f, height / 2f, 0);
        Matrix.multiplyMM(scratch, 0, uiProj, 0, model, 0);
        crosshair.draw(scratch);

        // 2. INFO PANEL (Bottom Left)
        Matrix.setIdentityM(model, 0);
        Matrix.translateM(model, 0, 20f, height - 200f, 0);
        Matrix.scaleM(model, 0, 480f, 180f, 1f);
        Matrix.multiplyMM(scratch, 0, uiProj, 0, model, 0);
        infoOverlay.draw(scratch);

        // 3. TITLE PANEL (Top Left)
        Matrix.setIdentityM(model, 0);
        Matrix.translateM(model, 0, 20f, 80f, 0);
        Matrix.scaleM(model, 0, 350f, 80f, 1f);
        Matrix.multiplyMM(scratch, 0, uiProj, 0, model, 0);
        titleOverlay.draw(scratch);
    }

    public static int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }
}
