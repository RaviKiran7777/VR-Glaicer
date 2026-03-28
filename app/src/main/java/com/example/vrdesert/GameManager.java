package com.example.vrdesert;

import android.content.Context;
import android.widget.ProgressBar;
import android.widget.Toast;

public class GameManager {
    private int dataProgress = 0;
    private final int MAX_DATA = 100;
    private boolean gameLocked = false;
    private Context context;
    private ProgressBar dataBarLeft;
    private ProgressBar dataBarRight;

    public GameManager(Context context, ProgressBar dataBarLeft, ProgressBar dataBarRight) {
        this.context = context;
        this.dataBarLeft = dataBarLeft;
        this.dataBarRight = dataBarRight;
        updateDataBar();
    }

    public boolean isGameLocked() {
        return gameLocked;
    }

    public void processItemCollection(GameObject.Type type, BackpackManager backpack) {
        if (gameLocked) return;

        // Data-giving items
        dataProgress = Math.min(dataProgress + 25, MAX_DATA);
        updateDataBar();

        checkWinCondition(backpack);
    }

    private void updateDataBar() {
        if (dataBarLeft != null && dataBarRight != null) {
            dataBarLeft.post(() -> {
                dataBarLeft.setProgress(dataProgress);
                dataBarRight.setProgress(dataProgress);
            });
        }
    }

    private void checkWinCondition(BackpackManager backpack) {
        // Condition: Collected enough scientific data
        if (dataProgress >= MAX_DATA && backpack.getCameraTrapCount() > 0 && backpack.getSnowSampleCount() > 0) {
            gameLocked = true; // Lock game loop
            
            Toast.makeText(context, "Observation complete! Analyzing climate data.", Toast.LENGTH_LONG).show();
            
            // Simulating a delay for the final event outcome
            if (dataBarLeft != null) {
                dataBarLeft.postDelayed(() -> {
                    Toast.makeText(context, "Data uploaded to research center. Mission Complete!", Toast.LENGTH_LONG).show();
                }, 4000);
            }
        }
    }
}
