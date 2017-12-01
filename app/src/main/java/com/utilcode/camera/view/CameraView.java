package com.utilcode.camera.view;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.utilcode.camera.controller.TextureController;
import com.utilcode.camera.filter.AFilter;
import com.utilcode.camera.filter.Beauty;
import com.utilcode.camera.filter.LookupFilter;

import java.io.IOException;
import java.util.Arrays;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.content.Context.CAMERA_SERVICE;
import static android.hardware.camera2.CameraDevice.TEMPLATE_PREVIEW;

/**
 * Description:
 */
public class CameraView extends SurfaceView {

    private int cameraId=1;
    private Context context;
    private LookupFilter mLookupFilter;
    private Beauty mBeautyFilter;
    private TextureController mController;
    private com.utilcode.camera.ifs.Renderer mRenderer;
    private String[] images = {"","purity.png","amatorka.png","clearLookup.jpg","highkey.png","peachLookup.jpg","purityLookup.png","ruddyLookup.jpg"};

    public CameraView(Context context) {
        this(context,null);
    }

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init();
    }

    private void init(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mRenderer = new Camera2Renderer();
        }else{
            mRenderer = new Camera1Renderer();
        }
        mController = new TextureController(context);
        onFilterSet();
        mController.setFrameCallback(720, 1280, context);
        getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                mController.surfaceCreated(holder);
                mController.setRenderer(mRenderer);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                mController.surfaceChanged(width, height);
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                mController.surfaceDestroyed();
            }
        });
    }

    public void switchCamera(){
        cameraId=cameraId==1?0:1;
        mRenderer.open();
    }

    public void onResume() {
        if (mController != null) {
            mController.onResume();
        }
    }

    public void onPause() {
        if (mController!=null){
            mController.onPause();
        }
    }

    public void onDestroy(){
        if (mController != null) {
            mController.destroy();
        }
    }

    public void takePhoto(){
        mController.takePhoto();
    }

    public void setBeautyFilterProgress(int progress){
        mLookupFilter.setIntensity(progress/100f);
        mBeautyFilter.setFlag(progress/20+1);
    }


    private void onFilterSet() {
        mLookupFilter=new LookupFilter(getResources());
        mLookupFilter.setIntensity(0.0f);
        mBeautyFilter=new Beauty(getResources());
        onSelectImage(0);
    }

    public void onSelectImage(int pos){
        if (pos >= images.length){
            pos = 0;
        }

        if (pos==0){
            removeFilter(mLookupFilter);
            removeFilter(mBeautyFilter);
        }else {
            removeFilter(mLookupFilter);
            removeFilter(mBeautyFilter);
            mLookupFilter.setMaskImage("lookup/"+images[pos]);
            addFilter(mLookupFilter);
            addFilter(mBeautyFilter);
        }
    }

    private void addFilter(AFilter filter){
        if(mController!=null){
            mController.addFilter(filter);
        }
    }

    private void removeFilter(AFilter filter){
        if(mController!=null) {
            mController.removeFilter(filter);
        }
    }

    public int getImageFilterCount(){
        return images.length;
    }

    private class Camera1Renderer implements com.utilcode.camera.ifs.Renderer {
        private Camera mCamera;

        @Override
        public void onDestroy() {
            if (mCamera != null) {
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }
        }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            open();
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {

        }

        @Override
        public void onDrawFrame(GL10 gl) {

        }

        @Override
        public void open(){
            if (mCamera != null) {
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }
            mCamera = Camera.open(cameraId);
            mController.setImageDirection(cameraId);
            Camera.Size size = mCamera.getParameters().getPreviewSize();
            mController.setDataSize(size.height, size.width);
            try {
                mCamera.setPreviewTexture(mController.getTexture());
                mController.getTexture().setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                    @Override
                    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                        mController.requestRender();
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
            mCamera.startPreview();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private class Camera2Renderer implements com.utilcode.camera.ifs.Renderer {

        CameraDevice mDevice;
        CameraManager mCameraManager;
        private HandlerThread mThread;
        private Handler mHandler;
        private Size mPreviewSize;

        Camera2Renderer() {
            mCameraManager = (CameraManager)context.getSystemService(CAMERA_SERVICE);
            mThread = new HandlerThread("camera2 ");
            mThread.start();
            mHandler = new Handler(mThread.getLooper());
        }

        @Override
        public void onDestroy() {
            if(mDevice!=null){
                mDevice.close();
                mDevice=null;
            }
        }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            open();
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {

        }

        @Override
        public void onDrawFrame(GL10 gl) {

        }

        @Override
        public void open(){
            try {
                if(mDevice!=null){
                    mDevice.close();
                    mDevice=null;
                }
                CameraCharacteristics c=mCameraManager.getCameraCharacteristics(cameraId+"");
                StreamConfigurationMap map=c.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                Size[] sizes=map.getOutputSizes(SurfaceHolder.class);
                //自定义规则，选个大小
                mPreviewSize=sizes[0];
                mController.setDataSize(mPreviewSize.getHeight(),mPreviewSize.getWidth());
                mCameraManager.openCamera(cameraId + "", new CameraDevice.StateCallback() {
                    @Override
                    public void onOpened(CameraDevice camera) {
                        mDevice=camera;
                        try {
                            Surface surface=new Surface(mController
                                    .getTexture());
                            final CaptureRequest.Builder builder=mDevice.createCaptureRequest
                                    (TEMPLATE_PREVIEW);
                            builder.addTarget(surface);
                            mController.getTexture().setDefaultBufferSize(
                                    mPreviewSize.getWidth(),mPreviewSize.getHeight());
                            mDevice.createCaptureSession(Arrays.asList(surface), new
                                    CameraCaptureSession.StateCallback() {
                                        @Override
                                        public void onConfigured(CameraCaptureSession session) {
                                            try {
                                                session.setRepeatingRequest(builder.build(), new CameraCaptureSession.CaptureCallback() {
                                                    @Override
                                                    public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request, CaptureResult partialResult) {
                                                        super.onCaptureProgressed(session, request, partialResult);
                                                    }

                                                    @Override
                                                    public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                                                        super.onCaptureCompleted(session, request, result);
                                                        mController.requestRender();
                                                    }
                                                },mHandler);
                                            } catch (CameraAccessException e) {
                                                e.printStackTrace();
                                            }
                                        }

                                        @Override
                                        public void onConfigureFailed(CameraCaptureSession session) {

                                        }
                                    },mHandler);
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onDisconnected(CameraDevice camera) {
                        mDevice=null;
                    }

                    @Override
                    public void onError(CameraDevice camera, int error) {

                    }
                }, mHandler);
            } catch (SecurityException | CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }
}
