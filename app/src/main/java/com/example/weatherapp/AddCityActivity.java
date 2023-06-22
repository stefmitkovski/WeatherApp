package com.example.weatherapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import java.util.ArrayList;

public class AddCityActivity extends AppCompatActivity {

    private static final String DATABASE_NAME = "cities.db";
    public static final String DATABASE_PATH = "/data/data/com.example.weatherapp/databases/";
    private static final String TAG = "SEARCH CITY";
    private static ArrayList<String> cities;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_city);

        // Search bar
        SearchView searchView = findViewById(R.id.search_view);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                performSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                performSearch(newText);
                return true;
            }
        });

        // Back button
        ImageButton back = findViewById(R.id.back_button);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(),MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putStringArrayListExtra("cities",cities);
                v.getContext().startActivity(intent);
            }
        });
    }

    private void performSearch(String query) {
        SQLiteDatabase database = openDatabase();

        // ArrayList
        ArrayList<String> searchList = new ArrayList<String>();

        ArrayList<String> cityArrayList = getIntent().getStringArrayListExtra("cities");

        // Recycle view
        RecyclerView mRecyclerView = findViewById(R.id.list_view);

        mRecyclerView.setHasFixedSize(true);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mRecyclerView.setItemAnimator(new DefaultItemAnimator());

        Cursor cursor = database.rawQuery("SELECT * FROM worldcities_Sheet1 WHERE city LIKE '%" + query + "%'", null);

        int columnIndex = cursor.getColumnIndex("city");
        if (columnIndex >= 0) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                String city = cursor.getString(columnIndex);
                searchList.add(city);
                cursor.moveToNext();
            }
        }

        SearchAdapter searchAdapter = new SearchAdapter(searchList, R.layout.search_row,this, cityArrayList);

        mRecyclerView.setAdapter(searchAdapter);

        cities = (ArrayList<String>) searchAdapter.getCitiesList();

        closeDatabase(database);
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