package com.example.weatherapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.ViewHolder>{

    private List<String> searchCities;
    private ArrayList<String> cities;
    private int rowLayout;
    private Context mContext;

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView city_name;

        public ViewHolder(View itemView) {
            super(itemView);
            city_name = (TextView) itemView.findViewById(R.id.search_city_name);
        }
    }

    public SearchAdapter(List<String> searchCities, int rowLayout, Context context, ArrayList<String> cities){
        this.searchCities = searchCities;
        this.rowLayout = rowLayout;
        this.mContext = context;
        this.cities = cities;
    }

    @NonNull
    @Override
    public SearchAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(rowLayout, parent, false);
        return new SearchAdapter.ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String name = searchCities.get(position);
        holder.city_name.setText(name);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(cities.isEmpty() || !cities.contains(name)){
                    cities.add(name);
                    Toast.makeText(mContext, "Added: " + name, Toast.LENGTH_SHORT).show();
                }else {
                    cities.remove(name);
                    Toast.makeText(mContext, "Removed: " + name, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public List<String> getCitiesList() {
        return cities;
    }
    @Override
    public int getItemCount() {
        return searchCities == null ? 0 : searchCities.size();
    }
}
