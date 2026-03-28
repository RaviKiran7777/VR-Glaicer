package com.example.vrdesert.shapes;

import android.opengl.GLES20;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Low-poly Arctic Fox — small, white, with bushy tail and pointed ears.
 */
public class ArcticFox {

    private FloatBuffer vertexBuffer, colorBuffer;
    private int mProgram, vertexCount;
    public float worldX, worldY, worldZ;

    public ArcticFox(int programId, float wx, float wy, float wz) {
        this.mProgram = programId;
        this.worldX = wx; this.worldY = wy; this.worldZ = wz;

        List<Float> v = new ArrayList<>(), c = new ArrayList<>();
        float r = 0.97f, g = 0.97f, b = 0.98f; // pure white

        // Body
        addCube(v, c, 0f, 0.4f, 0f, 0.9f, 0.55f, 0.5f, r, g, b);
        // Head
        addCube(v, c, 0f, 0.55f, 0.45f, 0.45f, 0.4f, 0.4f, r, g, b);
        // Snout (slightly grey)
        addCube(v, c, 0f, 0.48f, 0.72f, 0.18f, 0.15f, 0.22f, 0.85f, 0.82f, 0.8f);
        // Nose (dark)
        addCube(v, c, 0f, 0.5f, 0.84f, 0.06f, 0.06f, 0.04f, 0.1f, 0.1f, 0.1f);
        // Eyes
        addCube(v, c, -0.1f, 0.6f, 0.62f, 0.06f, 0.06f, 0.04f, 0.1f, 0.1f, 0.1f);
        addCube(v, c,  0.1f, 0.6f, 0.62f, 0.06f, 0.06f, 0.04f, 0.1f, 0.1f, 0.1f);
        // Ears — tall and pointed (dark-tipped)
        addCube(v, c, -0.12f, 0.82f, 0.42f, 0.1f, 0.22f, 0.08f, r, g, b);
        addCube(v, c,  0.12f, 0.82f, 0.42f, 0.1f, 0.22f, 0.08f, r, g, b);
        addCube(v, c, -0.12f, 0.95f, 0.42f, 0.06f, 0.06f, 0.05f, 0.2f, 0.2f, 0.2f); // dark tip
        addCube(v, c,  0.12f, 0.95f, 0.42f, 0.06f, 0.06f, 0.05f, 0.2f, 0.2f, 0.2f);
        // Front legs
        addCube(v, c, -0.22f, 0.13f, 0.2f, 0.15f, 0.28f, 0.15f, r*0.9f, g*0.9f, b*0.9f);
        addCube(v, c,  0.22f, 0.13f, 0.2f, 0.15f, 0.28f, 0.15f, r*0.9f, g*0.9f, b*0.9f);
        // Back legs
        addCube(v, c, -0.22f, 0.13f, -0.2f, 0.15f, 0.28f, 0.15f, r*0.9f, g*0.9f, b*0.9f);
        addCube(v, c,  0.22f, 0.13f, -0.2f, 0.15f, 0.28f, 0.15f, r*0.9f, g*0.9f, b*0.9f);
        // Bushy tail
        addCube(v, c, 0f, 0.45f, -0.5f, 0.3f, 0.25f, 0.45f, r, g, b);

        vertexCount = v.size() / 3;
        vertexBuffer = toBuffer(v);
        colorBuffer  = toBuffer(c);
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

    private static final float[] UC = {
        -0.5f,-0.5f,0.5f, 0.5f,-0.5f,0.5f, -0.5f,0.5f,0.5f,
        -0.5f,0.5f,0.5f, 0.5f,-0.5f,0.5f, 0.5f,0.5f,0.5f,
        -0.5f,-0.5f,-0.5f, -0.5f,0.5f,-0.5f, 0.5f,-0.5f,-0.5f,
        0.5f,-0.5f,-0.5f, -0.5f,0.5f,-0.5f, 0.5f,0.5f,-0.5f,
        -0.5f,-0.5f,-0.5f, -0.5f,-0.5f,0.5f, -0.5f,0.5f,-0.5f,
        -0.5f,0.5f,-0.5f, -0.5f,-0.5f,0.5f, -0.5f,0.5f,0.5f,
        0.5f,-0.5f,-0.5f, 0.5f,0.5f,-0.5f, 0.5f,-0.5f,0.5f,
        0.5f,-0.5f,0.5f, 0.5f,0.5f,-0.5f, 0.5f,0.5f,0.5f,
        -0.5f,0.5f,-0.5f, -0.5f,0.5f,0.5f, 0.5f,0.5f,-0.5f,
        0.5f,0.5f,-0.5f, -0.5f,0.5f,0.5f, 0.5f,0.5f,0.5f,
        -0.5f,-0.5f,-0.5f, 0.5f,-0.5f,-0.5f, -0.5f,-0.5f,0.5f,
        -0.5f,-0.5f,0.5f, 0.5f,-0.5f,-0.5f, 0.5f,-0.5f,0.5f
    };

    private void addCube(List<Float> v, List<Float> c, float tx, float ty, float tz,
                         float sx, float sy, float sz, float cr, float cg, float cb) {
        for (int i = 0; i < UC.length; i += 3) {
            v.add(UC[i]*sx+tx); v.add(UC[i+1]*sy+ty); v.add(UC[i+2]*sz+tz);
            int face = i / 18;
            float sh = (face == 4) ? 1.15f : (face < 2 ? 0.95f : 0.7f);
            c.add(Math.min(1f,cr*sh)); c.add(Math.min(1f,cg*sh)); c.add(Math.min(1f,cb*sh)); c.add(1f);
        }
    }

    private FloatBuffer toBuffer(List<Float> list) {
        float[] a = new float[list.size()];
        for (int i = 0; i < a.length; i++) a[i] = list.get(i);
        ByteBuffer bb = ByteBuffer.allocateDirect(a.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer(); fb.put(a).position(0); return fb;
    }
}
