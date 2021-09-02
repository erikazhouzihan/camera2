/*
 * Copyright 2017 The Android Open Source Project
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

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ExifInterface;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaActionSound;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static android.os.Environment.DIRECTORY_DCIM;


public class Camera2BasicFragment extends Fragment
        implements View.OnClickListener, ActivityCompat.OnRequestPermissionsResultCallback {

    /**
     * Conversion from screen rotation to JPEG orientation.从屏幕旋转到 JPEG 方向的转换。
     */
    private static final Camera2BasicFragment camera2BasicFragment = new Camera2BasicFragment();
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final String FRAGMENT_DIALOG = "dialog";
    private static final String[] VIDEO_PERMISSIONS = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private static final int VIDEO_PERMISSIONS_CODE = 1;
    private ImageView imageView;
    public Activity activity;
    private boolean isDelay = false;
    private Button delayBtn;
    private int delaystate = 0;
    private LinearLayout btn_delay_layout;
    private Button btn_delay_setting;
    private Button btn_change_preview;
    private LinearLayout btn_Paint;
    private short mDelayTime;
    public static final int TIME_INTERVAL = 1000;
    private TextView mTimeText;
    public  int mRotation;
    boolean isFirstStart = true;
    MyOrientationListener listener;
    private float mOldDistance;
    public CameraCharacteristics characteristics;
    private int mZoom = 0; // 缩放
    private CameraManager mCameraManager;
    public int delayZero = 0;
    public List<Size> outputsizes;
    public int changePreview;
    public String mCameraId = "0";
    String PaintFlag = "4:3";
    SurfaceTexture texture;
    private Bitmap selectdBitmap;
    private  int REQUEST_GET_IMAGE = 1;
    private  int MAX_SIZE = 769;
    //设置两套角度，用于前置和后置摄像头拍照
    private void Orientations() {
        //前置时，照片竖直显示
        if (mCameraId.equals("1")) {
            ORIENTATIONS.append(Surface.ROTATION_0, 270);
            ORIENTATIONS.append(Surface.ROTATION_90, 0);
            ORIENTATIONS.append(Surface.ROTATION_180, 90);
            ORIENTATIONS.append(Surface.ROTATION_270, 180);
        } else {
            ORIENTATIONS.append(Surface.ROTATION_0, 90);
            ORIENTATIONS.append(Surface.ROTATION_90, 0);
            ORIENTATIONS.append(Surface.ROTATION_180, 270);
            ORIENTATIONS.append(Surface.ROTATION_270, 180);
        }

    }


    /**
     * Tag for the {@link Log}.{@link } 的标签。
     */
    public static final String TAG = "camera2BasicFragment";

    /**
     * Camera state: Showing camera preview.相机状态：显示相机预览。
     */
    private static final int STATE_PREVIEW = 0;

    /**
     * Camera state: Waiting for the focus to be locked.相机状态：等待对焦锁定。
     */
    private static final int STATE_WAITING_LOCK = 1;

    /**
     * Camera state: Waiting for the exposure to be precapture state.相机状态：等待曝光为预捕获状态。
     */
    private static final int STATE_WAITING_PRECAPTURE = 2;

    /**
     * Camera state: Waiting for the exposure state to be something other than precapture.相机状态：等待曝光状态不是预捕获
     */
    private static final int STATE_WAITING_NON_PRECAPTURE = 3;

    /**
     * Camera state: Picture was taken.相机状态：照片已拍摄。
     */
    private static final int STATE_PICTURE_TAKEN = 4;

    /**
     * Max preview width that is guaranteed by Camera2 API. Camera2 API 保证的最大预览宽度
     */
    private static final int MAX_PREVIEW_WIDTH = 1920;

    /**
     * Max preview height that is guaranteed by Camera2 API. Camera2 API 保证的最大预览高度
     */
    private static final int MAX_PREVIEW_HEIGHT = 1080;


    private static int count = 0;
    public static final String GALLERY_Path = DIRECTORY_DCIM + File.separator + "test";
    /**
     * {@link TextureView.SurfaceTextureListener} handles several lifecycle events on a
     * {@link TextureView}.{@link TextureView.SurfaceTextureListener} 在 * {@link TextureView} 上处理多个生命周期事件。
     */
    private final TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        //打开预览的回调函数
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            openCamera(width,height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
//            configureTransform(width, height);
            Log.e(TAG, "onSurfaceTextureSizeChanged: " + width + "X"+height );
            mTextureView.setAspectRatio(mPreviewSize.getWidth(),mPreviewSize.getHeight());
//            if(isNeedRatio){
//                isNeedRatio = false;
//                Reopen();
//            }

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }

    };

    /**
     * ID of the current {@link CameraDevice}.当前相机的ID
     */

    /**
     * An {@link AutoFitTextureView} for camera preview.为相机预览界面创建一个AutoFitTextureView
     */
    //用来显示的页面
    private AutoFitTextureView mTextureView;

    /**
     * A {@link CameraCaptureSession } for camera preview.为相机预览创建一个CameraCaptureSession
     */
    //CameraCaptureSession的实例用于创建session对象来控制预览和拍照
    private CameraCaptureSession mCaptureSession;

    /**
     * A reference to the opened {@link CameraDevice}.
     */
    //定义的CameraDevice对象
    private CameraDevice mCameraDevice;

    /**
     * The {@link android.util.Size} of camera preview.
     */
    //用于预览的尺码参数
    private Size mPreviewSize;

    /**
     * {@link CameraDevice.StateCallback} is called when {@link CameraDevice} changes its state.
     */
    //用于接收有关摄像机设备状态的更新的回调对象。
    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        //摄像头打开时激发该方法
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            System.out.println("onOpened方法调用了");
            // This method is called when the camera is opened.  We start camera preview here.
            mCameraDevice = cameraDevice;
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            createCameraPreviewSession();
            mCameraOpenCloseLock.release();


        }

        @Override
        //摄像头断开连接时激发该方法
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            System.out.println("onDisconnected方法调用了");
            mCameraOpenCloseLock.release();
//            cameraDevice.close();
        }

        @Override
        //摄像头出现错误时激发该方法
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            System.out.println("onError方法调用了");
            mCameraOpenCloseLock.release();
            Log.i(TAG, "onError: mCameraDevice = null");
            cameraDevice.close();
            mCameraDevice = null;
            Activity activity = getActivity();
            if (null != activity) {
                activity.finish();
            }
        }

    };

    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    //用于运行不应阻塞 UI 的任务的附加线程。
    private HandlerThread mBackgroundThread;

    /**
     * A {@link Handler} for running tasks in the background.
     */
    //用于在后台运行任务的 Handler
    private Handler mBackgroundHandler;

    /**
     * An {@link ImageReader} that handles still image capture.
     */
    //处理静态图像捕获的imagereader类
    private ImageReader mImageReader;

    /**
     * This is the output file for our picture.
     */
    //这是我们图片的输出文件。
    private File mFile;


    /**
     * This a callback object for the {@link ImageReader}. "onImageAvailable" will be called when a
     * still image is ready to be saved.
     */
    //这是 ImageReader 的回调对象。当准备好保存静止图像时，将调用“onImageAvailable”。
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            System.out.println("mOnImageAvailableListener方法调用了");
            long cTIme = System.currentTimeMillis();
            mFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + File.separator + "Camera", cTIme + ".jpg");
            Log.e(TAG, "onImageAvailable: " + mFile);
            mBackgroundHandler.post(new ImageSaver(reader.acquireNextImage(), mFile));


        }

    };

    /**
     * {@link CaptureRequest.Builder} for the camera preview
     */
    //用于相机预览的CaptureRequest.Builder,他可以生成CaptureRequest对象（用来设置捕获照片的各种参数）
    private CaptureRequest.Builder mPreviewRequestBuilder;

    /**
     * {@link CaptureRequest} generated by {@link #mPreviewRequestBuilder}
     */

    private CaptureRequest mPreviewRequest;

    /**
     * The current state of camera state for taking pictures.
     *
     * @see #mCaptureCallback
     */
    //用于拍照的相机状态的当前状态。
    private int mState = STATE_PREVIEW;

    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the camera.
     */
    //一个 {@link Semaphore} 以防止应用程序在关闭相机之前退出。
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    /**
     * Whether the current camera device supports Flash or not.
     */
    //当前的摄像头设备是否支持闪光灯。
    private boolean mFlashSupported;

    /**
     * Orientation of the camera sensor
     */
    //相机传感器的方向
    private int mSensorOrientation;

    /**
     * A {@link CameraCaptureSession.CaptureCallback} that handles events related to JPEG capture.
     */
    //处理与 JPEG 捕获相关的事件的 {@link CameraCaptureSession.CaptureCallback}。
    private CameraCaptureSession.CaptureCallback mCaptureCallback
            = new CameraCaptureSession.CaptureCallback() {

        private void process(CaptureResult result) {
            switch (mState) {
                case STATE_PREVIEW: {
                    break;
                }
                //等待对焦锁定
                case STATE_WAITING_LOCK: {
                    //自动聚焦状态
                    if(mCameraId.equals("1")){
                        mState = STATE_PICTURE_TAKEN;
                        captureStillPicture();
                        startPreview();
                    }
                    Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                    Log.e(TAG, "process: " + afState );
                    if (afState == null) {
                        captureStillPicture();
                    } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                            CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                        // CONTROL_AE_STATE can be null on some devices
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeState == null ||
                                aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            mState = STATE_PICTURE_TAKEN;
                            int d = 0;
                            d = d + 1;
                            captureStillPicture();
                            startPreview();
                        } else {
                            runPrecaptureSequence();
                        }
                    }
                    break;
                }
                case STATE_WAITING_PRECAPTURE: {
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null ||
                            aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                            aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        mState = STATE_WAITING_NON_PRECAPTURE;
                    }
                    break;
                }
                case STATE_WAITING_NON_PRECAPTURE: {
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        mState = STATE_PICTURE_TAKEN;
                        captureStillPicture();
                    }
                    break;
                }
            }
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull CaptureResult partialResult) {
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {

            process(result);
        }

    };

    /**
     * Shows a {@link Toast} on the UI thread.
     *
     * @param text The message to show
     */
    private void showToast(final String text) {
        final Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * Given {@code choices} of {@code Size}s supported by a camera, choose the smallest one that
     * is at least as large as the respective texture view size, and that is at most as large as the
     * respective max size, and whose aspect ratio matches with the specified value. If such size
     * doesn't exist, choose the largest one that is at most as large as the respective max size,
     * and whose aspect ratio matches with the specified value.
     *
     * @param choices           The list of sizes that the camera supports for the intended output
     *                          class
     * @param textureViewWidth  The width of the texture view relative to sensor coordinate
     * @param textureViewHeight The height of the texture view relative to sensor coordinate
     * @param maxWidth          The maximum width that can be chosen
     * @param maxHeight         The maximum height that can be chosen
     * @param aspectRatio       The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth,
                                          int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {

        // Collect the supported resolutions that are at least as big as the preview Surface
        //收集至少与预览 Surface 一样大的受支持分辨率
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        //收集支持的小于预览 Surface 的分辨率
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        //选择那些足够大的最小的。如果没有足够大的，则从那些不够大的中选择最大的。
        if (bigEnough.size() > 0) {
            System.out.println("++++++++++++++++++++"+Collections.min(bigEnough, new CompareSizesByArea()));
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            System.out.println("++++++++++++++++++++"+Collections.max(notBigEnough, new CompareSizesByArea()));
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            System.out.println("++++++++++++++++++++"+choices[0]);
            return choices[0];
        }
    }

    public static Camera2BasicFragment newInstance() {
        return camera2BasicFragment;
    }

    @Override
    public void onAttach(Context context) {
        System.out.println("onAttach方法调用了====================");
        super.onAttach(context);
        this.activity = (Activity) context;
        listener = new MyOrientationListener(context);
        mCameraManager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        listener.enable();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
//        requestPermission();
        System.out.println("onCreateView方法调用了====================");
        return inflater.inflate(R.layout.fragment_camera2_basic, container, false);
    }

//    CameraActivity cameraActivity;
    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
//        cameraActivity = new CameraActivity();

        System.out.println("onViewCreated方法调用了====================");
        view.findViewById(R.id.picture).setOnClickListener(this);
        view.findViewById(R.id.exchangeCamera2).setOnClickListener(this);
        view.findViewById(R.id.to_recorder).setOnClickListener(this);
        imageView = view.findViewById(R.id.image);
        imageView.setOnClickListener(this);
        //延迟
        btn_delay_setting = view.findViewById(R.id.btn_delay_setting);
        btn_delay_setting.setOnClickListener(this);
        btn_change_preview = view.findViewById(R.id.btn_change_preview);
        btn_change_preview.setOnClickListener(this);
        mTimeText = (TextView) view.findViewById(R.id.timer_text);

        btn_delay_layout = view.findViewById(R.id.btn_delay_layout);
        view.findViewById(R.id.btn_delay3).setOnClickListener(this);
        view.findViewById(R.id.btn_delay5).setOnClickListener(this);
        view.findViewById(R.id.btn_delay10).setOnClickListener(this);
        btn_Paint = view.findViewById(R.id.btn_Paint);
        view.findViewById(R.id.btn_Paint_1).setOnClickListener(this);
        view.findViewById(R.id.btn_Paint_4).setOnClickListener(this);
        view.findViewById(R.id.btn_Paint_16).setOnClickListener(this);


        delayBtn = view.findViewById(R.id.btn_delay);
        isDelay = (delaystate == 1);
        if (isDelay)
            delayBtn.setBackground(getContext().getResources().getDrawable(R.drawable.is_selected));//查询是否开启延迟拍照
        delayBtn.setOnClickListener(this);
        mTextureView = (AutoFitTextureView) view.findViewById(R.id.texture);
        long cTIme = System.currentTimeMillis();

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        System.out.println("onActivityCreated方法调用了====================");
        super.onActivityCreated(savedInstanceState);

    }
    private float ratio4_3 = (float) 4 / 3;
    private float ratio1_1 = (float) 1 / 1;
    private float ratio16_9 = (float) 18 / 9;
    @Override
    public void onResume() {
        super.onResume();
        try {
            setOrientations();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        PaintFlag = "4:3";
        System.out.println("onResume方法调用了====================");
        startBackgroundThread();
        mPreviewSize = findBestPreviewSize(ratio4_3);
        mTextureView.setAspectRatio(mPreviewSize.getWidth(),mPreviewSize.getHeight());

        if (mTextureView.isAvailable()) {
            Log.e(TAG, "onResume:__1 " );
            btn_change_preview.setText(PaintFlag);
            openCamera(mTextureView.getWidth(),mTextureView.getHeight());
        } else {
            Log.e(TAG, "onResume:__2 " );
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    public void onPause() {
        System.out.println("onPause方法调用了====================");
        super.onPause();
        PaintFlag = "4:3";
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        listener.disable();
        System.out.println("onDestroyView方法调用了=============mCameraDevice = null");
        if (null != mCameraDevice) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        if (null != mCaptureSession) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
        stopBackgroundThread();
    }

    private void requestCameraPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            new ConfirmationDialog().show(getChildFragmentManager(), FRAGMENT_DIALOG);
        } else {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }


    /**
     * Sets up member variables related to camera.设置与相机相关的成员变量。
     *
     *
     */
    @SuppressWarnings("SuspiciousNameCombination")
    private void setUpCameraOutputs() {
        System.out.println("setUpCameraOutputs()方法调用了");
        Activity activity = getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
                characteristics = manager.getCameraCharacteristics(mCameraId);
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                mPreviewSize = chooseImageSize(map.getOutputSizes(ImageReader.class));
                mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                if (map == null) {
                    throw new RuntimeException("Cannot get available preview/video sizes");
                }
            Log.e(TAG, "setUpCameraOutputs: "+ mPreviewSize.getWidth() + "X" + mPreviewSize.getHeight());
                int orientation = getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    mTextureView.setAspectRatio(mPreviewSize.getWidth(),mPreviewSize.getHeight());
                } else {
                    mTextureView.setAspectRatio(mPreviewSize.getHeight(),mPreviewSize.getWidth());
                }
                mImageReader = ImageReader.newInstance(mTextureView.getWidth(), mTextureView.getHeight(),
                        ImageFormat.JPEG, /*maxImages*/2);

                mImageReader.setOnImageAvailableListener(
                        mOnImageAvailableListener, mBackgroundHandler);
                //noinspection ConstantConditions
                //没有检查恒定条件//ConstantConditions

                Point displaySize = new Point();
//                activity.getWindowManager().getDefaultDisplay().getSize(displaySize);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            ErrorDialog.newInstance(getString(R.string.camera_error))
                    .show(getChildFragmentManager(), FRAGMENT_DIALOG);
        }
    }

    /**
     * Opens the camera specified by {@link Camera2BasicFragment#mCameraId}.打开由 {@link Camera2BasicFragment#mCameraId} 指定的相机。
     */
    //打开由 {@link Camera2BasicFragment#mCameraId} 指定的相机
    void openCamera(int width,int height) {
        System.out.println("openCamera方法调用了");
        Activity activity = getActivity();
        if (null == activity || activity.isFinishing()) {
            return;
        }
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();
            return;
        }

        try {
            Log.d(TAG, "tryAcquire");
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            manager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            Toast.makeText(activity, "Cannot access the camera.", Toast.LENGTH_SHORT).show();
            activity.finish();
          throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.");
        }
    }


    private Size findBestPreviewSize(float ratio) {
        //configure surfaces

        int mPreviewWidth = 0;
        int mPreviewHeight = 0;
        try {
            characteristics = mCameraManager.getCameraCharacteristics(mCameraId);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        if (characteristics != null) {
            StreamConfigurationMap streamMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size[] supportSizes = streamMap.getOutputSizes(ImageFormat.JPEG);

            float tolerant = 0.05f;
            int fullScreenWidth = getActivity().getResources().getDisplayMetrics().widthPixels;
            int fullScreenHeight = getActivity().getResources().getDisplayMetrics().heightPixels;
            Log.i(TAG, "findBestPreviewSize : fullScreenWidth = " + fullScreenWidth + " fullScreenHeight = " + fullScreenHeight);
            Size bestCaptureSize = new Size((int) (fullScreenWidth * ratio), fullScreenWidth);
            float bestRatio = ratio;
            for (Size size : supportSizes) {
                if (Math.abs((float) size.getWidth() / size.getHeight() - ratio) < tolerant) {
                    tolerant = Math.abs((float) size.getWidth() / size.getHeight() - ratio);
                    bestCaptureSize = size;
                    bestRatio = (float) size.getWidth() / size.getHeight();
                }
            }
            Log.i(TAG, "bestCaptureSize = " + bestCaptureSize);
            //capture surface
            int mCaptureWidth = bestCaptureSize.getWidth();
            int mCaptureHeight = bestCaptureSize.getHeight();
            //init imageReader
            mImageReader = ImageReader.newInstance(mCaptureWidth, mCaptureHeight, ImageFormat.JPEG, 1);
            mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler);

            //preview surface
            int previewHeight = fullScreenWidth - fullScreenWidth % (16 * 3);
            int previewWidth = (int) (previewHeight * ratio);
            if (previewWidth != mPreviewWidth || previewHeight != mPreviewHeight) {
                mPreviewHeight = previewHeight;
                mPreviewWidth = previewWidth;
            }
        }
        return new Size(mPreviewWidth, mPreviewHeight);
    }

    /**
     * Closes the current {@link CameraDevice}.
     */
    public void closeCamera() {
        System.out.println("closeCamera方法调用了===========mCameraDevice = null");
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
        }
    }




    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a new {@link CameraCaptureSession} for camera preview.
     * 为相机预览创建一个新的 {@link CameraCaptureSession}。
     */
    private void createCameraPreviewSession() {
        System.out.println("createCameraPreviewSession()方法调用了");
        try {
//            closePreviewSession();
            System.out.println("mTextureView"+mTextureView.getWidth()+mTextureView.getHeight());
            texture = mTextureView.getSurfaceTexture();


            assert texture != null;
            // 我们将默认缓冲区的大小配置为我们想要的相机预览的大小。
            Log.e(TAG, "Default_buffer_size = " + mPreviewSize.getWidth() + "X" + mPreviewSize.getHeight());
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            // 这是我们需要开始预览的输出 Surface。
            Surface surface = new Surface(texture);
            //我们使用输出 Surface 设置了一个 CaptureRequest.Builder。
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);
            //在这里，我们为相机预览创建了一个 CameraCaptureSession。
            Log.e(TAG, "createCameraPreviewSession: " + mImageReader + "__" + mCameraDevice);
            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            // The camera is already closed
                            //相机已经关闭
                            if (null == mCameraDevice) {
                                return;
                            }
                            // 当会话准备好时，我们开始显示预览。
                            mCaptureSession = cameraCaptureSession;
//                            try {
//                                cameraCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), mCaptureCallback, mBackgroundHandler);
//                            } catch (CameraAccessException e) {
//                                e.printStackTrace();
//                            }
//                            mPreviewRequest = mPreviewRequestBuilder.build();
                            startPreview();
                        }
                        @Override
                        public void onConfigureFailed(
                                @NonNull CameraCaptureSession cameraCaptureSession) {
                            showToast("Failed");
                        }
                    }, null
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private class MyOrientationListener extends OrientationEventListener{
        public MyOrientationListener(Context context){
            super(context);
        }

        public void onOrientationChanged(int orientation){
            mRotation = (orientation + 45) /90 *90;
        }
    }

    /**
     * Configures the necessary {@link android.graphics.Matrix} transformation to `mTextureView`.
     * This method should be called after the camera preview size is determined in
     * setUpCameraOutputs and also the size of `mTextureView` is fixed.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    private void configureTransform(int viewWidth, int viewHeight) {
        System.out.println("configureTransform方法调用了");
        Activity activity = getActivity();
        if (null == mTextureView || null == mPreviewSize || null == activity) {
            return;
        }
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        mTextureView.setTransform(matrix);

    }

    /**
     * Initiate a still image capture.
     */
    public void takePicture() {
        System.out.println("takePicture方法调用了");
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        lockFocus();


    }

    
    /**
     * Lock the focus as the first step for a still image capture.
     * 锁定焦点是拍摄静止图像的第一步。
     */
    private void lockFocus() {
        System.out.println("lockFocus方法调用了");
        try {
            // This is how to tell the camera to lock focus.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the lock.
            mState = STATE_WAITING_LOCK;
            //lockFocus主要是实现下面这个方法，并回调mCaptureCallback
            Log.e(TAG, "run---------> 2");
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Run the precapture sequence for capturing a still image. This method should be called when
     * we get a response in {@link #mCaptureCallback} from {@link #lockFocus()}.
     * 运行预捕获序列以捕获静止图像。当
     * 我们从 {@link #lockFocus()}在{@link #mCaptureCallback}中获得响应时，应该调用此方法。
     */
    private void runPrecaptureSequence() {
        System.out.println("runPrecaptureSequence方法调用了");
        try {
            // This is how to tell the camera to trigger.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the precapture sequence to be set.
            mState = STATE_WAITING_PRECAPTURE;
            Log.e(TAG, "run---------> 3");
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Capture a still picture. This method should be called when we get a response in
     * {@link #mCaptureCallback} from both {@link #lockFocus()}.
     * 拍摄静止图像。当我们在 * {@link #mCaptureCallback} 中从 {@link #lockFocus()} 获得响应时，应该调用此方法。
     */
    private void captureStillPicture() {
        System.out.println("captureStillPicture方法调用了");
        try {
            System.out.println("captureStillPicture()执行开始");
            final Activity activity = getActivity();
            if (null == activity || null == mCameraDevice) {
                return;
            }
            // This is the CaptureRequest.Builder that we use to take a picture.
            final CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());

            // Use the same AE and AF modes as the preview.
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            setAutoFlash(captureBuilder);
            // 预览如果有放大，拍照的时候也应该保存相同的缩放
            Rect zoomRect = mPreviewRequestBuilder.get(CaptureRequest.SCALER_CROP_REGION);
            //在正常情况下，也就是为进行任何缩放操作前，zoomRect为null。
            if (zoomRect != null) {
                captureBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoomRect);
            }
            System.out.println("mRotation+++++++++++++++++"+mRotation);
            mPreviewRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION,mRotation);
            // Orientation
            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();

            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(rotation));
            CameraCaptureSession.CaptureCallback CaptureCallback
                    = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    showToast("已存入系统相册");
                    //Log.d(TAG, mFile.toString());
                    unlockFocus();
                }
            };

            mCaptureSession.stopRepeating();
            mCaptureSession.abortCaptures();
            mCaptureSession.capture(captureBuilder.build(), CaptureCallback, null);
            System.out.println("captureStillPicture()执行结束");
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves the JPEG orientation from the specified screen rotation.
     *
     * @param rotation The screen rotation.
     * @return The JPEG orientation (one of 0, 90, 270, and 360)
     */
    private int getOrientation(int rotation) {
        Orientations();
        // Sensor orientation is 90 for most devices, or 270 for some devices (eg. Nexus 5X)
        // We have to take that into account and rotate JPEG properly.
        // For devices with orientation of 90, we simply return our mapping from ORIENTATIONS.
        // For devices with orientation of 270, we need to rotate the JPEG 180 degrees.
        return (ORIENTATIONS.get(rotation) + mSensorOrientation + 270) % 360;
    }

    /**
     * Unlock the focus. This method should be called when still image capture sequence is
     * finished.
     */
    //解锁焦点。当静止图像捕获序列完成时应调用此方法。
    private void unlockFocus(){
        System.out.println("unlockFocus方法调用了");
        try {
            // Reset the auto-focus trigger
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            setAutoFlash(mPreviewRequestBuilder);
            Log.e( TAG, "run---------> 1");
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
            // After this, the camera will go back to the normal state of preview.
            mState = STATE_PREVIEW;

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void startPreview() {
        if (mCaptureSession == null || mPreviewRequestBuilder == null) {
            Log.w(TAG, "startPreview: mCaptureSession or mPreviewRequestBuilder is null");
            return;
        }
        try {
            // 开始预览，即一直发送预览的请求
            mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), mCaptureCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    public void stopPreview() {
        System.out.println("stopPreview方法调用了");
        if (mCaptureSession != null) {
            try {
                mCaptureSession.abortCaptures();
                mCaptureSession.close();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private void switchCamera() {

        Activity activity = getActivity();
        if (mCameraId.equals("0")) {
            mCameraId = "1";
        } else if (mCameraId.equals("1")) {
            mCameraId = "0";
        }
        Reopen();
    }


    @SuppressLint("ResourceType")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.picture: {
                    if(delaystate == 0){
                        MediaActionSound mediaActionSound = new MediaActionSound();
                        mediaActionSound.play(0);
                        takePicture();
                    }else{

                            new CountDownTimer(mDelayTime, TIME_INTERVAL) {
                                MediaActionSound mediaActionSound = new MediaActionSound();
    //                          mediaActionSound.load(0);
                                @Override
                                public void onTick(long millisUntilFinished) {
                                    mTimeText.setVisibility(View.VISIBLE);
                                    mTimeText.setText("" + (millisUntilFinished +1000)/ TIME_INTERVAL);
                                    if((millisUntilFinished+1000)/TIME_INTERVAL<= 3){
                                        mediaActionSound.play(0);
                                    }

                                }

                                @Override
                                public void onFinish() {
                                    mTimeText.setVisibility(View.GONE);
                                    takePicture();
                                }
                            }.start();

                    }
                    break;
            }
            case R.id.image:{
                selectImage();
                break;
            }
            case R.id.exchangeCamera2: {
    //                Activity activity = getActivity();
                    switchCamera();
                    break;
            }
            case R.id.to_recorder: {
    //                isFirstStart = false;
                    ((CameraActivity)activity).switchFragment(Camera2VideoFragment.newInstance());
                    PaintFlag = "4:3";
                    break;
            }
            case R.id.btn_delay:
                if (isDelay){
                    isDelay = false;
                    delaystate = 0;
                    delayBtn.setBackground(getContext().getResources().getDrawable(R.drawable.not_selected));
                    System.out.println("btn_delay-----------1");
                }
                else{
                    isDelay = true;
                    delaystate = 1;
                    Toast.makeText(activity,"延时拍摄设置" ,Toast.LENGTH_SHORT).show();
                    delayBtn.setBackground(getContext().getResources().getDrawable(R.drawable.is_selected));
                    System.out.println("btn_delay-----------2");
                }
                break;

            case R.id.btn_delay_setting:
                if(delayZero == 0){
                    delayZero = 1;
                    btn_delay_layout.setVisibility(View.VISIBLE);
                }else{
                    delayZero = 0;
                    btn_delay_layout.setVisibility(View.INVISIBLE);
                }

                break;
            case R.id.btn_delay3:
                    btn_delay_layout.setVisibility(View.INVISIBLE);
                    Toast.makeText(activity,"已选中3秒延时" ,Toast.LENGTH_SHORT).show();
                    btn_delay_setting.setText("3");
                    mDelayTime = 3 * 1000;
                    break;
            case R.id.btn_delay5:
                    btn_delay_layout.setVisibility(View.INVISIBLE);
                    Toast.makeText(activity,"已选中5秒延时" ,Toast.LENGTH_SHORT).show();
                    btn_delay_setting.setText("5");
                    mDelayTime = 5 * 1000;
                    break;
            case R.id.btn_delay10:
                    btn_delay_layout.setVisibility(View.INVISIBLE);
                    Toast.makeText(activity,"已选中10秒延时" ,Toast.LENGTH_SHORT).show();
                    btn_delay_setting.setText("10");
                    mDelayTime = 10 * 1000;
                    break;
            case R.id.btn_change_preview:
                    System.out.println("btn_change_preview");
                    if(changePreview == 0){
                        changePreview = 1;
                        btn_Paint.setVisibility(View.VISIBLE);
                    }else{
                         changePreview= 0;
                        btn_Paint.setVisibility(View.INVISIBLE);
                    }
                    break;
            case R.id.btn_Paint_1:
                    btn_Paint.setVisibility(View.INVISIBLE);
                    btn_change_preview.setText("1:1");
                    PaintFlag = "1:1";
                try {
                    handleRatioChange(ratio1_1);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
                Reopen();
                    break;
            case R.id.btn_Paint_4:
                    btn_Paint.setVisibility(View.INVISIBLE);
                    btn_change_preview.setText("4:3");
                    PaintFlag = "4:3";
                try {
                    handleRatioChange(ratio4_3);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
                Reopen();
                    break;
            case R.id.btn_Paint_16:
                    btn_Paint.setVisibility(View.INVISIBLE);
                    btn_change_preview.setText("full");
                    PaintFlag = "full";
                try {
                    handleRatioChange(ratio16_9);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
                Reopen();
                    break;
        }
    }
    private boolean isNeedRatio = false;
    private void handleRatioChange(float ratio) throws CameraAccessException {
        setOrientations();
        StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        mPreviewSize = findBestPreviewSize(ratio);
        if(PaintFlag == "1:1"){
            mPreviewSize = chooseImageSize(map.getOutputSizes(ImageReader.class));
        }else{

        }

        Log.e(TAG, "handleRatioChange: " + mPreviewSize.getWidth() + "X" + mPreviewSize.getHeight());
        mTextureView.setAspectRatio(mPreviewSize.getWidth(),mPreviewSize.getHeight());
        isNeedRatio = true;
    }

    private void setAutoFlash(CaptureRequest.Builder requestBuilder) {
        if (mFlashSupported) {
            requestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
        }

    }


    /**
     * Saves a JPEG {@link Image} into the specified {@link File}.
     */
    private class ImageSaver implements Runnable {

        /**
         * The JPEG image
         */
        private final Image mImage;
        /**
         * The file we save the image into.
         */
        private final File mFile;

        ImageSaver(Image image, File file) {
            mImage = image;
            mFile = file;
        }

        @Override
        public void run() {
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            final byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            final Bitmap bitmap = adjustSourcePic(mCameraId,bytes);
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    imageView.setImageBitmap(bitmap);
                }
            });

            //拍完照片并存储到系统相册目录下后，让系统相册更新。
            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            Uri uri = Uri.fromFile(mFile);
            intent.setData(uri);
            getContext().sendBroadcast(intent);

            FileOutputStream output = null;
            try {
                output = new FileOutputStream(mFile);
                output.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mImage.close();
                if (null != output) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }
    public static Bitmap adjustSourcePic(String CurrentCamera, byte[] data){
        int orientation = 0;
        try{
            ExifInterface exifInterface = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                exifInterface = new ExifInterface(new ByteArrayInputStream(data));
            }
            int value = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION,-1);
            Log.e(TAG, "values "+ value);
            switch (value){
                case ExifInterface.ORIENTATION_NORMAL:
                    if (CurrentCamera.equals("0")){//后置摄像头
                        orientation = 90;
                    }else if(CurrentCamera.equals("1")){//前置摄像头
                    orientation = 270;
                    }
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    orientation = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    if(CurrentCamera.equals("0")){//后置摄像头
                        orientation = 270;
                    }else if(CurrentCamera.equals("1")){//前置摄像头
                        orientation = 90;
                    }
                    break;
                case  ExifInterface.ORIENTATION_ROTATE_270:
                    orientation = 0;
                    break;
            }
        }catch (IOException e){
            e.printStackTrace();
        }
        Bitmap bitmap = BitmapFactory.decodeByteArray(data,0,data.length);
        Bitmap calBitmap;
        Matrix rmatrix = new Matrix();
        Matrix nmatrix = new Matrix();

        System.out.println("orientation+++++++++++++++++++++++++++++"+orientation);
        rmatrix.setRotate(orientation,bitmap.getWidth()/2,bitmap.getHeight()/2);
        calBitmap = Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),rmatrix,false);

        if(CurrentCamera.equals("1")){
            nmatrix.setScale(-1,1);
            calBitmap = Bitmap.createBitmap(calBitmap,0,0,calBitmap.getWidth(),calBitmap.getHeight(),rmatrix,false);
        }
        return calBitmap;
    }


    /**
     * Compares two {@code Size}s based on their areas.
     */
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }

    /**
     * Shows an error message dialog.
     */
    public static class ErrorDialog extends DialogFragment {

        private static final String ARG_MESSAGE = "message";

        public static ErrorDialog newInstance(String message) {
            ErrorDialog dialog = new ErrorDialog();
            Bundle args = new Bundle();
            args.putString(ARG_MESSAGE, message);
            dialog.setArguments(args);
            return dialog;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();
            return new AlertDialog.Builder(activity)
                    .setMessage(getArguments().getString(ARG_MESSAGE))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            activity.finish();
                        }
                    })
                    .create();
        }

    }

    /**
     * Shows OK/Cancel confirmation dialog about camera permission.
     */
    public static class ConfirmationDialog extends DialogFragment {

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Fragment parent = getParentFragment();
            return new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.request_permission)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            parent.requestPermissions(new String[]{Manifest.permission.CAMERA},
                                    REQUEST_CAMERA_PERMISSION);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Activity activity = parent.getActivity();
                                    if (activity != null) {
                                        activity.finish();
                                    }
                                }
                            })
                    .create();
        }
    }
    //缩放事件手势处理
    public void handleZoom(boolean isZoomIn) {
        System.out.println("handleZoom++++++++++++++++++++++");
        if (mCameraDevice == null || characteristics == null || mPreviewRequestBuilder == null) {
            return;
        }
        // maxZoom 表示 active_rect 宽度除以 crop_rect 宽度的最大值
        float maxZoom = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
        Log.d(TAG, "handleZoom: maxZoom: " + maxZoom);
        int factor = 50; // 放大/缩小的一个因素，设置越大越平滑，相应放大的速度也越慢
        if (isZoomIn && mZoom < factor) {
            mZoom++;
        } else if (mZoom > 0) {
            mZoom--;
        }
        System.out.println("handleZoom---------------------");
        Log.d(TAG, "handleZoom: mZoom: " + mZoom);
        Rect rect = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        int minW = (int) ((rect.width() - rect.width() / maxZoom) / (2 * factor));
        int minH = (int) ((rect.height() - rect.height() / maxZoom) / (2 * factor));
        int cropW = minW * mZoom;
        int cropH = minH * mZoom;
        Log.d(TAG, "handleZoom: cropW: " + cropW + ", cropH: " + cropH);
        Rect zoomRect = new Rect(cropW, cropH, rect.width() - cropW, rect.height() - cropH);
        mPreviewRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoomRect);
        mPreviewRequest = mPreviewRequestBuilder.build();
        startPreview(); // 需要重新 start preview 才能生效0
    }
    public Size getPreviewSize() {
        return mPreviewSize;
    }

    public void Reopen(){
        System.out.println("Reopen方法调用了");
//       stopPreview();
        closeCamera();
        Log.i(TAG, "Reopen: " + mTextureView.getWidth() + "X" + mTextureView.getHeight());
        openCamera(mTextureView.getWidth(),mTextureView.getHeight());
    }

    public Size chooseImageSize(Size[] choices) {
        System.out.println("chooseImageSize方法调用了" + choices);
        for(Size size : choices){
            if(PaintFlag == "1:1" && size.getWidth() == size.getHeight() * 1 / 1){
                return size;
            }else if(PaintFlag == "4:3" && size.getWidth() == size.getHeight() * 4 / 3){
                return size;
            }else if(PaintFlag == "full" && size.getWidth() == size.getHeight() * 18 / 9){
                return size;
            }
        }

        return choices[choices.length - 1];
    }
    private void selectImage() {
        Intent intent;
        if (Build.VERSION.SDK_INT < 19) {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
        } else {
            intent = new Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);         }
        startActivityForResult(intent, REQUEST_CAMERA_PERMISSION);
    }

    private void setOrientations() throws CameraAccessException {
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        characteristics = manager.getCameraCharacteristics(mCameraId);
        mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
    }
}



