package com.example.vrdesert.shapes;

import android.opengl.GLES20;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Random;

/**
 * A burst-style particle system triggered when a calving ice block hits the water.
 * When trigger() is called with a (x, z) position, 200 particles are launched
 * upward and outward with random velocities, arc under gravity, and fade out.
 */
public class SplashParticles {

    private static final int PARTICLE_COUNT = 200;
    private static final float GRAVITY = -9.8f;
    private static final float LIFETIME = 1.8f; // seconds before particles disappear

    private final String vertexShaderCode =
        "uniform mat4 uMVPMatrix;" +
        "attribute vec4 vPosition;" +
        "attribute float aAlpha;" +
        "varying float vAlpha;" +
        "void main() {" +
        "  gl_Position = uMVPMatrix * vPosition;" +
        "  gl_PointSize = max(4.0, 20.0 / (gl_Position.w + 1.0));" +
        "  vAlpha = aAlpha;" +
        "}";

    private final String fragmentShaderCode =
        "precision mediump float;" +
        "varying float vAlpha;" +
        "void main() {" +
        "  vec2 coord = gl_PointCoord - vec2(0.5);" +
        "  if(length(coord) > 0.5) discard;" +
        // White-blue water splash color
        "  gl_FragColor = vec4(0.7, 0.9, 1.0, vAlpha);" +
        "}";

    private FloatBuffer vertexBuffer;
    private FloatBuffer alphaBuffer;
    private int mProgram;

    private float[] posX = new float[PARTICLE_COUNT];
    private float[] posY = new float[PARTICLE_COUNT];
    private float[] posZ = new float[PARTICLE_COUNT];
    private float[] velX = new float[PARTICLE_COUNT];
    private float[] velY = new float[PARTICLE_COUNT];
    private float[] velZ = new float[PARTICLE_COUNT];

    private float[] coords = new float[PARTICLE_COUNT * 3];
    private float[] alphas = new float[PARTICLE_COUNT];

    private boolean active = false;
    private float age = 0f;
    private long lastTime = 0;

    private final Random rand = new Random();

    public SplashParticles() {
        ByteBuffer bb = ByteBuffer.allocateDirect(coords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();

        ByteBuffer ab = ByteBuffer.allocateDirect(alphas.length * 4);
        ab.order(ByteOrder.nativeOrder());
        alphaBuffer = ab.asFloatBuffer();

        int vs = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fs = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
        mProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgram, vs);
        GLES20.glAttachShader(mProgram, fs);
        GLES20.glLinkProgram(mProgram);
    }

    /** Triggers a new splash burst at world position (x, 0, z). */
    public void trigger(float x, float z) {
        active = true;
        age = 0f;
        lastTime = System.currentTimeMillis();

        for (int i = 0; i < PARTICLE_COUNT; i++) {
            posX[i] = x + (rand.nextFloat() - 0.5f) * 2f;
            posY[i] = 0.2f;
            posZ[i] = z + (rand.nextFloat() - 0.5f) * 2f;

            // Random upward explosion + radial scatter
            float angle = rand.nextFloat() * (float)(Math.PI * 2);
            float speed = 3f + rand.nextFloat() * 8f;
            velX[i] = (float)(Math.cos(angle)) * speed * 0.6f;
            velY[i] = 5f + rand.nextFloat() * 9f; // strong upward kick
            velZ[i] = (float)(Math.sin(angle)) * speed * 0.6f;
        }
    }

    public boolean isActive() { return active; }

    /** Advances particle physics. Call once per frame (not per eye). */
    public void update() {
        if (!active) return;

        long now = System.currentTimeMillis();
        float dt = (now - lastTime) / 1000f;
        if (dt > 0.1f) dt = 0.1f;
        lastTime = now;
        age += dt;

        if (age >= LIFETIME) {
            active = false;
            return;
        }

        float t = age / LIFETIME;
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            velY[i] += GRAVITY * dt;
            posX[i] += velX[i] * dt;
            posY[i] += velY[i] * dt;
            posZ[i] += velZ[i] * dt;
            if (posY[i] < 0f) { posY[i] = 0f; velY[i] = 0f; }
            coords[i*3]     = posX[i];
            coords[i*3 + 1] = posY[i];
            coords[i*3 + 2] = posZ[i];
            alphas[i] = Math.max(0f, 1.0f - t * t);
        }
        vertexBuffer.position(0); vertexBuffer.put(coords); vertexBuffer.position(0);
        alphaBuffer.position(0);  alphaBuffer.put(alphas);  alphaBuffer.position(0);
    }

    /** Draws the current particle state. Call once per eye per frame. */
    public void updateAndDraw(float[] mvpMatrix) {
        if (!active) return;
        GLES20.glUseProgram(mProgram);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        int posHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        GLES20.glEnableVertexAttribArray(posHandle);
        GLES20.glVertexAttribPointer(posHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        int alphaHandle = GLES20.glGetAttribLocation(mProgram, "aAlpha");
        GLES20.glEnableVertexAttribArray(alphaHandle);
        GLES20.glVertexAttribPointer(alphaHandle, 1, GLES20.GL_FLOAT, false, 0, alphaBuffer);

        int mvpHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        GLES20.glUniformMatrix4fv(mvpHandle, 1, false, mvpMatrix, 0);

        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, PARTICLE_COUNT);

        GLES20.glDisableVertexAttribArray(posHandle);
        GLES20.glDisableVertexAttribArray(alphaHandle);
        GLES20.glDisable(GLES20.GL_BLEND);
    }

    private int loadShader(int type, String code) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, code);
        GLES20.glCompileShader(shader);
        return shader;
    }
}
