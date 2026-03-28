package com.example.vrdesert.shapes;

import android.opengl.GLES20;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Low-poly Arctic Seal — elongated body lying on ice.
 * Grey/silver coloring.
 */
public class Seal {

    private FloatBuffer vertexBuffer, colorBuffer;
    private int mProgram, vertexCount;
    public float worldX, worldY, worldZ;

    public Seal(int programId, float wx, float wy, float wz) {
        this.mProgram = programId;
        this.worldX = wx; this.worldY = wy; this.worldZ = wz;

        List<Float> v = new ArrayList<>(), c = new ArrayList<>();
        float r = 0.52f, g = 0.55f, b = 0.58f; // grey

        // Body — elongated
        addCube(v, c, 0f, 0.25f, 0f, 1.6f, 0.45f, 0.65f, r, g, b);
        // Head
        addCube(v, c, 0f, 0.35f, 0.6f, 0.55f, 0.45f, 0.5f, r*1.05f, g*1.05f, b*1.05f);
        // Snout / nose (dark)
        addCube(v, c, 0f, 0.3f, 0.9f, 0.2f, 0.15f, 0.15f, 0.15f, 0.15f, 0.15f);
        // Eyes
        addCube(v, c, -0.15f, 0.45f, 0.78f, 0.07f, 0.07f, 0.04f, 0.08f, 0.08f, 0.08f);
        addCube(v, c,  0.15f, 0.45f, 0.78f, 0.07f, 0.07f, 0.04f, 0.08f, 0.08f, 0.08f);
        // Front flippers
        addCube(v, c, -0.55f, 0.1f, 0.3f, 0.35f, 0.06f, 0.2f, r*0.85f, g*0.85f, b*0.85f);
        addCube(v, c,  0.55f, 0.1f, 0.3f, 0.35f, 0.06f, 0.2f, r*0.85f, g*0.85f, b*0.85f);
        // Tail flippers
        addCube(v, c, -0.2f, 0.1f, -0.7f, 0.25f, 0.05f, 0.3f, r*0.85f, g*0.85f, b*0.85f);
        addCube(v, c,  0.2f, 0.1f, -0.7f, 0.25f, 0.05f, 0.3f, r*0.85f, g*0.85f, b*0.85f);

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
