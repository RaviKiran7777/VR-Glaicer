package com.example.vrdesert;

import com.example.vrdesert.shapes.SplashParticles;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Random;

/**
 * Realistic glacier calving simulation.
 * Each event spawns 3-6 chunks that tumble forward with rotation,
 * scatter in X, and create dramatic splashes on water impact.
 */
public class CalvingManager {

    public static class IceChunk {
        public float x, y, z;
        public float vx, vy, vz;        // 3D velocity (forward kick + gravity)
        public float rotX, rotY, rotZ;   // Current rotation angles
        public float rotSpeedX, rotSpeedY, rotSpeedZ; // Tumbling speeds (deg/s)
        public float scale;
        public boolean hasImpacted;
        public float impactTimer;        // Time since impact (for lingering)

        public IceChunk(float x, float y, float z, float scale,
                        float vx, float vz,
                        float rsx, float rsy, float rsz) {
            this.x = x; this.y = y; this.z = z;
            this.vx = vx; this.vy = 0f; this.vz = vz;
            this.rotX = 0; this.rotY = 0; this.rotZ = 0;
            this.rotSpeedX = rsx; this.rotSpeedY = rsy; this.rotSpeedZ = rsz;
            this.scale = scale;
            this.hasImpacted = false;
            this.impactTimer = 0f;
        }
    }

    private static final float GRAVITY = -12.5f; // slightly stronger for dramatic effect
    public static final float WALL_Z = -30f;
    public static final float WALL_WIDTH = 80f;

    private static final float CALVING_MIN_S = 10f;
    private static final float CALVING_MAX_S = 20f;
    private static final float LINGER_TIME = 3.0f; // chunks stay visible 3s after splash

    private final List<IceChunk> activeChunks = new CopyOnWriteArrayList<>();
    private final SplashParticles splashParticles;
    private final GazeInfoManager gazeInfoManager;
    private final Random rand = new Random();

    private float nextCalvingIn;
    private long lastUpdateTime;
    private boolean enabled = true;

    public CalvingManager(SplashParticles splashParticles, GazeInfoManager gazeInfoManager) {
        this.splashParticles = splashParticles;
        this.gazeInfoManager = gazeInfoManager;
        this.lastUpdateTime = System.currentTimeMillis();
        this.nextCalvingIn = CALVING_MIN_S;
    }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public void update() {
        long now = System.currentTimeMillis();
        float dt = (now - lastUpdateTime) / 1000f;
        if (dt > 0.1f) dt = 0.1f;
        lastUpdateTime = now;

        if (enabled) {
            nextCalvingIn -= dt;
            if (nextCalvingIn <= 0f) {
                spawnCalvingEvent();
                nextCalvingIn = CALVING_MIN_S + rand.nextFloat() * (CALVING_MAX_S - CALVING_MIN_S);
                gazeInfoManager.showFact(InfoData.getFact(InfoData.TARGET_CALVING));
            }
        }

        List<IceChunk> toRemove = new ArrayList<>();
        for (IceChunk c : activeChunks) {
            if (c.hasImpacted) {
                c.impactTimer += dt;
                if (c.impactTimer >= LINGER_TIME) toRemove.add(c);
                continue;
            }

            // Apply gravity
            c.vy += GRAVITY * dt;

            // Apply velocity
            c.x += c.vx * dt;
            c.y += c.vy * dt;
            c.z += c.vz * dt;

            // Tumbling rotation
            c.rotX += c.rotSpeedX * dt;
            c.rotY += c.rotSpeedY * dt;
            c.rotZ += c.rotSpeedZ * dt;

            // Water impact
            if (c.y <= 0f) {
                c.y = 0f;
                c.vy = 0f; c.vx = 0f; c.vz = 0f;
                c.rotSpeedX = 0; c.rotSpeedY = 0; c.rotSpeedZ = 0;
                c.hasImpacted = true;
                splashParticles.trigger(c.x, c.z);
            }
        }
        activeChunks.removeAll(toRemove);
    }

    /** Spawns a cluster of 3-6 chunks near the same wall position. */
    private void spawnCalvingEvent() {
        // Pick a center point on the wall
        float centerX = -WALL_WIDTH / 2f + rand.nextFloat() * WALL_WIDTH;
        float centerY = 14f + rand.nextFloat() * 8f;
        float baseZ   = WALL_Z + 1f;

        int count = 3 + rand.nextInt(4); // 3 to 6 chunks

        for (int i = 0; i < count; i++) {
            // Scatter each chunk slightly from center
            float x = centerX + (rand.nextFloat() * 6f - 3f);
            float y = centerY + (rand.nextFloat() * 4f - 2f);
            float z = baseZ + rand.nextFloat() * 1.5f;
            float scale = 0.5f + rand.nextFloat() * 1.8f;

            // Forward kick (away from wall) + slight random sideways
            float vx = (rand.nextFloat() - 0.5f) * 3f;  // random sideways scatter
            float vz = 2f + rand.nextFloat() * 5f;       // forward away from wall

            // Random tumbling speeds (degrees per second)
            float rsx = 60f + rand.nextFloat() * 200f;
            float rsy = 30f + rand.nextFloat() * 150f;
            float rsz = 40f + rand.nextFloat() * 180f;
            // Randomize direction of spin
            if (rand.nextBoolean()) rsx = -rsx;
            if (rand.nextBoolean()) rsy = -rsy;
            if (rand.nextBoolean()) rsz = -rsz;

            activeChunks.add(new IceChunk(x, y, z, scale, vx, vz, rsx, rsy, rsz));
        }
    }

    public List<IceChunk> getActiveChunks() { return activeChunks; }
}
