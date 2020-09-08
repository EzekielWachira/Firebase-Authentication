package com.ezzy.firebaseauthentication;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.DialogFragment;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ChangePhotoDialog extends DialogFragment {
    private static final String TAG = "ChangePhotoDialog";
    public static final int CAMERA_REQUEST_CODE = 5467;
    public static final int PICKFILE_REQUEST_CODE = 8352;

    public interface OnPhotoReceivedListener{
        void getImagePath(Uri imagePath);
        void getImageBitMap(Bitmap bitmap);
    }

    OnPhotoReceivedListener mOnPhotoReceived;
    private String mCurrentPhotoPath;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_change_photo, container, false);

        TextView selectPhoto = view.findViewById(R.id.dialogChoosePhoto);
        selectPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: accessing phone memory");
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, PICKFILE_REQUEST_CODE);
            }
        });

        TextView takePhoto = view.findViewById(R.id.dialogOpenCamera);
        takePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: Accessing phone camera");
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                if (cameraIntent.resolveActivity(getActivity().getPackageManager()) != null){
                    File photoFile = null;
                    try {
                        photoFile = createImageFile();
                    }catch (IOException ex){
                        Log.d(TAG, "onClick: error!!!: " + ex.getMessage());
                    }

                    if (photoFile != null){
                        Uri photoUri = FileProvider.getUriForFile(getActivity(),
                                "com.example.android.fileprovider",
                                photoFile);
                        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                        startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
                    }
                }
            }
        });
        return view;
    }
//
//    private File createImageFile() throw IOException {
//        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
//        String imageFileName = "JPEG_" + timeStamp + "_";
//        File storageDir = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
//        File image = File.createTempFile(
//                imageFileName,
//                ".jpg",
//                storageDir
//        );
//
//        mCurrentPhotoPath = image.getAbsolutePath();
//        return image;
//    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICKFILE_REQUEST_CODE && resultCode == Activity.RESULT_OK){
            Uri selectedImageUri = data.getData();
            Log.d(TAG, "onActivityResult: image: "  + selectedImageUri);

            mOnPhotoReceived.getImagePath(selectedImageUri);
            getDialog().dismiss();
        }else if (requestCode == CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK){
            Log.d(TAG, "onActivityResult: image uri: " + mCurrentPhotoPath);
            Bitmap bitmap;
            bitmap = (Bitmap) data.getExtras().get("data");
            mOnPhotoReceived.getImageBitMap(bitmap);
//            mOnPhotoReceived.getImagePath(Uri.fromFile(new File(mCurrentPhotoPath)));
            getDialog().dismiss();

        }

//        switch (requestCode){
//            case PICKFILE_REQUEST_CODE:
//                if (resultCode == Activity.RESULT_OK){
//                    Uri selectedImageUri = data.getData();
//                    Log.d(TAG, "onActivityResult: image: "  + selectedImageUri);
//
//                    mOnPhotoReceived.getImagePath(selectedImageUri);
//                    getDialog().dismiss();
//                }
//                break;
//            case CAMERA_REQUEST_CODE:
//                if (resultCode == Activity.RESULT_OK){
//                    Log.d(TAG, "onActivityResult: image uri: " + mCurrentPhotoPath);
//                    mOnPhotoReceived.getImagePath(Uri.fromFile(new File(mCurrentPhotoPath)));
//                    getDialog().dismiss();
//                }
//                break;
//        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        try {
            mOnPhotoReceived = (OnPhotoReceivedListener) getActivity();
        }catch (ClassCastException e){
            Log.e(TAG, "onAttach: ClassCastException: ",  e.getCause());
        }
        super.onAttach(context);
    }
}
