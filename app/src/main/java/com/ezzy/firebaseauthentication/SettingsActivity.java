package com.ezzy.firebaseauthentication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.ChangedPackages;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ezzy.firebaseauthentication.models.User;
import com.ezzy.firebaseauthentication.utility.FilePaths;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.SignInMethodQueryResult;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class SettingsActivity extends AppCompatActivity implements ChangePhotoDialog.OnPhotoReceivedListener {

    private static final String TAG = "SettingsActivity";

    private static final String DOMAIN_NAME = "gmail.com";
    private static final int REQUEST_CODE = 4321;
    private static final double MB_THRESHHOLD = 5.0;
    private static final double MB = 1000000.0;

    private FirebaseAuth.AuthStateListener mAuthListener;

    private EditText mEmail, mCurrentPassword, mName, mPhone;
    private ImageView mProfileImage;
    private Button mButton;
    private ProgressBar mProgressbar;
    private TextView mResetPasswordLink;

    private boolean mStoragePermissions;
    private Uri mSelectedImageUri;
    private Bitmap mSelectedImageBitMap;
    private byte[] mBytes;
    private double progress;
    public static boolean isActivityRunning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mEmail = findViewById(R.id.input_email);
        mCurrentPassword = findViewById(R.id.input_password);
        mButton = findViewById(R.id.btn_save);
        mProgressbar = findViewById(R.id.progressBar);
        mProfileImage = findViewById(R.id.profile_image);
        mResetPasswordLink = findViewById(R.id.change_password);
        mName = findViewById(R.id.input_name);
        mPhone = findViewById(R.id.input_phone);

        verifyStoragePermissions();
        setUpFirebaseAuth();
        setCurrentEmail();
        init();
        hideSoftKeyboard();

    }

    @Override
    public void getImagePath(Uri imagePath) {
        if (!imagePath.toString().equals("")){
            mSelectedImageBitMap = null;
            mSelectedImageUri = imagePath;
            Log.d(TAG, "getImagePath: Image Uri: " + mSelectedImageUri);

            ImageLoader.getInstance().displayImage(imagePath.toString(), mProfileImage);
        }
    }

    @Override
    public void getImageBitMap(Bitmap bitmap) {
        if (bitmap != null){
            mSelectedImageUri = null;
            mSelectedImageBitMap = bitmap;
            Log.d(TAG, "getImageBitMap: got the image bitmap" + mSelectedImageBitMap);
        }
    }

    private void init(){
        getUserAccountData();
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: saving user settings");
                if (!mEmail.getText().toString().equals(FirebaseAuth.getInstance().getCurrentUser().getEmail())){
                    if (!isEmpty(mEmail.getText().toString()) && !isEmpty(mCurrentPassword.getText().toString())){
                        if (isValidDomain(mEmail.getText().toString())){
                            editUserEmail();
                        }else {
                            makeToast("Invalid domain");
                        }
                    }else {
                        makeToast(getResources().getString(R.string.fill_out_all_fields));
                    }
                }

                DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
                // Change name
                if (!isEmpty(mName.getText().toString())){
                    reference.child(getResources().getString(R.string.dbnode_users))
                            .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                            .child(getResources().getString(R.string.field_name))
                            .setValue(mName.getText().toString());
                }

                //change phone number
                if (!isEmpty(mPhone.getText().toString())){
                    reference.child(getResources().getString(R.string.dbnode_users))
                            .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                            .child(getResources().getString(R.string.field_phone))
                            .setValue(mPhone.getText().toString());
                }

                //upload a new photo
                if (mSelectedImageUri != null){
                    uploadNewPhoto(mSelectedImageUri);
                }else if (mSelectedImageBitMap != null){
                    uploadNewPhoto(mSelectedImageBitMap);
                }

                makeToast("Saved");
            }
        });

        mResetPasswordLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: Sending password reset link to email");
                sendResetPasswordLink();
            }
        });

        mProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mStoragePermissions){
                    ChangePhotoDialog dialog = new ChangePhotoDialog();
                    dialog.show(getSupportFragmentManager(), "ChangePhotoDialog");
                }else {
                    verifyStoragePermissions();
                }
            }
        });
    }

    public void uploadNewPhoto(Uri imageUri){
        Log.d(TAG, "uploadNewPhoto: Uploading new photo");
        BackgroundImageResize resize = new BackgroundImageResize(null);
        resize.execute(imageUri);
    }

    public void uploadNewPhoto(Bitmap imageBitmap){
        Log.d(TAG, "uploadNewPhoto: Uploading new photo");
        BackgroundImageResize resize = new BackgroundImageResize(imageBitmap);
        Uri uri = null;
        resize.execute(uri);
    }

    public void verifyStoragePermissions() {
        Log.d(TAG, "verifyStoragePermissions: asking user permissions");
        String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA};

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), permissions[0]) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this.getApplicationContext(), permissions[1]) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this.getApplicationContext(), permissions[2]) == PackageManager.PERMISSION_GRANTED) {
            mStoragePermissions = true;
        }else {
            ActivityCompat.requestPermissions(SettingsActivity.this, permissions, REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "onRequestPermissionsResult: User has allowed permission to access: " + permissions[0]);
            }
        }
    }

    private void sendResetPasswordLink(){
        FirebaseAuth.getInstance().sendPasswordResetEmail(FirebaseAuth.getInstance().getCurrentUser().getEmail())
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()){
                            Log.d(TAG, "onComplete: Password reset email set");
                            makeToast("Password reset link was sent to your email");
                        }else {
                            Log.d(TAG, "onComplete: No User associated with that email");
                            makeToast("No User associated with that email");
                        }
                    }
                });
    }

    private void setUpFirebaseAuth(){
        Log.d(TAG, "setUpFirebaseAuth: started");
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null){
                    Log.d(TAG, "onAuthStateChanged: signed in");
                }else {
                    Log.d(TAG, "onAuthStateChanged: signed out");
                    makeToast("Signed out");
                    Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        };
    }

    private void setCurrentEmail(){
        Log.d(TAG, "setCurrentEmail: setting current email to editext");
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null){
            String email = user.getEmail();
            mEmail.setText(email);
        }
    }

    private void getUserAccountData(){
        Log.d(TAG, "getUserAccountData: getting user account data");
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();

        Query query1 = reference.child(getResources().getString(R.string.dbnode_users))
                .orderByKey()
                .equalTo(FirebaseAuth.getInstance().getCurrentUser().getUid());
        query1.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot singleSnapShot: snapshot.getChildren()){
                    Log.d(TAG, "onDataChange: QUERY 1 found user: " + singleSnapShot.getValue(User.class).toString());
                    User user = singleSnapShot.getValue(User.class);
                    mName.setText(user.getName());
                    mPhone.setText(user.getPhone());
                    ImageLoader.getInstance().displayImage(user.getProfile_image(), mProfileImage);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                makeToast("An error occured!!!");
            }
        });

        mEmail.setText(FirebaseAuth.getInstance().getCurrentUser().getEmail());

//        Query query2 = reference.child(getResources().getString(R.string.dbnode_users))
//                .orderByChild(getResources().getString(R.string.field_user_id))
//                .equalTo(FirebaseAuth.getInstance().getCurrentUser().getUid());
//
//        query2.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                for (DataSnapshot snapshot1 : snapshot.getChildren()){
//                    Log.d(TAG, "onDataChange: Query 2 found user" + snapshot1.getValue(User.class).toString());
//                    User user = snapshot1.getValue(User.class);
//                    mName.setText(user.getName());
//                    mPhone.setText(user.getPhone());
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//
//            }
//        });
    }

    private void editUserEmail(){
        showDialog();
        AuthCredential credential = EmailAuthProvider.getCredential(FirebaseAuth.getInstance().getCurrentUser().getEmail(),
                mCurrentPassword.getText().toString());

        FirebaseAuth.getInstance().getCurrentUser().reauthenticate(credential)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()){
                            if (isValidDomain(mEmail.getText().toString())){
                               FirebaseAuth.getInstance().fetchSignInMethodsForEmail(mEmail.getText().toString())
                                       .addOnCompleteListener(new OnCompleteListener<SignInMethodQueryResult>() {
                                           @Override
                                           public void onComplete(@NonNull Task<SignInMethodQueryResult> task) {
                                                if (task.isSuccessful()){
                                                    if (task.getResult().getSignInMethods().size() == 1){
                                                        Log.d(TAG, "onComplete: EMail is already in use");
                                                        hideDialog();
                                                        makeToast("The email address is already in use");
                                                    }else {
                                                        Log.d(TAG, "onComplete: Email is available");
                                                        FirebaseAuth.getInstance().getCurrentUser().updateEmail(mEmail.getText().toString())
                                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                    @Override
                                                                    public void onComplete(@NonNull Task<Void> task) {
                                                                        if (task.isSuccessful()){
                                                                            makeToast("User email updated");
                                                                            sendVerificationEmail();
                                                                            FirebaseAuth.getInstance().signOut();
                                                                        }else {
                                                                            makeToast("Could not update email");
                                                                        }
                                                                        hideDialog();
                                                                    }
                                                                }).addOnFailureListener(new OnFailureListener() {
                                                            @Override
                                                            public void onFailure(@NonNull Exception e) {
                                                                hideDialog();
                                                                makeToast("Unable to update email");
                                                            }
                                                        });
                                                    }
                                                }
                                           }
                                       }).addOnFailureListener(new OnFailureListener() {
                                   @Override
                                   public void onFailure(@NonNull Exception e) {
                                       hideDialog();
                                       makeToast("Unable to update email");
                                   }
                               });
                            }else {
                                makeToast("You must use the company domain to register");
                            }
                        }else {
                            makeToast("Incorrect password");
                            hideDialog();
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                hideDialog();
                makeToast("Unable to update email");
            }
        });
    }

    public void sendVerificationEmail(){
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
        String domain = email.substring(email.indexOf("@") + 1).toLowerCase();
        return domain.equals(DOMAIN_NAME);
    }

    private boolean isEmpty(String s){
        return  s.equals("");
    }

    private void makeToast(String message){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showDialog(){
        mProgressbar.setVisibility(View.VISIBLE);
    }

    private void hideDialog(){
        if (mProgressbar.getVisibility() == View.VISIBLE){
            mProgressbar.setVisibility(View.INVISIBLE);
        }
    }

    private void hideSoftKeyboard(){
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    public class BackgroundImageResize extends AsyncTask<Uri, Integer, byte[]>{

        Bitmap mBitmap;
        public BackgroundImageResize(Bitmap bm){
            if (bm != null){
                mBitmap = bm;
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showDialog();
        }

        @Override
        protected byte[] doInBackground(Uri... uris) {
            Log.d(TAG, "doInBackground: started");
            if (mBitmap == null){
//                try{
//                    mBitmap = MediaStore.Images.Media.getBitmap(SettingsActivity.this.getContentResolver(), uris[0]);
//                }catch (IOException e){
//                    Log.e(TAG, "doInBackground: ", e.getCause());
//                }
//
//                byte[] bytes = null;
//                for (int i = 0; i < 11; i++){
//                    if (i == 10){
//                        makeToast("The Image is too large");
//                    }
//                    bytes = getBytesFromBitmap(mBitmap, 100 / i);
//                    if (bytes.length/MB < MB_THRESHHOLD){
//                        return bytes;
//                    }
//                }

                InputStream inputStream = null;
                try {
                    inputStream = SettingsActivity.this.getContentResolver().openInputStream(uris[0]);
                }catch (FileNotFoundException e){
                    e.printStackTrace();
                }

                ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
                int bufferSize = 1024;
                byte[] buffer = new byte[bufferSize];

                int len = 0;
                try {
                    while ((len = inputStream.read(buffer)) != -1){
                        byteBuffer.write(buffer, 0, len);
                    }
                    inputStream.close();
                }catch (IOException e){
                    e.printStackTrace();
                }
                return byteBuffer.toByteArray();
            }else {
                int size = mBitmap.getRowBytes() * mBitmap.getHeight();
                ByteBuffer byteBuffer = ByteBuffer.allocate(size);
                mBitmap.copyPixelsToBuffer(byteBuffer);
                byte[] bytes = byteBuffer.array();
                byteBuffer.rewind();
                return bytes;
            }
        }

        @Override
        protected void onPostExecute(byte[] bytes) {
            super.onPostExecute(bytes);
            hideDialog();
            mBytes = bytes;
            executeUploadTask();
        }
    }

    private void executeUploadTask(){
        showDialog();
        FilePaths filePaths = new FilePaths();

        final StorageReference storageReference = FirebaseStorage.getInstance().getReference()
                .child(filePaths.FIREBASE_IMAGE_STORAGE + "/" + FirebaseAuth.getInstance().getCurrentUser().getUid() + "/profile_image");

        if (mBytes.length / MB < MB_THRESHHOLD){
            StorageMetadata metadata = new StorageMetadata.Builder()
                    .setContentType("image/jpg")
                    .setContentLanguage("en")
                    .setCustomMetadata("Ezzy Extra Metadata", "Really fun")
                    .setCustomMetadata("Location", "Australia")
                    .build();

            UploadTask uploadTask = null;
//            uploadTask = storageReference.putBytes(mBytes);  without metadata
            uploadTask = storageReference.putBytes(mBytes, metadata);

            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Uri firebaseURL = taskSnapshot.getDownloadUrl();

                    FirebaseDatabase.getInstance().getReference()
                            .child(getResources().getString(R.string.dbnode_users))
                            .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                            .child(getResources().getString(R.string.field_profile_image))
                            .setValue(firebaseURL.toString());

                    hideDialog();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    makeToast("Couldn't upload photo");
                    hideDialog();
                }
            }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                    double currentProgress = (100 * snapshot.getBytesTransferred());
                    if (currentProgress > (progress + 15)){
                        progress = (100 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                        Log.d(TAG, "onProgress: upload is: " + progress + "% done");
                        makeToast(progress + "%");
                    }
                }
            });
        }else {
            makeToast("The image is too large");
        }
    }

    public static byte[] getBytesFromBitmap(Bitmap bitmap, int quality){
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream);
        return stream.toByteArray();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAuthenticationState();
    }

    private void checkAuthenticationState(){
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null){
            Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }else {
            makeToast("Ypu are already authenticated");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseAuth.getInstance().addAuthStateListener(mAuthListener);
        isActivityRunning  = true;
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