package com.example.vrdesert.shapes;

import android.opengl.GLES20;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Random;

/**
 * Majestic glacial arches for the right side.
 */
public class IceArches {

    private FloatBuffer vertexBuffer;
    private FloatBuffer colorBuffer;
    private int mProgram;
    private int vertexCount;

    public IceArches(int programId) {
        this.mProgram = programId;

        Random rand = new Random(555L);
        int numArches = 6;
        
        // Each arch is made of 5 segments, each segment is a box (6 faces = 12 triangles = 36 vertices)
        // 5 segments * 36 vertices = 180 vertices per arch
        int vertsPerArch = 5 * 36;
        float[] verts = new float[numArches * vertsPerArch * 3];
        float[] colors = new float[numArches * vertsPerArch * 4];

        int vi = 0, ci = 0;
        for (int i = 0; i < numArches; i++) {
            float ox = 40f + rand.nextFloat() * 40f; // Right side
            float oz = -60f + (rand.nextFloat() * 120f);
            float baseW = 4f + rand.nextFloat() * 4f;
            float height = 12f + rand.nextFloat() * 10f;

            float[] c = { 0.4f, 0.8f, 0.95f, 1.0f }; // Crystal blue

            // Segment 1 & 2: Vertical pillars
            addBox(verts, vi, colors, ci, ox-baseW, 0, oz-2, ox-baseW+2, height, oz+2, c); vi+=36*3; ci+=36*4;
            addBox(verts, vi, colors, ci, ox+baseW, 0, oz-2, ox+baseW+2, height, oz+2, c); vi+=36*3; ci+=36*4;
            // Segment 3: Top beam
            addBox(verts, vi, colors, ci, ox-baseW, height, oz-2, ox+baseW+2, height+2, oz+2, c); vi+=36*3; ci+=36*4;
            // Segment 4 & 5: Extra jagged bits
            addBox(verts, vi, colors, ci, ox-baseW-1, height+1, oz-1, ox-baseW+1, height+3, oz+1, c); vi+=36*3; ci+=36*4;
            addBox(verts, vi, colors, ci, ox+baseW+1, height+1, oz-1, ox+baseW+3, height+3, oz+1, c); vi+=36*3; ci+=36*4;
        }

        vertexCount = vi / 3;
        vertexBuffer = ByteBuffer.allocateDirect(verts.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        vertexBuffer.put(verts).position(0);
        colorBuffer = ByteBuffer.allocateDirect(colors.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        colorBuffer.put(colors).position(0);
    }

    private void addBox(float[] v, int vi, float[] cArr, int ci, float x0, float y0, float z0, float x1, float y1, float z1, float[] c) {
        // Simple 6-face box
        float[][] faces = {
            {x0,y0,z1, x1,y0,z1, x0,y1,z1, x1,y1,z1}, // Front
            {x0,y0,z0, x0,y1,z0, x1,y0,z0, x1,y1,z0}, // Back
            {x0,y1,z0, x0,y1,z1, x1,y1,z0, x1,y1,z1}, // Top
            {x0,y0,z0, x1,y0,z0, x0,y0,z1, x1,y0,z1}, // Bottom
            {x0,y0,z0, x0,y0,z1, x0,y1,z0, x0,y1,z1}, // Left
            {x1,y0,z0, x1,y1,z0, x1,y0,z1, x1,y1,z1}  // Right
        };
        for (float[] f : faces) {
            // tri 1
            v[vi]=f[0]; v[vi+1]=f[1]; v[vi+2]=f[2]; vi+=3;
            v[vi]=f[3]; v[vi+1]=f[4]; v[vi+2]=f[5]; vi+=3;
            v[vi]=f[6]; v[vi+1]=f[7]; v[vi+2]=f[8]; vi+=3;
            // tri 2
            v[vi]=f[3]; v[vi+1]=f[4]; v[vi+2]=f[5]; vi+=3;
            v[vi]=f[9]; v[vi+1]=f[10]; v[vi+2]=f[11]; vi+=3;
            v[vi]=f[6]; v[vi+1]=f[7]; v[vi+2]=f[8]; vi+=3;
            
            for(int k=0; k<6; k++) {
                cArr[ci]=c[0]; cArr[ci+1]=c[1]; cArr[ci+2]=c[2]; cArr[ci+3]=c[3]; ci+=4;
            }
        }
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
}
