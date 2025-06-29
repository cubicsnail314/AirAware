package com.example.myapplication;

public class AirQualityResult {
    public int aqi;
     public String stationName;
    public String[] forecastDates;
    public int[] forecastAqi;

    public AirQualityResult(int aqi, String stationName, String[] forecastDates, int[] forecastAqi) {
        this.aqi = aqi;
        this.stationName = stationName;
        this.forecastDates = forecastDates;
        this.forecastAqi = forecastAqi;
    }
}