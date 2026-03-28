package com.example.vrdesert.shapes;

import android.opengl.GLES20;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * A flat icy ground surface extending in all directions.
 * Light blue-white color to simulate frozen ice/snow ground.
 */
public class IceGround {

    private FloatBuffer vertexBuffer;
    private FloatBuffer colorBuffer;
    private int mProgram;
    private static final int VERTEX_COUNT = 6;

    public IceGround(int programId) {
        this.mProgram = programId;

        float S = 80f; // half-extent
        float[] verts = {
            -S, 0, -S,   S, 0, -S,   -S, 0, S,
            -S, 0,  S,   S, 0, -S,    S, 0, S
        };

        float[] colors = new float[VERTEX_COUNT * 4];
        for (int i = 0; i < VERTEX_COUNT; i++) {
            colors[i*4]   = 0.82f;  // R — icy white-blue
            colors[i*4+1] = 0.88f;  // G
            colors[i*4+2] = 0.95f;  // B
            colors[i*4+3] = 1.0f;   // A
        }

        vertexBuffer = makeBuffer(verts);
        colorBuffer  = makeBuffer(colors);
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
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(data).position(0);
        return fb;
    }
}
