package com.example.vrdesert.shapes;

import android.opengl.GLES20;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Random;

/**
 * Distant, massive mountain peaks for background immersion.
 */
public class GlacierMountain {

    private FloatBuffer vertexBuffer;
    private FloatBuffer colorBuffer;
    private int mProgram;
    private int vertexCount;

    public GlacierMountain(int programId) {
        this.mProgram = programId;

        Random rand = new Random(888L);
        int numPeaks = 8;
        float range = 150f;
        
        // Each peak is a 4-sided pyramid (4 base triangles + 4 side triangles, simplified to 4 side triangles)
        // 4 triangles per peak = 12 vertices per peak
        int vertsPerPeak = 4 * 3;
        float[] verts = new float[numPeaks * vertsPerPeak * 3];
        float[] colors = new float[numPeaks * vertsPerPeak * 4];

        int vi = 0, ci = 0;
        for (int i = 0; i < numPeaks; i++) {
            float px = (rand.nextFloat() - 0.5f) * range;
            float pz = range + rand.nextFloat() * 50f; // Far behind the viewer
            float h = 40f + rand.nextFloat() * 40f;
            float w = 30f + rand.nextFloat() * 20f;

            // Colors: deep blue at base, white at peak
            float[] baseC = { 0.05f, 0.2f, 0.5f, 1.0f };
            float[] peakC = { 1.0f, 1.0f, 1.0f, 1.0f };

            // 4 Triangles forming a pyramid
            // Front
            addV(verts, vi, px, h, pz); vi+=3; addC(colors, ci, peakC); ci+=4;
            addV(verts, vi, px-w, 0, pz+w); vi+=3; addC(colors, ci, baseC); ci+=4;
            addV(verts, vi, px+w, 0, pz+w); vi+=3; addC(colors, ci, baseC); ci+=4;

            // Back
            addV(verts, vi, px, h, pz); vi+=3; addC(colors, ci, peakC); ci+=4;
            addV(verts, vi, px+w, 0, pz-w); vi+=3; addC(colors, ci, baseC); ci+=4;
            addV(verts, vi, px-w, 0, pz-w); vi+=3; addC(colors, ci, baseC); ci+=4;

            // Left
            addV(verts, vi, px, h, pz); vi+=3; addC(colors, ci, peakC); ci+=4;
            addV(verts, vi, px-w, 0, pz-w); vi+=3; addC(colors, ci, baseC); ci+=4;
            addV(verts, vi, px-w, 0, pz+w); vi+=3; addC(colors, ci, baseC); ci+=4;

            // Right
            addV(verts, vi, px, h, pz); vi+=3; addC(colors, ci, peakC); ci+=4;
            addV(verts, vi, px+w, 0, pz+w); vi+=3; addC(colors, ci, baseC); ci+=4;
            addV(verts, vi, px+w, 0, pz-w); vi+=3; addC(colors, ci, baseC); ci+=4;
        }

        vertexCount = vi / 3;
        vertexBuffer = ByteBuffer.allocateDirect(verts.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        vertexBuffer.put(verts).position(0);
        colorBuffer = ByteBuffer.allocateDirect(colors.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        colorBuffer.put(colors).position(0);
    }

    private void addV(float[] a, int i, float x, float y, float z) { a[i]=x; a[i+1]=y; a[i+2]=z; }
    private void addC(float[] a, int i, float[] c) { a[i]=c[0]; a[i+1]=c[1]; a[i+2]=c[2]; a[i+3]=c[3]; }

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
}
