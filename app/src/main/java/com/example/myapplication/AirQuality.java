package com.example.myapplication;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class AirQuality {
    private static final String API_TOKEN = "a216862100a39d6530de9a623889e273588fab3f";
    private static Double longitude;
    private static Double latitude;
    private static String city;

    public static int getAirQuality(double lat, double lon) {
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
                return -1;
            }

            JsonObject data = jobject.getAsJsonObject("data");
            int aqi = data.get("aqi").getAsInt();
            String city = data.getAsJsonObject("city").get("name").getAsString();
            String time = data.getAsJsonObject("time").get("s").getAsString();

            return aqi;

        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }
}
