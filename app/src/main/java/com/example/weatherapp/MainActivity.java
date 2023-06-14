package com.example.weatherapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    RecyclerView mRecyclerView;
    CityAdapter cityAdapter;
    PeriodicWorkRequest workRequestLocation;
    OneTimeWorkRequest  workRequestCities;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        List<String> cities = Arrays.asList("Paris","London","Sydney");

        mRecyclerView = (RecyclerView) findViewById(R.id.list_cities);

        mRecyclerView.setHasFixedSize(true);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mRecyclerView.setItemAnimator(new DefaultItemAnimator());

        final WorkManager mWorkManager = WorkManager.getInstance(getApplication());

        // Get the current weather at the users location
        SharedPreferences sharedPreferences = getSharedPreferences("Location Weather", Context.MODE_PRIVATE);
        Data inputData = new Data.Builder().putString("runtype","weather_location")
                .putDouble("latitude",42.004748)
                .putDouble("longitude",21.408907)
                .build();
        workRequestLocation = new PeriodicWorkRequest.Builder(WeatherWorker.class, 15, TimeUnit.MINUTES)
                .setInputData(inputData)
                .build();

        mWorkManager.enqueueUniquePeriodicWork("weather_location", ExistingPeriodicWorkPolicy.UPDATE,workRequestLocation);
        mWorkManager.getWorkInfosForUniqueWorkLiveData("weather_location").observe(MainActivity.this, workInfos -> {
                if(workInfos.get(0).getState() == WorkInfo.State.ENQUEUED){
                    String outputValue = sharedPreferences.getString("key","");
                    if(!outputValue.isEmpty()){
                        try {
                            JSONObject jsonObject = new JSONObject(outputValue.toString());
                            String temp = jsonObject.getString("temperature");
                            Double wind_speed = jsonObject.getDouble("windspeed");
                            Integer wind_direction = jsonObject.getInt("winddirection");
                            Integer weather_code = jsonObject.getInt("weathercode");
                            Integer day = jsonObject.getInt("is_day");
                            updateWeather(temp,wind_speed,wind_direction,weather_code,day);
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                Log.d("MAIN",workInfos.get(0).getState().toString());
        });

        // Get the current weather at the cities that the user has selected
        StringBuilder city_list = new StringBuilder();
        for (String city: cities) {
            city_list.append(city).append(",");
        }
        inputData = new Data.Builder().putString("runtype","weather_cities").putString("cities", city_list.toString()).build();
        workRequestCities = new OneTimeWorkRequest.Builder(WeatherWorker.class)
                .setInputData(inputData)
                .build();

        mWorkManager.enqueueUniqueWork("weather_cities",ExistingWorkPolicy.REPLACE,workRequestCities);
        mWorkManager.getWorkInfosForUniqueWorkLiveData("weather_cities").observe(MainActivity.this, workInfos -> {
            if(workInfos.get(0).getState() == WorkInfo.State.SUCCEEDED){
                Data outputData = workInfos.get(0).getOutputData();
                String[] outputValue = outputData.getStringArray("key");

                cityAdapter = new CityAdapter(cities, R.layout.city_row, this,outputValue);

                mRecyclerView.setAdapter(cityAdapter);

                Log.d("MAIN","CITIES DONE");
            }
            Log.d("MAIN",workInfos.get(0).getState().toString());
        });


    }

    private void updateWeather(String temp, Double wind_speed, Integer wind_direction, Integer weather_code,Integer day) {
        ImageView weather_icon = findViewById(R.id.weather_icon);
        TextView degrees = findViewById(R.id.degrees);
        TextView type = findViewById(R.id.weather_type);
        TextView speed = findViewById(R.id.wind_speed);
        TextView direction = findViewById(R.id.wind_direction);

        degrees.setText(temp+"°");
        speed.setText("Wind Speed: " + wind_speed + " km/h");

        switch (weather_code){
            case 2:
                weather_icon.setImageResource(R.drawable.baseline_cloudy);
                type.setText("Cloudy");
                break;
            case 3:
                weather_icon.setImageResource(R.drawable.baseline_rainy);
                type.setText("Rainy");
                break;
            case 4:
                weather_icon.setImageResource(R.drawable.baseline_thunderstorm);
                type.setText("Thunderstorm");
                break;
            case 5:
                weather_icon.setImageResource(R.drawable.baseline_snow);
                type.setText("Snowing");
                break;
            default:
                if(day == 1){
                    weather_icon.setImageResource(R.drawable.baseline_sunny);
                    type.setText("Sunny");
                }else {
                    weather_icon.setImageResource(R.drawable.baseline_night);
                    type.setText("Clear Sky");
                }
        }

        if(wind_direction > 315 || wind_direction <= 45){
            direction.setText("Wind Direction: North");
        } else if (wind_direction > 45 && wind_direction <= 135) {
            direction.setText("Wind Direction: East");
        } else if (wind_direction > 135 && wind_direction <= 225) {
            direction.setText("Wind Direction: South");
        }else if(wind_direction > 225 && wind_direction <= 315){
            direction.setText("Wind Direction: West");
        }
    }

    // Check if there is internet connection
//    private boolean isInternetAvailable() {
//        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
//        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
//        return networkInfo != null && networkInfo.isConnectedOrConnecting();
//    }


}