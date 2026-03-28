package com.example.vrdesert;

import android.opengl.GLES20;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import java.util.Random;
import com.example.vrdesert.shapes.Cube;
import com.example.vrdesert.shapes.Plane;
import com.example.vrdesert.shapes.TextOverlay;
import com.example.vrdesert.shapes.Crosshair;
import com.example.vrdesert.shapes.SnowParticles;
import com.example.vrdesert.shapes.Iceberg;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class VRRenderer implements GLSurfaceView.Renderer {

    private final SensorHandler sensorHandler;
    private final InteractionManager interactionManager;

    private Plane floor;
    private Cube iceCoreModel;
    private Cube thermometerModel;
    private Cube cameraTrapModel;
    private Cube snowSampleModel;
    private SnowParticles snowParticles;
    private Iceberg icebergModel;
    
    // Arrays for Iceberg positions
    private float[] icebergX = new float[20];
    private float[] icebergZ = new float[20];
    private float[] icebergScaleY = new float[20];
    
    private int shaderProgram;

    // View & Projection
    private float[] leftProjectionMatrix = new float[16];
    private float[] rightProjectionMatrix = new float[16];
    private float[] viewMatrix = new float[16];
    private float[] vPMatrix = new float[16];
    private float[] scratchMatrix = new float[16];
    private float[] modelMatrix = new float[16];

    // Camera state
    private float camX = 0f;
    private float camY = 1.0f; // standing height lowered to match block centers
    private float camZ = 0f;
    
    // IPD for stereoscopic effect
    private static final float EYE_OFFSET = 0.05f;

    // Objects in Scene (Spawns 15 randomized items)
    private GameObject[] objects = new GameObject[15];

    // UI Elements
    private TextOverlay inventoryOverlay;
    private TextOverlay factOverlay;
    
    private Crosshair crosshair;
    private float[] uiProjectionMatrix = new float[16];
    private float[] uiModelMatrix = new float[16];
    private float[] uiMVPMatrix = new float[16];
    
    private String inventoryString = "";
    private boolean updateInventoryTextFlag = false;
    
    private String displayFact = "";
    private long factDisplayEndTime = 0;
    private boolean updateFactTextFlag = false;

    public void displayFact(String fact) {
        this.displayFact = fact;
        this.factDisplayEndTime = System.currentTimeMillis() + 6000; // 6 seconds
        this.updateFactTextFlag = true;
    }

    public String getDisplayFact() {
        return displayFact;
    }

    public void updateInventory(String text) {
        this.inventoryString = text;
        this.updateInventoryTextFlag = true;
    }

    public String getInventoryString() {
        return inventoryString;
    }

    private int width, height;

    private final String vertexShaderCode =
        "uniform mat4 uMVPMatrix;" +
        "attribute vec4 vPosition;" +
        "attribute vec4 vColor;" +
        "varying vec4 _vColor;" +
        "varying float vDistance;" +
        "void main() {" +
        "  gl_Position = uMVPMatrix * vPosition;" +
        "  _vColor = vColor;" +
        "  vDistance = gl_Position.w;" + // Using w natively extracts linear view depth out to far plane!
        "}";

    private final String fragmentShaderCode =
        "precision mediump float;" +
        "varying vec4 _vColor;" +
        "varying float vDistance;" +
        "void main() {" +
        "  float fogFactor = clamp((vDistance - 8.0) / 32.0, 0.0, 1.0);" + // Fades cleanly from 8m out to 40m
        "  vec4 fogColor = vec4(0.85, 0.9, 0.95, 1.0);" + // Base Sky Color
        "  gl_FragColor = mix(_vColor, fogColor, fogFactor);" + // Seamlessly merge colors!
        "}";

    public VRRenderer(SensorHandler sensorHandler, InteractionManager interactionManager) {
        this.sensorHandler = sensorHandler;
        this.interactionManager = interactionManager;
    }

    public void moveForward() {
        // Move camera forward based on its current yaw
        float yawRad = (float) Math.toRadians(sensorHandler.getYaw());
        float forwardX = (float) -Math.sin(yawRad);
        float forwardZ = (float) -Math.cos(yawRad);

        // 1 meter step size
        camX += forwardX * 1.0f;
        camZ += forwardZ * 1.0f;
    }

    private int loadShader(int type, String shaderCode){
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        // Glacier sky color: Overcast light blue
        GLES20.glClearColor(0.85f, 0.9f, 0.95f, 1.0f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        shaderProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(shaderProgram, vertexShader);
        GLES20.glAttachShader(shaderProgram, fragmentShader);
        GLES20.glLinkProgram(shaderProgram);

        floor = new Plane(shaderProgram);
        inventoryOverlay = new TextOverlay();
        factOverlay = new TextOverlay();
        crosshair = new Crosshair();
        
        snowParticles = new SnowParticles();
        icebergModel = new Iceberg(shaderProgram);
        
        // Instantiate colored geometry variations dynamically using our Cube template
        iceCoreModel = new Cube(shaderProgram, 0.2f, 0.8f, 1.0f); // Cyan 
        thermometerModel = new Cube(shaderProgram, 0.8f, 0.1f, 0.1f); // Red 
        cameraTrapModel = new Cube(shaderProgram, 0.2f, 0.2f, 0.2f); // Dark Grey
        snowSampleModel = new Cube(shaderProgram, 1.0f, 1.0f, 1.0f); // White

        // Scatter random objects around the desert immediately
        Random rand = new Random();
        GameObject.Type[] types = GameObject.Type.values();
        for (int i = 0; i < objects.length; i++) {
            float rx = (rand.nextFloat() * 40f) - 20f;
            float rz = (rand.nextFloat() * 40f) - 20f;
            
            // Prevent spawning directly on the origin camera (inside the player)
            if (Math.abs(rx) < 2f) rx += 2f;
            if (Math.abs(rz) < 2f) rz += 2f;
            
            GameObject.Type t = types[rand.nextInt(types.length)];
            objects[i] = new GameObject(rx, 0f, rz, t);
        }

        // Scatter 20 massive icebergs in the distance
        for(int i=0; i<20; i++) {
            float dist = 25f + rand.nextFloat() * 25f; // between 25m and 50m away
            float angle = rand.nextFloat() * (float)Math.PI * 2f;
            icebergX[i] = (float)Math.cos(angle) * dist;
            icebergZ[i] = (float)Math.sin(angle) * dist;
            icebergScaleY[i] = 4f + rand.nextFloat() * 6f; // Random heights
        }
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        this.width = width;
        this.height = height;

        GLES20.glViewport(0, 0, width, height);

        // Aspect ratio for half the screen
        float ratio = (float) (width / 2) / height;
        Matrix.perspectiveM(leftProjectionMatrix, 0, 75f, ratio, 0.1f, 100f);
        Matrix.perspectiveM(rightProjectionMatrix, 0, 75f, ratio, 0.1f, 100f);
        
        // UI orthographic projection (Origin at top-left 0,0) mapped accurately to pixel densities
        Matrix.orthoM(uiProjectionMatrix, 0, 0, width / 2, height, 0, -1, 1);
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        // Handle UI text updates safely on the GL Thread exactly when necessary
        if (updateInventoryTextFlag && inventoryOverlay != null) {
            inventoryOverlay.updateText(inventoryString);
            updateInventoryTextFlag = false;
        }
        
        if (updateFactTextFlag && factOverlay != null) {
            factOverlay.updateText(displayFact);
            updateFactTextFlag = false;
        }
        
        // Pass Gaze targeting boolean direct to internal crosshair rendering!
        if (crosshair != null) {
            crosshair.setTargeting(interactionManager.isTargeting());
        }

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        float pitchInfo = sensorHandler.getPitch();
        float yawInfo = sensorHandler.getYaw();

        // Convert to radians to compute look vectors
        float yawRad = (float) Math.toRadians(yawInfo);
        float pitchRad = (float) Math.toRadians(pitchInfo);

        float forwardX = (float) (-Math.sin(yawRad) * Math.cos(pitchRad));
        float forwardY = (float) Math.sin(-pitchRad);
        float forwardZ = (float) (-Math.cos(yawRad) * Math.cos(pitchRad));

        float targetX = camX + forwardX;
        float targetY = camY + forwardY;
        float targetZ = camZ + forwardZ;

        // Up vector (assuming purely vertical Y-up most of the time is safe enough given limits)
        float upX = 0f;
        float upY = 1f;
        float upZ = 0f;

        // Interaction Check once per frame (using center camera gaze)
        for (int i = 0; i < objects.length; i++) {
            interactionManager.checkGaze(
                camX, camY, camZ,
                forwardX, forwardY, forwardZ,
                objects[i],
                i
            );
        }

        // Draw Left Eye
        GLES20.glViewport(0, 0, width / 2, height);
        // compute left eye offset
        float leftOffX =  (float) Math.cos(yawRad) * EYE_OFFSET; // rough approx of horizontal shift
        float leftOffZ = (float) -Math.sin(yawRad) * EYE_OFFSET; 
        Matrix.setLookAtM(viewMatrix, 0, 
                camX - leftOffX, camY, camZ - leftOffZ, 
                targetX - leftOffX, targetY, targetZ - leftOffZ, 
                upX, upY, upZ);
        Matrix.multiplyMM(vPMatrix, 0, leftProjectionMatrix, 0, viewMatrix, 0);
        drawScene(vPMatrix);
        drawUI();

        // Draw Right Eye
        GLES20.glViewport(width / 2, 0, width / 2, height);
        // compute right eye offset
        float rightOffX = (float) -Math.cos(yawRad) * EYE_OFFSET; 
        float rightOffZ = (float) Math.sin(yawRad) * EYE_OFFSET;
        Matrix.setLookAtM(viewMatrix, 0, 
                camX - rightOffX, camY, camZ - rightOffZ, 
                targetX - rightOffX, targetY, targetZ - rightOffZ, 
                upX, upY, upZ);
        Matrix.multiplyMM(vPMatrix, 0, rightProjectionMatrix, 0, viewMatrix, 0);
        drawScene(vPMatrix);
        drawUI();
    }

    private void drawUI() {
        GLES20.glDisable(GLES20.GL_DEPTH_TEST); // UI renders entirely on top!

        // Draw HUD Text at top left
        Matrix.setIdentityM(uiModelMatrix, 0);
        Matrix.translateM(uiModelMatrix, 0, 20f, 20f, 0f); // 20px padding offset from Top-Left corner
        Matrix.scaleM(uiModelMatrix, 0, 512f, 512f, 1f); // Bitmap canvas 1:1 scale multiplier mapping the new multi-line 512x512 texture!
        Matrix.multiplyMM(uiMVPMatrix, 0, uiProjectionMatrix, 0, uiModelMatrix, 0);
        inventoryOverlay.draw(uiMVPMatrix);
        
        // Draw Educational Fact at bottom center
        if (System.currentTimeMillis() < factDisplayEndTime && !displayFact.isEmpty()) {
            Matrix.setIdentityM(uiModelMatrix, 0);
            float eyeWidth = width / 2f;
            // Center 512px canvas horizontally, push near bottom
            Matrix.translateM(uiModelMatrix, 0, (eyeWidth / 2f) - 256f, height - 300f, 0f); 
            Matrix.scaleM(uiModelMatrix, 0, 512f, 512f, 1f);
            Matrix.multiplyMM(uiMVPMatrix, 0, uiProjectionMatrix, 0, uiModelMatrix, 0);
            factOverlay.draw(uiMVPMatrix);
        }

        // Draw standard Crosshair precisely at center
        Matrix.setIdentityM(uiModelMatrix, 0);
        float eyeWidth = width / 2f;
        Matrix.translateM(uiModelMatrix, 0, eyeWidth / 2f, height / 2f, 0f); // Local eye width centroid
        Matrix.multiplyMM(uiMVPMatrix, 0, uiProjectionMatrix, 0, uiModelMatrix, 0);
        crosshair.draw(uiMVPMatrix);

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }

    private void drawScene(float[] projectionAndViewMatrix) {
        // Draw Floor
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.multiplyMM(scratchMatrix, 0, projectionAndViewMatrix, 0, modelMatrix, 0);
        floor.draw(scratchMatrix);

        // Draw Scatter Objects: skip if already collected
        for (int i = 0; i < objects.length; i++) {
            if (objects[i].isCollected) continue;

            float ox = objects[i].x;
            float oy = objects[i].y; 
            float oz = objects[i].z;

            Matrix.setIdentityM(modelMatrix, 0);
            
            if (objects[i].type == GameObject.Type.ICE_CORE) {
                Matrix.translateM(modelMatrix, 0, ox, oy + 1.0f, oz); 
                Matrix.scaleM(modelMatrix, 0, 0.3f, 2.0f, 0.3f); // Tall cylinder shape
                Matrix.multiplyMM(scratchMatrix, 0, projectionAndViewMatrix, 0, modelMatrix, 0);
                iceCoreModel.draw(scratchMatrix);
                
            } else if (objects[i].type == GameObject.Type.THERMOMETER) {
                Matrix.translateM(modelMatrix, 0, ox, oy + 0.5f, oz); 
                Matrix.scaleM(modelMatrix, 0, 0.1f, 1.0f, 0.1f); // Thin stick
                Matrix.multiplyMM(scratchMatrix, 0, projectionAndViewMatrix, 0, modelMatrix, 0);
                thermometerModel.draw(scratchMatrix);
                
            } else if (objects[i].type == GameObject.Type.CAMERA_TRAP) {
                Matrix.translateM(modelMatrix, 0, ox, oy + 0.4f, oz); 
                Matrix.scaleM(modelMatrix, 0, 0.6f, 0.6f, 0.4f); // Camera box
                Matrix.multiplyMM(scratchMatrix, 0, projectionAndViewMatrix, 0, modelMatrix, 0);
                cameraTrapModel.draw(scratchMatrix);
                
            } else if (objects[i].type == GameObject.Type.SNOW_SAMPLE) {
                Matrix.translateM(modelMatrix, 0, ox, oy + 0.2f, oz); 
                Matrix.scaleM(modelMatrix, 0, 0.4f, 0.4f, 0.4f); // Small box
                Matrix.multiplyMM(scratchMatrix, 0, projectionAndViewMatrix, 0, modelMatrix, 0);
                snowSampleModel.draw(scratchMatrix);
            }
        }
        
        // Draw Distant Icebergs
        for(int i=0; i<20; i++) {
            Matrix.setIdentityM(modelMatrix, 0);
            Matrix.translateM(modelMatrix, 0, icebergX[i], 0f, icebergZ[i]);
            Matrix.scaleM(modelMatrix, 0, 8f, icebergScaleY[i], 8f);
            Matrix.multiplyMM(scratchMatrix, 0, projectionAndViewMatrix, 0, modelMatrix, 0);
            icebergModel.draw(scratchMatrix);
        }

        // Draw SnowParticles overlay
        if (snowParticles != null) {
            snowParticles.updateAndDraw(projectionAndViewMatrix, camX, camZ);
        }
    }
}
