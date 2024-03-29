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
import com.fraczekkrzysztof.gocycling.model.ClubModel;

import java.util.ArrayList;
import java.util.List;

public class ClubAdapter extends ArrayAdapter<ClubModel> {

    private final LayoutInflater mInflater;
    private Context mContext;
    private List<ClubModel> clubsList = new ArrayList<>();
    private final int mResource;

    public ClubAdapter(@NonNull Context context, int resource, @NonNull List<ClubModel> objects) {
        super(context, resource, objects);
        mInflater = LayoutInflater.from(context);
        mContext = context;
        clubsList = objects;
        mResource = resource;
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView,
                                @NonNull ViewGroup parent) {
        return createItemView(position, convertView, parent);
    }

    @Override
    public @NonNull View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createItemView(position, convertView, parent);
    }

    private View createItemView(int position, View convertView, ViewGroup parent){
        final View view = mInflater.inflate(mResource, parent, false);
        TextView clubName = view.findViewById(R.id.club_list_item_name);
        ClubModel currentClub = clubsList.get(position);
        clubName.setText(currentClub.getName());
        return view;
    }

}
