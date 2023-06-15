package com.example.weatherapp;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class CityAdapter extends RecyclerView.Adapter<CityAdapter.ViewHolder>{
    private List<String> cities;
    private int rowLayout;
    private Context mContext;

    private String[] data;
    public class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView icon;
        public TextView city_name;
        public TextView city_weather;
        public TextView city_temperature;
        public TextView city_last_updated;

        public ViewHolder(View itemView) {
            super(itemView);
            icon = (ImageView) itemView.findViewById(R.id.weather_icon);
            city_name = (TextView) itemView.findViewById(R.id.city_name);
            city_weather = (TextView) itemView.findViewById(R.id.city_weather);
            city_temperature = (TextView) itemView.findViewById(R.id.city_temperature);
            city_last_updated = (TextView) itemView.findViewById(R.id.city_update);
        }
    }

    public CityAdapter(List<String> cities, int rowLayout, Context context, String[] data){
        this.cities = cities;
        this.rowLayout = rowLayout;
        this.mContext = context;
        this.data = data;
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


        try {
            JSONObject jsonObject = new JSONObject(data[position]);
            holder.city_temperature.setText(jsonObject.getString("temperature")+"°");

            // Time
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
            Date date = null;
            date = inputFormat.parse(jsonObject.getString("time"));
            SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm");
            String timeString = outputFormat.format(date);
            holder.city_last_updated.setText("Last updated on: " + timeString);
            switch (jsonObject.getInt("weathercode")){
                case 2:
                    holder.icon.setImageResource(R.drawable.baseline_cloudy);
                    holder.city_weather.setText("Cloudy");
                    break;
                case 3:
                    holder.icon.setImageResource(R.drawable.baseline_rainy);
                    holder.city_weather.setText("Rainy");
                    break;
                case 4:
                    holder.icon.setImageResource(R.drawable.baseline_thunderstorm);
                    holder.city_weather.setText("Thunderstorm");
                    break;
                case 5:
                    holder.icon.setImageResource(R.drawable.baseline_snow);
                    holder.city_weather.setText("Snowing");
                    break;
                default:
                    if(jsonObject.getInt("is_day") == 1){
                        holder.icon.setImageResource(R.drawable.baseline_sunny);
                        holder.city_weather.setText("Sunny");
                    }else {
                        holder.icon.setImageResource(R.drawable.baseline_night);
                        holder.city_weather.setText("Clear Sky");
                    }
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

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
