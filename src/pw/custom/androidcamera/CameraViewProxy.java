/**
 * This file was auto-generated by the Titanium Module SDK helper for Android
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 *
 */
package pw.custom.androidcamera;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.view.TiUIView;

import android.util.Log;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;


// This proxy can be created by calling CustomAndroidCamera.createExample({message: "hello world"})
@Kroll.proxy(creatableInModule=CustomAndroidCameraModule.class)
public class CameraViewProxy extends TiViewProxy
{
	// Constructor
	public CameraViewProxy()
	{
		super();
	}
	
	// Standard Debugging variables
	private static final String TAG = "CameraViewProxy";
	private static String SAVE = "camera";
	private static Boolean FRONT_CAMERA = false;
	private static int PICTURE_TIMEOUT = 1000;
	
	private double aspectRatio = 1;
	
	private class CameraView extends TiUIView implements SurfaceHolder.Callback
	{
		private Camera camera;

		public CameraView(TiViewProxy proxy) {
			super(proxy);
			
			SurfaceView preview = new SurfaceView(proxy.getActivity());
			SurfaceHolder previewHolder = preview.getHolder();
			previewHolder.addCallback(this);
			previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
			
			FrameLayout previewLayout = new FrameLayout(proxy.getActivity());
			previewLayout.addView(preview, layoutParams);
			
			setNativeView(previewLayout);
		}
		
		// added by michael browne
		// Return the current camera instance
		public Camera currentCameraInstance(){
			return this.camera;
		}
		
		@Override
		public void processProperties(KrollDict d)
		{	
			super.processProperties(d);
			
			if(d.containsKey("save_location")){
				SAVE = d.getString("save_location");
			}
			
			if( d.containsKey("useFrontCamera") ){
				Log.i(TAG, "Front Camera Property exists!");
				FRONT_CAMERA = d.getBoolean("useFrontCamera");
			}
			
			if( d.containsKey("pictureTimeout")){
				PICTURE_TIMEOUT = d.getInt("pictureTimeout");
			}
		}

		@Override
		public void surfaceChanged(SurfaceHolder previewHolder, int format, int width,
				int height) {
			// TODO Auto-generated method stub
			Log.i(TAG, "Starting Preview");
			camera.startPreview();
		}

		@Override
		public void surfaceCreated(SurfaceHolder previewHolder) {
			// TODO Auto-generated method stub
			Log.i(TAG, "Opening Camera");
			try
			{
				this.camera = getCameraInstance();
				
				Log.i(TAG, "Setting Preview Display");
				camera.setPreviewDisplay(previewHolder);
				camera.setDisplayOrientation(90);
			
				Parameters cameraParams = camera.getParameters();
				Camera.Size optimalSize=getPreviewSize(cameraParams, previewHolder.getSurfaceFrame());
				//cameraParams.setPreviewSize(optimalSize.width, optimalSize.height);
				if( isAutoFocusSupported() ) {
					Log.i(TAG, "Auto Focus is Supported");
					cameraParams.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
				}
				
				if( hasFlash() ) {
					Log.i(TAG, "Flash is supported");
					cameraParams.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
				}
				
				//Camera.Size pictureSize=getLowResolutionPictureSize(cameraParams);
				cameraParams.setPictureSize(optimalSize.width, optimalSize.height);
				
				camera.setParameters(cameraParams);
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder arg0) {
			// TODO Auto-generated method stub
			camera.release();
			camera=null;
		}
		
		public Camera getCameraInstance()
		{
			Camera c = null;
			try
			{
				if( FRONT_CAMERA && hasFrontCamera() ) {
					Log.i(TAG, "Using Front Camera");
					c = Camera.open( Camera.CameraInfo.CAMERA_FACING_FRONT );
				} else {
					Log.i(TAG, "Using Back Camera");
					c = Camera.open();
				}
			}
			catch( Exception e )
			{
				Log.d(TAG, "Camera not available");
			}
			return c;
		}
	}
	
	// changed by michael browne
	private TiUIView view = null;
	private Activity act = null;
	
	@Override
	public TiUIView createView(Activity activity)
	{
		view = new CameraView(this);
		act = activity;
		view.getLayoutParams().autoFillsHeight = true;
		view.getLayoutParams().autoFillsWidth = true;
		return view;
	}

	// Handle creation options
	@Override
	public void handleCreationDict(KrollDict options)
	{
		super.handleCreationDict(options);
		
		if (options.containsKey("save_location")) {
			SAVE = options.getString("save_location");
		}
	}
	
	// Added by michael browne
	@Kroll.method
	public void setSaveLocation(String location)
	{
		SAVE = location;
	}
	
	// Added by michael browne
	@Kroll.method
	public void snapPicture()
	{
		Log.i(TAG, "Snap");
		Camera cam = ((CameraView) view).currentCameraInstance();
		cam.autoFocus(mAutoFocusCallback);
		// cam.takePicture(null, null, mPicture);
	}
	
	// Added by michael browne
	private void triggerEvent( String path )
	{
		KrollDict imagePath = new KrollDict();
		
		File extFile = new File(path);
		Uri uriPath = Uri.fromFile(extFile);
		imagePath.put("path", uriPath.toString());
		
		Log.i(TAG, "Sending path back to Titanium. Image Path > "+uriPath.toString());
		
		fireEvent("picture_taken", imagePath);
	}
	
	//Added by michael browne
	private void rotatePicture( String path ){
		// Try and get the images metadata
		try {
			ExifInterface ei = new ExifInterface(path);
			
			// Get the orientation from the meta data
			int picture_orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
			int device_orientation = act.getWindowManager().getDefaultDisplay().getOrientation();
			
			// Both give the same reading so wondering if the camera is rotated at all??
			Log.i(TAG, "Picture Orientation is "+picture_orientation);
			Log.i(TAG, "Device Orientation is "+device_orientation);
			
			doRotation(path, FRONT_CAMERA ? 270 : 90); // Just rotate 90 degrees.... may cause problems on some devices
			
			// Do the rotation depending on the orientation
			/*switch(picture_orientation){
				case ExifInterface.ORIENTATION_ROTATE_90:
					doRotation(path, 90);
					break;
				case ExifInterface.ORIENTATION_ROTATE_180:
					doRotation(path, 180);
					break;
			}*/
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
	
	// Added by michael browne
	private void doRotation( String path, float rotate ){
		// Get a Bitmap representation of the image
		BitmapFactory bFactory = new BitmapFactory();
		Bitmap bmap = bFactory.decodeFile(path);
		
		// Create the matrix for rotating the bitmap
		Matrix matrix = new Matrix();
		matrix.setRotate(rotate, bmap.getWidth()/2, bmap.getHeight()/2);
		
		// Create a new version of the bitmap - but rotated
		Bitmap rotated = Bitmap.createBitmap(bmap, 0, 0, bmap.getWidth(), bmap.getHeight(), matrix, true);
		
		// Save the new bitmap to a byte array
		File rotatedFile = new File(path);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		rotated.compress(CompressFormat.JPEG, 80, bos); // Best quality over 80
		byte[] bitmapData = bos.toByteArray();
		
		// Try to write (overwrite) the file
		try{
			FileOutputStream fos = new FileOutputStream(rotatedFile);
			fos.write(bitmapData);
			fos.close();
		} catch (FileNotFoundException e){
			Log.i(TAG, "File Not Found: "+e);
		} catch (IOException e){
			Log.i(TAG, "IO Error: "+e);
		}
		
	}
	
	private AutoFocusCallback mAutoFocusCallback = new AutoFocusCallback()
	{

		@Override
		public void onAutoFocus(boolean arg0, Camera camera) {
			// TODO Auto-generated method stub
			Log.i(TAG, "On Auto Focus");
			camera.takePicture(null, null, mPicture);
		}
		
	};
	
	// Added by michael browne
	private PictureCallback mPicture = new PictureCallback()
	{

		@Override
		public void onPictureTaken(byte[] data, Camera c) {
			// TODO Auto-generated method stub
			Log.i(TAG, "On Picture Taken");
			File pictureFile = getOutputMediaFile(); //1 corresponds to MEDIA_TYPE_IMAGE
			
			if( pictureFile == null ) return;
			
			try{
				FileOutputStream fos = new FileOutputStream(pictureFile);
				fos.write(data);
				fos.close();
				
				// Rotate Picture
				rotatePicture(pictureFile.getPath());
				
				// Trigger 
				triggerEvent(pictureFile.getPath());
				
				// Restart Preview
				if (PICTURE_TIMEOUT >= 0) {
					final Camera cam = c;
					new android.os.Handler().postDelayed(
					    new Runnable() {
					        public void run() {
					            Log.i("tag", "This'll run 300 milliseconds later");
					            cam.startPreview();
					        }
					    }, PICTURE_TIMEOUT);
				}
			} catch (FileNotFoundException e){
				Log.i(TAG, "File Not Found: "+e);
			} catch (IOException e){
				Log.i(TAG, "IO Error: "+e);
			}
		}
		
	};
	
	// Added by michael browne
	private static File getOutputMediaFile(){
		
		File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),SAVE);
		
		if( !mediaStorageDir.exists() ){
			if( !mediaStorageDir.mkdirs()){
				Log.i("CAMERA FILE SYSTEM", "failed to create directory");
				return null;
			}
		}
		
		// Create a media file
		String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		
		File mediaFile;
		mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timestamp + ".jpg");
		return mediaFile;
	}
	
	
	/*
	 * Function to get a Low Resolution Picture Size
	 * Low Res defined as 720x480 pixels (are close to it)
	 * @param Camera.Parameters Parameters for the camera
	 * @return Camera.Size Best size match
	 */
	private Camera.Size getLowResolutionPictureSize(Camera.Parameters parameters){
		int idealArea = 720*480;
		int diff = Integer.MAX_VALUE;
		Camera.Size result = null;
		
		for( Camera.Size size : parameters.getSupportedPictureSizes() ){
			int area = size.width * size.height;
			if( Math.abs(idealArea - area) < diff ){
				diff = Math.abs(idealArea - area);
				result = size;
			}
		}
		
		return result;
	}
	
	/*
	 * Function to get a High Resolution Picture Size
	 * @param Camera.Parameters Parameters for the camera
	 * @return Camera.Size Best size match
	 */
	private Camera.Size getPreviewSize(Camera.Parameters parameters, Rect holderSize){
		int previewWidth = holderSize.width();
		int previewHeight = holderSize.height();

		// Set the preview size to the most optimal given the target size
		Camera.Size optimalPreviewSize = getOptimalPreviewSize(getSupportedPictureSizes(parameters), previewWidth, previewHeight);
		if (optimalPreviewSize != null) {
			if (previewWidth > previewHeight) {
				aspectRatio = (double) optimalPreviewSize.width / optimalPreviewSize.height;
			} else {
				aspectRatio = (double) optimalPreviewSize.height / optimalPreviewSize.width;
			}
		}
		if (previewHeight < previewWidth / aspectRatio) {
			previewHeight = (int) (previewWidth / aspectRatio + .5);

		} else {
			previewWidth = (int) (previewHeight * aspectRatio + .5);
		}
		
		return optimalPreviewSize;
	}
	
	/**
	 * Computes the optimal preview size given the target display size and aspect ratio.
	 * 
	 * @param supportPreviewSizes
	 *            a list of preview sizes the camera supports
	 * @param targetSize
	 *            the target display size that will render the preview
	 * @return the optimal size of the preview
	 */
	private static Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h)
	{
		double targetRatio = 1;
		if (w > h) {
			targetRatio = (double) w / h;
		} else {
			targetRatio = (double) h / w;
		}
		if (sizes == null) {
			return null;
		}
		Camera.Size optimalSize = null;
		double minAspectDiff = Double.MAX_VALUE;

		// Try to find an size match aspect ratio and size
		for (Camera.Size size : sizes) {
			double ratio = (double) size.width / size.height;
			if (Math.abs(ratio - targetRatio) < minAspectDiff) {
				optimalSize = size;
				minAspectDiff = Math.abs(ratio - targetRatio);
			}
		}
		
		return optimalSize;
	}
	
	/**
	 * Android에서 지원되는 사진 Size 리스트를 반환한다.
	 * 
	 * @return
	 */
	public List<Camera.Size> getSupportedPictureSizes(Camera.Parameters parameters) {
	    List<Camera.Size> pictureSizes = parameters.getSupportedPictureSizes();
	             
	    pictureSizes = checkSupportedPictureSizeAtPreviewSize(pictureSizes, parameters);
	     
	    return pictureSizes;
	}
	 
	private List<Camera.Size> checkSupportedPictureSizeAtPreviewSize(List<Camera.Size> pictureSizes, Camera.Parameters parameters) {
	    List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();
	    Camera.Size pictureSize;
	    Camera.Size previewSize;
	    double  pictureRatio = 0;
	    double  previewRatio = 0;
	    final double aspectTolerance = 0.05;
	    boolean isUsablePicture = false;
	     
	    for (int indexOfPicture = pictureSizes.size() - 1; indexOfPicture >= 0; --indexOfPicture) {
	        pictureSize = pictureSizes.get(indexOfPicture);
	        pictureRatio = (double) pictureSize.width / (double) pictureSize.height;
	        isUsablePicture = false;
	         
	        for (int indexOfPreview = previewSizes.size() - 1; indexOfPreview >= 0; --indexOfPreview) {
	            previewSize = previewSizes.get(indexOfPreview);
	             
	            previewRatio = (double) previewSize.width / (double) previewSize.height;
	             
	            if (Math.abs(pictureRatio - previewRatio) < aspectTolerance) {
	                isUsablePicture = true;
	                break;
	            }
	        }
	         
	        if (isUsablePicture == false) {
	            pictureSizes.remove(indexOfPicture);
	            //Logger.d(TAG, "remove picture size : " + pictureSize.width + ", " + pictureSize.height);
	        }
	    }
	    
	    return pictureSizes;
	}
	
	/**
	 * Function to determine if flash support is available
	 * @return Boolean Flash Support
	 */
	private boolean hasFlash(){
		Camera cam = ((CameraView) view).currentCameraInstance();
		Parameters params = cam.getParameters();
	    List<String> flashModes = params.getSupportedFlashModes();
	    if(flashModes == null) {
	        return false;
	    }

	    for(String flashMode : flashModes) {
	        if(Parameters.FLASH_MODE_ON.equals(flashMode)) {
	            return true;
	        }
	    }

	    return false;
	}
	
	/**
	 * Function to determine if a front camera exists
	 * @return Boolean Front Camera Exists
	 */
	
	private boolean hasFrontCamera(){
		int numCameras= Camera.getNumberOfCameras();
		for(int i=0;i<numCameras;i++){
		    Camera.CameraInfo info = new CameraInfo();
		    Camera.getCameraInfo(i, info);
		    if(Camera.CameraInfo.CAMERA_FACING_FRONT == info.facing){
		        return true;
		    }
		}
		return false;
	}
	
	/**
	 * Function to determine if Auto Focus is supported
	 * @return Boolean Auto Focus Supported
	 */
	
	private boolean isAutoFocusSupported() {
		Camera cam = ((CameraView) view).currentCameraInstance();
		List<String> supportedFocusModes = cam.getParameters().getSupportedFocusModes();
		return supportedFocusModes != null && supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO);
	}
}