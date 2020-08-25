package com.ezzy.firebaseauthentication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.w3c.dom.Text;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private static final int ERROR_DIALOG_REQUEST = 9001;

    private FirebaseAuth.AuthStateListener mAuthListener;

    private EditText mEmail, mPassword;
    private ProgressBar mProgressBar;

    public static boolean isActivityRunning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mEmail = findViewById(R.id.email);
        mPassword = findViewById(R.id.password);
        mProgressBar = findViewById(R.id.progressBar);

        setUpFirebaseAuth();
        if (serviceOK()){
            init();
        }
        hideSoftKeyBoard();

    }

    private void init(){
        Button signInBtn = findViewById(R.id.email_sign_in_btn);
        signInBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isEmpty(mEmail.getText().toString()) && !isEmpty(mPassword.getText().toString())){
                    Log.d(TAG, "onClick: attempting to authenticate");
                    showDialog();
                    FirebaseAuth.getInstance().signInWithEmailAndPassword(mEmail.getTextColors().toString(), mPassword.getTextColors().toString())
                            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    hodeDialog();
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            makeToast("Authentication failed");
                            hodeDialog();
                        }
                    });
                }else{
                    makeToast(getResources().getString(R.string.fill_out_all_fields));
                }
            }
        });
        TextView register = findViewById(R.id.link_register);
        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });

        TextView resetPassword = findViewById(R.id.forgot_password);
        resetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PasswordResetDialog dialog = new PasswordResetDialog();
                dialog.show(getSupportFragmentManager(), "dialog_password_reset");
            }
        });

        TextView resendEmailVerification = findViewById(R.id.resend_verification_email);
        resendEmailVerification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ResendVerificationDialog dialog = new ResendVerificationDialog();
                dialog.show(getSupportFragmentManager(), "dialog_resend_email_verification");
            }
        });
    }

    public boolean serviceOK(){
        Log.d(TAG, "serviceOK: checking google services");
        int isAvailable = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(LoginActivity.this);
        if (isAvailable == ConnectionResult.SUCCESS){
            Log.d(TAG, "serviceOK: Play services is OKAY");
            return true;
        }else if (GoogleApiAvailability.getInstance().isUserResolvableError(isAvailable)){
            Log.d(TAG, "serviceOK: An error occurred");
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(LoginActivity.this, isAvailable, ERROR_DIALOG_REQUEST);
            dialog.show();
        }else {
            makeToast("Cannot connect to mapping services");
        }

        return false;
    }

    private void initImageLoader(){
//
    }

    private boolean isEmpty(String string){
        return string.equals("");
    }

    private void showDialog(){
        mProgressBar.setVisibility(View.VISIBLE);
    }

    private void hodeDialog(){
        if (mProgressBar.getVisibility() == View.VISIBLE){
            mProgressBar.setVisibility(View.INVISIBLE);
        }
    }

    private void hideSoftKeyBoard(){
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    private void setUpFirebaseAuth(){
        Log.d(TAG, "setUpFirebaseAuth: started");
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null){
                    //check if email is verified
                    if (user.isEmailVerified()){
                        Log.d(TAG, "onAuthStateChanged: signed in" + user.getUid());
                        makeToast("Authenticated with: " + user.getEmail());
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                        if (getIntent().getExtras() != null){
                            Log.d(TAG, "initFCM: found intent extras: " + getIntent().getExtras().toString());
                            for (String key : getIntent().getExtras().keySet()){
                                Object value = getIntent().getExtras().get(key);
                                Log.d(TAG, "initFCM: key: " + key + "Value: " + value);
                            }
                            String data = getIntent().getStringExtra("data");
                            Log.d(TAG, "initFCM: data: " + data);
                        }

                        startActivity(intent);
                        finish();
                    }else {
                        makeToast("Email is not verified\nCheck your inbox to verify");
                        FirebaseAuth.getInstance().signOut();
                    }
                }else {
                    Log.d(TAG, "onAuthStateChanged: signed_out");
                }
            }
        };
    }

    private void makeToast(String message){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
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
}