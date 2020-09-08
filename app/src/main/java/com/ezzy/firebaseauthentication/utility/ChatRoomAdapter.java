package com.ezzy.firebaseauthentication.utility;

import android.app.Dialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ezzy.firebaseauthentication.ChatActivity;
import com.ezzy.firebaseauthentication.R;
import com.ezzy.firebaseauthentication.models.Chatroom;
import com.ezzy.firebaseauthentication.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.nostra13.universalimageloader.core.ImageLoader;

import org.w3c.dom.Text;

import java.util.List;

public class ChatRoomAdapter extends ArrayAdapter<Chatroom> {

    private static final String TAG = "ChatRoomAdapter";

    private int mLayoutResource;
    private Context mContext;
    private LayoutInflater mInflater;

    public ChatRoomAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull List<Chatroom> objects){
        super(context, resource, objects);
        mContext = context;
        mLayoutResource = resource;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public static class ViewHolder{
        TextView name, creatorName, numberMessages;
        ImageView mProfileImage, mTrash;
        Button leaveChat;
        RelativeLayout layoutContainer;
    }

    @NonNull
    @Override
    public View getView(final int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        final ViewHolder holder;

        if (convertView == null){
            convertView = mInflater.inflate(mLayoutResource, parent, false);
            holder = new ViewHolder();

            holder.name = (TextView) convertView.findViewById(R.id.name);
            holder.creatorName = (TextView) convertView.findViewById(R.id.creator_name);
            holder.numberMessages = (TextView) convertView.findViewById(R.id.number_chat_messages);
            holder.mProfileImage = (ImageView) convertView.findViewById(R.id.profile_image);
            holder.mTrash = (ImageView) convertView.findViewById(R.id.icon_trash);
            holder.leaveChat = (Button) convertView.findViewById(R.id.leave_chat);
            holder.layoutContainer = (RelativeLayout) convertView.findViewById(R.id.layout_container);
        }else {
            holder = (ViewHolder) convertView.getTag();
        }

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        try{
            holder.name.setText(getItem(position).getChatroom_name());
            String chatMessages = String.valueOf(getItem(position).getChatroom_messages().size()) + " messages";
            holder.numberMessages.setText(chatMessages);;

            Query query = reference.child(mContext.getResources().getString(R.string.dbnode_users))
                    .orderByKey()
                    .equalTo(getItem(position).getCreator_id());

            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot singleSnapshot: snapshot.getChildren()){
                        Log.d(TAG, "onDataChange: ChatRoom creator: " + singleSnapshot.getValue(User.class).getName());
                        String createdBy = "created by " + singleSnapshot.getValue(User.class).getName();
                        holder.creatorName.setText(createdBy);
                        ImageLoader.getInstance().displayImage(singleSnapshot.getValue(User.class).getProfile_image(), holder.mProfileImage);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });

            holder.mTrash.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getItem(position).getCreator_id().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())){
                        Log.d(TAG, "onClick: asking for permission to delete chat");
                        ((ChatActivity)mContext).showDeleteChatroomDialog(getItem(position).getChatroom_id());
                    }else {
                        makeToast("You didn't create this chatroom");
                    }
                }
            });

            holder.layoutContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "onClick: navigating to chatroom");
                    ((ChatActivity)mContext).joinChatroom(getItem(position));
                }
            });

            List<String> usersInChatRoom = getItem(position).getUsers();
            if (usersInChatRoom.contains(FirebaseAuth.getInstance().getCurrentUser().getUid())){
                holder.leaveChat.setVisibility(View.VISIBLE);

                holder.leaveChat.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d(TAG, "onClick: leaving chatroom with id: " + getItem(position).getChatroom_id());
                        DatabaseReference reference1 = FirebaseDatabase.getInstance().getReference();
                        reference1.child(mContext.getResources().getString(R.string.dbnode_chatrooms))
                                .child(getItem(position).getChatroom_id())
                                .child(mContext.getString(R.string.field_users))
                                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                .removeValue();

                        holder.leaveChat.setVisibility(View.GONE);
                    }
                });
            }
        }catch (NullPointerException e){
            Log.e(TAG, "getView: ", e.getCause());
        }
        return convertView;
    }

    public void makeToast(String message){
        Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
    }
}