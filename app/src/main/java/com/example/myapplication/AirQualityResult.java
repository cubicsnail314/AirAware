package com.example.myapplication;

public class AirQualityResult {
    public int aqi;
    public String city;
    public String country;
    public String stationName;
    public String[] forecastDates;
    public int[] forecastAqi;

    public AirQualityResult(int aqi, String city, String country) {
        this.aqi = aqi;
        this.city = city;
        this.country = country;
        this.stationName = "";
        this.forecastDates = new String[0];
        this.forecastAqi = new int[0];
    }

    public AirQualityResult(int aqi, String city, String country, String stationName, String[] forecastDates, int[] forecastAqi) {
        this.aqi = aqi;
        this.city = city;
        this.country = country;
        this.stationName = stationName;
        this.forecastDates = forecastDates;
        this.forecastAqi = forecastAqi;
    }
} 