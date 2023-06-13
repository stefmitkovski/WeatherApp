package com.example.weatherapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class WeatherWorker extends Worker {

    SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("Location Weather", Context.MODE_PRIVATE);

    public WeatherWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {

        // Perform your background task here
        String runType = getInputData().getString("runtype");

        switch (runType){
            case "weather_current_location":
                Log.d("WEATHER_WORKER","CURRENT LOCATION");
                String weatherData = fetchWeatherData();
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("key", weatherData);
                editor.apply();
                break;
            case "weather_cities":
                Log.d("WEATHER_WORKER","CITIES");
                Data outputData = new Data.Builder().putString("key","RAIN").build();
                return Result.success(outputData);
        }
        return Result.success();
    }

    private String fetchWeatherData() {
        // Simulate fetching weather data
        return "Sunny";
    }
}

