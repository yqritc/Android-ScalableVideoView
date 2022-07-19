package com.yqritc.scalablevideoview.sample;

import com.yqritc.scalablevideoview.ScalableType;
import com.yqritc.scalablevideoview.ScalableVideoView;

import android.content.Context;
import android.media.MediaPlayer;
import androidx.annotation.RawRes;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.IOException;

/**
 * Created by yqritc on 2015/06/14.
 */
public class SampleAdapter extends RecyclerView.Adapter<SampleAdapter.ViewHolder> {

    @RawRes
    private int mVideoResId;
    private LayoutInflater mLayoutInflater;

    public SampleAdapter(Context context) {
        super();
        mVideoResId = R.raw.landscape_sample;
        mLayoutInflater = LayoutInflater.from(context);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mLayoutInflater.inflate(R.layout.layout_sample_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Context context = holder.itemView.getContext();

        ScalableType scalableType = ScalableType.values()[position];
        holder.mTextView.setText(context.getString(R.string.sample_scale_title, position,
                scalableType.toString()));
        holder.setScalableType(scalableType);
    }

    @Override
    public void onViewAttachedToWindow(ViewHolder holder) {
        setVideo(holder.mVideoView);
        holder.mVideoView.setScalableType(holder.mScalableType);
    }

    private void setVideo(final ScalableVideoView videoView) {
        try {
            videoView.setRawData(mVideoResId);
            videoView.setVolume(0, 0);
            videoView.setLooping(true);
            videoView.prepare(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    videoView.start();
                }
            });
        } catch (IOException ioe) {
            //ignore
        }
    }

    @Override
    public int getItemCount() {
        return ScalableType.values().length;
    }

    public void setVideoResId(@RawRes int id) {
        mVideoResId = id;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView mTextView;
        ScalableVideoView mVideoView;
        ScalableType mScalableType;

        public ViewHolder(View view) {
            super(view);
            mTextView = (TextView) view.findViewById(R.id.video_text);
            mVideoView = (ScalableVideoView) view.findViewById(R.id.video_view);
        }

        public void setScalableType(ScalableType type) {
            mScalableType = type;
        }
    }
}
