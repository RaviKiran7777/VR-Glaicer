package com.example.vrdesert.shapes;

import android.opengl.GLES20;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class Iceberg {

    private final FloatBuffer vertexBuffer;
    private final FloatBuffer colorBuffer;
    
    private int mProgram;
    private int mPositionHandle;
    private int mColorHandle;
    private int mVPMatrixHandle;

    static final int COORDS_PER_VERTEX = 3;
    static final int COLORS_PER_VERTEX = 4;

    // A pyramid shape pointing upwards 
    static float icebergCoords[] = {
        // Base (2 triangles)
        -1.0f, 0.0f, -1.0f,
         1.0f, 0.0f, -1.0f,
        -1.0f, 0.0f,  1.0f,
        
        -1.0f, 0.0f,  1.0f,
         1.0f, 0.0f, -1.0f,
         1.0f, 0.0f,  1.0f,
         
        // Front Face
        -1.0f, 0.0f,  1.0f,
         1.0f, 0.0f,  1.0f,
         0.0f, 2.5f,  0.0f, // Peak
         
        // Right Face
         1.0f, 0.0f,  1.0f,
         1.0f, 0.0f, -1.0f,
         0.0f, 2.5f,  0.0f,
         
        // Back Face
         1.0f, 0.0f, -1.0f,
        -1.0f, 0.0f, -1.0f,
         0.0f, 2.5f,  0.0f,
         
        // Left Face
        -1.0f, 0.0f, -1.0f,
        -1.0f, 0.0f,  1.0f,
         0.0f, 2.5f,  0.0f
    };

    // Colors matching overcast glacier shadows
    static float colors[] = {
        // Base (not really seen)
        0.5f, 0.7f, 0.9f, 1.0f,  0.5f, 0.7f, 0.9f, 1.0f,  0.5f, 0.7f, 0.9f, 1.0f,
        0.5f, 0.7f, 0.9f, 1.0f,  0.5f, 0.7f, 0.9f, 1.0f,  0.5f, 0.7f, 0.9f, 1.0f,
        
        // Front Face (Well lit cyan)
        0.7f, 0.85f, 1.0f, 1.0f,  0.7f, 0.85f, 1.0f, 1.0f,  0.9f, 0.95f, 1.0f, 1.0f,
        
        // Right Face (Deep shadow blue)
        0.3f, 0.5f, 0.7f, 1.0f,   0.3f, 0.5f, 0.7f, 1.0f,   0.4f, 0.6f, 0.8f, 1.0f,
        
        // Back Face (Medium shadow)
        0.4f, 0.6f, 0.8f, 1.0f,   0.4f, 0.6f, 0.8f, 1.0f,   0.6f, 0.75f, 0.9f, 1.0f,
        
        // Left Face (Lightly lit)
        0.6f, 0.8f, 0.95f, 1.0f,  0.6f, 0.8f, 0.95f, 1.0f,  0.8f, 0.9f, 1.0f, 1.0f
    };

    private final int vertexCount = icebergCoords.length / COORDS_PER_VERTEX;
    private final int vertexStride = COORDS_PER_VERTEX * 4;
    private final int colorStride = COLORS_PER_VERTEX * 4;

    public Iceberg(int programId) {
        this.mProgram = programId;

        ByteBuffer bb = ByteBuffer.allocateDirect(icebergCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(icebergCoords);
        vertexBuffer.position(0);

        ByteBuffer cb = ByteBuffer.allocateDirect(colors.length * 4);
        cb.order(ByteOrder.nativeOrder());
        colorBuffer = cb.asFloatBuffer();
        colorBuffer.put(colors);
        colorBuffer.position(0);
    }

    public void draw(float[] mvpMatrix) {
        GLES20.glUseProgram(mProgram);

        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);

        mColorHandle = GLES20.glGetAttribLocation(mProgram, "vColor");
        GLES20.glEnableVertexAttribArray(mColorHandle);
        GLES20.glVertexAttribPointer(mColorHandle, COLORS_PER_VERTEX,
                GLES20.GL_FLOAT, false, colorStride, colorBuffer);

        mVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        GLES20.glUniformMatrix4fv(mVPMatrixHandle, 1, false, mvpMatrix, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);

        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mColorHandle);
    }
}
