package com.example.vrdesert.shapes;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import android.opengl.GLES20;

/**
 * A simple colored cube with baked directional lighting.
 * Used for falling ice chunks.
 */
public class Cube {

    private final FloatBuffer vertexBuffer, colorBuffer;
    private int mProgram;
    private static final int VERTEX_COUNT = 36;

    static float[] COORDS = {
        -0.5f,-0.5f, 0.5f,  0.5f,-0.5f, 0.5f, -0.5f, 0.5f, 0.5f,
        -0.5f, 0.5f, 0.5f,  0.5f,-0.5f, 0.5f,  0.5f, 0.5f, 0.5f,
        -0.5f,-0.5f,-0.5f, -0.5f, 0.5f,-0.5f,  0.5f,-0.5f,-0.5f,
         0.5f,-0.5f,-0.5f, -0.5f, 0.5f,-0.5f,  0.5f, 0.5f,-0.5f,
        -0.5f,-0.5f,-0.5f, -0.5f,-0.5f, 0.5f, -0.5f, 0.5f,-0.5f,
        -0.5f, 0.5f,-0.5f, -0.5f,-0.5f, 0.5f, -0.5f, 0.5f, 0.5f,
         0.5f,-0.5f,-0.5f,  0.5f, 0.5f,-0.5f,  0.5f,-0.5f, 0.5f,
         0.5f,-0.5f, 0.5f,  0.5f, 0.5f,-0.5f,  0.5f, 0.5f, 0.5f,
        -0.5f, 0.5f,-0.5f, -0.5f, 0.5f, 0.5f,  0.5f, 0.5f,-0.5f,
         0.5f, 0.5f,-0.5f, -0.5f, 0.5f, 0.5f,  0.5f, 0.5f, 0.5f,
        -0.5f,-0.5f,-0.5f,  0.5f,-0.5f,-0.5f, -0.5f,-0.5f, 0.5f,
        -0.5f,-0.5f, 0.5f,  0.5f,-0.5f,-0.5f,  0.5f,-0.5f, 0.5f
    };

    public Cube(int programId, float r, float g, float b) {
        this.mProgram = programId;
        vertexBuffer = makeBuffer(COORDS);
        float[] colors = new float[VERTEX_COUNT * 4];
        for (int i = 0; i < VERTEX_COUNT; i++) {
            int face = i / 6;
            float shade = (face == 4) ? 1.2f : (face < 2 ? 0.9f : 0.55f);
            colors[i*4]   = Math.min(1f, r * shade);
            colors[i*4+1] = Math.min(1f, g * shade);
            colors[i*4+2] = Math.min(1f, b * shade);
            colors[i*4+3] = 1.0f;
        }
        colorBuffer = makeBuffer(colors);
    }

    public void draw(float[] mvp) {
        GLES20.glUseProgram(mProgram);
        int pos = GLES20.glGetAttribLocation(mProgram, "vPosition");
        GLES20.glEnableVertexAttribArray(pos);
        GLES20.glVertexAttribPointer(pos, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        int col = GLES20.glGetAttribLocation(mProgram, "vColor");
        GLES20.glEnableVertexAttribArray(col);
        GLES20.glVertexAttribPointer(col, 4, GLES20.GL_FLOAT, false, 0, colorBuffer);
        int mvpH = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        GLES20.glUniformMatrix4fv(mvpH, 1, false, mvp, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, VERTEX_COUNT);
        GLES20.glDisableVertexAttribArray(pos);
        GLES20.glDisableVertexAttribArray(col);
    }

    private FloatBuffer makeBuffer(float[] data) {
        ByteBuffer bb = ByteBuffer.allocateDirect(data.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer(); fb.put(data).position(0); return fb;
    }
}
