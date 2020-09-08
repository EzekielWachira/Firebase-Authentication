package com.ezzy.firebaseauthentication.utility;

import android.content.Context;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ezzy.firebaseauthentication.R;
import com.ezzy.firebaseauthentication.models.ChatMessage;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatMessageListAdapter extends ArrayAdapter<ChatMessage> {

    private static final String TAG = "ChatMessageListAdapter";
    private int mLayoutResource;
    private Context mContext;

    public ChatMessageListAdapter(@NonNull Context context, int resource, @NonNull List<ChatMessage> objects) {
        super(context, resource, objects);
        mContext = context;
        mLayoutResource = resource;
    }

    public static class ViewHolder{
        TextView message;
        CircleImageView mProfileImage;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        final ViewHolder viewHolder;

        if (convertView == null){
            LayoutInflater inflater = LayoutInflater.from(mContext);
            convertView = inflater.inflate(mLayoutResource, parent, false);
            viewHolder = new ViewHolder();

            viewHolder.message = convertView.findViewById(R.id.message);
            viewHolder.mProfileImage = convertView.findViewById(R.id.profile_image);

            convertView.setTag(viewHolder);
        }else {
            viewHolder = (ViewHolder) convertView.getTag();
            viewHolder.message.setText("");
        }

        try {
            viewHolder.message.setText(getItem(position).getMessage());

            if (viewHolder.mProfileImage.getTag() == null || !viewHolder.mProfileImage.getTag().equals(getItem(position).getProfile_image())){
                ImageLoader.getInstance().displayImage(getItem(position).getProfile_image(), viewHolder.mProfileImage,
                        new SimpleImageLoadingListener());
                viewHolder.mProfileImage.setTag(getItem(position).getProfile_image());
            }
        }catch (NullPointerException e){
            Log.e(TAG, "getView: NullPointerException", e.getCause());
        }

        return convertView;
    }
}
