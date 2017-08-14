package com.clarkgarrent.instagramviewer.Adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.clarkgarrent.instagramviewer.Models.UserMediaData;
import com.clarkgarrent.instagramviewer.R;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * Created by karlc on 8/11/2017.
 * This is a pretty simple RecyclerView adapter with a few features.  First it defines a listener
 * interface that we call when the user clicks the button in an item.  The interface
 * returns the position of the clicked item in the list.  The calling activity can use this interface
 * to listen for button clicks.  Second the adapter uses the Picasso library from Square to download
 * images from the Instagram website and load them into the ImageViews.  Finally the adapter changes
 * the background on the button in each item to reflect whether the image is liked or not.
 */

public class LargeViewAdapter extends RecyclerView.Adapter<LargeViewAdapter.ViewHolder> {
    private ArrayList<UserMediaData> mUserMediaData;
    private Context mContext;

    // Used to store reference to who is listening.
    private static LargeViewAdapter.OnItemClickListener mListener;

    // Define the mListener interface
    public interface OnItemClickListener {
        void onItemClick(View itemView, int position);
    }
    // Called by parent activity to store itself in our mListener field.
    public void setOnItemClickListener(LargeViewAdapter.OnItemClickListener listener) {
        this.mListener = listener;
    }

    public LargeViewAdapter(Context context, ArrayList<UserMediaData> userMediaData){
        mContext = context;
        mUserMediaData = userMediaData;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public ImageView largeImageView;
        public TextView tvUserName;
        public Button btnLike;

        public ViewHolder (View view){
            super(view);
            largeImageView =(ImageView)view.findViewById(R.id.ivLarge);
            tvUserName = (TextView)view.findViewById(R.id.tvLargeViewUserName);
            btnLike = (Button)view.findViewById(R.id.btnLike);
            btnLike.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // if we have a listener call its onItemClick method.
                    if (mListener != null){
                        mListener.onItemClick(itemView, getLayoutPosition());
                    }
                }
            });
        }
    }

    @Override
    public LargeViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View largeView = inflater.inflate(R.layout.large_item, parent, false);
        LargeViewAdapter.ViewHolder viewHolder = new LargeViewAdapter.ViewHolder(largeView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(LargeViewAdapter.ViewHolder viewHolder, int position){
        viewHolder.tvUserName.setText(mUserMediaData.get(position).getUser().getName());
        viewHolder.largeImageView.setImageResource(android.R.color.transparent);
        Picasso.with(mContext).load(mUserMediaData.get(position).getImages().getStandard_resolution().getUrl()).into(viewHolder.largeImageView);
        if (mUserMediaData.get(position).isLiked()){
            viewHolder.btnLike.setBackgroundResource(0);
            viewHolder.btnLike.setBackgroundResource(R.drawable.heart_red);
        }else {
            viewHolder.btnLike.setBackgroundResource(0);
            viewHolder.btnLike.setBackgroundResource(R.drawable.heart_clear);
        }
    }
    @Override
    public int getItemCount(){
        return mUserMediaData.size();
    }
}

