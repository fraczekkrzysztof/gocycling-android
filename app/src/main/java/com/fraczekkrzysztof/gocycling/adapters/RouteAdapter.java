package com.fraczekkrzysztof.gocycling.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fraczekkrzysztof.gocycling.R;
import com.fraczekkrzysztof.gocycling.model.v2.route.RouteDto;

import java.util.ArrayList;
import java.util.List;

public class RouteAdapter extends ArrayAdapter<RouteDto> {

    private Context mContext;
    private List<RouteDto> routesList = new ArrayList<>();

    public RouteAdapter(@NonNull Context context, List<RouteDto> list) {
        super(context,0,list);
        mContext = context;
        routesList = list;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItem = convertView;
        if(listItem == null)
            listItem = LayoutInflater.from(mContext).inflate(R.layout.route_list_item,parent,false);

        RouteDto currentRoute = routesList.get(position);

        TextView appType = listItem.findViewById(R.id.route_list_source);
        appType.setText(currentRoute.getAppType().name());
        TextView name = listItem.findViewById(R.id.route_list_name);
        name.setText(currentRoute.getName());
        TextView length = listItem.findViewById(R.id.route_list_length);
        length.setText(currentRoute.getLength());
        TextView elevation = listItem.findViewById(R.id.route_list_elevation);
        elevation.setText(currentRoute.getElevation());

        return listItem;


    }
}
