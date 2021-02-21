package com.fraczekkrzysztof.gocycling.conversation;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.fraczekkrzysztof.gocycling.R;
import com.fraczekkrzysztof.gocycling.model.v2.event.ConversationDto;
import com.fraczekkrzysztof.gocycling.utils.DateUtils;

import java.util.ArrayList;
import java.util.List;

import lombok.SneakyThrows;

public class ConversationListRecyclerViewAdapter extends RecyclerView.Adapter<ConversationListRecyclerViewAdapter.ViewHolder>{

    private static final String TAG = "EventListRVAdapter";

    private List<ConversationDto> mConversationList = new ArrayList<>();
    private Context mContext;

    public void addConversation(List<ConversationDto> conversationList) {
        mConversationList.addAll(conversationList);
        notifyDataSetChanged();
    }

    public void addSingleConversation(ConversationDto conversation) {
        mConversationList.add(conversation);
        notifyDataSetChanged();
    }

    public void clearEvents(){
        mConversationList.clear();
    }

    public ConversationListRecyclerViewAdapter(Context context) {
        mContext = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_conversation_item,parent,false);
        return new ViewHolder(view);
    }

    @SneakyThrows
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        Log.d(TAG, "onBindViewHolder: called");
        holder.textAuthor.setText(mConversationList.get(position).getUserName());
        holder.textTime.setText(DateUtils.formatDefaultDateToDateWithTime(mConversationList.get(position).getCreated()));
        holder.textMessage.setText(mConversationList.get(position).getMessage());


    }

    @Override
    public int getItemCount() {
        return mConversationList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        TextView textAuthor;
        TextView textTime;
        TextView textMessage;
        ConstraintLayout parentLayout;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textAuthor = itemView.findViewById(R.id.conversation_list_author);
            textTime = itemView.findViewById(R.id.conversation_list_time);
            textMessage = itemView.findViewById(R.id.conversation_list_message);
            parentLayout = itemView.findViewById(R.id.single_row_conversation_layout);
        }
    }
}

