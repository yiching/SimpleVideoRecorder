package com.example.yiching.videorecordertest;

import android.app.Activity;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.hardware.Camera.Size;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends Activity implements SurfaceHolder.Callback {

    private SurfaceView m_surfaceView;
    private SurfaceHolder m_surfaceHolder;
    private Button m_functionRecord;

    private MediaRecorder m_MediaRecorder;
    private Camera m_camera;
    private Camera.Parameters m_cameraParam;
    private List<Size> m_cameraPreviewSizes = new ArrayList<Size>();
    private List<Size> m_previewSizes = new ArrayList<Size>();
    private List<Size> m_cameraSizes = new ArrayList<Size>();
    private boolean m_inRecordInProcess;

    // private int m_cameraNumber = 0; // 0:back , 1:front
    private Size m_picSize, m_previewSize;
    private static File mediaFile;
    private final String CAMERA_NAME_BACK_CAMERA = "back_camera";
    private final String CAMERA_NAME_FRONT_CAMERA = "front_camera";
    private String m_cameraName = CAMERA_NAME_BACK_CAMERA;
    private int m_cameraNumber = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.e("Activity", "onCreate");

        m_surfaceView = (SurfaceView) findViewById(R.id.surface_camera);
        m_surfaceHolder = m_surfaceView.getHolder();
        m_surfaceHolder.addCallback(this);
        m_surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        m_functionRecord = (Button) findViewById(R.id.button);
        m_functionRecord.setOnClickListener(recordClick);
        m_functionRecord.setText("Start Recording");

        checkCameraLens();
    }


    private void InitCamera() {

        Log.e("Activity", "InitCamera");

        m_cameraParam = m_camera.getParameters();
        m_previewSizes = m_cameraParam.getSupportedPreviewSizes();
        m_cameraSizes = m_cameraParam.getSupportedPictureSizes();

        m_picSize = m_cameraSizes.get(0);
        m_cameraParam.setPictureSize(m_picSize.width, m_picSize.height);

        m_previewSize = m_previewSizes.get(0);
        m_cameraParam.setPreviewSize(m_previewSize.width, m_previewSize.height);

        m_camera.setParameters(m_cameraParam);
        m_camera.startPreview();
    }


    private void checkCameraLens() {

        int cameraCount = Camera.getNumberOfCameras();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();

        for (int camera = 0; camera < cameraCount; camera++) {
            Camera.getCameraInfo(camera, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                Log.e("Camera","Camera Front Exist! Number = "+camera);
            }else if(cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK){
                Log.e("Camera","Camera Back Exist! Number = "+camera);
            }
        }
        m_cameraName = CAMERA_NAME_BACK_CAMERA;
        m_cameraNumber = 0;
    }

    private View.OnClickListener recordClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            Log.e("Activity", "recordClick");

            if (!m_inRecordInProcess) {

                Log.e("Record", "Start Recording");

                if (m_MediaRecorder == null) {
                    m_MediaRecorder = new MediaRecorder();
                }

                m_camera.stopPreview();
                try {
                    m_camera.unlock();
                } catch (RuntimeException e) {
                    Log.e("Camera", "Camera unlock Error!");
                }

                m_MediaRecorder.setCamera(m_camera);

                m_MediaRecorder.setAudioSource(MediaRecorder.VideoSource.DEFAULT);
                m_MediaRecorder.setVideoSource(MediaRecorder.AudioSource.MIC);

                CamcorderProfile camcorderProfile_HQ = null;
                if(CamcorderProfile.hasProfile(m_cameraNumber, CamcorderProfile.QUALITY_HIGH)){
                    camcorderProfile_HQ = CamcorderProfile.get(m_cameraNumber, CamcorderProfile.QUALITY_HIGH);
                    Log.e("CamcCoder","camcorderProfile_HQ = "+camcorderProfile_HQ);
                }else{
                    camcorderProfile_HQ.videoFrameWidth = 640;
                    camcorderProfile_HQ.videoFrameHeight = 480;
                    camcorderProfile_HQ.videoCodec = MediaRecorder.VideoEncoder.H264;
                    camcorderProfile_HQ.fileFormat = MediaRecorder.OutputFormat.MPEG_4;
                }

                m_MediaRecorder.setProfile(camcorderProfile_HQ);
                m_MediaRecorder.setOutputFile(getOutputMediaFile(null).getAbsolutePath());
                m_MediaRecorder.setPreviewDisplay(m_surfaceHolder.getSurface());

                try {
                    m_MediaRecorder.prepare();
                } catch (IOException e) {
                    Log.e("MediaRecorder", "MediaRecorder prepare Fail!");
                }

                m_MediaRecorder.start();
                m_inRecordInProcess = true;
                m_functionRecord.setText("Stop Recording");
            } else {

                Log.e("Record", "Stop Recording");

                m_MediaRecorder.stop();
                m_MediaRecorder.reset();

                try {
                    m_camera.reconnect();
                } catch (IOException e) {
                    Log.e("MediaRecorder", "MediaRecorder reconnect Fail!");
                }

                m_inRecordInProcess = false;
                m_functionRecord.setText("Start Recording");
            }

        }
    };


    private static File getOutputMediaFile(String timeStamp) {

        Log.e("Activity", "getOutputMediaFile");
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory(), "DCIM/Camera");

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.e("File", "LivingCam_Shot failed to create directory");
                return null;
            }
            Log.e("File", "mediaStorageDir = " + mediaStorageDir.getAbsolutePath());
        } else {
            mediaStorageDir.mkdirs();
            Log.e("File", "mediaStorageDir = " + mediaStorageDir.getAbsolutePath());
        }

        if (timeStamp == null) {
            timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmssSSS").format(new Date());
        }

        mediaFile = new File(mediaStorageDir.getPath() + File.separator + "VID_" + timeStamp + ".mp4");
        Log.e("File", "mediaFile = " + mediaFile.getAbsolutePath());

        return mediaFile;
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        Log.e("Activity", "surfaceCreated!");

        try {
            m_camera = Camera.open(m_cameraNumber);
        } catch (Exception e) {
            Log.e("Camera", "Camera is in Use!");
        }

        if (m_camera == null) {
            Toast.makeText(this.getApplicationContext(), "Camera is in Use!", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            InitCamera();
        }

        try {
            if (m_camera == null) {
                Log.e("Camera", "Null");
            }
            if (m_surfaceHolder == null) {
                Log.e("SurfaceHolder", "Null");
            }
            m_camera.setPreviewDisplay(m_surfaceHolder);
        } catch (IOException e) {
            Log.e("Camera", "Camera preview Fail!");
        }

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.e("Activity", "surfaceChanged!");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

        Log.e("Activity", "surfaceDestroyed!");

        if (m_camera != null) {
            m_camera.stopPreview();
            m_camera.release();
            m_camera = null;
        }

        if (m_MediaRecorder != null) {
            m_MediaRecorder.release();
            m_MediaRecorder = null;
        }

        m_inRecordInProcess = false;
    }
}
