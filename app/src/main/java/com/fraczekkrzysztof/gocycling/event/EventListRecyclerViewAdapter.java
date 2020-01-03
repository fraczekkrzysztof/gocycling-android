package com.fraczekkrzysztof.gocycling.event;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fraczekkrzysztof.gocycling.R;

import java.util.List;

public class EventListRecyclerViewAdapter extends RecyclerView.Adapter<EventListRecyclerViewAdapter.ViewHolder>{

    private static final String TAG = "EventListRVAdapter";

    private List<EventModel> mEventList;
    private Context mContext;

    public void setEventList(List<EventModel> eventList) {
        mEventList = eventList;
    }

    public EventListRecyclerViewAdapter(Context context, List<EventModel> eventList) {
        mEventList = eventList;
        mContext = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_listitem,parent,false);
        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        Log.d(TAG, "onBindViewHolder: called");
        holder.text.setText(mEventList.get(position).getName());

        holder.parentLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: clicked on " + mEventList.get(position));
                Toast.makeText(mContext, mEventList.get(position).getName(),Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return mEventList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        TextView text;
        RelativeLayout parentLayout;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.event_text);
            parentLayout = itemView.findViewById(R.id.parent_layout);
        }
    }
}
