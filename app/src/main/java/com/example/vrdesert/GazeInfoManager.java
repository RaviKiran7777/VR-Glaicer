package com.example.vrdesert;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages gaze-based interaction for showing educational info.
 * When the user stares at a registered target for 2 seconds,
 * the associated fact text is displayed.
 */
public class GazeInfoManager {

    public static class GazeTarget {
        public int id;
        public float x, y, z;
        public float radius;
        public String fact;

        public GazeTarget(int id, float x, float y, float z, float radius, String fact) {
            this.id = id; this.x = x; this.y = y; this.z = z;
            this.radius = radius; this.fact = fact;
        }
    }

    private final List<GazeTarget> targets = new ArrayList<>();
    private int currentTargetId = -1;
    private long gazeStartTime = 0;
    private static final long GAZE_TRIGGER_MS = 2000;

    private String displayedFact = "";
    private long factEndTime = 0;
    private boolean targeting = false;

    public void registerTarget(int id, float x, float y, float z, float radius, String fact) {
        targets.add(new GazeTarget(id, x, y, z, radius, fact));
    }

    public void checkGaze(float camX, float camY, float camZ,
                          float fwdX, float fwdY, float fwdZ) {
        targeting = false;
        for (GazeTarget t : targets) {
            float dx = t.x - camX;
            float dy = t.y - camY;
            float dz = t.z - camZ;
            float dist = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
            if (dist < 0.01f) continue;
            dx /= dist; dy /= dist; dz /= dist;
            float dot = fwdX*dx + fwdY*dy + fwdZ*dz;

            if (dot > 0.90f && dist < 80f) {
                targeting = true;
                if (currentTargetId != t.id) {
                    currentTargetId = t.id;
                    gazeStartTime = System.currentTimeMillis();
                } else {
                    long elapsed = System.currentTimeMillis() - gazeStartTime;
                    if (elapsed >= GAZE_TRIGGER_MS) {
                        displayedFact = t.fact;
                        factEndTime = System.currentTimeMillis() + 6000;
                    }
                }
                return;
            }
        }
        currentTargetId = -1;
    }

    /** Trigger a fact display directly (e.g. on calving event) */
    public void showFact(String fact) {
        displayedFact = fact;
        factEndTime = System.currentTimeMillis() + 5000;
    }

    public String getCurrentFact() {
        if (System.currentTimeMillis() < factEndTime) return displayedFact;
        return "";
    }

    public boolean isTargeting() { return targeting; }
}
