package com.ezzy.firebaseauthentication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.ezzy.firebaseauthentication.models.ChatMessage;
import com.ezzy.firebaseauthentication.models.Chatroom;
import com.ezzy.firebaseauthentication.models.User;
import com.ezzy.firebaseauthentication.utility.ChatRoomAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";

    private ListView mListView;
    private FloatingActionButton mFab;

    private ArrayList<Chatroom> mChatrooms;
    private ChatRoomAdapter mAdapter;
    private int mSecurityLevel;
    private DatabaseReference mChatroomReference;
    private Boolean isActivityRunning;
    private HashMap<String, String> mNumChatroomMessages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mListView = findViewById(R.id.listview);
        mFab = findViewById(R.id.fab);

        init();
    }

    private void init() {
        mChatrooms = new ArrayList<>();
        getUserSecurityLevel();
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NewChatroomDialog dialog = new NewChatroomDialog();
                dialog.show(getSupportFragmentManager(), getResources().getString(R.string.dialog_new_chatroom));
            }
        });
    }



    private void getUserSecurityLevel() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query query = reference.child(getResources().getString(R.string.dbnode_users))
                .orderByChild(getResources().getString(R.string.field_user_id))
                .equalTo(FirebaseAuth.getInstance().getCurrentUser().getUid());

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "onDataChange: data snapshot: " + snapshot);
                DataSnapshot singleSnapShot = snapshot.getChildren().iterator().next();
                int securityLevel = Integer.parseInt(singleSnapShot.getValue(User.class).getSecurity_level());
                Log.d(TAG, "onDataChange: user security level: " + securityLevel);
                mSecurityLevel = securityLevel;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    public void getChatrooms(){
        Log.d(TAG, "getChatrooms: getting list of chatrooms");
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();

        mNumChatroomMessages = new HashMap<>();
        if (mAdapter != null){
            mAdapter.clear();
            mChatrooms.clear();
        }
        Query query = reference.child(getResources().getString(R.string.dbnode_chatrooms))
                .orderByKey();

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dataSnapshot : snapshot.getChildren()){
                    try {
                        if (dataSnapshot.exists()){
                            Chatroom chatroom = new Chatroom();
                            Map<String, Object> objectMap = (HashMap<String, Object>) dataSnapshot.getValue();
                            Log.d(TAG, "onDataChange: found a chatroom: " + objectMap.get(getResources().getString(R.string.field_chatroom_name)).toString());
                            chatroom.setChatroom_id(objectMap.get(getResources().getString(R.string.field_chatroom_id)).toString());
                            chatroom.setChatroom_name(objectMap.get(getResources().getString(R.string.field_chatroom_name)).toString());
                            chatroom.setCreator_id(objectMap.get(getResources().getString(R.string.field_creator_id)).toString());
                            chatroom.setSecurity_level(objectMap.get(getResources().getString(R.string.field_security_level)).toString());

                            ArrayList<ChatMessage> messageList = new ArrayList<ChatMessage>();
                            int numMessages = 0;
                            for (DataSnapshot snapshot1 : dataSnapshot.child(getResources().getString(R.string.field_chatroom_messages)).getChildren()){
                                ChatMessage message = new ChatMessage();
                                message.setTimestamp(snapshot1.getValue(ChatMessage.class).getTimestamp());
                                message.setUser_id(snapshot1.getValue(ChatMessage.class).getUser_id());
                                message.setMessage(snapshot1.getValue(ChatMessage.class).getMessage());
                                messageList.add(message);
                                numMessages++;
                            }

                            if (messageList.size() > 0){
                                chatroom.setChatroom_messages(messageList);
                                mNumChatroomMessages.put(chatroom.getChatroom_id(), String.valueOf(numMessages));
                            }

                            List<String> users = new ArrayList<>();
                            for (DataSnapshot snapshot1 : dataSnapshot.child(getResources().getString(R.string.field_users)).getChildren()){
                                String user_id = snapshot1.getKey();
                                Log.d(TAG, "onDataChange: user surrently on chatroom: " + user_id);
                                users.add(user_id);
                            }
                            if (users.size() > 0){
                                chatroom.setUsers(users);
                            }

                            mChatrooms.add(chatroom);

                        }
                        setUpCharoomList();
                    }catch (NullPointerException e){
                        Log.e(TAG, "onDataChange: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    private void setUpCharoomList() {
        Log.d(TAG, "setUpCharoomList: setting up chatroom listview");
        mAdapter = new ChatRoomAdapter(ChatActivity.this, R.layout.layout_chatroom_list_item, mChatrooms);
        mListView.setAdapter(mAdapter);
    }

    public void showDeleteChatroomDialog(String chatroom_id){
        DeleteChatroomDialog dialog = new DeleteChatroomDialog();
        Bundle args = new Bundle();
        args.putString(getResources().getString(R.string.field_chatroom_id), chatroom_id);
        dialog.setArguments(args);
        dialog.show(getSupportFragmentManager(), getResources().getString(R.string.dialog_delete_chatroom));
    }

    public void joinChatroom(final Chatroom chatroom){
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();

        Query query = reference.child(getResources().getString(R.string.dbnode_chatrooms))
                .orderByKey();
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dataSnapshot : snapshot.getChildren()){
                    Map<String, Object> objectMap = (HashMap<String, Object>) dataSnapshot.getValue();
                    if (objectMap.get(getResources().getString(R.string.field_chatroom_id)).toString()
                        .equals(chatroom.getChatroom_id())){
                        if (mSecurityLevel >= Integer.parseInt(chatroom.getSecurity_level())){
                            addUserToChatroom(chatroom);
                            Intent intent = new Intent(ChatActivity.this, ChatroomActivity.class);
                            intent.putExtra(getResources().getString(R.string.intent_chatroom), chatroom);
                            startActivity(intent);
                        }else {
                            makeToast("You security level is below the chatroom's security level");
                        }
                        break;
                    }
                }
                getChatrooms();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void addUserToChatroom(Chatroom chatroom) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        reference.child(getResources().getString(R.string.dbnode_chatrooms))
                .child(chatroom.getChatroom_id())
                .child(getResources().getString(R.string.field_users))
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child(getResources().getString(R.string.field_last_message_seen))
                .setValue(mNumChatroomMessages.get(chatroom.getChatroom_id()));
    }

    private void makeToast(String message){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: called");
        checkAuthenticationState();
        getChatrooms();
    }

    @Override
    protected void onStart() {
        super.onStart();
        isActivityRunning = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        isActivityRunning = false;
    }

    private void checkAuthenticationState() {
        Log.d(TAG, "checkAuthenticationState: checking authentication state");
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null){
            Log.d(TAG, "checkAuthenticationState: User not authenticated, moving to login screen");
            Intent intent = new Intent(ChatActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }else {
            Log.d(TAG, "checkAuthenticationState: user is authenticated");
        }
    }
}