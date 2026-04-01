package com.example.vrdesert.shapes;

import android.opengl.GLES20;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Low-poly Snowy Owl model for Arctic immersion.
 */
public class SnowyOwl {
    private FloatBuffer vertexBuffer;
    private FloatBuffer colorBuffer;
    private int mProgram;
    private int vertexCount;

    public float worldX, worldY, worldZ;

    public SnowyOwl(int programId, float wx, float wy, float wz) {
        this.mProgram = programId;
        this.worldX = wx; this.worldY = wy; this.worldZ = wz;

        List<Float> verts = new ArrayList<>();
        List<Float> colors = new ArrayList<>();

        float r = 1.0f, g = 1.0f, b = 1.0f; // white

        // Body
        addCube(verts, colors, 0f, 0.4f, 0f, 0.5f, 0.6f, 0.45f, r, g, b);
        // Head
        addCube(verts, colors, 0f, 0.8f, 0.05f, 0.4f, 0.35f, 0.35f, r, g, b);
        // Eyes (yellow)
        addCube(verts, colors, -0.1f, 0.85f, 0.2f, 0.12f, 0.12f, 0.05f, 1.0f, 0.9f, 0.0f);
        addCube(verts, colors,  0.1f, 0.85f, 0.2f, 0.12f, 0.12f, 0.05f, 1.0f, 0.9f, 0.0f);
        // Beak (black)
        addCube(verts, colors, 0f, 0.78f, 0.22f, 0.05f, 0.1f, 0.05f, 0.1f, 0.1f, 0.1f);
        // Wings
        addCube(verts, colors, -0.3f, 0.45f, 0f, 0.1f, 0.5f, 0.4f, r*0.95f, g*0.95f, b*0.95f);
        addCube(verts, colors,  0.3f, 0.45f, 0f, 0.1f, 0.5f, 0.4f, r*0.95f, g*0.95f, b*0.95f);

        vertexCount = verts.size() / 3;
        vertexBuffer = toBuffer(verts);
        colorBuffer  = toBuffer(colors);
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
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);
        GLES20.glDisableVertexAttribArray(pos);
        GLES20.glDisableVertexAttribArray(col);
    }

    private static final float[] UNIT_CUBE = {
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

    private void addCube(List<Float> v, List<Float> c, float tx, float ty, float tz,
                         float sx, float sy, float sz, float cr, float cg, float cb) {
        for (int i = 0; i < UNIT_CUBE.length; i += 3) {
            v.add(UNIT_CUBE[i]   * sx + tx);
            v.add(UNIT_CUBE[i+1] * sy + ty);
            v.add(UNIT_CUBE[i+2] * sz + tz);
            int face = i / 18;
            float shade = (face == 4) ? 1.15f : (face < 2 ? 0.95f : 0.75f);
            c.add(Math.min(1f, cr * shade));
            c.add(Math.min(1f, cg * shade));
            c.add(Math.min(1f, cb * shade));
            c.add(1.0f);
        }
    }

    private FloatBuffer toBuffer(List<Float> list) {
        float[] arr = new float[list.size()];
        for (int i = 0; i < arr.length; i++) arr[i] = list.get(i);
        ByteBuffer bb = ByteBuffer.allocateDirect(arr.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(arr).position(0);
        return fb;
    }
}
