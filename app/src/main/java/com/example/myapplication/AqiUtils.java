package com.example.myapplication;

import android.content.Context;
import android.graphics.Color;

public class AqiUtils {
    public static class WienerAqiInfo {
        public int index;
        public String description;
        public int color;
        public WienerAqiInfo(int index, String description, int color) {
            this.index = index;
            this.description = description;
            this.color = color;
        }
    }

    public static WienerAqiInfo getWienerAqiInfo(Context context, int aqi) {
        if (aqi <= 60) {
            return new WienerAqiInfo(1, "sehr gut", Color.parseColor("#B7F6B7"));
        } else if (aqi <= 90) {
            return new WienerAqiInfo(2, "gut", Color.parseColor("#7ED87E"));
        } else if (aqi <= 120) {
            return new WienerAqiInfo(3, "befriedigend", Color.parseColor("#3A9B3A"));
        } else if (aqi <= 180) {
            return new WienerAqiInfo(4, "unbefriedigend", Color.parseColor("#FFFF66"));
        } else if (aqi <= 240) {
            return new WienerAqiInfo(5, "schlecht", Color.parseColor("#FF9900"));
        } else {
            return new WienerAqiInfo(6, "sehr schlecht", Color.parseColor("#FF3333"));
        }
    }
} 