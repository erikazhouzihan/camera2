/*
 * Copyright 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.camera2basic;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.TextureView;


/**
 * A {@link TextureView} that can be adjusted to a specified aspect ratio.
 */
public class AutoFitTextureView1 extends TextureView {

    private int mRatioWidth = 0;
    private int mRatioHeight = 0;
    private float mOldDistance;
    private static final String TAG = "AutoFitTextureView1";
    Camera2VideoFragment camera2VideoFragment = Camera2VideoFragment.newInstance();

    public AutoFitTextureView1(Context context) {
        this(context, null);
    }

    public AutoFitTextureView1(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        init(context);
    }

    public AutoFitTextureView1(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    private void init(Context context) {
        setSurfaceTextureListener(mSurfaceTextureListener);
        camera2VideoFragment = Camera2VideoFragment.newInstance();
    }
    private SurfaceTextureListener mSurfaceTextureListener = new SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            Log.v(TAG, "onSurfaceTextureAvailable. width: " + width + ", height: " + height);
            camera2VideoFragment.openCamera(width, height);
            //camera2BasicFragment.setPreviewSurface(surface);
            // resize TextureView
            int previewWidth = camera2VideoFragment.getPreviewSize().getWidth();
            int previewHeight = camera2VideoFragment.getPreviewSize().getHeight();
            if (width > height) {
                setAspectRatio(previewWidth, previewHeight);
            } else {
                setAspectRatio(previewHeight, previewWidth);
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            Log.v(TAG, "onSurfaceTextureSizeChanged. width: " + width + ", height: " + height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            Log.v(TAG, "onSurfaceTextureDestroyed");
            camera2VideoFragment.closeCamera();
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };
    /**
     * Sets the aspect ratio for this view. The size of the view will be measured based on the ratio
     * calculated from the parameters. Note that the actual sizes of parameters don't matter, that
     * is, calling setAspectRatio(2, 3) and setAspectRatio(4, 6) make the same result.
     *
     * @param width  Relative horizontal size
     * @param height Relative vertical size
     */
    public void setAspectRatio(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Size cannot be negative.");
        }
        mRatioWidth = width;
        mRatioHeight = height;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (0 == mRatioWidth || 0 == mRatioHeight) {
            setMeasuredDimension(width, height);
        } else {
            if (width < height * mRatioWidth / mRatioHeight) {
                setMeasuredDimension(width, width * mRatioHeight / mRatioWidth);
            } else {
                setMeasuredDimension(height * mRatioWidth / mRatioHeight, height);
            }
        }
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        System.out.println("touch+++++++++++++++++++++++++++++++++");
        System.out.println("getPointerCount = "+event.getPointerCount());
        if (event.getPointerCount() == 2) { // 当触碰点有2个时，才去放大缩小
            System.out.println("MotionEvent.ACTION_MOVE"+MotionEvent.ACTION_MOVE);
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_POINTER_DOWN:
                    // 点下时，得到两个点间的距离为mOldDistance
                    mOldDistance = getFingerSpacing(event);
                    break;
                case MotionEvent.ACTION_MOVE:
                    // 移动时，根据距离是变大还是变小，去放大还是缩小预览画面
                    float newDistance = getFingerSpacing(event);
                    if (newDistance > mOldDistance) {
                        camera2VideoFragment.handleZoom(true);
                    } else if (newDistance < mOldDistance) {
                        camera2VideoFragment.handleZoom(false);
                    }
                    // 更新mOldDistance
                    mOldDistance = newDistance;
                    break;
                default:
                    break;
            }
        }
        return true;
    }
    private static float getFingerSpacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }


}
