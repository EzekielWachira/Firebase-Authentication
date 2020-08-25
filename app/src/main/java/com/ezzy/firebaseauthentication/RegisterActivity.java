package com.ezzy.firebaseauthentication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.ezzy.firebaseauthentication.models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class RegisterActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private static final String DOMAIN_NAME = "gmail.com";

    private EditText mEmail, mPassword, mConfirmPassword;
    private Button mRegisterBtn;
    private ProgressBar mProgressBar;

    public static Boolean isActivityRunning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mEmail = findViewById(R.id.input_email);
        mPassword = findViewById(R.id.input_password);
        mConfirmPassword = findViewById(R.id.input_password_confirm);
        mRegisterBtn = findViewById(R.id. btn_register);
        mProgressBar = findViewById(R.id.progressBar);

        mRegisterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: Attempting to register.");
                if (!isEmpty(mEmail.getText().toString()) && !isEmpty(mPassword.getText().toString())
                        && !isEmpty(mConfirmPassword.getText().toString())){
                    if (isValidDomain(mEmail.getText().toString())){
                        if (doPasswordsMatch(mPassword.getText().toString(), mConfirmPassword.getText().toString())){
                            registerNewEmail(mEmail.getText().toString(), mPassword.getText().toString());
                        }else {
                            makeToast(getResources().getString(R.string.passwords_match_error));
                        }
                    }else{
                        makeToast(getResources().getString(R.string.register_with_company_domain));
                    }
                }else{
                    makeToast(getResources().getString(R.string.fill_out_all_fields));
                }
            }
        });

        hideSoftKeyboard();
    }

    public void registerNewEmail(final String email, String password){
        showDialog();
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "onComplete: createUserWithEmail" + task.isSuccessful());
                        if (task.isSuccessful()){
                            Log.d(TAG, "onComplete: AuthState: " + FirebaseAuth.getInstance().getCurrentUser().getUid());
                            makeToast(getResources().getString(R.string.user_creation_success));
                            sendVerificationEmail();
                            redirectToLoginScreen();

//                            User user = new User();
//
//                            user.setName(email.substring(0, email.indexOf("@")));
//                            user.setPhone("1");
//                            user.setProfile_image("");
//                            user.setSecurity_level("1");
//                            user.setUser_id(FirebaseAuth.getInstance().getCurrentUser().getUid());

                        }else {
                            makeToast(getResources().getString(R.string.registration_error));
                        }
                        hideDialog();
                    }
                });
    }

    private void sendVerificationEmail(){
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null){
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

    private boolean isValidDomain(String email){
        Log.d(TAG, "isValidDomain: veryfying email has correct domain" + email);
        String domain = email.substring(email.indexOf("@") + 1).toLowerCase();
        Log.d(TAG, "isValidDomain: users domain: " + domain);
        return domain.equals(DOMAIN_NAME);
    }

    private void redirectToLoginScreen(){
        Log.d(TAG, "redirectToLoginScreen: Redirecting to login screen");
        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
        startActivity(intent);
        //The user will not be able to navigate back to register activity on back button press
        finish();
    }

    private boolean isEmpty(String string){
        return string.equals("");
    }

    private boolean doPasswordsMatch(String pass1, String pass2){
        return pass1.equals(pass2);
    }

    private void showDialog(){
        mProgressBar.setVisibility(View.VISIBLE);
    }

    private void hideDialog(){
        if (mProgressBar.getVisibility() == View.VISIBLE){
            mProgressBar.setVisibility(View.INVISIBLE);
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
        isActivityRunning = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        isActivityRunning = false;
    }
}