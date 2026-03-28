package com.example.vrdesert.shapes;

import android.opengl.GLES20;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Random;

public class SnowParticles {
    private final String vertexShaderCode =
            "uniform mat4 uMVPMatrix;" +
            "attribute vec4 vPosition;" +
            "void main() {" +
            "  gl_Position = uMVPMatrix * vPosition;" +
            // Make particles larger and scale them consistently by distance
            "  gl_PointSize = max(3.0, 15.0 / (gl_Position.w + 1.0));" + 
            "}";

    private final String fragmentShaderCode =
            "precision mediump float;" +
            "void main() {" +
            "  vec2 coord = gl_PointCoord - vec2(0.5);" +
            "  if(length(coord) > 0.5) discard;" + // Render as soft circles!
            "  gl_FragColor = vec4(1.0, 1.0, 1.0, 0.8);" + 
            "}";

    private FloatBuffer vertexBuffer;
    private int mProgram;
    private int particleCount = 500;
    private float[] coords;
    
    // Arrays for physics tracking independent of GPU
    private float[] velocitiesY;
    private float[] driftX;
    private float[] driftZ;
    private float[] timeOffsets;

    public SnowParticles() {
        coords = new float[particleCount * 3];
        velocitiesY = new float[particleCount];
        driftX = new float[particleCount];
        driftZ = new float[particleCount];
        timeOffsets = new float[particleCount];

        Random rand = new Random();
        for (int i = 0; i < particleCount; i++) {
            // Spawn around player in a 40x40 area, up to 20m high
            coords[i*3] = (rand.nextFloat() * 40f) - 20f;     // x
            coords[i*3 + 1] = (rand.nextFloat() * 20f);       // y
            coords[i*3 + 2] = (rand.nextFloat() * 40f) - 20f; // z
            
            velocitiesY[i] = -(rand.nextFloat() * 3f + 1f); // Fall speed
            driftX[i] = (rand.nextFloat() * 4f - 2f); // Base wind drift X
            driftZ[i] = (rand.nextFloat() * 4f - 2f); // Base wind drift Z
            timeOffsets[i] = rand.nextFloat() * 100f; // For natural organic wave offset
        }

        ByteBuffer bb = ByteBuffer.allocateDirect(coords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(coords);
        vertexBuffer.position(0);

        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        mProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgram, vertexShader);
        GLES20.glAttachShader(mProgram, fragmentShader);
        GLES20.glLinkProgram(mProgram);
    }
    
    private long lastTime = System.currentTimeMillis();

    public void updateAndDraw(float[] mvpMatrix, float camX, float camZ) {
        long now = System.currentTimeMillis();
        float dt = (now - lastTime) / 1000f;
        lastTime = now;
        
        float globalTime = now / 1000f;

        // Animate Physics on CPU because ES2 lacks compute shaders
        for (int i = 0; i < particleCount; i++) {
            // Apply straight vertical gravity
            coords[i*3 + 1] += velocitiesY[i] * dt;
            
            // Apply heavy blizzard wind + wave wobble
            coords[i*3] += (driftX[i] + (float)Math.sin(globalTime + timeOffsets[i]) * 1.5f) * dt;
            coords[i*3 + 2] += (driftZ[i] + (float)Math.cos(globalTime + timeOffsets[i]) * 1.5f) * dt;

            // Reset logic: wrap floor hits back up into the sky!
            if (coords[i*3 + 1] < 0f) {
                coords[i*3 + 1] = 20f;
            }
            
            // Render distance chunk wrap: keep particles within a 20m box of the camera 
            if (coords[i*3] < camX - 20f) coords[i*3] += 40f;
            if (coords[i*3] > camX + 20f) coords[i*3] -= 40f;
            if (coords[i*3 + 2] < camZ - 20f) coords[i*3 + 2] += 40f;
            if (coords[i*3 + 2] > camZ + 20f) coords[i*3 + 2] -= 40f;
        }

        vertexBuffer.position(0);
        vertexBuffer.put(coords);
        vertexBuffer.position(0);

        GLES20.glUseProgram(mProgram);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        int positionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        int mvpMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);

        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, particleCount);

        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisable(GLES20.GL_BLEND);
    }

    private int loadShader(int type, String shaderCode){
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }
}
