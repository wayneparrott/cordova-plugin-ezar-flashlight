/**
 * Copyright 2015, ezAR Technologies
 * http://ezartech.com
 *
 * By @wayne_parrott, @vridosh, @kwparrott
 *
 * Licensed under a modified MIT license. 
 * Please see LICENSE or http://ezartech.com/ezarstartupkit-license for more information
 *
 */
package com.ezartech.ezar.flashlight;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CordovaWebViewImpl;
import org.apache.cordova.PluginManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;

import android.util.Log;


/**
 * This class echoes a string called from JavaScript.
 */
public class Flashlight extends CordovaPlugin {
	private static final String TAG = "Flashlight";
    
    static final int UNDEFINED  = -1;
    static final int BACK_LIGHT = 0;
    static final int FRONT_LIGHT= 1;
	static final int LIGHT_OFF  = 0;
	static final int LIGHT_ON   = 1;

    private int frontCameraId   = UNDEFINED;
	private int backCameraId    = UNDEFINED;
    private int backLightState 	= LIGHT_OFF;
    private int frontLightState = LIGHT_OFF;

	private int deferredLightLoc = UNDEFINED;
	private int deferredLightState = UNDEFINED;

	private int activeLightCameraId = UNDEFINED;
	private Camera localPreviewCamera = null;
	private Camera activePreviewCamera = null;

	private boolean isVOInstalled = false;
	private int voCameraDir = UNDEFINED;
    private int voCameraId = UNDEFINED;
	private Camera voCamera = null;

	@Override
	public void initialize(CordovaInterface cordova, CordovaWebView webView) {
		super.initialize(cordova, webView);
		// your init code here
	}

	@Override
	public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
		Log.v(TAG, action + " " + args.length());

		if (action.equals("init")) {
			//JPG: 0, PNG: 1
            //save to gallery
			this.init(args, callbackContext);
			return true;
		} else if (action.equals("updateLight")) {
			//JPG: 0, PNG: 1
            //save to gallery
            this.updateLight(args, callbackContext);
			return true;
		}

		return false;
	}

	private void init(final JSONArray args, final CallbackContext callbackContext) {

		try {
			boolean isVOInstalled = args.getBoolean(0);
			int voRunningCameraDir = args.getInt(1);

			init(isVOInstalled,voRunningCameraDir,callbackContext);

		} catch (JSONException e1) {
			e1.printStackTrace();
			callbackContext.error("Invalid argument");
		}
	}

	private void init(boolean isVOInstalled, int voRunningCameraDir, final CallbackContext callbackContext) {

		JSONObject jsonResult = new JSONObject();

		try {
			this.isVOInstalled = isVOInstalled;

			jsonResult.put("front",false);
			jsonResult.put("back",false);

			int mNumberOfCameras = Camera.getNumberOfCameras();

			// Find the ID of the back-facing ("default") camera
			Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
			for (int id = 0; id < mNumberOfCameras; id++) {
				Camera.getCameraInfo(id, cameraInfo);

				Parameters parameters;
				Camera camera = null;
				Camera releasableCamera = null;
				try {

					try {
						if (id != voCameraId) {
							camera = Camera.open(id);
						} else {
							camera = voCamera;
						}

					} catch (RuntimeException re) {
						System.out.println("Failed to open CAMERA: " + id);
						continue;
					}

					parameters = camera.getParameters();

					List<String>torchModes = parameters.getSupportedFlashModes();
					boolean hasLight = torchModes != null && torchModes.contains(Parameters.FLASH_MODE_TORCH);

					if (hasLight) {
						String key=null;

						if (cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK) {
							backCameraId = id;
							key = "back";
						} else {
							frontCameraId = id;
							key = "front";
						}

						jsonResult.put(key,true);
					}

					//determine if camera should be released
					if (id != voCameraId) {
						releasableCamera = camera;
					}
//					if 	((videoOverlayRunningCamera == BACK_LIGHT && cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK) ||
//						 (videoOverlayRunningCamera == FRONT_LIGHT && cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT)) {
//						releasableCamera = null;
//					}

				} finally {
					if (releasableCamera != null) {
						releasableCamera.release();
					}
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "Can't set exception", e);
            callbackContext.error("Unable to access Camera for light information.");
			return;
		}

		callbackContext.success(jsonResult);
	}

	private void updateLight(final JSONArray args, final CallbackContext callbackContext) {

		int lightLoc = 0;
		int newLightState = 0;
		int videoOverlayRunningCameraLoc = 0;
		try {
			lightLoc = args.getInt(0);
			newLightState = args.getInt(1);
			videoOverlayRunningCameraLoc = args.getInt(2);
		} catch (JSONException e1) {
			callbackContext.error("Invalid argument");
			return;
		}

		updateLight(lightLoc,newLightState,videoOverlayRunningCameraLoc,callbackContext);
	}

	private void updateLight(int lightLoc, int newLightState, int videoOverlayRunningCameraLoc, final CallbackContext callbackContext) {
		Camera releaseableCamera = null;
			int cameraId = UNDEFINED;
			int otherCameraId = UNDEFINED;

			if (lightLoc == BACK_LIGHT && backCameraId != UNDEFINED) {
				cameraId = backCameraId;
				otherCameraId = frontCameraId;
			} else if (lightLoc == FRONT_LIGHT && frontCameraId != UNDEFINED) {
				cameraId = frontCameraId;
				otherCameraId = backCameraId;
			} else {
				if (callbackContext != null) {
					callbackContext.error("Invalid camera referenced");
				}
				return;
			}

			//turn off current light if its state == LIGHT_ON
			if (activeLightCameraId != UNDEFINED && activeLightCameraId != cameraId) {
				if (activeLightCameraId == backCameraId && backLightState == LIGHT_ON) {
					//turn light off before turning on new light
					updateLight(BACK_LIGHT,LIGHT_OFF,videoOverlayRunningCameraLoc,null);
				} else if (activeLightCameraId == frontCameraId && frontLightState == LIGHT_ON) {
					//turn light off before turning on new light
					updateLight(FRONT_LIGHT,LIGHT_OFF,videoOverlayRunningCameraLoc,null);
				}
			}

			try {
//				if (isVideoOverlayInstalled && videoOverlayRunningCameraLoc == UNDEFINED) {
//					//defer changes
//					deferredLightLoc = lightLoc;
//					deferredLightState = newLightState;
//				} else if (videoOverlayRunningCameraLoc != UNDEFINED) {
//					//get camera from VO only if it is same camera as lightLoc
//					activePreviewCamera = getActiveVideoOverlayCamera(lightLoc);
//					if (activePreviewCamera != null) {
//						localPreviewCamera = null;
//					}
//				}

				if (localPreviewCamera == null) {
					localPreviewCamera = Camera.open(cameraId);
					activeLightCameraId = cameraId; //CONFIRM THIS IS CORRECT?????
					activePreviewCamera = localPreviewCamera;
				}
				Parameters parameters;
				parameters = activePreviewCamera.getParameters();
				parameters.setFlashMode(newLightState == LIGHT_ON ?
						Parameters.FLASH_MODE_TORCH :
						Parameters.FLASH_MODE_OFF);
				activePreviewCamera.setParameters(parameters);

//				if (localPreviewCamera != null) {
//					if (newLightState == LIGHT_ON && !
//							isVideoOverlayInstalled) {
//						localPreviewCamera.startPreview();
//					} else {
//						localPreviewCamera.stopPreview();
//						releaseableCamera = localPreviewCamera;
//						localPreviewCamera = null;
//						activeLightCameraId = UNDEFINED;
//					}
//				}

			} finally {
				if (releaseableCamera != null) {
					releaseableCamera.release();
				}
			}

			if (callbackContext != null) {
				callbackContext.success();
			}
	}


	@Override
	public void onPause(boolean multitasking) {
		super.onPause((multitasking));

		//todo: turn off light & release camera
	}

	@Override
	public void onResume(boolean multitasking) {
		super.onResume(multitasking);

		//todo: reacquire camera and setup light
	}

	private int getLightLocation(int cameraId) {
		if (cameraId == UNDEFINED) return UNDEFINED;
		if (cameraId == frontCameraId) return FRONT_LIGHT;
		if (cameraId == backCameraId) return BACK_LIGHT;
		return UNDEFINED;
	}

	//reflectively access VideoOverlay plugin to get camera in same direction as lightLoc
	private Camera getActiveVideoOverlayCamera(int lightLoc) {
		Camera camera = null;

		CordovaPlugin videoOverlayPlugin = getVideoOverlayPlugin();
		if (videoOverlayPlugin == null) {
			return camera;
		}

		String methodName = lightLoc == BACK_LIGHT ? "getBackCamera" : "getFrontCamera";
		Method method = null;

		try {
			method = videoOverlayPlugin.getClass().getMethod(methodName);
		} catch (SecurityException e) {
			//e.printStackTrace();
		} catch (NoSuchMethodException e) {
			//e.printStackTrace();
		}

		try {
			if (method != null) {
				camera = (Camera)method.invoke(videoOverlayPlugin);
			}
		} catch (IllegalArgumentException e) { // exception handling omitted for brevity
			//e.printStackTrace();
		} catch (IllegalAccessException e) { // exception handling omitted for brevity
			//e.printStackTrace();
		} catch (InvocationTargetException e) { // exception handling omitted for brevity
			//e.printStackTrace();
		}

		return camera;
	}

	private CordovaPlugin getVideoOverlayPlugin() {
		String pluginName = "videoOverlay";
		CordovaPlugin voPlugin = webView.getPluginManager().getPlugin(pluginName);
		return voPlugin;
	}

	public void videoOverlayStarted(int voCameraDir, int voCameraId, Camera voCamera) {
		this.voCameraDir = voCameraDir;
		this.voCameraId = voCameraId;
		this.voCamera = voCamera;

		//activePreviewCamera = voCamera;

		//updateLight(deferredLightLoc,deferredLightState,voCameraDirection,null);
	}

	public void videoOverlayStopped(int voCameraDir, int voCameraId, Camera voCamera) {
		this.voCameraDir = UNDEFINED;
		this.voCameraId = UNDEFINED;
		this.voCamera = null;

	}

}
