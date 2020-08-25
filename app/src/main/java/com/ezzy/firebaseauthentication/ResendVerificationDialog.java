package com.ezzy.firebaseauthentication;

import android.content.Context;
import android.os.Bundle;
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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ResendVerificationDialog extends DialogFragment {
    private static final String TAG = "ResendVerificationDialo";

    private EditText mConfirmEmail, mConfirmPassword;

    private Context mContext;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_resend_verification, container, false);

        mConfirmEmail = view.findViewById(R.id.confirm_email);
        mConfirmPassword = view.findViewById(R.id.confirm_password);
        mContext = getActivity();

        TextView confirmDialog = view.findViewById(R.id.dialogConfirm);
        confirmDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: Attempting to resend verification email");
                if (!isEmpty(mConfirmEmail.getText().toString()) && !isEmpty(mConfirmPassword.getText().toString())){
                    authenticateAndResendEmail(mConfirmEmail.getText().toString(), mConfirmPassword.getText().toString());
                }else {
                    makeToast(getResources().getString(R.string.fill_out_all_fields));
                }
            }
        });

        TextView cancelDialog = view.findViewById(R.id.dialogCancel);
        cancelDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDialog().dismiss();
            }
        });

        return view;
    }
    
    private void authenticateAndResendEmail(String email, String password){
        AuthCredential credential = EmailAuthProvider.getCredential(email, password);
        FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()){
                            Log.d(TAG, "onComplete: reAuthenticateSuccess");
                            sendVerificationEmail();
                            FirebaseAuth.getInstance().signOut();
                            getDialog().dismiss();
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                makeToast("Invalid credentials. \nReset your password and try again");
                getDialog().dismiss();
            }
        });
    }

    public void sendVerificationEmail(){
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null ){
            user.sendEmailVerification()
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()){
                                makeToast(getResources().getString(R.string.email_verification_sent));
                            }else {
                                makeToast(getResources().getString(R.string.email_verification_code_error));
                            }
                        }
                    });
        }
    }

    private void makeToast(String message){
        Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
    }

    private boolean isEmpty(String s){
        return s.equals("");
    }
}
