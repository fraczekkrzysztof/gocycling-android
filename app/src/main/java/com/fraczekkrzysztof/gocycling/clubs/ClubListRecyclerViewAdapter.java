package com.fraczekkrzysztof.gocycling.clubs;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.fraczekkrzysztof.gocycling.R;
import com.fraczekkrzysztof.gocycling.clubdetails.ClubDetailActivity;
import com.fraczekkrzysztof.gocycling.model.v2.club.ClubDto;

import java.util.ArrayList;
import java.util.List;

public class ClubListRecyclerViewAdapter extends RecyclerView.Adapter<ClubListRecyclerViewAdapter.ViewHolder>{

    private static final String TAG = "ClubRecycleAdapter";

    private List<ClubDto> mClubList = new ArrayList<>();
    private Context mContext;


    public void addClubs(List<ClubDto> clubList) {
        mClubList.addAll(clubList);
        notifyDataSetChanged();
    }

    public void clearClubs(){
        mClubList.clear();
    }

    public ClubListRecyclerViewAdapter(Context context) {
        mContext = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_clubs_item,parent,false);
        return new ViewHolder(view);

    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        Log.d(TAG, "onBindViewHolder: called");
        holder.textName.setText(mClubList.get(position).getName());
        holder.textLocation.setText(mClubList.get(position).getLocation());
        holder.parentLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(mContext, ClubDetailActivity.class);
                intent.putExtra("Club",mClubList.get(position));
                mContext.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mClubList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        TextView textName;
        TextView textLocation;
        ConstraintLayout parentLayout;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.club_list_name_item);
            textLocation = itemView.findViewById(R.id.club_list_location_item);
            parentLayout = itemView.findViewById(R.id.single_row_club_item);
        }
    }
}
