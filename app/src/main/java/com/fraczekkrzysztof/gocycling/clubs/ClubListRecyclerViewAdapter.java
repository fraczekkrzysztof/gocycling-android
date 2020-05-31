package com.fraczekkrzysztof.gocycling.clubs;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.fraczekkrzysztof.gocycling.R;
import com.fraczekkrzysztof.gocycling.apiutils.ApiUtils;
import com.fraczekkrzysztof.gocycling.eventdetails.EventDetailActivity;
import com.fraczekkrzysztof.gocycling.model.ClubModel;
import com.fraczekkrzysztof.gocycling.model.EventModel;
import com.fraczekkrzysztof.gocycling.utils.DateUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.Header;

public class ClubListRecyclerViewAdapter extends RecyclerView.Adapter<ClubListRecyclerViewAdapter.ViewHolder>{

    private static final String TAG = "ClubRecycleAdapter";

    private List<ClubModel> mClubList = new ArrayList<>();
    private Context mContext;
    private AlertDialog mDialog;



    public void addClubs(List<ClubModel> clubList) {
        mClubList.addAll(clubList);
        notifyDataSetChanged();
    }

    public void clearEvents(){
        mClubList.clear();
    }

    public ClubListRecyclerViewAdapter(Context context) {
        mContext = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_clubs_item,parent,false);
        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        Log.d(TAG, "onBindViewHolder: called");
        holder.textName.setText(mClubList.get(position).getName());

    }

    @Override
    public int getItemCount() {
        return mClubList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        TextView textName;
        ConstraintLayout parentLayout;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.club_list_name_item);

            parentLayout = itemView.findViewById(R.id.single_row_club_item);
        }
    }
}
