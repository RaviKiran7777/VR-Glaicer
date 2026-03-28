package com.example.vrdesert;

/**
 * All educational facts about glaciers and climate change.
 * Each target in the scene has a unique ID mapped to a fact string.
 */
public class InfoData {

    public static final int TARGET_GLACIER   = 0;
    public static final int TARGET_WATER     = 1;
    public static final int TARGET_BEAR      = 2;
    public static final int TARGET_SEAL      = 3;
    public static final int TARGET_FOX       = 4;
    public static final int TARGET_CALVING   = 5;
    public static final int TARGET_SNOW      = 6;

    public static String getFact(int targetId) {
        switch (targetId) {
            case TARGET_GLACIER:
                return "Glaciers store about 69% of the\nworld's fresh water. They have been\nretreating at accelerating rates\nsince the 1990s.";
            case TARGET_WATER:
                return "Global temperatures have risen 1.1C\nsince pre-industrial times, causing\nglaciers to lose 267 billion tonnes\nof ice per year.";
            case TARGET_BEAR:
                return "Polar bears depend on sea ice for\nhunting seals. As ice melts earlier\neach year, they face longer fasting\nperiods and declining populations.";
            case TARGET_SEAL:
                return "Arctic seals give birth on sea ice.\nShrinking ice reduces safe pupping\nhabitat, threatening populations\nacross the Arctic.";
            case TARGET_FOX:
                return "Arctic foxes' white winter camouflage\nis becoming a disadvantage as snow\nseasons shorten due to warming\nclimate changes.";
            case TARGET_CALVING:
                return "Glacier calving is accelerating.\nThe rate of ice loss has tripled\nin the last two decades due to\nrising global temperatures.";
            case TARGET_SNOW:
                return "Arctic snowfall patterns are shifting.\nRain is increasingly replacing snow,\ndestabilizing permafrost and altering\nentire ecosystems.";
            default:
                return "";
        }
    }
}
