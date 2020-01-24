package com.fraczekkrzysztof.gocycling.myconfirmations;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.fraczekkrzysztof.gocycling.R;
import com.fraczekkrzysztof.gocycling.event.EventModel;
import com.fraczekkrzysztof.gocycling.eventdetails.EventDetailActivity;
import com.fraczekkrzysztof.gocycling.utils.DateUtils;

import java.util.ArrayList;
import java.util.List;

public class MyConfirmationListRecyclerViewAdapter extends RecyclerView.Adapter<MyConfirmationListRecyclerViewAdapter.ViewHolder>{

    private static final String TAG = "MyConfirmationListRecyc";

    private List<EventModel> mEventList = new ArrayList<>();
    private Context mContext;
    private AlertDialog mDialog;



    public void addEvents(List<EventModel> eventList) {
        mEventList.addAll(eventList);
        notifyDataSetChanged();
    }

    public void clearEvents(){
        mEventList.clear();
    }

    public MyConfirmationListRecyclerViewAdapter(Context context) {
        mContext = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_my_confirmations_item,parent,false);
        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        Log.d(TAG, "onBindViewHolder: called");
        holder.textDate.setText(DateUtils.sdfWithTime.format( mEventList.get(position).getDateAndTime()));
        holder.textTitle.setText(mEventList.get(position).getName());

        mDialog = new AlertDialog.Builder(mContext).setMessage("123").create();
        //TODO finish creating dialog and deleting confirmartion

        holder.parentLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: clicked on " + mEventList.get(position));
                Intent newCityIntent = new Intent(mContext, EventDetailActivity.class);
                newCityIntent.putExtra("Event",mEventList.get(position));
                mContext.startActivity(newCityIntent);
            }
        });

        holder.mDeleteButton.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: deleting confirmation " + position);
                mDialog.show();
            }
        }));
    }

    @Override
    public int getItemCount() {
        return mEventList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        TextView textDate;
        TextView textTitle;
        ImageButton mDeleteButton;
        ConstraintLayout parentLayout;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textDate = itemView.findViewById(R.id.event_list_confirmation_time);
            textTitle = itemView.findViewById(R.id.event_list_confirmation_title);
            mDeleteButton = itemView.findViewById(R.id.event_list_confirmation_delete);
            parentLayout = itemView.findViewById(R.id.single_row_confirmation_layout);
        }
    }
}
