package com.example.vrdesert.shapes;

import android.opengl.GLES20;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Low-poly Polar Bear built from combined cube geometry.
 * All parts baked into a single vertex/color buffer for efficient draw.
 * Cream-white coloring with slight shading per face direction.
 */
public class PolarBear {

    private FloatBuffer vertexBuffer;
    private FloatBuffer colorBuffer;
    private int mProgram;
    private int vertexCount;

    // World position for gaze targeting
    public float worldX, worldY, worldZ;

    public PolarBear(int programId, float wx, float wy, float wz) {
        this.mProgram = programId;
        this.worldX = wx; this.worldY = wy; this.worldZ = wz;

        List<Float> verts = new ArrayList<>();
        List<Float> colors = new ArrayList<>();

        float r = 0.95f, g = 0.92f, b = 0.85f; // cream white

        // Body (center at 0, 0.8, 0)
        addCube(verts, colors, 0f, 0.8f, 0f, 1.6f, 1.0f, 0.9f, r, g, b);
        // Head
        addCube(verts, colors, 0f, 1.1f, 0.7f, 0.7f, 0.7f, 0.6f, r, g, b);
        // Snout
        addCube(verts, colors, 0f, 0.95f, 1.1f, 0.3f, 0.25f, 0.3f, 0.9f, 0.85f, 0.78f);
        // Eyes (dark)
        addCube(verts, colors, -0.15f, 1.2f, 1.0f, 0.08f, 0.08f, 0.05f, 0.1f, 0.1f, 0.1f);
        addCube(verts, colors,  0.15f, 1.2f, 1.0f, 0.08f, 0.08f, 0.05f, 0.1f, 0.1f, 0.1f);
        // Ears
        addCube(verts, colors, -0.22f, 1.5f, 0.65f, 0.15f, 0.15f, 0.1f, r, g, b);
        addCube(verts, colors,  0.22f, 1.5f, 0.65f, 0.15f, 0.15f, 0.1f, r, g, b);
        // Front legs
        addCube(verts, colors, -0.45f, 0.25f, 0.35f, 0.25f, 0.55f, 0.25f, r*0.9f, g*0.9f, b*0.9f);
        addCube(verts, colors,  0.45f, 0.25f, 0.35f, 0.25f, 0.55f, 0.25f, r*0.9f, g*0.9f, b*0.9f);
        // Back legs
        addCube(verts, colors, -0.45f, 0.25f, -0.35f, 0.25f, 0.55f, 0.25f, r*0.9f, g*0.9f, b*0.9f);
        addCube(verts, colors,  0.45f, 0.25f, -0.35f, 0.25f, 0.55f, 0.25f, r*0.9f, g*0.9f, b*0.9f);
        // Tail
        addCube(verts, colors, 0f, 1.0f, -0.6f, 0.15f, 0.15f, 0.12f, r, g, b);

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

    // --- Cube builder helpers ---
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
            // Simple face-based shading
            int face = i / 18;
            float shade = (face == 4) ? 1.2f : (face < 2 ? 0.95f : 0.7f);
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
