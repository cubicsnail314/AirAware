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
                return new AirQualityResult(-1, "Unknown", "Unknown");
            }

            JsonObject data = jobject.getAsJsonObject("data");
            int aqi = data.get("aqi").getAsInt();

            // Get city information from the API's city object
            String city = "Unknown";
            String country = "Unknown";
            String stationName = "Unknown";

            JsonObject cityObj = data.getAsJsonObject("city");
            if (cityObj != null) {
                // Check if the API provides city name directly
                if (cityObj.has("name")) {
                    stationName = cityObj.get("name").getAsString();

                    // Check if the name contains street-level information
                    if (stationName.contains("gasse") || stationName.contains("straße") ||
                            stationName.contains("street") || stationName.contains("avenue") ||
                            stationName.contains("gegenüber") || stationName.contains("opposite")) {

                        // Try to extract city from URL if available
                        if (cityObj.has("url")) {
                            String cityUrl = cityObj.get("url").getAsString();

                            // URL format: https://aqicn.org/city/country/city/station
                            String[] urlParts = cityUrl.split("/");
                            if (urlParts.length >= 6) {
                                // The structure is: https://aqicn.org/city/country/city/station
                                String urlCountry = urlParts[4];
                                String urlCity = urlParts[5];

                                // Clean up the URL city name
                                urlCity = urlCity.replace("--", " ").replace("-", " ");

                                // Try to extract meaningful city name
                                String[] words = urlCity.split(" ");
                                for (String word : words) {
                                    if (word.length() > 2 && !word.equals("gegenuber") &&
                                            !word.equals("opposite") && !word.equals("near")) {
                                        city = word.substring(0, 1).toUpperCase() +
                                                word.substring(1).toLowerCase();
                                        break;
                                    }
                                }

                                country = urlCountry.substring(0, 1).toUpperCase() +
                                        urlCountry.substring(1).toLowerCase();
                            }
                        }
                    } else {
                        // The name doesn't contain street terms, use normal parsing
                        String[] parts = stationName.split(",");
                        if (parts.length >= 2) {
                            country = parts[parts.length - 1].trim();

                            if (parts.length >= 3) {
                                city = parts[parts.length - 2].trim();
                            } else {
                                city = parts[0].trim();
                            }
                        } else {
                            city = stationName;
                        }
                    }
                }

                // Check if API provides country separately
                if (cityObj.has("country")) {
                    country = cityObj.get("country").getAsString();
                }
            }

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
}


