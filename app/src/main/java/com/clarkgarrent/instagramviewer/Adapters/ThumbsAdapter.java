package com.clarkgarrent.instagramviewer.Adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import com.clarkgarrent.instagramviewer.Models.UserMediaData;
import com.clarkgarrent.instagramviewer.R;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * Created by karlc on 8/9/2017.
 * Very similar to the LargeViewAdapter except we have to listener interfaces, one for the
 * button and one for clicking on the image itself.
 */

public class ThumbsAdapter extends Adapter<ThumbsAdapter.ViewHolder> {

    private ArrayList<UserMediaData> mUserMediaData;
    private Context mContext;

    private static OnItemClickListener itemListener;

    private static OnButtonClickListener buttonListener;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public interface OnButtonClickListener {
        void onButtonClick (int position);
    }
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemListener = listener;
    }

    public void setOnButtonClickListener(OnButtonClickListener listener){
        buttonListener = listener;
    }

    public ThumbsAdapter(Context context, ArrayList<UserMediaData> userMediaData){
        mContext = context;
        mUserMediaData = userMediaData;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public ImageView thumbImageView;
        public Button btnLike;

        public ViewHolder (View view){
            super(view);
            thumbImageView =(ImageView)view.findViewById(R.id.thumbImageView);
            btnLike = (Button)view.findViewById(R.id.btnLike);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (itemListener != null){
                        itemListener.onItemClick(getLayoutPosition());
                    }
                }
            });

            btnLike.setOnClickListener(new View.OnClickListener() {
                @Override

                public void onClick(View v) {
                    if (buttonListener != null){
                        buttonListener.onButtonClick(getLayoutPosition());
                    }
                }
            });
        }
    }

    @Override
    public ThumbsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View thumbView = inflater.inflate(R.layout.thumb_item, parent, false);
        ViewHolder viewHolder = new ViewHolder(thumbView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position){
        viewHolder.thumbImageView.setImageResource(android.R.color.transparent);
        Picasso.with(mContext).load(mUserMediaData.get(position).getImages().getThumbnail().getUrl()).into(viewHolder.thumbImageView);
        if (mUserMediaData.get(position).isLiked()){
            viewHolder.btnLike.setBackgroundResource(0);
            viewHolder.btnLike.setBackgroundResource(R.drawable.heart_red);
        }else {
            viewHolder.btnLike.setBackgroundResource(0);
            viewHolder.btnLike.setBackgroundResource(R.drawable.heart_white);
        }
    }
    @Override
    public int getItemCount(){
        return mUserMediaData.size();
    }
}
