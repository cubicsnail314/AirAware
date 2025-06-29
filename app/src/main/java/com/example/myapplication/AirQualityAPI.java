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
import java.util.Locale;


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
                return new AirQualityResult(-1, "Unknown", "Unknown");
            }

            JsonObject data = jobject.getAsJsonObject("data");
            int aqi = data.get("aqi").getAsInt();

            JsonObject address = getAddressFromLatLon(lat,lon);
            String city = address.get("city").getAsString();
            String country = address.get("country").getAsString();
            String stationName = data.get("city").getAsJsonObject().get("name").getAsString();

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

            return new AirQualityResult(aqi, city, country, stationName, forecastDates, forecastAqi);

        } catch (Exception e) {
            e.printStackTrace();
            return new AirQualityResult(-1, "Error", "Error");
        }
    }

    public static JsonObject getAddressFromLatLon(Double lat, Double lon) throws Exception {
        String urlString = String.format(Locale.US, "https://nominatim.openstreetmap.org/reverse?format=json&lat=%f&lon=%f", lat, lon);
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        // Required User-Agent header for Nominatim
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Java App)");

        // Read response
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        // Parse JSON response
        JsonElement jelement = JsonParser.parseString(response.toString());
        JsonObject jobject = jelement.getAsJsonObject();
        return jobject.get("address").getAsJsonObject();
    }
}


