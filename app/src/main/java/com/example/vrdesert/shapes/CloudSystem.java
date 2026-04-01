package com.example.vrdesert.shapes;

import android.opengl.GLES20;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Random;

/**
 * Procedural overhead clouds to replace the flat sky.
 */
public class CloudSystem {

    private static final String vertexShaderCode =
            "uniform mat4 uMVPMatrix;" +
            "uniform float uTime;" +
            "attribute vec4 vPosition;" +
            "attribute vec2 vTexCoord;" +
            "varying vec2 _vTexCoord;" +
            "void main() {" +
            "  vec4 pos = vPosition;" +
            "  pos.x += uTime * 2.0;" + // Drift clouds horizontally
            "  pos.x = mod(pos.x + 250.0, 500.0) - 250.0;" + // Loop them
            "  gl_Position = uMVPMatrix * pos;" +
            "  _vTexCoord = vTexCoord;" +
            "}";

    private static final String fragmentShaderCode =
            "precision mediump float;" +
            "varying vec2 _vTexCoord;" +
            "void main() {" +
            "  float dist = length(_vTexCoord - vec2(0.5));" +
            "  if (dist > 0.5) discard;" +
            "  float alpha = 1.0 - smoothstep(0.2, 0.5, dist);" +
            "  gl_FragColor = vec4(0.95, 0.98, 1.0, alpha * 0.7);" +
            "}";

    private FloatBuffer vertexBuffer;
    private FloatBuffer texCoordBuffer;
    private int mProgram;
    private int vertexCount;

    public CloudSystem(int unused) {
        // Create specialized cloud shader
        int vs = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fs = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
        mProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgram, vs);
        GLES20.glAttachShader(mProgram, fs);
        GLES20.glLinkProgram(mProgram);

        Random rand = new Random(111L);
        int numCloudClusters = 15;
        int circlesPerCluster = 8;
        int totalCircles = numCloudClusters * circlesPerCluster;
        
        int vertsPerCircle = 6;
        float[] verts = new float[totalCircles * vertsPerCircle * 3];
        float[] texCoords = new float[totalCircles * vertsPerCircle * 2];

        int vi = 0, ti = 0;
        for (int i = 0; i < numCloudClusters; i++) {
            float clusterX = (rand.nextFloat() - 0.5f) * 450f;
            float clusterZ = (rand.nextFloat() - 0.5f) * 450f;
            float clusterY = 90f + rand.nextFloat() * 30f;

            for (int j = 0; j < circlesPerCluster; j++) {
                // Offset each circle within the cluster to form a cloud shape
                float offsetX = (rand.nextFloat() - 0.5f) * 30f;
                float offsetZ = (rand.nextFloat() - 0.5f) * 20f;
                float offsetY = (rand.nextFloat() - 0.5f) * 5f;

                float cx = clusterX + offsetX;
                float cz = clusterZ + offsetZ;
                float cy = clusterY + offsetY;
                
                float radius = 15f + rand.nextFloat() * 25f;

                // Quad for the circle
                // tri 1
                addV(verts, vi, cx-radius, cy, cz-radius); vi+=3; addT(texCoords, ti, 0, 0); ti+=2;
                addV(verts, vi, cx+radius, cy, cz-radius); vi+=3; addT(texCoords, ti, 1, 0); ti+=2;
                addV(verts, vi, cx-radius, cy, cz+radius); vi+=3; addT(texCoords, ti, 0, 1); ti+=2;
                // tri 2
                addV(verts, vi, cx+radius, cy, cz-radius); vi+=3; addT(texCoords, ti, 1, 0); ti+=2;
                addV(verts, vi, cx+radius, cy, cz+radius); vi+=3; addT(texCoords, ti, 1, 1); ti+=2;
                addV(verts, vi, cx-radius, cy, cz+radius); vi+=3; addT(texCoords, ti, 0, 1); ti+=2;
            }
        }

        vertexCount = vi / 3;
        vertexBuffer = ByteBuffer.allocateDirect(verts.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        vertexBuffer.put(verts).position(0);
        texCoordBuffer = ByteBuffer.allocateDirect(texCoords.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        texCoordBuffer.put(texCoords).position(0);
    }

    private void addV(float[] a, int i, float x, float y, float z) { a[i]=x; a[i+1]=y; a[i+2]=z; }
    private void addT(float[] a, int i, float u, float v) { a[i]=u; a[i+1]=v; }

    public void draw(float[] mvp, float time) {
        GLES20.glUseProgram(mProgram);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        int pos = GLES20.glGetAttribLocation(mProgram, "vPosition");
        GLES20.glEnableVertexAttribArray(pos);
        GLES20.glVertexAttribPointer(pos, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        int tex = GLES20.glGetAttribLocation(mProgram, "vTexCoord");
        GLES20.glEnableVertexAttribArray(tex);
        GLES20.glVertexAttribPointer(tex, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer);

        int timeH = GLES20.glGetUniformLocation(mProgram, "uTime");
        GLES20.glUniform1f(timeH, time);

        int mvpH = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        GLES20.glUniformMatrix4fv(mvpH, 1, false, mvp, 0);
        
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);
        
        GLES20.glDisableVertexAttribArray(pos);
        GLES20.glDisableVertexAttribArray(tex);
        GLES20.glDisable(GLES20.GL_BLEND);
    }

    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }
}
