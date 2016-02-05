/*
 * CDVezARSnapshot.m
 *
 * Copyright 2016, ezAR Technologies
 * http://ezartech.com
 *
 * By @wayne_parrott
 *
 * Licensed under a modified MIT license. 
 * Please see LICENSE or http://ezartech.com/ezarstartupkit-license for more information
 *
 */
 
#import "CDVezARFlashlight.h"


const int BACK_LIGHT = 0;
const int FRONT_LIGHT = 1;
const int LIGHT_OFF = 0;
const int LIGHT_ON = 1;

@implementation CDVezARFlashlight
{
    AVCaptureDevice* frontLight;
    AVCaptureDevice* backLight;
    
    int frontLightState;
    int backLightState;
}

// INIT PLUGIN - does nothing atm
- (void) pluginInitialize
{
    [super pluginInitialize];
}

// SETUP EZAR 
// Create camera view and preview, make webview transparent.
// return camera, light features and display details
// 
- (void)init:(CDVInvokedUrlCommand*)command
{
    NSMutableDictionary* lightsInfo = [NSMutableDictionary dictionaryWithCapacity:4];
    [lightsInfo setObject: @NO forKey:@"front"];
    [lightsInfo setObject: @NO forKey:@"back"];
    
    NSError *error;
    NSArray *devices = [AVCaptureDevice devicesWithMediaType:AVMediaTypeVideo];
    for (AVCaptureDevice *device in devices) {
        if (error) break;
        if (!([device isTorchAvailable] && [device isTorchModeSupported:AVCaptureTorchModeOn])) 
            continue;
        
        if ([device position] == AVCaptureDevicePositionBack) {
            backLight = device;
            [lightsInfo setObject: @YES forKey:@"back"];
        } else if ([device position] == AVCaptureDevicePositionFront) {
            frontLight = device;
            [lightsInfo setObject: @YES forKey:@"front"];
        }
    }
    
   NSMutableDictionary* immutableLightsInfo =
    	[NSMutableDictionary dictionaryWithDictionary: lightsInfo];
    
    CDVPluginResult* pluginResult =
        [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary: immutableLightsInfo];
    
        
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

//var BACK = 0, FRONT = 1; //light locations
//var OFF = 0, ON = 1;     //light states
- (void)updateLight:(CDVInvokedUrlCommand*)command
{
    NSNumber* lightLocArg = [command.arguments objectAtIndex:0];
    int lightLoc = (int)[lightLocArg integerValue];
    AVCaptureDevice* light = lightLoc == BACK ? backLight : frontLight;
    AVCaptureDevice* otherLight = light == backLight ? frontLight : backLight;

    NSNumber* newLightStateArg = [command.arguments objectAtIndex:1];
    int newLightState = (int)[newLightStateArg integerValue];
    AVCaptureTorchMode torchMode = newLightState == LIGHT_OFF ? AVCaptureTorchModeOff : AVCaptureTorchModeOn;
    
    if (!light) {
        //error, invalid light referenced
        CDVPluginResult* pluginResult =
            [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString: @"Invalid light identifier, no such light found"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        return;
    }
    
    //check for NOP
    if ((lightLoc == BACK_LIGHT && backLightState == newLightState) ||
        (lightLoc == FRONT_LIGHT && frontLightState == newLightState)) {
        //light is already in newLightState, i.e. NOP
        CDVPluginResult* pluginResult =
            [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        return;        
    }
    
    //check if otherLight needs to be turned off before setting newLightState
    if (otherLight && 
        ((otherLight==frontLight && frontLightState==LIGHT_ON) ||
         (otherLight==backLight && backLightState==LIGHT_ON))) {
        BOOL success = [otherLight lockForConfiguration:nil];
        if (success) {  
          [otherLight setTorchMode: AVCaptureTorchModeOff];
          [otherLight unlockForConfiguration];
         }
         if (otherLight == frontLight) frontLightState = LIGHT_OFF;
         else if (otherLight == backLight) backLightState = LIGHT_OFF;
    }
    
    BOOL success = [light lockForConfiguration:nil];
    if (success) {  
        [light setTorchMode:torchMode];
        [light unlockForConfiguration];
        if (light == frontLight) frontLightState = newLightState;
         else if (light == backLight) backLightState = newLightState;
         
    } else {
        //error, 
        CDVPluginResult* pluginResult =
            [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString: @"Unable to lock light for update"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        return;
    }
    
    CDVPluginResult* pluginResult =
        [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

@end
