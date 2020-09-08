package com.ezzy.firebaseauthentication;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.ezzy.firebaseauthentication.models.ChatMessage;
import com.ezzy.firebaseauthentication.models.Chatroom;
import com.ezzy.firebaseauthentication.models.User;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class NewChatroomDialog extends DialogFragment {

    private static final String TAG = "NewChatroomDialog";

    private SeekBar mSeekBar;
    private TextInputEditText mChatroomName;
    private TextView mCreateChatroom, mSecurityLevel;

    private int mUserSecurityLevel;
    private int mSeekProgress;
    private Context mContext;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_new_chatroom, container, false);

        mChatroomName = view.findViewById(R.id.input_chatroom_name);
        mSeekBar = view.findViewById(R.id.input_security_level);
        mCreateChatroom = view.findViewById(R.id.createChatRoom);
        mSecurityLevel = view.findViewById(R.id.security_level);
        mSeekProgress = 0;
        mSecurityLevel.setText(String.valueOf(mSeekProgress));
        getUserSecurityLevel();

        mCreateChatroom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isEmpty(mChatroomName.getTextColors().toString())){
                    Log.d(TAG, "onClick: creating new chat room");

                    if (mUserSecurityLevel >= mSeekBar.getProgress()){
                        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();

                        String chatroomId = reference.child(mContext.getResources().getString(R.string.dbnode_chatrooms))
                                .push().getKey();

                        Chatroom chatroom = new Chatroom();
                        chatroom.setSecurity_level(String.valueOf(mSeekBar.getProgress()));
                        chatroom.setChatroom_name(mChatroomName.getText().toString());
                        chatroom.setCreator_id(FirebaseAuth.getInstance().getCurrentUser().getUid());
                        chatroom.setChatroom_id(chatroomId);

                        //insert the new chatroom to the database
                        reference.child(mContext.getResources().getString(R.string.dbnode_chatrooms))
                                .child(chatroomId)
                                .setValue(chatroom);

                        //create a unique id for message
                        String messageId = reference
                                .child(mContext.getResources().getString(R.string.dbnode_chatrooms))
                                .push().getKey();

                        //Insert first message into the chatroom
                        ChatMessage message = new ChatMessage();

                        message.setMessage("Welcome to the new chatroom");
                        message.setTimestamp(getTimestamp());
                        reference.child(mContext.getResources().getString(R.string.dbnode_chatrooms))
                                .child(chatroomId)
                                .child(mContext.getResources().getString(R.string.field_chatroom_messages))
                                .child(messageId)
                                .setValue(message);

                        ((ChatActivity)getActivity()).getChatrooms();
                        getDialog().dismiss();
                    }else {
                        makeToast("Security level cannot be 0");
                    }
                }
            }
        });

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mSeekProgress = progress;
                mSecurityLevel.setText(String.valueOf(mSeekProgress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        return view;
    }

    private void getUserSecurityLevel() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();

        Query query = reference.child(mContext.getResources().getString(R.string.dbnode_users))
                .orderByKey()
                .equalTo(FirebaseAuth.getInstance().getCurrentUser().getUid());
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot singleSnapshot : snapshot.getChildren()){
                    Log.d(TAG, "onDataChange: user security level: " + singleSnapshot.getValue(User.class).getSecurity_level());
                    mUserSecurityLevel = Integer.parseInt(String.valueOf(singleSnapshot.getValue(User.class).getSecurity_level()));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private String getTimestamp(){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Canada/Pacific"));
        return simpleDateFormat.format(new Date());
    }

    private boolean isEmpty(String s){
        return s.equals("");
    }

    private void makeToast(String message){
        Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
    }
}
