package com.example.vrdesert;

public class BackpackManager {
    private int iceCoreCount = 0;
    private int thermometerCount = 0;
    private int cameraTrapCount = 0;
    private int snowSampleCount = 0;

    public void addIceCore() { iceCoreCount++; }
    public void addThermometer() { thermometerCount++; }
    public void addCameraTrap() { cameraTrapCount++; }
    public void addSnowSample() { snowSampleCount++; }

    public int getCameraTrapCount() { return cameraTrapCount; }
    public int getSnowSampleCount() { return snowSampleCount; }

    public String getInventoryText() {
        StringBuilder sb = new StringBuilder("Data:\n");
        if (iceCoreCount > 0) sb.append("Ice Core: ").append(iceCoreCount).append("\n");
        if (thermometerCount > 0) sb.append("Temperature: ").append(thermometerCount).append("\n");
        if (cameraTrapCount > 0) sb.append("Camera Trap: ").append(cameraTrapCount).append("\n");
        if (snowSampleCount > 0) sb.append("Snow Sample: ").append(snowSampleCount).append("\n");
        
        if (iceCoreCount == 0 && thermometerCount == 0 && cameraTrapCount == 0 && snowSampleCount == 0) {
            sb.append("No Data");
        }
        return sb.toString().trim();
    }
}
