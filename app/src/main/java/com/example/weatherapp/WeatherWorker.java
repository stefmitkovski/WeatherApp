package com.example.weatherapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class WeatherWorker extends Worker {

    SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("Location Weather", Context.MODE_PRIVATE);
    private static final String TAG = "WEATHER_WORKER";
    private static final String DATABASE_NAME = "cities.db";
    public static final String DATABASE_PATH = "/data/data/com.example.weatherapp/databases/";

    // Example URL:
    // https://api.open-meteo.com/v1/forecast?
    // latitude=52.52&longitude=13.41
    // &current_weather=true
    private static final String API_URL = "https://api.open-meteo.com/v1/forecast?";
    public WeatherWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {

        // Perform your background task here
        String runType = getInputData().getString("runtype");

        switch (runType){
            case "weather_location":
                Log.d(TAG,"CURRENT LOCATION");
                Double latitude = getInputData().getDouble("latitude",0);
                Double longitude = getInputData().getDouble("longitude",0);
                String weatherData = fetchWeatherDataByLocation(latitude,longitude);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("key", weatherData);
                editor.apply();
                break;
            case "weather_cities":
                Log.d(TAG,"CITIES");
                String cities = getInputData().getString("cities");

                if (cities == null || cities.isEmpty()) {
                    return Result.failure();
                }
                String[] value = fetchWeatherDataByString(cities);
                if (value.length != 0){
                    Data outputData = new Data.Builder().putStringArray("key",value).build();
                    return Result.success(outputData);
                }else{
                    Log.d(TAG,"Value is null or is empty");
                    return Result.failure();
                }
        }
        return Result.success();
    }


    static String fetchWeatherDataByLocation(double lat, double lon) {
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        String weatherJSONString = null;

        try {
            Uri builtURI = Uri.parse(API_URL).buildUpon()
                    .appendQueryParameter("latitude", String.valueOf(lat))
                    .appendQueryParameter("longitude",String.valueOf(lon))
                    .appendQueryParameter("current_weather","true")
                    .build();

            URL requestURL = new URL(builtURI.toString());

            urlConnection = (HttpURLConnection) requestURL.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            InputStream inputStream = urlConnection.getInputStream();

            reader = new BufferedReader(new InputStreamReader(inputStream));

            StringBuilder builder = new StringBuilder();

            String line;

            while ((line = reader.readLine()) != null) {
                builder.append(line);
                builder.append("\n");
            }

            if (builder.length() == 0) {
                // Stream was empty.  Exit without parsing.
                return null;
            }

            JSONObject jsonObject = new JSONObject(builder.toString()).getJSONObject("current_weather");
            weatherJSONString = jsonObject.toString();

        } catch (IOException e) {
            Log.d(TAG, "Can't connect to the URL");
            e.printStackTrace();
            return null;
        } catch (JSONException e) {
            Log.d(TAG, "Can't parse JSON data");
            e.printStackTrace();
            return null;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        Log.d(TAG,weatherJSONString);
        return weatherJSONString;

    }

    private String[] fetchWeatherDataByString(String cities){
        String[]list_cities = cities.split(",");
        if(list_cities.length == 0){
            Log.d(TAG,"No cities in the array");
            return null;
        }
        SQLiteDatabase database = openDatabase();
        if (database == null) {
            Log.d(TAG,"Failed to open the database");
            return null;
        }

        double latitude = 0.0;
        double longitude = 0.0;

        try {

            for (int i=0; i<list_cities.length;i++){

                latitude = 0.0;
                longitude = 0.0;
                Cursor cursor = database.rawQuery("SELECT lat, lng FROM worldcities_Sheet1 WHERE city = ?", new String[]{list_cities[i]});
                if (cursor.moveToFirst()) {
                    int latitudeIndex = cursor.getColumnIndex("lat");
                    int longitudeIndex = cursor.getColumnIndex("lng");

                    if (latitudeIndex != -1 && longitudeIndex != -1) {
                        latitude = cursor.getDouble(latitudeIndex);
                        longitude = cursor.getDouble(longitudeIndex);
                    }
                }
                Log.d(TAG,"lat: " + latitude + " lon: " + longitude);
                list_cities[i] = fetchWeatherDataByLocation(latitude,longitude);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG,"Failed when operation with the database");
            return null;
        } finally {
            closeDatabase(database);
        }

        return list_cities;
    }

    private SQLiteDatabase openDatabase() {
        try {
            String dbPath = DATABASE_PATH + DATABASE_NAME;
            return SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY);
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG,"Failed to get the path");
            return null;
        }
    }

    private void closeDatabase(SQLiteDatabase database) {
        if (database != null && database.isOpen()) {
            database.close();
        }
    }

}

