package com.example.myvideo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

	public static final int MEDIA_TYPE_IMAGE = 1;
	public static final int MEDIA_TYPE_VIDEO = 2;
	private Camera mCamera;
	private CameraPreview mPreview;
	private MediaRecorder mMediaRecorder;
	private boolean isRecording = false;
	private Button captureButton;
	private TextView tv_recorderTime;
	private LinearLayout layout;
	Timer timer = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mCamera = getCameraInstance();
		Parameters p = mCamera.getParameters();
		p.setPreviewSize(240, 160);
		mCamera.setParameters(p);
		mPreview = new CameraPreview(this, mCamera);
		FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
		preview.addView(mPreview);
		tv_recorderTime = (TextView) findViewById(R.id.tv_recorderTime);
		layout = (LinearLayout) findViewById(R.id.ll_timer);

		captureButton = (Button) findViewById(R.id.button_capture);


		Button exitButton = (Button) findViewById(R.id.button_exit);
		captureButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// get an image from the camera
				if (isRecording) {
					timer.cancel();  // 停止录制时间显示
					timer = null;
					layout.setVisibility(View.GONE);  //取消时间显示
					// stop recording and release camera
					mMediaRecorder.stop(); // stop the recording
					releaseMediaRecorder(); // release the MediaRecorder
											// object
					mCamera.lock(); // take camera access back from
									// MediaRecorder
					mCamera.stopPreview();
					mCamera.startPreview();
					// inform the user that recording has stopped
					setCaptureButtonText("Capture");
					
					isRecording = false;
				} else {
					// initialize video camera
					if (prepareVideoRecorder()) {
						// Camera is available and unlocked, MediaRecorder
						// is prepared,
						// now you can start recording
						mMediaRecorder.start();
						// inform the user that recording has started
						setCaptureButtonText("Stop");
						isRecording = true;
						timer = new Timer();
						timer.schedule(new mTimerTask(),0,1000);
					} else {
						// prepare didn't work, release the camera
						releaseMediaRecorder();
						// inform user
					}
				}
			}
		});

		exitButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				MainActivity.this.finish();
				System.exit(0);
			}
		});
	}
	
	private class mTimerTask extends TimerTask {

		private long start_time;
		private long end_time;
		private SimpleDateFormat simpleDateFormat;
		public mTimerTask() {
			super();
			// 设置录制时间布局显示
			layout.setVisibility(View.VISIBLE);
			start_time = System.currentTimeMillis();  // 记录开始时间
			simpleDateFormat = new SimpleDateFormat("mm:ss");
		}

		@Override
		public void run() {
			// 计算从开始到现在所用的时间 
			end_time = System.currentTimeMillis();
			Date date = new Date(end_time - start_time);
			
			// 通知主线程显示
			Message msg = new Message();
			msg.what = UPDATETIME;
			msg.obj = simpleDateFormat.format(date);
			
			handler.sendMessage(msg);
		}
	}
	private static final int UPDATETIME = 1;
	Handler handler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case UPDATETIME:
				tv_recorderTime.setText(msg.obj.toString());
				break;

			default:
				break;
			}
		}
	};

	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		super.onBackPressed();
		MainActivity.this.finish();
		System.exit(0);
	}

	protected void setCaptureButtonText(String string) {
		// TODO Auto-generated method stub
		captureButton.setText(string);
	}

	private static File getOutputMediaFile(int type) {

		File mediaStorageDir = new File(
				Environment
						.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
				"MyCameraApp");

		if (!mediaStorageDir.exists()) {
			if (!mediaStorageDir.mkdirs()) {
				Log.d("MyCameraApp", "failed to create directory");
				return null;
			}
		}

		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmssSSS")
				.format(new Date());
		File mediaFile;
		if (type == MEDIA_TYPE_IMAGE) {
			mediaFile = new File(mediaStorageDir.getPath() + File.separator
					+ "IMG_" + timeStamp + ".jpg");
		} else if (type == MEDIA_TYPE_VIDEO) {
			mediaFile = new File(mediaStorageDir.getPath() + File.separator
					+ "VID_" + timeStamp + ".mp4");
		} else {
			return null;
		}

		return mediaFile;
	}

	public static Camera getCameraInstance() {
		Camera c = null;
		try {
			c = Camera.open(); // attempt to get a Camera instance
		} catch (Exception e) {
			// Camera is not available (in use or does not exist)
		}
		return c; // returns null if camera is unavailable
	}

	private boolean prepareVideoRecorder() {
		releaseCamera();

		mCamera = getCameraInstance();
		Parameters param = mCamera.getParameters();
		param.setPreviewSize(240, 160);
		param.setFlashMode(Parameters.FLASH_MODE_TORCH);
		mCamera.setParameters(param);

		mMediaRecorder = new MediaRecorder();

		// mPreview = new CameraPreview(MainActivity.this, mCamera);

		// Step 1: Unlock and set camera to MediaRecorder
		mCamera.unlock();
		mMediaRecorder.setCamera(mCamera);

		// Step 2: Set sources
		mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
		mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

		// Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
//		mMediaRecorder.setProfile(CamcorderProfile
//				.get(CamcorderProfile.QUALITY_HIGH));
		 mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
		 mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
		 mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);
		// Step 4: Set output file
		mMediaRecorder.setOutputFile(getOutputMediaFile(MEDIA_TYPE_VIDEO)
				.toString());
		mMediaRecorder.setVideoFrameRate(10);
		//mMediaRecorder.setVideoSize(240, 160);

		// Step 5: Set the preview output
		mMediaRecorder.setPreviewDisplay(mPreview.getHolder().getSurface());

		// Step 6: Prepare configured MediaRecorder
		try {
			mMediaRecorder.prepare();
		} catch (IllegalStateException e) {
			// Log.d(TAG, "IllegalStateException preparing MediaRecorder: " +
			// e.getMessage());
			releaseMediaRecorder();
			return false;
		} catch (IOException e) {
			// Log.d(TAG, "IOException preparing MediaRecorder: " +
			// e.getMessage());
			releaseMediaRecorder();
			return false;
		}
		return true;
	}

	@Override
	protected void onPause() {
		super.onPause();
		releaseMediaRecorder(); // if you are using MediaRecorder, release it
								// first
		releaseCamera(); // release the camera immediately on pause event
	}

	private void releaseMediaRecorder() {
		if (mMediaRecorder != null) {
			mMediaRecorder.reset(); // clear recorder configuration
			mMediaRecorder.release(); // release the recorder object
			mMediaRecorder = null;
			mCamera.lock(); // lock camera for later use
		}
		/*
		 if (mMediaRecorder != null)
	        {
	            // 内部标识是否正在录像的变量，如果不需要可以去掉
	            if (isRecording)
	            {
	                try
	                {
	                    mMediaRecorder.setOnErrorListener(null);
	                    mMediaRecorder.setOnInfoListener(null);
	                    // 停止
	                    mMediaRecorder.stop();
	                    
	                }
	                catch (RuntimeException e)
	                {
	                    e.printStackTrace();
	                    
	                    // 如果发生异常，很可能是在不合适的状态执行了stop操作
	                    // 所以等待一会儿
	                    try
	                    {
	                        Thread.sleep(100);
	                    }
	                    catch (InterruptedException e1)
	                    {
	                        Log.e("test", "sleep for second stop error!!");
	                    }
	                }
	                isRecording = false;
	            }
	            
	            // 再次尝试停止MediaRecorder
	            try
	            {
	                mMediaRecorder.stop();
	            }
	            catch (Exception e)
	            {
	                Log.e("test", "stop fail2", e);
	            }
	            
	            // 等待，让停止彻底执行完毕
	            try
	            {
	                Thread.sleep(100);
	            }
	            catch (InterruptedException e1)
	            {
	                Log.e("test", "sleep for reset error Error", e1);
	            }
	            
	            // 然后再进行reset、release
	            mMediaRecorder.reset();
	            mMediaRecorder.release();
	            
	            mMediaRecorder = null;
	        }*/
	}

	private void releaseCamera() {
		if (mCamera != null) {
			mCamera.release(); // release the camera for other applications
			mCamera = null;
		}
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		releaseMediaRecorder();
		releaseCamera();
		//System.out.println("调用OnDestroy了！");
	}
	
	

}
