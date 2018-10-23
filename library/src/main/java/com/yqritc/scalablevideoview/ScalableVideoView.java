package com.yqritc.scalablevideoview;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.content.res.TypedArray;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RawRes;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.TextureView;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.Map;

/**
 * Created by yqritc on 2015/06/11.
 * Updated by 4eRTuk on 2018/10/23.
 */
public class ScalableVideoView extends TextureView implements TextureView.SurfaceTextureListener,
        MediaPlayer.OnVideoSizeChangedListener {

    protected MediaPlayer mMediaPlayer;
    protected MediaPlayer.OnErrorListener mErrorListener;
    protected MediaPlayer.OnCompletionListener mCompletionListener;
    protected MediaPlayer.OnPreparedListener mPrepareListener;
    protected MediaPlayer.OnInfoListener mInfoListener;
    protected int mLatestPosition;
    protected int mAssetId = -1;
    protected long mOffset;
    protected long mLength;
    protected FileDescriptor mFileDescriptor;
    protected Context mContext;
    protected Map<String, String> mHeaders;
    protected Uri mUri;
    protected String mAssetName;
    protected String mFilePath;
    protected ScalableType mScalableType = ScalableType.NONE;

    public ScalableVideoView(Context context) {
        this(context, null);
    }

    public ScalableVideoView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScalableVideoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        if (attrs == null) {
            return;
        }

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.scaleStyle, 0, 0);
        if (a == null) {
            return;
        }

        int scaleType = a.getInt(R.styleable.scaleStyle_scalableType, ScalableType.NONE.ordinal());
        a.recycle();
        mScalableType = ScalableType.values()[scaleType];
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        Surface surface = new Surface(surfaceTexture);
        if (mMediaPlayer != null) {
            mMediaPlayer.setSurface(surface);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (isDataSet()) {
            initializeMediaPlayer();
            try {
                if (mFilePath != null)
                    setDataSource(mFilePath);
                else if (mAssetId >= 0)
                    setRawData(mAssetId);
                else if (mAssetName != null)
                    setAssetData(mAssetName);
                else if (mFileDescriptor != null) {
                    if (mOffset > 0 || mLength > 0)
                        setDataSource(mFileDescriptor, mOffset, mLength);
                    else
                        setDataSource(mFileDescriptor);
                } else if (mUri != null && mContext != null) {
                    if (mHeaders != null)
                        setDataSource(mContext, mUri, mHeaders);
                    else
                        setDataSource(mContext, mUri);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            setOnCompletionListener(mCompletionListener);
            setOnErrorListener(mErrorListener);
            setOnInfoListener(mInfoListener);
            if (mPrepareListener != null)
                prepareAsync(mPrepareListener);
            mMediaPlayer.seekTo(mLatestPosition);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mMediaPlayer == null) {
            return;
        }

        if (isPlaying()) {
            stop();
        }
        mLatestPosition = mMediaPlayer.getCurrentPosition();
        release();
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        scaleVideoSize(width, height);
    }

    private void scaleVideoSize(int videoWidth, int videoHeight) {
        if (videoWidth == 0 || videoHeight == 0) {
            return;
        }

        Size viewSize = new Size(getWidth(), getHeight());
        Size videoSize = new Size(videoWidth, videoHeight);
        ScaleManager scaleManager = new ScaleManager(viewSize, videoSize);
        Matrix matrix = scaleManager.getScaleMatrix(mScalableType);
        if (matrix != null) {
            setTransform(matrix);
        }
    }

    private void initializeMediaPlayer() {
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setOnVideoSizeChangedListener(this);
            setSurfaceTextureListener(this);
        } else {
            reset();
        }
    }

    private boolean isDataSet() {
        return mAssetId >= 0 || mAssetName != null || mFilePath != null || mUri != null || mFileDescriptor != null;
    }

    private void clearData() {
        mAssetId = -1;
        mAssetName = null;
        mFilePath = null;
        mContext = null;
        mUri = null;
        mHeaders = null;
        mFileDescriptor = null;
        mLength = 0;
        mOffset = 0;
    }

    public void setRawData(@RawRes int id) throws IOException {
        AssetFileDescriptor afd = getResources().openRawResourceFd(id);
        setDataSource(afd);
        clearData();
        mAssetId = id;
    }

    public void setAssetData(@NonNull String assetName) throws IOException {
        AssetManager manager = getContext().getAssets();
        AssetFileDescriptor afd = manager.openFd(assetName);
        setDataSource(afd);
        clearData();
        mAssetName = assetName;
    }

    private void setDataSource(@NonNull AssetFileDescriptor afd) throws IOException {
        setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
        afd.close();
    }

    public void setDataSource(@NonNull String path) throws IOException {
        initializeMediaPlayer();
        mMediaPlayer.setDataSource(path);
        clearData();
        mFilePath = path;
    }

    public void setDataSource(@NonNull Context context, @NonNull Uri uri,
            @Nullable Map<String, String> headers) throws IOException {
        initializeMediaPlayer();
        mMediaPlayer.setDataSource(context, uri, headers);
        clearData();
        mContext = context;
        mUri = uri;
        mHeaders = headers;
    }

    public void setDataSource(@NonNull Context context, @NonNull Uri uri) throws IOException {
        initializeMediaPlayer();
        mMediaPlayer.setDataSource(context, uri);
        clearData();
        mContext = context;
        mUri = uri;
    }

    public void setDataSource(@NonNull FileDescriptor fd, long offset, long length)
            throws IOException {
        initializeMediaPlayer();
        mMediaPlayer.setDataSource(fd, offset, length);
        clearData();
        mOffset = offset;
        mLength = length;
        mFileDescriptor = fd;
    }

    public void setDataSource(@NonNull FileDescriptor fd) throws IOException {
        initializeMediaPlayer();
        mMediaPlayer.setDataSource(fd);
        clearData();
        mFileDescriptor = fd;
    }

    public void setScalableType(ScalableType scalableType) {
        mScalableType = scalableType;
        scaleVideoSize(getVideoWidth(), getVideoHeight());
    }

    public void prepare(@Nullable MediaPlayer.OnPreparedListener listener)
            throws IOException, IllegalStateException {
        mMediaPlayer.setOnPreparedListener(listener);
        mMediaPlayer.prepare();
        mPrepareListener = listener;
    }

    public void prepareAsync(@Nullable MediaPlayer.OnPreparedListener listener)
            throws IllegalStateException {
        mMediaPlayer.setOnPreparedListener(listener);
        mMediaPlayer.prepareAsync();
        mPrepareListener = listener;
    }

    public void prepare() throws IOException, IllegalStateException {
        prepare(null);
    }

    public void prepareAsync() throws IllegalStateException {
        prepareAsync(null);
    }

    public void setOnErrorListener(@Nullable MediaPlayer.OnErrorListener listener) {
        mMediaPlayer.setOnErrorListener(listener);
        mErrorListener = listener;
    }

    public void setOnCompletionListener(@Nullable MediaPlayer.OnCompletionListener listener) {
        mMediaPlayer.setOnCompletionListener(listener);
        mCompletionListener = listener;
    }

    public void setOnInfoListener(@Nullable MediaPlayer.OnInfoListener listener) {
        mMediaPlayer.setOnInfoListener(listener);
        mInfoListener = listener;
    }

    public int getCurrentPosition() {
        return mMediaPlayer.getCurrentPosition();
    }

    public int getDuration() {
        return mMediaPlayer.getDuration();
    }

    public int getVideoHeight() {
        return mMediaPlayer.getVideoHeight();
    }

    public int getVideoWidth() {
        return mMediaPlayer.getVideoWidth();
    }

    public boolean isLooping() {
        return mMediaPlayer.isLooping();
    }

    public boolean isPlaying() {
        return mMediaPlayer.isPlaying();
    }

    public void pause() {
        mMediaPlayer.pause();
    }

    public void seekTo(int msec) {
        mMediaPlayer.seekTo(msec);
    }

    public void setLooping(boolean looping) {
        mMediaPlayer.setLooping(looping);
    }

    public void setVolume(float leftVolume, float rightVolume) {
        mMediaPlayer.setVolume(leftVolume, rightVolume);
    }

    public void start() {
        mMediaPlayer.start();
    }

    public void stop() {
        mMediaPlayer.stop();
    }

    public void reset() {
        mMediaPlayer.reset();
    }

    public void release() {
        reset();
        mMediaPlayer.release();
        mMediaPlayer = null;
    }
}
