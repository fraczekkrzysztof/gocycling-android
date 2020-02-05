package com.fraczekkrzysztof.gocycling.myconfirmations;

import android.content.Context;
import android.content.DialogInterface;
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
import com.fraczekkrzysztof.gocycling.apiutils.ApiUtils;
import com.fraczekkrzysztof.gocycling.model.EventModel;
import com.fraczekkrzysztof.gocycling.eventdetails.EventDetailActivity;
import com.fraczekkrzysztof.gocycling.utils.DateUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.Header;

public class MyConfirmationListRecyclerViewAdapter extends RecyclerView.Adapter<MyConfirmationListRecyclerViewAdapter.ViewHolder>{

    private static final String TAG = "MyConfirmationListRecyc";

    private List<EventModel> mEventList = new ArrayList<>();
    private Context mContext;
    private AlertDialog mDialog;
    private int toDelete;


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

        DialogInterface.OnClickListener positiveAnswerListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Log.d(TAG, "onClick: confirmed deleting " + toDelete);
                deleteConfirmation(FirebaseAuth.getInstance().getCurrentUser().getUid(),mEventList.get(toDelete));
            }
        };

        DialogInterface.OnClickListener negativeAnswerListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Log.d(TAG, "onClick: deleting canceled");
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext).setMessage(R.string.confirmation_delete_question);
        builder.setPositiveButton(R.string.ok, positiveAnswerListener);
        builder.setNegativeButton(R.string.cancel, negativeAnswerListener);

        mDialog = builder.create();
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
                toDelete = position;
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


    void deleteConfirmation(String userUid, final EventModel event){
        Log.d(TAG, "deleteConfirmation: called");
        AsyncHttpClient client = new AsyncHttpClient();
        client.setBasicAuth(mContext.getResources().getString(R.string.api_user),mContext.getResources().getString(R.string.api_password));
        String requestAddress = mContext.getResources().getString(R.string.api_base_address) + mContext.getResources().getString(R.string.api_delete_confirmation_by_user_event);
        requestAddress = requestAddress + ApiUtils.PARAMS_START + ApiUtils.USER_UID + userUid;
        requestAddress = requestAddress + ApiUtils.PARAMS_AND  + ApiUtils.EventId + event.getId();
        Log.d(TAG, "deleteConfirmation: created request " + requestAddress);
        client.delete(requestAddress, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                Log.d(TAG, "onSuccess: Successfully removed confirmation");
                mEventList.remove(event);
                notifyDataSetChanged();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Log.e(TAG, "onFailure: error during deleting confirmation " +responseBody.toString() , error);

            }
        });
    }
}
