package com.example.frame;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

public abstract class BaseSurfaceView extends SurfaceView implements SurfaceHolder.Callback {
    private static final String THREAD_NAME = "SurfaceViewThread";
    private static final int FRAME_DURATION = 200;
    //用于计算帧数据的线程
    private HandlerThread mHandlerThread;
    private Handler mHandler;
    private boolean mIsAlive;
    private int mDuration = FRAME_DURATION;

    public BaseSurfaceView(Context context) {
        this(context, null);
    }

    public BaseSurfaceView(Context context, AttributeSet attrs) {
        this(context, attrs , 0);
    }

    public BaseSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        getHolder().addCallback(this);
        //设置透明背景，否则SurfaceView背景是黑的
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
        setZOrderOnTop(true);
        onInit();
    }

    protected void onInit(){}

    protected void setFrameDuration(int duration) {
        mDuration = duration;
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        mIsAlive = true;
        startDrawThread();
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        stopDrawThread();
        mIsAlive = false;
    }

    //启动帧绘制线程
    private void startDrawThread(){
        mHandlerThread = new HandlerThread(THREAD_NAME);
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
        mHandler.post(new DrawRunnable());
    }

    //停止帧绘制线程
    public void stopDrawThread() {
        if (mHandlerThread != null) {
            mHandlerThread.quit();
        }
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int width = widthMode == MeasureSpec.AT_MOST ? getDefaultWidth() : widthSize;
        int height = heightMode == MeasureSpec.AT_MOST ? getDefaultHeight() : heightSize;
        setMeasuredDimension(width, height);
    }

    protected abstract int getDefaultWidth();
    protected abstract int getDefaultHeight();
    protected abstract void onFrameDraw(Canvas canvas);
    protected abstract void onFrameDrawFinish();

    private class DrawRunnable implements Runnable{
        @Override
        public void run() {
            if (mIsAlive) {
                //1.获取画布
                Canvas canvas = getHolder().lockCanvas();
                try {
                    //2.绘制一帧
                    onFrameDraw(canvas);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    //3.将帧数据提交
                    getHolder().unlockCanvasAndPost(canvas);
                    //4.一帧绘制结束
                    onFrameDrawFinish();
                }
                //不停的将自己推送到绘制线程的消息队列以实现帧刷新
                if (mHandler != null) {
                    mHandler.postDelayed(this, mDuration);
                }
            }
        }
    }
}
