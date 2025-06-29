package com.example.myapplication;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class AirQualityAPI {
    private static final String API_TOKEN = "a216862100a39d6530de9a623889e273588fab3f";

    public static List<StationSearchResult> getStationsFromSearch(String keyword) {
        String encodedKeyword = keyword.replace(" ", "%20");
        String apiUrl = String.format("https://api.waqi.info/search/?keyword=%s&token=%s", encodedKeyword, API_TOKEN);
        try {
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream())
            );
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }

            in.close();
            conn.disconnect();

            JsonElement jelement = JsonParser.parseString(content.toString());
            JsonObject jobject = jelement.getAsJsonObject();

            if (!"ok".equals(jobject.get("status").getAsString())) {
                return List.of();
            }

            JsonElement dataElement = jobject.get("data");
            if (!dataElement.isJsonArray() || dataElement.getAsJsonArray().size() == 0) {
                return List.of();
            }
            List<StationSearchResult> stationSearchResults = new ArrayList<>();

            if (dataElement.isJsonArray()) {
                for (JsonElement element : dataElement.getAsJsonArray()) {
                    JsonObject stationObj = element.getAsJsonObject().get("station").getAsJsonObject();
                    String stationName = stationObj.get("name").getAsString();
                    Double longitude = stationObj.get("geo").getAsJsonArray().get(1).getAsDouble();
                    Double latitude = stationObj.get("geo").getAsJsonArray().get(0).getAsDouble();
                    stationSearchResults.add(new StationSearchResult(stationName, longitude, latitude));
                }
            }
            return stationSearchResults;

        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    public static AirQualityResult getAirQualityWithDetails(double lat, double lon) {
        String apiUrl = String.format("https://api.waqi.info/feed/geo:%f;%f/?token=%s", lat, lon, API_TOKEN);
        try {
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream())
            );
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }

            in.close();
            conn.disconnect();

            JsonElement jelement = JsonParser.parseString(content.toString());
            JsonObject jobject = jelement.getAsJsonObject();

            if (!"ok".equals(jobject.get("status").getAsString())) {
                return new AirQualityResult(-1, "Unknown", new String[0], new int[0]);
            }

            JsonObject data = jobject.getAsJsonObject("data");
            int aqi = data.get("aqi").getAsInt();

            JsonObject cityObj = data.getAsJsonObject("city");
            String stationName = cityObj.get("name").getAsString();

            // Extract forecast for next 3 days (pm10 AQI)
            String[] forecastDates = new String[3];
            int[] forecastAqi = new int[3];
            if (data.has("forecast")) {
                JsonObject forecast = data.getAsJsonObject("forecast");
                if (forecast.has("daily")) {
                    JsonObject daily = forecast.getAsJsonObject("daily");
                    if (daily.has("pm10")) {
                        // Get today's date
                        Calendar today = Calendar.getInstance();
                        today.set(Calendar.HOUR_OF_DAY, 0);
                        today.set(Calendar.MINUTE, 0);
                        today.set(Calendar.SECOND, 0);
                        today.set(Calendar.MILLISECOND, 0);
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
                        String todayStr = sdf.format(today.getTime());

                        // Collect all future dates
                        java.util.List<String> futureDates = new java.util.ArrayList<>();
                        java.util.Map<String, Integer> dateToAqi = new java.util.HashMap<>();
                        for (JsonElement elem : daily.getAsJsonArray("pm10")) {
                            JsonObject day = elem.getAsJsonObject();
                            String forecastDate = day.get("day").getAsString();
                            int avgAqi = day.get("avg").getAsInt();
                            if (forecastDate.compareTo(todayStr) > 0) { // strictly after today
                                futureDates.add(forecastDate);
                                dateToAqi.put(forecastDate, avgAqi);
                            }
                        }
                        // Sort the dates
                        java.util.Collections.sort(futureDates);
                        // Take the next three
                        for (int i = 0; i < 3 && i < futureDates.size(); i++) {
                            forecastDates[i] = futureDates.get(i);
                            forecastAqi[i] = dateToAqi.get(futureDates.get(i));
                        }
                    }
                }
            }

            return new AirQualityResult(aqi, stationName, forecastDates, forecastAqi);

        } catch (Exception e) {
            e.printStackTrace();
            return new AirQualityResult(-1, "Error", new String[0], new int[0]);
        }
    }
}


