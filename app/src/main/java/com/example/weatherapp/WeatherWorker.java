package com.example.weatherapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class WeatherWorker extends Worker {

    private static final String TAG = "WEATHER_WORKER";
    SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("Location Weather", Context.MODE_PRIVATE);
    private static final String DATABASE_NAME = "cities.db";
    public final static String DATABASE_PATH = "/data/data/com.example.weatherapp/databases/";
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
                String weatherData = fetchWeatherDataByLocation(1,2);
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
                String value = fetchWeatherDataByString(cities);
                if (value != null && !value.isEmpty()){
                    Data outputData = new Data.Builder().putString("key",value).build();
                    return Result.success(outputData);
                }else{
                    Log.d(TAG,"Value is null or is empty");
                    return Result.failure();
                }
        }
        return Result.success();
    }


    private String fetchWeatherDataByLocation(double lat, double lon) {
        // Simulate fetching weather data
        return "Sunny";
    }

    private String fetchWeatherDataByString(String cities){
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
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG,"Failed when operation with the database");
            return null;
        } finally {
            closeDatabase(database);
        }

        return "lat: " + latitude + " lon: " + longitude;
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

