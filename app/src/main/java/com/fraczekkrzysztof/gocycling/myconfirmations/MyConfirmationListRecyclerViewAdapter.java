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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.fraczekkrzysztof.gocycling.R;
import com.fraczekkrzysztof.gocycling.eventdetails.EventDetailActivity;
import com.fraczekkrzysztof.gocycling.httpclient.GoCyclingHttpClientHelper;
import com.fraczekkrzysztof.gocycling.model.v2.event.EventDto;
import com.fraczekkrzysztof.gocycling.utils.DateUtils;
import com.fraczekkrzysztof.gocycling.utils.ToastUtils;
import com.google.firebase.auth.FirebaseAuth;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import lombok.SneakyThrows;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MyConfirmationListRecyclerViewAdapter extends RecyclerView.Adapter<MyConfirmationListRecyclerViewAdapter.ViewHolder>{

    private static final String TAG = "MyConfirmationListRecyc";

    private List<EventDto> mEventList = new ArrayList<>();
    private Context mContext;
    private AlertDialog mDialog;
    private int toDelete;


    public void addEvents(List<EventDto> eventList) {
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
        return new ViewHolder(view);
    }

    @SneakyThrows
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        Log.d(TAG, "onBindViewHolder: called");
        holder.textDate.setText(DateUtils.formatDefaultDateToDateWithTime(mEventList.get(position).getDateAndTime()));
        holder.textTitle.setText(mEventList.get(position).getName());

        if (mEventList.get(position).isCanceled()){
            holder.textDate.setTextColor(mContext.getResources().getColor(R.color.hint));
            holder.textTitle.setTextColor(mContext.getResources().getColor(R.color.hint));
        }

        DialogInterface.OnClickListener positiveAnswerListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Log.d(TAG, "onClick: confirmed deleting " + toDelete);
                deleteConfirmation(mEventList.get(toDelete));
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

        holder.parentLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mEventList.get(position).isCanceled()) {
                    Toast.makeText(mContext, "This event is canceled", Toast.LENGTH_SHORT).show();
                } else{
                    Log.d(TAG, "onClick: clicked on " + mEventList.get(position));
                    Intent newCityIntent = new Intent(mContext, EventDetailActivity.class);
                    newCityIntent.putExtra("eventId", mEventList.get(position).getId());
                    newCityIntent.putExtra("clubId", mEventList.get(position).getClubId());
                    mContext.startActivity(newCityIntent);
                }
            }
        });

        holder.mDeleteButton.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mEventList.get(position).isCanceled()) {
                    Toast.makeText(mContext,"This event is canceled",Toast.LENGTH_SHORT).show();
                } else {
                    Log.d(TAG, "onClick: deleting confirmation " + position);
                    toDelete = position;
                    mDialog.show();
                }
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


    void deleteConfirmation(final EventDto event) {
        Log.d(TAG, "deleteConfirmation: called");
        setParentRefreshing(true);
        Request request = prepareRequest(event.getClubId(), event.getId());
        OkHttpClient httpClient = GoCyclingHttpClientHelper.getInstance(mContext.getResources());
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.e(TAG, "Confirmation clicked onFailure: error during confirmation", e);
                ToastUtils.backgroundThreadShortToast(mContext, "Error occurred. Try again!");
                setParentRefreshing(false);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Confirmation clicked onResponse: Successfully perform confirmation operation");
                    notifyParentAboutDataSetChange();
                    ToastUtils.backgroundThreadShortToast(mContext, "Successfully performed operation!");
                    return;
                }
                Log.w(TAG, String.format("Confirmation clicked onResponse: received response but %d status", response.code()));
                ToastUtils.backgroundThreadShortToast(mContext, "Error occurred. Try again!");
            }
        });
    }

    private Request prepareRequest(long clubId, long eventId) {
        String requestAddress = mContext.getResources().getString(R.string.api_base_address) +
                String.format(mContext.getResources().getString(R.string.api_event_address_confirmation), clubId, eventId);
        HttpUrl url = HttpUrl.parse(requestAddress).newBuilder()
                .addQueryParameter("userUid", FirebaseAuth.getInstance().getCurrentUser().getUid())
                .build();
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .delete();
        return requestBuilder.build();
    }

    private void setParentRefreshing(boolean refreshing) {
        ((MyConfirmationsLists) mContext).setRefreshing(refreshing);
    }

    private void notifyParentAboutDataSetChange() {
        ((MyConfirmationsLists) mContext).notifyAboutDataSetChange();
    }
}
