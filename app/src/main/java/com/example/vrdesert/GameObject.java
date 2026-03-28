package com.example.vrdesert;

public class GameObject {
    public enum Type { ICE_CORE, THERMOMETER, CAMERA_TRAP, SNOW_SAMPLE }
    
    public float x;
    public float y;
    public float z;
    public boolean isCollected;
    public Type type;

    public GameObject(float x, float y, float z, Type type) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.type = type;
        this.isCollected = false;
    }

    public String getFact() {
        switch (type) {
            case ICE_CORE: return "Ice cores trap ancient atmosphere,\nrevealing CO2 levels spanning\ntens of thousands of years.";
            case THERMOMETER: return "Glaciers are retreating rapidly\nas global average temperatures\ncontinue to steadily climb.";
            case CAMERA_TRAP: return "Melting ice threatens native\narctic wildlife by destroying\ntheir hunting grounds.";
            case SNOW_SAMPLE: return "Soot and dust lower the\nglacier's albedo, making it\nabsorb more sunlight.";
            default: return "Data recorded.";
        }
    }
}
