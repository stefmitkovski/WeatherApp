package com.example.weatherapp;

import static com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private RecyclerView mRecyclerView;
    private CityAdapter cityAdapter;
    private PeriodicWorkRequest workRequestLocation;
    private OneTimeWorkRequest workRequestCities;
    private FusedLocationProviderClient fusedLocationClient;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ArrayList<String> cities = getIntent().getStringArrayListExtra("cities");
        if (cities == null) {
            cities = new ArrayList<>();
        }

        // Recycle view
        mRecyclerView = findViewById(R.id.list_cities);

        mRecyclerView.setHasFixedSize(true);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mRecyclerView.setItemAnimator(new DefaultItemAnimator());

        final WorkManager mWorkManager = WorkManager.getInstance(getApplication());

        // Location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        requestLocationUpdates();

        // Checking for internet connection
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        // Get the current weather at the users location
        SharedPreferences sharedPreferences = getSharedPreferences("Weather Data", Context.MODE_PRIVATE);
        Data inputData = new Data.Builder().putString("runtype", "weather_location")
                .build();
        workRequestLocation = new PeriodicWorkRequest.Builder(WeatherWorker.class, 15, TimeUnit.MINUTES)
                .setInputData(inputData)
                .setConstraints(constraints)
                .build();

        mWorkManager.enqueueUniquePeriodicWork("weather_location", ExistingPeriodicWorkPolicy.UPDATE, workRequestLocation);
        mWorkManager.getWorkInfosForUniqueWorkLiveData("weather_location").observe(MainActivity.this, workInfos -> {
            if (workInfos.get(0).getState() == WorkInfo.State.ENQUEUED) {
                String outputValue = sharedPreferences.getString("key", "");
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

        // Get the current weather at the cities that the user has selected
        StringBuilder city_list = new StringBuilder();
        for (String city : cities) {
            city_list.append(city).append(",");
        }
        inputData = new Data.Builder().putString("runtype", "weather_cities").putString("cities", city_list.toString()).build();
        workRequestCities = new OneTimeWorkRequest.Builder(WeatherWorker.class)
                .setInputData(inputData)
                .setConstraints(constraints)
                .build();

        mWorkManager.enqueueUniqueWork("weather_cities", ExistingWorkPolicy.REPLACE, workRequestCities);
        ArrayList<String> finalCities = cities;
        mWorkManager.getWorkInfosForUniqueWorkLiveData("weather_cities").observe(MainActivity.this, workInfos -> {
            if (workInfos.get(0).getState() == WorkInfo.State.SUCCEEDED) {
                Data outputData = workInfos.get(0).getOutputData();
                String[] outputValue = outputData.getStringArray("key");

                cityAdapter = new CityAdapter(finalCities, R.layout.city_row, this, outputValue);

                mRecyclerView.setAdapter(cityAdapter);

                Log.d("MAIN", "CITIES DONE");
            }
            Log.d("MAIN", workInfos.get(0).getState().toString());
        });

        // Add button functionality
        ImageButton button = findViewById(R.id.add_city);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), AddCityActivity.class);
                intent.putStringArrayListExtra("cities", finalCities);
                startActivity(intent);
            }
        });
    }


    @Override
    protected void onStop() {
        super.onStop();
        WorkManager workManager = WorkManager.getInstance(getApplication());
        workManager.cancelUniqueWork("weather_cities");
    }

    @Override
    protected void onResume() {
        super.onResume();
        WorkManager workManager = WorkManager.getInstance(getApplication());
        workManager.enqueue(workRequestCities);
    }

    private void updateWeather(String temp, Double wind_speed, Integer wind_direction, Integer weather_code, Integer day, String time) {
        ImageView weather_icon = findViewById(R.id.weather_icon);
        TextView degrees = findViewById(R.id.degrees);
        TextView type = findViewById(R.id.weather_type);
        TextView speed = findViewById(R.id.wind_speed);
        TextView direction = findViewById(R.id.wind_direction);
        TextView last_update = findViewById(R.id.last_update);

        // Time
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
            Date date = null;
            date = inputFormat.parse(time);
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

    // Location
    private LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            // Called when a new location update is available
            if (locationResult != null) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    SharedPreferences currentLocation = getSharedPreferences("Location Coordinates", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = currentLocation.edit();
                    editor.putFloat("latitude", (float) location.getLatitude());
                    editor.putFloat("longitude", (float) location.getLongitude());
                    editor.apply();
                }
            }
        }

        @Override
        public void onLocationAvailability(LocationAvailability locationAvailability) {
            // Called when the location availability changes
        }
    };

    private void requestLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(PRIORITY_HIGH_ACCURACY,1000).build();
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }
}