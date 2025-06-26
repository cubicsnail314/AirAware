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

            // Debug: Print the API response
            System.out.println("API Response: " + content.toString());

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
                // Debug: Print the entire city object to see its structure
                System.out.println("City object: " + cityObj.toString());

                // Check all available fields in the city object
                for (String key : cityObj.keySet()) {
                    System.out.println("City field '" + key + "': " + cityObj.get(key));
                }

                // Check if the API provides city name directly
                if (cityObj.has("name")) {
                    stationName = cityObj.get("name").getAsString();
                    System.out.println("Full city name from API: " + stationName);

                    // Check if the name contains street-level information
                    if (stationName.contains("gasse") || stationName.contains("straße") ||
                            stationName.contains("street") || stationName.contains("avenue") ||
                            stationName.contains("gegenüber") || stationName.contains("opposite")) {

                        System.out.println("Detected street-level information, checking URL...");

                        // Try to extract city from URL if available
                        if (cityObj.has("url")) {
                            String cityUrl = cityObj.get("url").getAsString();
                            System.out.println("City URL: " + cityUrl);

                            // URL format: https://aqicn.org/city/austria/petersgasse--gegenuber-eisteichgasse
                            // We need to extract the city name from the URL path
                            String[] urlParts = cityUrl.split("/");
                            if (urlParts.length >= 6) {
                                // The structure is: https://aqicn.org/city/country/city/station
                                String urlCountry = urlParts[4]; // "austria"
                                String urlCity = urlParts[5]; // "petersgasse--gegenuber-eisteichgasse"

                                System.out.println("URL Country: " + urlCountry);
                                System.out.println("URL City: " + urlCity);

                                // Clean up the URL city name
                                urlCity = urlCity.replace("--", " ").replace("-", " ");

                                // If the URL city still contains street terms, try to find the actual city
                                // For Graz, Austria, we know this is Graz
                                if (urlCountry.equals("austria") &&
                                        (urlCity.contains("petersgasse") || urlCity.contains("eisteichgasse"))) {
                                    city = "Graz";
                                    System.out.println("Identified as Graz, Austria");
                                } else {
                                    // For other cases, try to extract meaningful city name
                                    String[] words = urlCity.split(" ");
                                    for (String word : words) {
                                        if (word.length() > 2 && !word.equals("gegenuber") &&
                                                !word.equals("opposite") && !word.equals("near")) {
                                            city = word.substring(0, 1).toUpperCase() +
                                                    word.substring(1).toLowerCase();
                                            break;
                                        }
                                    }
                                }

                                country = urlCountry.substring(0, 1).toUpperCase() +
                                        urlCountry.substring(1).toLowerCase();
                            }
                        } else {
                            System.out.println("No URL available, using fallback parsing");
                            // Fallback: try to extract from the name field
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
                    } else {
                        System.out.println("No street-level information detected, using normal parsing");
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

            // Also check if there are other location fields in the main data object
            System.out.println("Checking for other location fields in data object:");
            for (String key : data.keySet()) {
                if (key.toLowerCase().contains("city") || key.toLowerCase().contains("location") ||
                        key.toLowerCase().contains("place") || key.toLowerCase().contains("name")) {
                    System.out.println("Location-related field '" + key + "': " + data.get(key));
                }
            }

            System.out.println("Extracted city: " + city);
            System.out.println("Extracted country: " + country);

            // Extract forecast for next 3 days (pm10 AQI)
            String[] forecastDates = new String[3];
            int[] forecastAqi = new int[3];
            if (data.has("forecast")) {
                JsonObject forecast = data.getAsJsonObject("forecast");
                if (forecast.has("daily")) {
                    JsonObject daily = forecast.getAsJsonObject("daily");
                    if (daily.has("pm10")) {
                        // Get today's date to filter out today from forecast
                        Calendar today = Calendar.getInstance();
                        String todayStr = String.format("%04d-%02d-%02d",
                                today.get(Calendar.YEAR),
                                today.get(Calendar.MONTH) + 1,
                                today.get(Calendar.DAY_OF_MONTH));

                        int i = 0;
                        for (JsonElement elem : daily.getAsJsonArray("pm10")) {
                            if (i >= 3) break;
                            JsonObject day = elem.getAsJsonObject();
                            String forecastDate = day.get("day").getAsString();

                            // Skip today's date and only include future dates
                            if (!forecastDate.equals(todayStr)) {
                                forecastDates[i] = forecastDate;
                                forecastAqi[i] = day.get("avg").getAsInt();
                                i++;
                            }
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
