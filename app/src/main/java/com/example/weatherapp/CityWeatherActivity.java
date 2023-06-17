package com.example.weatherapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class CityWeatherActivity extends AppCompatActivity {

    PeriodicWorkRequest workRequestCity;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_city_weather);

        String name = getIntent().getStringExtra("name");
        TextView city_name = findViewById(R.id.city_name);
        city_name.setText(name);

        // Check for internet connection
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        final WorkManager mWorkManager = WorkManager.getInstance(getApplication());
        Data inputData = new Data.Builder()
                .putString("runtype", "weather_city")
                .putDouble("latitude",getIntent().getDoubleExtra("latitude",0))
                .putDouble("longitude",getIntent().getDoubleExtra("longitude",0))
                .build();
        workRequestCity = new PeriodicWorkRequest.Builder(WeatherWorker.class, 15, TimeUnit.MINUTES)
                .setInputData(inputData)
                .setConstraints(constraints)
                .build();

        SharedPreferences cityPreferences = getSharedPreferences("City Data", Context.MODE_PRIVATE);
        mWorkManager.enqueueUniquePeriodicWork("weather_city", ExistingPeriodicWorkPolicy.UPDATE, workRequestCity);
        mWorkManager.getWorkInfosForUniqueWorkLiveData("weather_city").observe(CityWeatherActivity.this, workInfos -> {
            if (workInfos.get(0).getState() == WorkInfo.State.ENQUEUED) {
                String outputValue = cityPreferences.getString("key", "");
                if (!outputValue.isEmpty()) {
                    try {
                        JSONObject jsonObject = new JSONObject(outputValue).getJSONObject("current_weather");
                        String temp = jsonObject.getString("temperature");
                        Double wind_speed = jsonObject.getDouble("windspeed");
                        Integer wind_direction = jsonObject.getInt("winddirection");
                        Integer weather_code = jsonObject.getInt("weathercode");
                        Integer day = jsonObject.getInt("is_day");
                        String time = jsonObject.getString("time");
                        updateWeather(temp, wind_speed, wind_direction, weather_code, day, time);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            Log.d("MAIN", workInfos.get(0).getState().toString());
        });


        ImageButton back = findViewById(R.id.back_button);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(),MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                v.getContext().startActivity(intent);
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        WorkManager workManager = WorkManager.getInstance(getApplication());
        workManager.cancelUniqueWork("weather_city");
    }

    @Override
    protected void onResume() {
        super.onResume();
        WorkManager workManager = WorkManager.getInstance(getApplication());
        workManager.enqueue(workRequestCity);
    }

    private void updateWeather(String temp, Double wind_speed, Integer wind_direction, Integer weather_code, Integer day, String time) {
        ImageView weather_icon = findViewById(R.id.city_weather_icon);
        TextView degrees = findViewById(R.id.city_weather_degrees);
        TextView type = findViewById(R.id.city_weather_type);
        TextView speed = findViewById(R.id.city_weather_wind_speed);
        TextView direction = findViewById(R.id.city_weather_wind_direction);
        TextView last_update = findViewById(R.id.city_weather_last_update);

        // Time
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
            Date date = inputFormat.parse(time);
            SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm");
            String timeString = outputFormat.format(date);
            last_update.setText("Last updated on: " + timeString);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        degrees.setText(temp + "°");
        speed.setText("Wind Speed: " + wind_speed + " km/h");

        switch (weather_code) {
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
                if (day == 1) {
                    weather_icon.setImageResource(R.drawable.baseline_sunny);
                    type.setText("Sunny");
                } else {
                    weather_icon.setImageResource(R.drawable.baseline_night);
                    type.setText("Clear Sky");
                }
        }

        if (wind_direction > 315 || wind_direction <= 45) {
            direction.setText("Wind Direction: North");
        } else if (wind_direction > 45 && wind_direction <= 135) {
            direction.setText("Wind Direction: East");
        } else if (wind_direction > 135 && wind_direction <= 225) {
            direction.setText("Wind Direction: South");
        } else if (wind_direction > 225 && wind_direction <= 315) {
            direction.setText("Wind Direction: West");
        }
    }
}