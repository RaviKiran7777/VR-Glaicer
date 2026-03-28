package com.example.vrdesert;

import com.example.vrdesert.shapes.SplashParticles;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Random;

/**
 * Manages glacier calving events.
 * Every 10-20 seconds (random), an ice chunk breaks off
 * the glacier wall and falls under gravity into the meltwater.
 */
public class CalvingManager {

    public static class IceChunk {
        public float x, y, z;
        public float vy;
        public float scale;
        public boolean hasImpacted;

        public IceChunk(float x, float y, float z, float scale) {
            this.x = x; this.y = y; this.z = z;
            this.vy = 0f; this.scale = scale; this.hasImpacted = false;
        }
    }

    private static final float GRAVITY = -9.8f;
    public static final float WALL_Z = -30f;
    public static final float WALL_WIDTH = 80f;

    private static final float CALVING_MIN_S = 10f;
    private static final float CALVING_MAX_S = 20f;

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
                spawnChunk();
                nextCalvingIn = CALVING_MIN_S + rand.nextFloat() * (CALVING_MAX_S - CALVING_MIN_S);
                gazeInfoManager.showFact(InfoData.getFact(InfoData.TARGET_CALVING));
            }
        }

        List<IceChunk> toRemove = new ArrayList<>();
        for (IceChunk chunk : activeChunks) {
            if (chunk.hasImpacted) { toRemove.add(chunk); continue; }
            chunk.vy += GRAVITY * dt;
            chunk.y  += chunk.vy * dt;
            if (chunk.y <= 0f) {
                chunk.y = 0f;
                chunk.hasImpacted = true;
                splashParticles.trigger(chunk.x, chunk.z);
            }
        }
        activeChunks.removeAll(toRemove);
    }

    private void spawnChunk() {
        float x = -WALL_WIDTH / 2f + rand.nextFloat() * WALL_WIDTH;
        float y = 12f + rand.nextFloat() * 10f;
        float z = WALL_Z + 1f + rand.nextFloat() * 2f;
        float scale = 0.8f + rand.nextFloat() * 1.5f;
        activeChunks.add(new IceChunk(x, y, z, scale));
    }

    public List<IceChunk> getActiveChunks() { return activeChunks; }
}
