/*
 * flashlight.js
 * Copyright 2016, ezAR Technologies
 * Licensed under a modified MIT license, see LICENSE or http://ezartech.com/ezarstartupkit-license
 *
 * @file Implements the ezar flashlight api for controlling device lights, . 
 * @author @wayne_parrott
 * @version 0.1.0 
 */

var exec = require('cordova/exec'),
    utils = require('cordova/utils');

module.exports = (function() {
	 //--------------------------------------
    var BACK = 0, FRONT = 1; //light locations
	var UNDEFINED = -1, OFF = 0, ON = 1;     //light states
	
	var _flashlight       = {};
	var _isInitialized    = false;
	var _frontLightState  = UNDEFINED;
	var _backLightState   = UNDEFINED;	    
	
	
    _flashlight.areLightsInitialized = function() {
        return _isInitialized;
    }

	/**
	* Manages a mobile device lights
	* @class
	* 
	* Created by ezar flashlight during initialization. Do not use directly.
	* 
	* @param {ezAR} ezar  protected 
	* @param {string} id  unique camera id assigned by the device os
	* @param {string} position  side of the device the camera resides, BACK
	* 			faces away from the user, FRONT faces the user
	* @param {boolean} hasZoom  true if the camera's magnification can be changed
	* @param {float} zoom  current magnification level of the camera up 
	* 			to the maxZoom
	* @param {boolean} hasLight true when the device has a light on the
	* 			same side as the camera position; false otherwise
	* @param {integer} light  if the device has a light on the same side 
	* 			(position) as the camera then 1 turns on the device light, 
	* 			0 turns the light off
	*/  
    _flashlight.initializeLights = function(successCallback,errorCallback) {
        //execute successCallback immediately if already initialized
    	if (_flashlight.areLightsInitialized()) {
           if (isFunction(successCallback)) successCallback();
           return;
        }
        
        var onInit = function(lightData) {
            console.log(lightData);
            _isInitialized = true;
			
			//todo init state here
			_frontLightState = lightData.front ? OFF : UNDEFINED;
			_backLightState = lightData.back ? OFF : UNDEFINED;
			
            if (successCallback) {
                successCallback();
            }
        }
        
        exec(onInit,
             errorCallback,
            "flashlight",
            "init",
            [isVideoOverlayInstalled(), getVideoOverlayRunningCameraLocation()]);
    }
	
    
    /**
	 * 
	 */
    _flashlight.hasFrontLight = function() {
		return _frontLightState != UNDEFINED;
	}
	 
    /**
	 * 
	 */
	 _flashlight.hasBackLight = function() {
		 return _backLightState != UNDEFINED;
	 }
	 

	 /**
	  * 
	  */
	 _flashlight.isBackLightOn = function() {
		 return _backLightState == ON;
	 }
	 
	 /**
	  * 
	  */
	 _flashlight.isFrontLightOn = function() {
		 return _frontLightState == ON;
	 }
	 
	 /**
	  * 
	  */
     _flashlight.setBackLightOn = function(successCallback,errorCallback) {
		 updateLight(BACK,ON,successCallback,errorCallback);
	 }
	 
	 /**
	  * 
	  */
	 _flashlight.setFrontLightOn = function(successCallback,errorCallback) {
		 updateLight(FRONT,ON,successCallback,errorCallback);
	 }
     
     /**
	 * 
	 */
	_flashlight.setCurrentLightOff = function(successCallback,errorCallback) {
		var curLightOn = getCurrentOnLight();
		if (curLightOn==UNDEFINED) return;
		
		updateLight(curLightOn,OFF,successCallback,errorCallback);
	}


  	function getCurrentOnLight() {
		  if (_flashlight.isFrontLightOn()) {
			  return FRONT;
		  } else  if (_flashlight.isBackLightOn()) {
			  return BACK;
		  } 
		  
		  return UNDEFINED;
	  }
 
	function updateLight(lightPosition, onOffState, successCallback,errorCallback) {
		if (lightPosition == BACK) {
		 	if (!_flashlight.hasBackLight()) return;
			if (_backLightState == onOffState) {
				if (isFunction(successCallback)) successCallback();
				return;	
			}
		}
		
		if (lightPosition == FRONT) {
		 	if (!_flashlight.hasFrontLight()) return;
			if (_frontLightState == onOffState) {
				if (isFunction(successCallback)) successCallback();
				return;
			}
		} 
				
        //set default state
		_frontLightState = _frontLightState == UNDEFINED ? UNDEFINED : OFF;
		_backLightState = _backLightState == UNDEFINED ? UNDEFINED : OFF;
		 
		var onSuccess = function() {
            //console.log(deviceData);
            if (lightPosition == BACK) 
				_backLightState = onOffState; 
			else  
				_frontLightState = onOffState;
			
			if (successCallback) {
                successCallback();
            }
        }
		
		exec(onSuccess,
             errorCallback,
            "flashlight",
            "updateLight",
            [lightPosition, onOffState, getVideoOverlayRunningCameraLocation()]);
	 }        
      
       
	function isFunction(f) {
    	return typeof f == "function";
	}
  
    function isAndroidPlatform() {
        return window.cordova.platformId == "android";
    }
    
    function isVideoOverlayInstalled() {
        return !!window.ezar["initializeVideoOverlay"]
    }
    
    function isVideoOverlayRunning() {
        if (!isVideoOverlayInstalled) return false;
        
        return window.ezar.hasActiveCamera();
    }
    
    function getVideoOverlayRunningCameraLocation() {
        if (!isVideoOverlayRunning) return UNDEFINED;
        
        //todo access & return the running camera location
        
        return UNDEFINED
    }
	
	return _flashlight;
}())



