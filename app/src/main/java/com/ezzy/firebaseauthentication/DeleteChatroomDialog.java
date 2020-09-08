package com.ezzy.firebaseauthentication;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class DeleteChatroomDialog extends DialogFragment {

    private static final String TAG = "DeleteChatroomDialog";
    Context mContext;

    public DeleteChatroomDialog(){
        super();
        setArguments(new Bundle());
    }

    private String mChatroomId;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: started");
        mChatroomId = getArguments().getString(mContext.getResources().getString(R.string.field_chatroom_id));
        if (mChatroomId != null){
            Log.d(TAG, "onCreate: chatroom id: " + mChatroomId);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_delete_chatroom, container, false);

        TextView delete = view.findViewById(R.id.confirm_delete);
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mChatroomId != null){
                    Log.d(TAG, "onClick: deleting chatroom: " + mChatroomId);
                    DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
                    reference.child(mContext.getResources().getString(R.string.dbnode_chatrooms))
                            .child(mChatroomId)
                            .removeValue();

                    ((ChatActivity)getActivity()).getChatrooms();
                    getDialog().dismiss();
                }
            }
        });

        TextView cancel = view.findViewById(R.id.cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: dismissing the dialog");
                getDialog().dismiss();
            }
        });


        return view;
    }
}
