package com.example.vrdesert.shapes;

import android.opengl.GLES20;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * A massive procedural glacier wall mesh.
 * Built from a grid of tall columns with randomized heights
 * to simulate the jagged, fractured face of ancient glacier ice.
 * Rendered with deep cyan/white color gradients (lighter at top, deeper blue at base).
 */
public class GlacierWall {

    private FloatBuffer vertexBuffer;
    private FloatBuffer colorBuffer;
    private int mProgram;
    private int vertexCount;

    // Glacier shape: 20 columns across, height of each column randomised on first init
    private static final int COLS = 20;
    private static final float WALL_WIDTH = 80f;
    private static final float WALL_DEPTH = 8f;
    private static final float BASE_HEIGHT = 14f;

    public GlacierWall(int programId, long seed) {
        this.mProgram = programId;

        java.util.Random rand = new java.util.Random(seed);

        // We build the wall as one giant set of triangles (no index buffer needed)
        // Each column = 1 rectangular block, made of 12 triangles (2 per face * 6 sides minus base)
        // We do front + top + left-edge + right-edge per block for efficiency
        // Actually let's do front face + top cap with per-column height variation
        // Each column: front quad (2 tri), left face (2 tri), right face (2 tri), top cap (2 tri) = 8 tri = 24 verts

        int triPerCol = 10; // 5 faces × 2 triangles each
        int vertsPerCol = triPerCol * 3;
        float[] verts = new float[COLS * vertsPerCol * 3];
        float[] colors = new float[COLS * vertsPerCol * 4];

        float colWidth = WALL_WIDTH / COLS;
        float zFront = 0f;
        float zBack = -WALL_DEPTH;

        int vi = 0, ci = 0;

        for (int c = 0; c < COLS; c++) {
            float x0 = -WALL_WIDTH / 2f + c * colWidth;
            float x1 = x0 + colWidth;
            float h = BASE_HEIGHT + rand.nextFloat() * 10f - rand.nextFloat() * 4f;
            if (h < 6f) h = 6f;

            // Color: deep blue at base, bright icy white at peak
            float[] baseColor  = { 0.1f, 0.35f, 0.65f, 1.0f };
            float[] midColor   = { 0.4f, 0.72f, 0.92f, 1.0f };
            float[] topColor   = { 0.85f, 0.95f, 1.0f, 1.0f };

            // --- FRONT FACE (z = zFront) ---
            // tri 1
            addVert(verts, vi, x0, 0,  zFront); addColor(colors, ci, baseColor); vi+=3; ci+=4;
            addVert(verts, vi, x1, 0,  zFront); addColor(colors, ci, baseColor); vi+=3; ci+=4;
            addVert(verts, vi, x0, h,  zFront); addColor(colors, ci, topColor);  vi+=3; ci+=4;
            // tri 2
            addVert(verts, vi, x1, 0,  zFront); addColor(colors, ci, baseColor); vi+=3; ci+=4;
            addVert(verts, vi, x1, h,  zFront); addColor(colors, ci, topColor);  vi+=3; ci+=4;
            addVert(verts, vi, x0, h,  zFront); addColor(colors, ci, topColor);  vi+=3; ci+=4;

            // --- BACK FACE (z = zBack) ---
            // tri 1
            addVert(verts, vi, x1, 0,  zBack); addColor(colors, ci, baseColor); vi+=3; ci+=4;
            addVert(verts, vi, x0, 0,  zBack); addColor(colors, ci, baseColor); vi+=3; ci+=4;
            addVert(verts, vi, x1, h,  zBack); addColor(colors, ci, midColor);  vi+=3; ci+=4;
            // tri 2
            addVert(verts, vi, x0, 0,  zBack); addColor(colors, ci, baseColor); vi+=3; ci+=4;
            addVert(verts, vi, x0, h,  zBack); addColor(colors, ci, midColor);  vi+=3; ci+=4;
            addVert(verts, vi, x1, h,  zBack); addColor(colors, ci, midColor);  vi+=3; ci+=4;

            // --- TOP CAP ---
            // tri 1
            addVert(verts, vi, x0, h, zFront); addColor(colors, ci, topColor); vi+=3; ci+=4;
            addVert(verts, vi, x1, h, zFront); addColor(colors, ci, topColor); vi+=3; ci+=4;
            addVert(verts, vi, x0, h, zBack);  addColor(colors, ci, midColor); vi+=3; ci+=4;
            // tri 2
            addVert(verts, vi, x1, h, zFront); addColor(colors, ci, topColor); vi+=3; ci+=4;
            addVert(verts, vi, x1, h, zBack);  addColor(colors, ci, midColor); vi+=3; ci+=4;
            addVert(verts, vi, x0, h, zBack);  addColor(colors, ci, midColor); vi+=3; ci+=4;

            // --- LEFT FACE ---
            addVert(verts, vi, x0, 0, zBack);  addColor(colors, ci, baseColor); vi+=3; ci+=4;
            addVert(verts, vi, x0, 0, zFront); addColor(colors, ci, baseColor); vi+=3; ci+=4;
            addVert(verts, vi, x0, h, zBack);  addColor(colors, ci, midColor);  vi+=3; ci+=4;
            addVert(verts, vi, x0, 0, zFront); addColor(colors, ci, baseColor); vi+=3; ci+=4;
            addVert(verts, vi, x0, h, zFront); addColor(colors, ci, topColor);  vi+=3; ci+=4;
            addVert(verts, vi, x0, h, zBack);  addColor(colors, ci, midColor);  vi+=3; ci+=4;

            // --- RIGHT FACE ---
            addVert(verts, vi, x1, 0, zFront); addColor(colors, ci, baseColor); vi+=3; ci+=4;
            addVert(verts, vi, x1, 0, zBack);  addColor(colors, ci, baseColor); vi+=3; ci+=4;
            addVert(verts, vi, x1, h, zFront); addColor(colors, ci, topColor);  vi+=3; ci+=4;
            addVert(verts, vi, x1, 0, zBack);  addColor(colors, ci, baseColor); vi+=3; ci+=4;
            addVert(verts, vi, x1, h, zBack);  addColor(colors, ci, midColor);  vi+=3; ci+=4;
            addVert(verts, vi, x1, h, zFront); addColor(colors, ci, topColor);  vi+=3; ci+=4;
        }

        vertexCount = vi / 3;

        ByteBuffer bb = ByteBuffer.allocateDirect(verts.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(verts).position(0);

        ByteBuffer cb = ByteBuffer.allocateDirect(colors.length * 4);
        cb.order(ByteOrder.nativeOrder());
        colorBuffer = cb.asFloatBuffer();
        colorBuffer.put(colors).position(0);
    }

    private void addVert(float[] arr, int i, float x, float y, float z) {
        arr[i] = x; arr[i+1] = y; arr[i+2] = z;
    }

    private void addColor(float[] arr, int i, float[] c) {
        arr[i] = c[0]; arr[i+1] = c[1]; arr[i+2] = c[2]; arr[i+3] = c[3];
    }

    public void draw(float[] mvpMatrix) {
        GLES20.glUseProgram(mProgram);

        int posHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        GLES20.glEnableVertexAttribArray(posHandle);
        GLES20.glVertexAttribPointer(posHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        int colHandle = GLES20.glGetAttribLocation(mProgram, "vColor");
        GLES20.glEnableVertexAttribArray(colHandle);
        GLES20.glVertexAttribPointer(colHandle, 4, GLES20.GL_FLOAT, false, 0, colorBuffer);

        int mvpHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        GLES20.glUniformMatrix4fv(mvpHandle, 1, false, mvpMatrix, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);

        GLES20.glDisableVertexAttribArray(posHandle);
        GLES20.glDisableVertexAttribArray(colHandle);
    }
}
