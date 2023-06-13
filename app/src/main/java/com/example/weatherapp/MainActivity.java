package com.example.weatherapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.Operation;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

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
        List<String> cities = Arrays.asList("Paris");

        final WorkManager mWorkManager = WorkManager.getInstance(getApplication());

        // Get the current weather at the users location
        SharedPreferences sharedPreferences = getSharedPreferences("Location Weather", Context.MODE_PRIVATE);
        Data inputData = new Data.Builder().putString("runtype","weather_current_location").build();
        workRequestLocation = new PeriodicWorkRequest.Builder(WeatherWorker.class, 15, TimeUnit.MINUTES)
                .setInputData(inputData)
                .build();

        mWorkManager.enqueueUniquePeriodicWork("weather_current_location", ExistingPeriodicWorkPolicy.UPDATE,workRequestLocation);
        mWorkManager.getWorkInfosForUniqueWorkLiveData("weather_current_location").observe(MainActivity.this, new Observer<List<WorkInfo>>() {
            @Override
            public void onChanged(List<WorkInfo> workInfos) {
                    if(workInfos.get(0).getState() == WorkInfo.State.ENQUEUED){
                        String outputValue = sharedPreferences.getString("key","");
                        if(!outputValue.isEmpty()){
                            Log.d("MAIN", "LOCATION DONE: " + outputValue);
                        }
                    }
                    Log.d("MAIN",workInfos.get(0).getState().toString());
            }
        });

        // Get the current weather at the cities that the user has selected
        inputData = new Data.Builder().putString("runtype","weather_cities").build();
        workRequestCities = new OneTimeWorkRequest.Builder(WeatherWorker.class)
                .setInputData(inputData)
                .build();

        mWorkManager.enqueueUniqueWork("weather_cities",ExistingWorkPolicy.REPLACE,workRequestCities);
        mWorkManager.getWorkInfosForUniqueWorkLiveData("weather_cities").observe(MainActivity.this, new Observer<List<WorkInfo>>() {
            @Override
            public void onChanged(List<WorkInfo> workInfos) {
                if(workInfos.get(0).getState() == WorkInfo.State.SUCCEEDED){
                    Data outputData = workInfos.get(0).getOutputData();
                    String outputValue = outputData.getString("key");
                    Log.d("MAIN","CITIES DONE: " + outputValue);
                }
                Log.d("MAIN",workInfos.get(0).getState().toString());
            }
        });

        mRecyclerView = (RecyclerView) findViewById(R.id.list_cities);

        mRecyclerView.setHasFixedSize(true);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mRecyclerView.setItemAnimator(new DefaultItemAnimator());

        cityAdapter = new CityAdapter(cities, R.layout.city_row, this);

        mRecyclerView.setAdapter(cityAdapter);
    }

    // Check if there is internet connection
    private boolean isInternetAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnectedOrConnecting();
    }


}