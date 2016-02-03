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

/**
 * Manages a mobile device camera
 * @class
 * 
 * Created by ezar during initialization. Do not use directly.
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
(function() {
	 //--------------------------------------
    var _flashLight = {};
	var _isInitialized;
	var _hasFrontLight;
	var _hasBackLight;
	var _isFrontLightOn;
	var _isBackLightOn;	    
	
	var BACK = 0, FRONT = 1; //light locations
	var OFF = 0, ON = 1;     //light states
	
	 /**
     * Has ezAR been successfully initialized.
     * @return {boolean} true when initialize() completes successfully
     */
    _ezAR.areLightsInitialized = function() {
        return _isInitialized;
    }
   
    /**
     * Initialize ezAR internal state, cameras, light and zoom features.
     * @param {function} [successCB] function called on success
	 * @param {function} [errorCB] function with error data parameter called on error
     */
    _ezAR.initializeLights = function(successCallback,errorCallback) {
        //execute successCallback immediately if already initialized
    	if (_flashLight.areLightsInitialized()) {
           if (isFunction(successCallback)) successCallback();
           return;
        }
        
        var onInit = function(deviceData) {
            //console.log(deviceData);
            _isInitialized = true;
			
			//todo init state here
			_hasFrontLight = true;
			_hasBackLight = true;
			
            if (successCallback) {
                successCallback();
            }
        }
        
        exec(onInit,
             _onError,
            "flashlight",
            "init",
            []);
    }
	
   /**
	 * Identifies if the camera side of the device includes a light
	 * @return {boolean} 
	 */
     _flashLight.hasFrontLight = function() {
		 return _hasFrontLight;
	 }
	 
 	/**
	 * Identifies if the camera side of the device includes a light
	 * @return {boolean} 
	 */
	 _flashLight.hasBackLight = function() {
		 return _hasBackLight;
	 }
	 
	 /**
	 * 
	 */
	 _flashLight.isBackLightOn = function() {
		 return _isBackLightOn;
	 }
	 
	 /**
	 * 
	 */
	 _flashLight.isFrontLightOn = function() {
		 return _isFrontLightOn;
	 }
	 
	
   
	/**
	 * The current light setting, 0 == off, 1 == on
	 * @return {integer} 
	 */
	 _flashLight.setBackLightOn = function(successCallback,errorCallback) {
		 updateLight(BACK,ON,successCallback,errorCallback);
	 }
  

	_flashlight.setCurrentLightOff = function(successCallback,errorCallback) {
		updateLight(BACK,ON,successCallback,errorCallback);
	}
	
	
    //  
	function updateLight(lightPosition, onOffState, successCallback,errorCallback) {
		if (lightPosition == BACK) {
		 	if (!_flashLight.hasBackLight()) return;
		}
		
		if (lightPosition == FRONT) {
		 	if (!_flashLight.hasFrontLight()) return;
		}
		
		if ((lightPosition == BACK  && _flashLight.isBackLightOn()) || 
		    (lightPosition == FRONT && _flashLight.isFrontLightOn()) {
			if (isFunction(successCallback)) successCallback();
           	return;
		}
		 
		var onSuccess = function() {
            //console.log(deviceData);
            if (lightPosition == BACK) _isBackLightOn = true; 
			else  _isBackLightOn = true;
			
			if (successCallback) {
                successCallback();
            }
        }
		
        /*
		exec(onSuccess,
             errorCallback,
            "flashlight",
            "updateLight",
            [lightPosition,onOffState]);
		*/
	 }        
        

	function isFunction(f) {
    	return typeof f == "function";
	}

	return _flashlight;
})()



