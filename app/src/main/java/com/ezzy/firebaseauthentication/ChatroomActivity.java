package com.ezzy.firebaseauthentication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.ezzy.firebaseauthentication.models.ChatMessage;
import com.ezzy.firebaseauthentication.models.Chatroom;
import com.ezzy.firebaseauthentication.models.User;
import com.ezzy.firebaseauthentication.utility.ChatMessageListAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

public class ChatroomActivity extends AppCompatActivity {

    private static final String TAG = "ChatroomActivity";

    private FirebaseAuth.AuthStateListener mAuthListener;
    private DatabaseReference mMessagesReference;

    private TextView mChatroomName;
    private ListView mListView;
    private EditText mMessage;
    private ImageView mSendMessage;

    private Chatroom mChatroom;
    private List<ChatMessage> mMessageList;
    private Set<String> mMessageIdSet;
    private ChatMessageListAdapter mAdapter;
    public static boolean isActivityRunning;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatroom);

        mChatroomName = findViewById(R.id.text_chatroom_name);
        mListView = findViewById(R.id.listView);
        mMessage = findViewById(R.id.input_message);
        mSendMessage = findViewById(R.id.send);
        getSupportActionBar().hide();
        Log.d(TAG, "onCreate: started");

        setUpFirebaseAuth();
        getChatroom();
        init();
        hideSoftKeyboard();
    }

    private void init() {
        mMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListView.setSelection(mAdapter.getCount() - 1);
            }
        });

        mSendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mMessage.getText().toString().equals("")){
                    String message = mMessage.getText().toString();
                    Log.d(TAG, "onClick: message: " + message);
                    ChatMessage newMessage = new ChatMessage();
                    newMessage.setMessage(message);
                    newMessage.setTimestamp(getTimeStamp());
                    newMessage.setUser_id(FirebaseAuth.getInstance().getCurrentUser().getUid());

                    DatabaseReference reference = FirebaseDatabase.getInstance().getReference()
                            .child(getResources().getString(R.string.dbnode_chatrooms))
                            .child(mChatroom.getChatroom_id())
                            .child(getResources().getString(R.string.field_chatroom_messages));

                            String newMessageId = reference.push().getKey();

                            reference.child(newMessageId)
                                    .setValue(newMessage);

                            mMessage.setText("");

                }
            }
        });
    }

    private String getTimeStamp(){
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("Canada/Pacific"));
        return dateFormat.format(new Date());
    }

    private void getChatroom() {
        Log.d(TAG, "getChatroom: getting chatroom details");
        Intent intent = getIntent();
        if (intent.hasExtra(getResources().getString(R.string.intent_chatroom))){
            Chatroom chatroom = intent.getParcelableExtra(getResources().getString(R.string.intent_chatroom));
            Log.d(TAG, "getChatroom: chatroom" + chatroom.toString());
            mChatroom = chatroom;
            mChatroomName.setText(chatroom.getChatroom_name());
            enableChatroomListener();
        }
    }

    ValueEventListener mValueEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot snapshot) {
            getChatroomMessages();
            int numMessages = (int) snapshot.getChildrenCount();
            updateNumMessages(numMessages);
        }

        @Override
        public void onCancelled(@NonNull DatabaseError error) {

        }
    };

    private void updateNumMessages(int numMessages) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        reference.child(getResources().getString(R.string.dbnode_chatrooms))
                .child(mChatroom.getChatroom_id())
                .child(getResources().getString(R.string.field_users))
                .child(getResources().getString(R.string.field_last_message_seen))
                .setValue(String.valueOf(numMessages));
    }

    private void getChatroomMessages() {
        if (mMessageList == null){
            mMessageList = new ArrayList<>();
            mMessageIdSet = new HashSet<>();
            initMessageList();
        }

        final DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query query = reference.child(getResources().getString(R.string.dbnode_chatrooms))
                .child(mChatroom.getChatroom_id())
                .child(getResources().getString(R.string.field_chatroom_messages));

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dataSnapshot : snapshot.getChildren()){
                    Log.d(TAG, "onDataChange: found chatroom message" + dataSnapshot.getValue());
                    try {
                        ChatMessage message = new ChatMessage();
                        String userId = dataSnapshot.getValue(ChatMessage.class).getUser_id();
                        if (!mMessageIdSet.contains(dataSnapshot.getKey())){
                            Log.d(TAG, "onDataChange: adding new message to the list");
                            mMessageIdSet.add(dataSnapshot.getKey());

                            if (userId != null){
                                message.setMessage(dataSnapshot.getValue(ChatMessage.class).getMessage());
                                message.setUser_id(dataSnapshot.getValue(ChatMessage.class).getUser_id());
                                message.setTimestamp(dataSnapshot.getValue(ChatMessage.class).getTimestamp());
                                message.setProfile_image("");
                                message.setName("");
                                mMessageList.add(message);
                            }else {
                                message.setMessage(dataSnapshot.getValue(ChatMessage.class).getMessage());
                                message.setTimestamp(dataSnapshot.getValue(ChatMessage.class).getTimestamp());
                                message.setProfile_image("");;
                                message.setName("");
                                mMessageList.add(message);
                            }
                        }
                    }catch (NullPointerException e){
                        Log.e(TAG, "onDataChange: NullPointerException: " + e.getMessage());
                    }
                }

                getUserDatails();
                mAdapter.notifyDataSetChanged();
                mListView.setSelection(mAdapter.getCount() - 1);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void initMessageList() {
        mAdapter = new ChatMessageListAdapter(ChatroomActivity.this, R.layout.layout_chatroom_list_item, mMessageList);
        mListView.setAdapter(mAdapter);
        mListView.setSelection(mAdapter.getCount() - 1);
    }

    private void getUserDatails() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        for (int i = 0; i < mMessageList.size(); i++){
            final int j = i;
            if (mMessageList.get(i).getUser_id() != null && mMessageList.get(i).getProfile_image().equals("")){
                Query query = reference.child(getResources().getString(R.string.dbnode_users))
                        .orderByKey()
                        .equalTo(mMessageList.get(i).getUser_id());
                query.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        DataSnapshot dataSnapshot = snapshot.getChildren().iterator().next();
                        mMessageList.get(j).setProfile_image(dataSnapshot.getValue(User.class).getProfile_image());
                        mMessageList.get(j).setName(dataSnapshot.getValue(User.class).getName());
                        mAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }
        }
    }

    private void enableChatroomListener() {
        mMessagesReference = FirebaseDatabase.getInstance().getReference()
                .child(getResources().getString(R.string.dbnode_chatrooms))
                .child(mChatroom.getChatroom_id())
                .child(getResources().getString(R.string.field_chatroom_messages));

        mMessagesReference.addValueEventListener(mValueEventListener);
    }

    private void setUpFirebaseAuth() {
        Log.d(TAG, "setUpFirebaseAuth: started");

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null){
                    Log.d(TAG, "onAuthStateChanged: signed in with id: " + user.getUid());
                }else {
                    Log.d(TAG, "onAuthStateChanged: Signed out");
                    makeToast("Signed out");
                    Intent intent = new Intent(ChatroomActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAuthenticationState();
    }

    private void checkAuthenticationState() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null){
            Intent intent = new Intent(ChatroomActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }else {
            Log.d(TAG, "checkAuthenticationState: user is authenticated");
            makeToast("Authenticated with: " + user.getUid());
        }
    }

    private void makeToast(String message){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void hideSoftKeyboard(){
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseAuth.getInstance().addAuthStateListener(mAuthListener);
        isActivityRunning = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mAuthListener != null){
            FirebaseAuth.getInstance().removeAuthStateListener(mAuthListener);
        }
        isActivityRunning = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMessagesReference.removeEventListener(mValueEventListener);
    }
}