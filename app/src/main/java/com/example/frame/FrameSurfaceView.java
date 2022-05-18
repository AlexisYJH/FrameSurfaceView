package com.example.frame;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.AttributeSet;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class FrameSurfaceView extends BaseSurfaceView{
    private static final String DECODE_THREAD_NAME = "DecodingThread";
    private static final int BUFFER_SIZE = 2;
    private static final int INITIAL_INDEX = -1;
    private static final int START_INDEX = 0;
    public static final int INFINITE = -1;
    private HandlerThread mDecodeThread;
    private Handler mHandler;
    private BitmapFactory.Options mOptions;
    private Paint mPaint;
    private Rect mSrcRect, mDstRect;
    private int mDefaultWidth, mDefaultHeight;
    private AtomicInteger mFrameIndex;

    /**
     * decoded bitmaps stores in this queue
     * consumer is drawing thread, producer is decoding thread.
     */
    private LinkedBlockingQueue<Bitmap> mDecodedBitmaps = new LinkedBlockingQueue(BUFFER_SIZE);
    private LinkedBlockingQueue<Bitmap> mDrawnBitmaps = new LinkedBlockingQueue(BUFFER_SIZE);

    private ArrayList<Integer> mDrawableIds;
    private int mDrawableIndex;
    private DecodeRunnable mDecodeRunnable;
    private int mRepeatTimes;
    private int mRepeatedCount;

    public FrameSurfaceView(Context context) {
        super(context);
    }

    public FrameSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FrameSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onInit() {
        mOptions = new BitmapFactory.Options();
        mOptions.inMutable = true;
        mPaint = new Paint();
        mDstRect = new Rect();
        mFrameIndex = new AtomicInteger(INITIAL_INDEX);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mDstRect.set(0, 0, getWidth(), getHeight());
    }

    @Override
    protected int getDefaultWidth() {
        return mDefaultWidth;
    }

    @Override
    protected int getDefaultHeight() {
        return mDefaultHeight;
    }

    @Override
    protected void onFrameDraw(Canvas canvas) {
        clearCanvas(canvas);
        if (!isStart()) {
            return;
        }
        drawFrame(canvas);
    }

    private void drawFrame(Canvas canvas) {
        Bitmap bitmap = null;
        try {
            bitmap = mDecodedBitmaps.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (bitmap != null) {
            canvas.drawBitmap(bitmap, mSrcRect, mDstRect, mPaint);
            mDrawnBitmaps.offer(bitmap);
            mFrameIndex.incrementAndGet();
        }
    }

    private void clearCanvas(Canvas canvas) {
        mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        canvas.drawPaint(mPaint);
        mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
    }

    //whether frame animation is started
    private boolean isStart() {
        return mFrameIndex.get() != INITIAL_INDEX;
    }

    //whether frame animation is finished
    private boolean isFinish() {
        return mFrameIndex.get() > mDrawableIds.size() - 1;
    }

    private void reset() {
        mFrameIndex.set(INITIAL_INDEX);
    }

    //start frame animation from the first frame
    public void start() {
        mFrameIndex.compareAndSet(INITIAL_INDEX, START_INDEX);
        if (mDecodeThread == null) {
            mDecodeThread = new HandlerThread(DECODE_THREAD_NAME);
        }
        if (!mDecodeThread.isAlive()) {
            mDecodeThread.start();
        }
        if (mHandler == null) {
            mHandler = new Handler(mDecodeThread.getLooper());
        }
        if (mDecodeRunnable != null) {
            mDecodeRunnable.setIndex(0);
        }
        mHandler.post(mDecodeRunnable);
    }

    @Override
    protected void onFrameDrawFinish() {
        if (isFinish()) {
            reset();
            if (mRepeatTimes == INFINITE) {
                start();
            } else if (mRepeatedCount < mRepeatTimes) {
                start();
                mRepeatedCount++;
            } else {
                stopDrawThread();
            }
        }
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        super.surfaceDestroyed(holder);
        destroy();
    }

    private void destroy() {
        //super.destroy();
        if (mDecodedBitmaps != null) {
            while(!mDecodedBitmaps.isEmpty()) {
                try {
                    Bitmap bitmap = mDecodedBitmaps.take();
                    bitmap.recycle();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        if (mDrawnBitmaps != null) {
            while(!mDrawnBitmaps.isEmpty()) {
                try {
                    Bitmap bitmap = mDrawnBitmaps.take();
                    bitmap.recycle();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        if (mDecodeThread != null) {
            mDecodeThread.quit();
            mDecodeThread = null;
        }
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
    }


    public void setDrawableIds(ArrayList<Integer> ids){
        if (ids == null || ids.size() == 0) {
            return;
        }
        mDrawableIds = ids;
        mDrawableIndex = 0;
        getDrawableSize(mDrawableIds.get(mDrawableIndex));
        preloadFrames();
        mDecodeRunnable = new DecodeRunnable(mDrawableIndex, mDrawableIds.size());
    }


    private void getDrawableSize(int id) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(getResources(), id, options);
        mDefaultWidth = options.outWidth;
        mDefaultHeight = options.outHeight;
        mSrcRect = new Rect(0, 0, mDefaultWidth, mDefaultHeight);
        //we have to re-measure to make defaultWidth in use in onMeasure()
        requestLayout();
    }

    private void preloadFrames() {
        for (int i = 0; i < BUFFER_SIZE; i++) {
            decodeAndCacheBitmap(mDrawableIds.get(mDrawableIndex++));
        }
    }


    public void setRepeatTimes(int repeatTimes) {
        this.mRepeatTimes = Math.max(INFINITE, repeatTimes);
    }

    //reuse bitmap in mDrawnBitmaps to decode new bitmap
    private void decodedBitmapByReuse(int index) {
        try {
            mOptions.inBitmap = mDrawnBitmaps.take();
            decodeAndCacheBitmap(mDrawableIds.get(index));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //decode bitmap and put it into mDecodedBitmaps
    private void decodeAndCacheBitmap(int resId) {
        InputStream in = getResources().openRawResource(resId);
        //decode bitmap by BitmapFactory.decodeStream(), it is about twice faster than BitmapFactory.decodeResource()
        Bitmap bitmap = BitmapFactory.decodeStream(in, null, mOptions);
        try {
            mDecodedBitmaps.put(bitmap);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private class DecodeRunnable implements Runnable {
        private int mIndex, mSize;

        public DecodeRunnable(int index, int size) {
            this.mIndex = index;
            this.mSize = size;
        }

        public void setIndex(int index) {
            this.mIndex = index;
        }

        @Override
        public void run() {
            decodedBitmapByReuse(mIndex);
            mIndex++;
            if (mIndex < mSize) {
                mHandler.post(this);
            } else {
                mIndex = 0;
            }
        }
    }
}
