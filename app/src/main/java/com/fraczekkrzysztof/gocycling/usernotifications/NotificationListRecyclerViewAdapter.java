package com.fraczekkrzysztof.gocycling.usernotifications;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.fraczekkrzysztof.gocycling.R;
import com.fraczekkrzysztof.gocycling.apiutils.ApiUtils;
import com.fraczekkrzysztof.gocycling.event.EventListActivity;
import com.fraczekkrzysztof.gocycling.eventdetails.EventDetailActivity;
import com.fraczekkrzysztof.gocycling.model.EventModel;
import com.fraczekkrzysztof.gocycling.model.NotificationModel;
import com.fraczekkrzysztof.gocycling.myconfirmations.MyConfirmationsLists;
import com.fraczekkrzysztof.gocycling.utils.DateUtils;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.TextHttpResponseHandler;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.Header;

public class NotificationListRecyclerViewAdapter extends RecyclerView.Adapter<NotificationListRecyclerViewAdapter.ViewHolder>{

    private static final String TAG = "NotificationRVA";

    private List<NotificationModel> mNotificationList = new ArrayList<>();
    private Context mContext;
    private AlertDialog mDialog;


    public void addNotification(List<NotificationModel> notificationList) {
        mNotificationList.addAll(notificationList);
        notifyDataSetChanged();
    }

    public void clearNotification(){
        mNotificationList.clear();
    }

    public NotificationListRecyclerViewAdapter(Context context) {
        mContext = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.notification_list_item,parent,false);
        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        Log.d(TAG, "onBindViewHolder: called");
        holder.textDate.setText(DateUtils.sdfWithTime.format( mNotificationList.get(position).getCreated()));
        holder.textTitle.setText(mNotificationList.get(position).getTitle());

        if (!mNotificationList.get(position).isRead()){
            holder.textDate.setTypeface(null, Typeface.BOLD);
            holder.textTitle.setTypeface(null,Typeface.BOLD);
        } else {
            holder.textDate.setTypeface(null, Typeface.NORMAL);
            holder.textTitle.setTypeface(null,Typeface.NORMAL);
        }


        holder.parentLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    Log.d(TAG, "onClick: clicked on " + mNotificationList.get(position).getId());
                    if (!mNotificationList.get(position).isRead()){
                        Log.d(TAG, "onClick: " + position);
                        markAsReadOnList(position);
                        markAsRead(mNotificationList.get(position).getId());
                    }
                    buildAndShowDialog(position);
            }
        });

    }

    private void buildAndShowDialog(final int position){

        DialogInterface.OnClickListener positiveAnswerListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Log.d(TAG, "onClick: redirect to confirmation ");
                getEventDetailsAndStartActivity(mNotificationList.get(position).getId());
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext).setMessage(mNotificationList.get(position).getContent());
        builder.setPositiveButton(R.string.check_details, positiveAnswerListener);
        builder.setNegativeButton(R.string.close,null);

        mDialog = builder.create();
        mDialog.show();

    }

    private void markAsReadOnList(int position){
        mNotificationList.get(position).setRead(true);
        notifyItemChanged(position);
    }

    @Override
    public int getItemCount() {
        return mNotificationList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        TextView textDate;
        TextView textTitle;
        ConstraintLayout parentLayout;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textDate = itemView.findViewById(R.id.notification_item_time);
            textTitle = itemView.findViewById(R.id.notification_item_title);
            parentLayout = itemView.findViewById(R.id.single_row_notification_layout);
        }
    }

    private void markAsRead(final long id){
        Log.d(TAG, "getNotifications: called");
        AsyncHttpClient client = new AsyncHttpClient();
        client.setBasicAuth(mContext.getResources().getString(R.string.api_user),mContext.getResources().getString(R.string.api_password));
        String requestAddress = mContext.getResources().getString(R.string.api_base_address) + mContext.getResources().getString(R.string.api_notification_mark_as_read);
        requestAddress = requestAddress + ApiUtils.PARAMS_START + "notificationId=" + id;
        Log.d(TAG, "getNotifications: created request" + requestAddress);
        client.put(requestAddress, new TextHttpResponseHandler() {
                    @Override
                    public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                        Log.e(TAG, "onFailure: Error druring marking notification as read",throwable);
                        if (responseString !=null){
                            Log.e(TAG, responseString);
                        }
                    }

                    @Override
                    public void onSuccess(int statusCode, Header[] headers, String responseString) {
                        Log.d(TAG, "onSuccess: Successfully mark notification as read");
                    }
                });
    }

    private void getEventDetailsAndStartActivity(long id){
        setParentRefreshing(true);
        Log.d(TAG, "getSingleEvent: called");
        AsyncHttpClient client = new AsyncHttpClient();
        client.setBasicAuth(mContext.getResources().getString(R.string.api_user),mContext.getResources().getString(R.string.api_password));
        String requestAddress = mContext.getResources().getString(R.string.api_base_address) + mContext.getResources().getString(R.string.api_event_by_notification_id);
        requestAddress = requestAddress + ApiUtils.PARAMS_START + "notificationId=" + id;
        Log.d(TAG, "getSingleEvent: created request " + requestAddress);
        client.get(requestAddress, new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);
                Log.d(TAG, "onSuccess: response successfully received");
                List<EventModel> listEvents = EventModel.fromJson(response);
                setParentRefreshing(false);
                startActivityForSingleEvent(listEvents.get(0));
            }
            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                super.onFailure(statusCode, headers, throwable, errorResponse);
                Toast.makeText(mContext,"There is an error. Please try again!",Toast.LENGTH_SHORT).show();
                Log.e(TAG,"Error during retrieving single event", throwable);
                if (errorResponse != null){
                    Log.d(TAG, errorResponse.toString());
                }

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                super.onFailure(statusCode,headers,responseString,throwable);
                Toast.makeText(mContext,"There is an error. Please try again!",Toast.LENGTH_SHORT).show();
                Log.e(TAG,"Error during retrieving single event", throwable);
                if (responseString != null){
                    Log.d(TAG, responseString);
                }
                setParentRefreshing(false);
            }
        });
    }

    private void startActivityForSingleEvent(EventModel event){
        if (event.isCanceled()){
            Toast.makeText(mContext,"Event is canceled. You're not able to view details",Toast.LENGTH_SHORT).show();
            return;
        }
        Intent eventDetails = new Intent(mContext, EventDetailActivity.class);
        eventDetails.putExtra("Event", event);
        mContext.startActivity(eventDetails);
    }

    private void setParentRefreshing(boolean refreshing){
        ((NotificationLists)mContext).setRefreshing(refreshing);
    }

}
