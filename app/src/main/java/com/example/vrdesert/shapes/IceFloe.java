package com.example.vrdesert.shapes;

import android.opengl.GLES20;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * A small floating ice platform for animals to sit on in the meltwater.
 */
public class IceFloe {
    private FloatBuffer vertexBuffer;
    private FloatBuffer colorBuffer;
    private int mProgram;
    private int vertexCount;

    public IceFloe(int programId, float radius) {
        this.mProgram = programId;
        
        // Let's make an 8-sided polygon for a "natural" chunk look.
        // center + 8 perimeter points = 8 triangles = 24 vertices
        int sides = 8;
        vertexCount = sides * 3;
        float[] verts = new float[vertexCount * 3];
        float[] colors = new float[vertexCount * 4];

        float[] iceColor = {0.9f, 0.95f, 1.0f, 1.0f};

        int vi = 0, ci = 0;
        for (int i = 0; i < sides; i++) {
            float angle1 = (float) (i * 2 * Math.PI / sides);
            float angle2 = (float) ((i + 1) * 2 * Math.PI / sides);

            // Center
            verts[vi++] = 0; verts[vi++] = 0; verts[vi++] = 0;
            for(int k=0; k<4; k++) colors[ci++] = iceColor[k];

            // Point 1
            verts[vi++] = (float) Math.cos(angle1) * radius;
            verts[vi++] = 0;
            verts[vi++] = (float) Math.sin(angle1) * radius;
            for(int k=0; k<4; k++) colors[ci++] = iceColor[k];

            // Point 2
            verts[vi++] = (float) Math.cos(angle2) * radius;
            verts[vi++] = 0;
            verts[vi++] = (float) Math.sin(angle2) * radius;
            for(int k=0; k<4; k++) colors[ci++] = iceColor[k];
        }

        vertexBuffer = ByteBuffer.allocateDirect(verts.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        vertexBuffer.put(verts).position(0);
        colorBuffer = ByteBuffer.allocateDirect(colors.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        colorBuffer.put(colors).position(0);
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
