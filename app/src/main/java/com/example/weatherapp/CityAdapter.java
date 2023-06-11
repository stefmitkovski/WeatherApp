package com.example.weatherapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CityAdapter extends RecyclerView.Adapter<CityAdapter.ViewHolder>{
    private List<String> cities;
    private int rowLayout;
    private Context mContext;
    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView city_name;

        public ViewHolder(View itemView) {
            super(itemView);
            city_name = (TextView) itemView.findViewById(R.id.city_name);
        }
    }

    public CityAdapter(List<String> cities, int rowLayout, Context context){
        this.cities = cities;
        this.rowLayout = rowLayout;
        this.mContext = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(rowLayout, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String entry = cities.get(position);
        holder.city_name.setText(entry);
        holder.city_name.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView tv = (TextView) v;
                Toast.makeText(mContext, tv.getText(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return cities == null ? 0 : cities.size();
    }



}
