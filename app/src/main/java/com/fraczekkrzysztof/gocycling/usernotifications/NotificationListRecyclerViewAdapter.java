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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.fraczekkrzysztof.gocycling.R;
import com.fraczekkrzysztof.gocycling.eventdetails.EventDetailActivity;
import com.fraczekkrzysztof.gocycling.httpclient.GoCyclingHttpClientHelper;
import com.fraczekkrzysztof.gocycling.model.v2.notificatication.NotificationDto;
import com.fraczekkrzysztof.gocycling.utils.DateUtils;
import com.google.firebase.auth.FirebaseAuth;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import lombok.SneakyThrows;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NotificationListRecyclerViewAdapter extends RecyclerView.Adapter<NotificationListRecyclerViewAdapter.ViewHolder> {

    private static final String TAG = "NotificationRVA";

    private List<NotificationDto> mNotificationList = new ArrayList<>();
    private Context mContext;
    private AlertDialog mDialog;


    public void addNotification(List<NotificationDto> notificationList) {
        mNotificationList.addAll(notificationList);
        notifyDataSetChanged();
    }

    public void clearNotification() {
        mNotificationList.clear();
    }

    public NotificationListRecyclerViewAdapter(Context context) {
        mContext = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.notification_list_item, parent, false);
        return new ViewHolder(view);
    }

    @SneakyThrows
    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        Log.d(TAG, "onBindViewHolder: called");
        holder.textDate.setText(DateUtils.formatDefaultDateToDateWithTime(mNotificationList.get(position).getCreated()));
        holder.textTitle.setText(mNotificationList.get(position).getTitle());

        if (!mNotificationList.get(position).isRead()) {
            holder.textDate.setTypeface(null, Typeface.BOLD);
            holder.textTitle.setTypeface(null, Typeface.BOLD);
        } else {
            holder.textDate.setTypeface(null, Typeface.NORMAL);
            holder.textTitle.setTypeface(null, Typeface.NORMAL);
        }


        holder.parentLayout.setOnClickListener(view -> {
            Log.d(TAG, "onClick: clicked on " + mNotificationList.get(position).getId());
            if (!mNotificationList.get(position).isRead()) {
                Log.d(TAG, "onClick: " + position);
                markAsReadOnList(position);
                markAsRead(mNotificationList.get(position).getId());
            }
            buildAndShowDialog(position);

        });
    }

    private void buildAndShowDialog(final int position) {

        DialogInterface.OnClickListener positiveAnswerListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Log.d(TAG, "onClick: redirect to confirmation ");
                startActivityForSingleEvent(mNotificationList.get(position).getEventId(), mNotificationList.get(position).getClubId());
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext).setMessage(mNotificationList.get(position).getContent());
        builder.setPositiveButton(R.string.check_details, positiveAnswerListener);
        builder.setNegativeButton(R.string.close, null);

        mDialog = builder.create();
        mDialog.show();

    }

    private void markAsReadOnList(int position) {
        mNotificationList.get(position).setRead(true);
        notifyItemChanged(position);
    }

    @Override
    public int getItemCount() {
        return mNotificationList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

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

    private void markAsRead(final long id) {
        Log.d(TAG, "getNotifications: called");
        Request request = prepareRequestForMarkAsRead(id);
        OkHttpClient httpClient = GoCyclingHttpClientHelper.getInstance(mContext.getResources());
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.e(TAG, "marksAsRead onFailure: error occurred during marking notification as read", e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d(TAG, "markAsRead onResponse: successfully marks notification as read");
                    return;
                }
                Log.w(TAG, String.format("markAsRead onResponse: received response but %d status", response.code()));
            }
        });
    }

    private Request prepareRequestForMarkAsRead(long id) {
        String requestAddress = mContext.getResources().getString(R.string.api_base_address) +
                String.format(mContext.getResources().getString(R.string.api_notification_mark_as_read), FirebaseAuth.getInstance().getCurrentUser(), id);
        return new Request.Builder()
                .url(requestAddress)
                .patch(RequestBody.create("", MediaType.parse("application/json; charset=utf-8")))
                .build();
    }


    private void startActivityForSingleEvent(long eventId, long clubId) {
        Intent eventDetails = new Intent(mContext, EventDetailActivity.class);
        eventDetails.putExtra("clubid", clubId);
        eventDetails.putExtra("eventId", eventId);
        mContext.startActivity(eventDetails);
    }

}
