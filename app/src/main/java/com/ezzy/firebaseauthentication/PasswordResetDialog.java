package com.ezzy.firebaseauthentication;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

public class PasswordResetDialog extends DialogFragment {

    private static final String TAG = "PasswordResetDialog";

    private EditText mEmail;

    private Context mContext;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_reset_password, container, false);
        mEmail = view.findViewById(R.id.email_password_reset);
        mContext = getActivity();

        TextView confirmDialog = view.findViewById(R.id.dialogConfirm);
        confirmDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isEmpty(mEmail.getText().toString())){
                    Log.d(TAG, "onClick: attempting to send reset link to: " + mEmail.getText().toString());

                }
            }
        });
        return view;
    }

    public void sendPasswordResetEmail(String email){
        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()){
                            Log.d(TAG, "onComplete: password reset email sent");
                            makeToast(getResources().getString(R.string.password_reset_resend));
                        }else {
                            Log.d(TAG, "onComplete: No user associated with that email");
                            makeToast(getResources().getString(R.string.no_user_email));
                        }
                    }
                });
    }

    private void makeToast(String message){
        Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
    }

    private boolean isEmpty(String string){
        return string.equals("");
    }
}
