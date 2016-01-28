package com.android.cole.notrackingflashlight;

import android.Manifest;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Size;
import android.view.Surface;
import android.view.View;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
{
    //Initialize variables need to create and manager flash on the camera
    private CameraManager cameraManager;
    private CameraCharacteristics cameraCharacteristics;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mSession;
    private CaptureRequest.Builder mBuilder;
    private boolean flashState = true;


    // create the activity
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set up Ads
        AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);


        // Initialize Camera
        initCamera();

    }

    //when toggle button is clicked this function runs
    // and checks the flashstate, which is either true
    // or false depending on whether the flash is on or
    // off. This determined whether the "click" turns
    // the flash on or off.
    public void click(View v)
    {
        if (flashState) {
            try {
                turnOnFlashLight();
                flashState = false;
            } catch (Exception e) {
                e.printStackTrace();
            }
       } else {
            try {
                turnOffFlashLight();
                flashState = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    // intialize the camera
    private void initCamera()
    {
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try
        {
            String[] id = cameraManager.getCameraIdList();
            if (id != null && id.length > 0)
            {
                cameraCharacteristics = cameraManager.getCameraCharacteristics(id[0]);
                boolean isFlash = cameraCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                if (isFlash)
                {

                    int permissionCheck = ContextCompat.checkSelfPermission(this,
                                Manifest.permission.CAMERA);
                    cameraManager.openCamera(id[0], new MyCameraDeviceStateCallback(), null);

                }
            }
        }
        catch (CameraAccessException e)
        {
            e.printStackTrace();
        }
    }

    //Creates a camera capture session.
    class MyCameraDeviceStateCallback extends CameraDevice.StateCallback
    {

        @Override
        public void onOpened(CameraDevice camera)
        {
            mCameraDevice = camera;
            // get builder
            try
            {
                mBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                List<Surface> list = new ArrayList<Surface>();
                SurfaceTexture mSurfaceTexture = new SurfaceTexture(1);
                Size size = getSmallestSize(mCameraDevice.getId());
                mSurfaceTexture.setDefaultBufferSize(size.getWidth(), size.getHeight());
                Surface mSurface = new Surface(mSurfaceTexture);
                list.add(mSurface);
                mBuilder.addTarget(mSurface);
                camera.createCaptureSession(list, new MyCameraCaptureSessionStateCallback(), null);
            }
            catch (CameraAccessException e)
            {
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnected(CameraDevice camera)
        {

        }

        @Override
        public void onError(CameraDevice camera, int error)
        {
            close();
            initCamera();
        }
    }

    private Size getSmallestSize(String cameraId) throws CameraAccessException
    {
        Size[] outputSizes = cameraManager.getCameraCharacteristics(cameraId).get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(SurfaceTexture.class);
        if (outputSizes == null || outputSizes.length == 0)
        {
            throw new IllegalStateException("Camera " + cameraId + "doesn't support any outputSize.");
        }
        Size chosen = outputSizes[0];
        for (Size s : outputSizes)
        {
            if (chosen.getWidth() >= s.getWidth() && chosen.getHeight() >= s.getHeight())
            {
                chosen = s;
            }
        }
        return chosen;
    }

    class MyCameraCaptureSessionStateCallback extends CameraCaptureSession.StateCallback
    {
        @Override
        public void onConfigured(CameraCaptureSession session)
        {
            mSession = session;
            try
            {
                mSession.setRepeatingRequest(mBuilder.build(), null, null);
            }
            catch (CameraAccessException e)
            {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session)
        {

        }
    }

    public void turnOnFlashLight()
    {
        try
        {
            mBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
            mSession.setRepeatingRequest(mBuilder.build(), null, null);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void turnOffFlashLight()
    {
        try
        {
            mBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);
            mSession.setRepeatingRequest(mBuilder.build(), null, null);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void close()
    {
        if (mCameraDevice == null || mSession == null)
        {
            return;
        }
        mSession.close();
        mCameraDevice.close();
        mCameraDevice = null;
        mSession = null;
    }
}



