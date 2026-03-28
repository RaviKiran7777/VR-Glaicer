package com.example.vrdesert.shapes;

import android.opengl.GLES20;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * Animated meltwater surface at the base of the glacier.
 * Uses vertex-shader sine waves for rolling water movement.
 * Semi-transparent dark blue with lighter crests.
 */
public class MeltWater {

    private final String vsCode =
        "uniform mat4 uMVPMatrix;" +
        "uniform float uTime;" +
        "attribute vec4 vPosition;" +
        "varying vec4 vColor;" +
        "void main() {" +
        "  float w1 = sin(vPosition.x * 0.5 + uTime * 1.3) * 0.25;" +
        "  float w2 = sin(vPosition.z * 0.6 + uTime * 0.8) * 0.18;" +
        "  float w3 = sin((vPosition.x + vPosition.z) * 0.35 + uTime * 1.7) * 0.12;" +
        "  vec4 pos = vPosition + vec4(0.0, w1 + w2 + w3, 0.0, 0.0);" +
        "  gl_Position = uMVPMatrix * pos;" +
        "  float h = (w1 + w2 + w3 + 0.55) / 1.1;" +
        "  vec4 deep  = vec4(0.02, 0.15, 0.38, 0.85);" +
        "  vec4 crest = vec4(0.3, 0.65, 0.88, 0.92);" +
        "  vColor = mix(deep, crest, h);" +
        "}";

    private final String fsCode =
        "precision mediump float;" +
        "varying vec4 vColor;" +
        "void main() { gl_FragColor = vColor; }";

    private FloatBuffer vertexBuffer;
    private ShortBuffer indexBuffer;
    private int mProgram;
    private int numIndices;
    private long startTime;

    private static final int GRID = 40;

    public MeltWater(float width, float depth) {
        startTime = System.currentTimeMillis();
        float stepX = width / GRID;
        float stepZ = depth / GRID;

        int numVerts = (GRID+1) * (GRID+1);
        float[] v = new float[numVerts * 3];
        int vi = 0;
        for (int r = 0; r <= GRID; r++) {
            for (int c = 0; c <= GRID; c++) {
                v[vi++] = c * stepX - width / 2f;
                v[vi++] = 0.05f; // slightly above ground to prevent z-fighting
                v[vi++] = r * stepZ - depth / 2f;
            }
        }

        short[] idx = new short[GRID * GRID * 6];
        int ii = 0;
        for (int r = 0; r < GRID; r++) {
            for (int c = 0; c < GRID; c++) {
                short tl = (short)(r * (GRID+1) + c);
                short tr = (short)(tl + 1);
                short bl = (short)(tl + (GRID+1));
                short br = (short)(bl + 1);
                idx[ii++] = tl; idx[ii++] = bl; idx[ii++] = tr;
                idx[ii++] = tr; idx[ii++] = bl; idx[ii++] = br;
            }
        }
        numIndices = ii;

        ByteBuffer bb = ByteBuffer.allocateDirect(v.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(v).position(0);

        ByteBuffer ib = ByteBuffer.allocateDirect(idx.length * 2);
        ib.order(ByteOrder.nativeOrder());
        indexBuffer = ib.asShortBuffer();
        indexBuffer.put(idx).position(0);

        int vs = loadShader(GLES20.GL_VERTEX_SHADER, vsCode);
        int fs = loadShader(GLES20.GL_FRAGMENT_SHADER, fsCode);
        mProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgram, vs);
        GLES20.glAttachShader(mProgram, fs);
        GLES20.glLinkProgram(mProgram);
    }

    public void draw(float[] mvp) {
        float time = (System.currentTimeMillis() - startTime) / 1000f;
        GLES20.glUseProgram(mProgram);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        int pos = GLES20.glGetAttribLocation(mProgram, "vPosition");
        GLES20.glEnableVertexAttribArray(pos);
        GLES20.glVertexAttribPointer(pos, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(mProgram, "uMVPMatrix"), 1, false, mvp, 0);
        GLES20.glUniform1f(GLES20.glGetUniformLocation(mProgram, "uTime"), time);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, numIndices, GLES20.GL_UNSIGNED_SHORT, indexBuffer);
        GLES20.glDisableVertexAttribArray(pos);
        GLES20.glDisable(GLES20.GL_BLEND);
    }

    private int loadShader(int type, String code) {
        int s = GLES20.glCreateShader(type);
        GLES20.glShaderSource(s, code);
        GLES20.glCompileShader(s);
        return s;
    }
}
