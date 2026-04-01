package com.example.vrdesert.shapes;

import android.opengl.GLES20;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Random;

/**
 * Creative ice spires for the left side of the environment.
 */
public class IceSpires {

    private FloatBuffer vertexBuffer;
    private FloatBuffer colorBuffer;
    private int mProgram;
    private int vertexCount;

    public IceSpires(int programId) {
        this.mProgram = programId;

        Random rand = new Random(777L);
        int numSpires = 12;
        
        // Each spire is a thin pyramid (4 faces = 12 vertices)
        int vertsPerSpire = 4 * 3;
        float[] verts = new float[numSpires * vertsPerSpire * 3];
        float[] colors = new float[numSpires * vertsPerSpire * 4];

        int vi = 0, ci = 0;
        for (int i = 0; i < numSpires; i++) {
            float px = -40f - rand.nextFloat() * 40f; // Left side
            float pz = -50f + (rand.nextFloat() * 100f);
            float h = 10f + rand.nextFloat() * 25f;
            float w = 2f + rand.nextFloat() * 4f;
            
            // Tilt for creativity
            float tiltX = (rand.nextFloat() - 0.5f) * 10f;
            float tiltZ = (rand.nextFloat() - 0.5f) * 10f;

            float[] baseC = { 0.2f, 0.4f, 0.7f, 1.0f }; // Translucent blue-ish
            float[] peakC = { 0.8f, 0.95f, 1.0f, 1.0f };

            // Front
            addV(verts, vi, px+tiltX, h, pz+tiltZ); vi+=3; addC(colors, ci, peakC); ci+=4;
            addV(verts, vi, px-w, 0, pz+w); vi+=3; addC(colors, ci, baseC); ci+=4;
            addV(verts, vi, px+w, 0, pz+w); vi+=3; addC(colors, ci, baseC); ci+=4;

            // Back
            addV(verts, vi, px+tiltX, h, pz+tiltZ); vi+=3; addC(colors, ci, peakC); ci+=4;
            addV(verts, vi, px+w, 0, pz-w); vi+=3; addC(colors, ci, baseC); ci+=4;
            addV(verts, vi, px-w, 0, pz-w); vi+=3; addC(colors, ci, baseC); ci+=4;

            // Left
            addV(verts, vi, px+tiltX, h, pz+tiltZ); vi+=3; addC(colors, ci, peakC); ci+=4;
            addV(verts, vi, px-w, 0, pz-w); vi+=3; addC(colors, ci, baseC); ci+=4;
            addV(verts, vi, px-w, 0, pz+w); vi+=3; addC(colors, ci, baseC); ci+=4;

            // Right
            addV(verts, vi, px+tiltX, h, pz+tiltZ); vi+=3; addC(colors, ci, peakC); ci+=4;
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
