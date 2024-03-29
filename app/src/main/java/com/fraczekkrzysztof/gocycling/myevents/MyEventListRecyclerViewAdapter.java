package com.fraczekkrzysztof.gocycling.myevents;

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
import com.fraczekkrzysztof.gocycling.model.EventModel;
import com.fraczekkrzysztof.gocycling.myconfirmations.MyConfirmationsLists;
import com.fraczekkrzysztof.gocycling.newevent.NewEventActivity;
import com.fraczekkrzysztof.gocycling.utils.DateUtils;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.Header;

public class MyEventListRecyclerViewAdapter extends RecyclerView.Adapter<MyEventListRecyclerViewAdapter.ViewHolder>{

    private static final String TAG = "EventListRVAdapter";

    private List<EventModel> mEventList = new ArrayList<>();
    private AlertDialog mDialog;
    private Context mContext;
    private int toDelete;

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
        DialogInterface.OnClickListener positiveAnswerListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Log.d(TAG, "onClick: confirmed canceling " + toDelete);
                cancelEvent(mEventList.get(toDelete).getId());
            }
        };

        DialogInterface.OnClickListener negativeAnswerListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Log.d(TAG, "onClick: canceling canceled");
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext).setMessage(R.string.event_cancel_question);
        builder.setPositiveButton(R.string.ok, positiveAnswerListener);
        builder.setNegativeButton(R.string.cancel, negativeAnswerListener);

        mDialog = builder.create();

        holder.mDeleteButton.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mEventList.get(position).isCanceled()) {
                    Toast.makeText(mContext,"This event is canceled",Toast.LENGTH_SHORT).show();
                } else {
                    Log.d(TAG, "onClick: canceling event " + position);
                    toDelete = position;
                    mDialog.show();
                }
            }
        }));

        holder.mEditButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mEventList.get(position).isCanceled()) {
                    Toast.makeText(mContext,"This event is canceled",Toast.LENGTH_SHORT).show();
                } else {
                    Intent intent = new Intent(mContext, NewEventActivity.class);
                    intent.putExtra("EventToEdit", mEventList.get(position));
                    intent.putExtra("mode","EDIT");
                    mContext.startActivity(intent);
                }
            }
        });

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

    private void cancelEvent(long eventId){
        Log.d(TAG, "cancelEvent: called");
        setParentRefreshing(true);
        AsyncHttpClient client = new AsyncHttpClient();
        client.setBasicAuth(mContext.getResources().getString(R.string.api_user),mContext.getResources().getString(R.string.api_password));
        String requestAddress = mContext.getResources().getString(R.string.api_base_address) + mContext.getResources().getString(R.string.api_cancel_event);
        requestAddress = requestAddress + ApiUtils.PARAMS_START + ApiUtils.EVENT_ID + eventId;
        Log.d(TAG, "cancelEvent: created request " + requestAddress);
        client.put(requestAddress, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                Log.d(TAG, "onSuccess: Successfully removed confirmation");
                if(mContext instanceof MyEventsLists){
                    ((MyEventsLists)mContext).refreshData();
                }
                Toast.makeText(mContext,"Successfully cancel event",Toast.LENGTH_SHORT).show();
                setParentRefreshing(false);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Log.e(TAG, "onFailure: error during deleting confirmation " +responseBody.toString() , error);
                Toast.makeText(mContext,"Error during cancel event",Toast.LENGTH_SHORT).show();
                setParentRefreshing(false);
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
        ImageButton mDeleteButton;
        ImageButton mEditButton;
        ConstraintLayout parentLayout;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textDate = itemView.findViewById(R.id.myevent_list_time);
            textTitle = itemView.findViewById(R.id.myevent_list_title);
            mDeleteButton = itemView.findViewById(R.id.myevent_list_cancel);
            mEditButton = itemView.findViewById(R.id.myevent_list_edit);
            parentLayout = itemView.findViewById(R.id.single_row_myevents_layout);
        }
    }

    private void setParentRefreshing(boolean refreshing){
        if(mContext instanceof MyEventsLists){
            ((MyEventsLists)mContext).setRefreshing(refreshing);
        }
    }


}
