package com.fraczekkrzysztof.gocycling.myevents;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.fraczekkrzysztof.gocycling.R;
import com.fraczekkrzysztof.gocycling.eventdetails.EventDetailActivity;
import com.fraczekkrzysztof.gocycling.model.EventModel;
import com.fraczekkrzysztof.gocycling.utils.DateUtils;

import java.util.ArrayList;
import java.util.List;

public class MyEventListRecyclerViewAdapter extends RecyclerView.Adapter<MyEventListRecyclerViewAdapter.ViewHolder>{

    private static final String TAG = "EventListRVAdapter";

    private List<EventModel> mEventList = new ArrayList<>();
    private Context mContext;

    public void addEvents(List<EventModel> eventList) {
        mEventList.addAll(eventList);
        notifyDataSetChanged();
    }

    public void clearEvents(){
        mEventList.clear();
    }

    public MyEventListRecyclerViewAdapter(Context context) {
        mContext = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_my_events_item,parent,false);
        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        Log.d(TAG, "onBindViewHolder: called");
        holder.textDate.setText(DateUtils.sdfWithTime.format( mEventList.get(position).getDateAndTime()));
        holder.textTitle.setText(mEventList.get(position).getName());

        if (mEventList.get(position).isCanceled()){
            holder.textDate.setTextColor(mContext.getResources().getColor(R.color.hint));
            holder.textTitle.setTextColor(mContext.getResources().getColor(R.color.hint));
        }

        holder.parentLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mEventList.get(position).isCanceled()) {
                    Toast.makeText(mContext,"This event is calceled",Toast.LENGTH_SHORT).show();
                } else {
                    Log.d(TAG, "onClick: clicked on " + mEventList.get(position));
                    Intent newCityIntent = new Intent(mContext, EventDetailActivity.class);
                    newCityIntent.putExtra("Event", mEventList.get(position));
                    mContext.startActivity(newCityIntent);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mEventList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        TextView textDate;
        TextView textTitle;
        ConstraintLayout parentLayout;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textDate = itemView.findViewById(R.id.myevent_list_time);
            textTitle = itemView.findViewById(R.id.myevent_list_title);
            parentLayout = itemView.findViewById(R.id.single_row_myevents_layout);
        }
    }
}
