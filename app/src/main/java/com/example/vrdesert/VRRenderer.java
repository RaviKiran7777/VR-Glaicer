package com.example.vrdesert;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import java.util.List;

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

    // Shared shader
    private int shaderProgram;

    // Matrices
    private float[] leftProj  = new float[16];
    private float[] rightProj = new float[16];
    private float[] viewMat   = new float[16];
    private float[] vpMat     = new float[16];
    private float[] scratch   = new float[16];
    private float[] model     = new float[16];
    private float[] uiProj    = new float[16];
    private float[] uiModel   = new float[16];
    private float[] uiMVP     = new float[16];

    // Camera
    private float camX = 0f, camY = 1.6f, camZ = 20f;
    private static final float EYE_OFFSET = 0.05f;
    private int width, height;

    // Climate mode
    private boolean isPastMode = false; // false = TODAY, true = 50 YEARS AGO

    // Deferred UI state
    private String lastInfoText = "";
    private boolean needsInfoUpdate = false;

    // Vertex + Fragment shaders with distance fog
    private final String vertexShader =
        "uniform mat4 uMVPMatrix;" +
        "attribute vec4 vPosition;" +
        "attribute vec4 vColor;" +
        "varying vec4 _vColor;" +
        "varying float vDist;" +
        "void main() {" +
        "  gl_Position = uMVPMatrix * vPosition;" +
        "  _vColor = vColor;" +
        "  vDist = gl_Position.w;" +
        "}";

    private final String fragmentShader =
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

        // Register gaze targets
        gazeInfoManager.registerTarget(InfoData.TARGET_GLACIER, 0f, 12f, -30f, 25f, InfoData.getFact(InfoData.TARGET_GLACIER));
        gazeInfoManager.registerTarget(InfoData.TARGET_WATER, 0f, 0f, -22f, 12f, InfoData.getFact(InfoData.TARGET_WATER));
        gazeInfoManager.registerTarget(InfoData.TARGET_BEAR, polarBear.worldX, 1f, polarBear.worldZ, 5f, InfoData.getFact(InfoData.TARGET_BEAR));
        gazeInfoManager.registerTarget(InfoData.TARGET_SEAL, seal.worldX, 0.5f, seal.worldZ, 5f, InfoData.getFact(InfoData.TARGET_SEAL));
        gazeInfoManager.registerTarget(InfoData.TARGET_FOX, arcticFox.worldX, 0.5f, arcticFox.worldZ, 5f, InfoData.getFact(InfoData.TARGET_FOX));
    }

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

        // 2. MELT WATER (near glacier base)
        float waterScale = isPastMode ? 0.3f : 1.0f;
        Matrix.setIdentityM(model, 0);
        Matrix.translateM(model, 0, 0f, 0.02f, -22f);
        Matrix.scaleM(model, 0, waterScale, 1f, waterScale);
        Matrix.multiplyMM(scratch, 0, vp, 0, model, 0);
        meltWater.draw(scratch);

        // 3. GLACIER WALL
        float wallScaleY = isPastMode ? 1.5f : 1.0f;
        Matrix.setIdentityM(model, 0);
        Matrix.translateM(model, 0, 0f, 0f, CalvingManager.WALL_Z);
        Matrix.scaleM(model, 0, 1f, wallScaleY, 1f);
        Matrix.multiplyMM(scratch, 0, vp, 0, model, 0);
        glacierWall.draw(scratch);

        // 4. FALLING ICE CHUNKS
        if (!isPastMode) {
            List<CalvingManager.IceChunk> chunks = calvingManager.getActiveChunks();
            for (CalvingManager.IceChunk c : chunks) {
                Matrix.setIdentityM(model, 0);
                Matrix.translateM(model, 0, c.x, c.y, c.z);
                Matrix.scaleM(model, 0, c.scale, c.scale, c.scale);
                Matrix.multiplyMM(scratch, 0, vp, 0, model, 0);
                iceChunkModel.draw(scratch);
            }
        }

        // 5. SPLASH PARTICLES
        Matrix.setIdentityM(model, 0);
        Matrix.multiplyMM(scratch, 0, vp, 0, model, 0);
        splashParticles.updateAndDraw(scratch);

        // 6. ANIMALS
        Matrix.setIdentityM(model, 0);
        Matrix.translateM(model, 0, polarBear.worldX, polarBear.worldY, polarBear.worldZ);
        Matrix.multiplyMM(scratch, 0, vp, 0, model, 0);
        polarBear.draw(scratch);

        Matrix.setIdentityM(model, 0);
        Matrix.translateM(model, 0, seal.worldX, seal.worldY, seal.worldZ);
        Matrix.multiplyMM(scratch, 0, vp, 0, model, 0);
        seal.draw(scratch);

        Matrix.setIdentityM(model, 0);
        Matrix.translateM(model, 0, arcticFox.worldX, arcticFox.worldY, arcticFox.worldZ);
        Matrix.multiplyMM(scratch, 0, vp, 0, model, 0);
        arcticFox.draw(scratch);

        // 7. SNOW PARTICLES (last)
        snowParticles.updateAndDraw(vp, camX, camZ);
    }

    private void drawUI() {
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        float ew = width / 2f;

        // Title — top center
        Matrix.setIdentityM(uiModel, 0);
        Matrix.translateM(uiModel, 0, (ew / 2f) - 256f, 10f, 0f);
        Matrix.scaleM(uiModel, 0, 512f, 512f, 1f);
        Matrix.multiplyMM(uiMVP, 0, uiProj, 0, uiModel, 0);
        titleOverlay.draw(uiMVP);

        // Info panel — bottom center
        String fact = gazeInfoManager.getCurrentFact();
        if (!fact.isEmpty()) {
            Matrix.setIdentityM(uiModel, 0);
            Matrix.translateM(uiModel, 0, (ew / 2f) - 256f, height - 280f, 0f);
            Matrix.scaleM(uiModel, 0, 512f, 512f, 1f);
            Matrix.multiplyMM(uiMVP, 0, uiProj, 0, uiModel, 0);
            infoOverlay.draw(uiMVP);
        }

        // Crosshair — center
        Matrix.setIdentityM(uiModel, 0);
        Matrix.translateM(uiModel, 0, ew / 2f, height / 2f, 0f);
        Matrix.multiplyMM(uiMVP, 0, uiProj, 0, uiModel, 0);
        crosshair.draw(uiMVP);

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }

    private int loadShader(int type, String code) {
        int s = GLES20.glCreateShader(type);
        GLES20.glShaderSource(s, code);
        GLES20.glCompileShader(s);
        return s;
    }
}
