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

	private int activeLightCameraId = UNDEFINED;
	private Camera localPreviewCamera = null;

	//------------ vo support ---------

	private int voCameraDir = UNDEFINED;	//updated by VO start/stop events
    private int voCameraId = UNDEFINED; 	//updated by VO start/stop events
	private Camera voCamera = null; 		//updated by VO start/stop events

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
			this.init(callbackContext);
//			this.init(args, callbackContext);
			return true;
		} else if (action.equals("updateLight")) {
			//JPG: 0, PNG: 1
            //save to gallery
            this.updateLight(args, callbackContext);
			return true;
		}

		return false;
	}

	private void init(final CallbackContext callbackContext) {

		JSONObject jsonResult = new JSONObject();

		try {
			jsonResult.put("front",false);
			jsonResult.put("back",false);

			int mNumberOfCameras = Camera.getNumberOfCameras();

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

		try {
			int lightLoc = lightLoc = args.getInt(0);
			int newLightState = args.getInt(1);

			if (isVOInstalled()) {
				updateLightWithVO(lightLoc, newLightState, callbackContext);
			} else {
				updateLight(lightLoc, newLightState, callbackContext);
			}

		} catch (JSONException e1) {
			callbackContext.error("Invalid argument");
			return;
		}

	}

	//assumptions:
	//  voInstalled is true
	//  voCamera == null -> no camera running atm so defer setting light
	//
	private void updateLightWithVO(int lightLoc, int newLightState, final CallbackContext callbackContext) {
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
				callbackContext.error("Light does not exist.");
			}
			return;
		}

		if (voCameraId != UNDEFINED && voCameraId != cameraId) {
			//not allowed to set light to different side of device than running camera
			if (callbackContext != null) {
				callbackContext.error("Light and active camera must be on same side of device.");
			}
			//todo reset state
			return;
		}

		activeLightCameraId = cameraId;
		if (activeLightCameraId == frontCameraId) {
			frontLightState = newLightState;
			if (backLightState != UNDEFINED) backLightState = (newLightState+1) % 2;
		} else {
			backLightState = newLightState;
			if (frontLightState != UNDEFINED) frontLightState = (newLightState+1) % 2;
		}

		boolean deferred = voCamera == null; //defer setting

		if (deferred && newLightState == LIGHT_OFF) {
			//SPECIAL CASE: force the light off for devices that can run light without camera running
			updateLight(lightLoc,LIGHT_OFF,null);
			return;
		}

		if (!deferred) { //deferred start
			//update Light
			Parameters parameters;
			parameters = voCamera.getParameters();
			parameters.setFlashMode(newLightState == LIGHT_ON ?
					Parameters.FLASH_MODE_TORCH :
					Parameters.FLASH_MODE_OFF);
			voCamera.setParameters(parameters);
		}

		if (callbackContext != null) {
			callbackContext.success();
		}
	}

	private void updateLightIfDeferred() {

	}

	private void updateLight(int lightLoc, int newLightState, final CallbackContext callbackContext) {
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
					updateLight(BACK_LIGHT,LIGHT_OFF,null);
				} else if (activeLightCameraId == frontCameraId && frontLightState == LIGHT_ON) {
					//turn light off before turning on new light
					updateLight(FRONT_LIGHT,LIGHT_OFF,null);
				}
			}

			try {

				if (localPreviewCamera == null) {
					localPreviewCamera = Camera.open(cameraId);
					activeLightCameraId = cameraId; //CONFIRM THIS IS CORRECT?????
				}
				Parameters parameters;
				parameters = localPreviewCamera.getParameters();
				parameters.setFlashMode(newLightState == LIGHT_ON ?
						Parameters.FLASH_MODE_TORCH :
						Parameters.FLASH_MODE_OFF);
				localPreviewCamera.setParameters(parameters);

				if (localPreviewCamera != null) {
					if (newLightState == LIGHT_ON) {
						localPreviewCamera.startPreview();
					} else {
						localPreviewCamera.stopPreview();
						releaseableCamera = localPreviewCamera;
						localPreviewCamera = null;
						activeLightCameraId = UNDEFINED;
					}
				}

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

	//------------------------------------------------------------------
	//reflectively access VideoOverlay plugin to get camera in same direction as lightLoc

	private CordovaPlugin getVideoOverlayPlugin() {
		String pluginName = "videoOverlay";
		CordovaPlugin voPlugin = webView.getPluginManager().getPlugin(pluginName);
		return voPlugin;
	}

	private boolean isVOInstalled() {
		return getVideoOverlayPlugin() != null;
	}

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


	public void videoOverlayStarted(int voCameraDir, int voCameraId, Camera voCamera) {
		this.voCameraDir = voCameraDir;
		this.voCameraId = voCameraId;
		this.voCamera = voCamera;

		if (backLightState == LIGHT_ON) {
			updateLightWithVO(BACK_LIGHT,LIGHT_ON,null);
		} else if  (frontLightState == LIGHT_ON) {
			updateLightWithVO(FRONT_LIGHT,LIGHT_ON,null);
		}
	}

	public void videoOverlayStopped(int voCameraDir, int voCameraId, Camera voCamera) {
		this.voCameraDir = UNDEFINED;
		this.voCameraId = UNDEFINED;
		this.voCamera = null;

		//apply rule that light can not be on unless camera is running
		//so turn active light off but reset the light back to its LIGHT_ON deferred state
		// and it will turn back on when camera is restarted. User must turn the light off explicitly.
		if (backLightState == LIGHT_ON) {
			updateLightWithVO(BACK_LIGHT,LIGHT_OFF,null);
			backLightState = LIGHT_ON; //set light back on until user turns it off
		} else if  (frontLightState == LIGHT_ON) {
			updateLightWithVO(FRONT_LIGHT,LIGHT_OFF,null);
			frontLightState = LIGHT_ON; //set light back on until user turns it off
		}
	}

}
