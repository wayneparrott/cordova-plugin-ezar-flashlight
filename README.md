#ezAR Flashlight Cordova Plugin
Control the on/off state of the lights on a mobile device.

##Supported Platforms
- iOS 7, 8 & 9
- Android 4.2 and greater 

##Getting Started
Add the flashlight plugin to your Corodva project the Cordova CLI

        cordova plugin add pathToEzar/com.ezartech.ezar.flashlight

Next in your Cordova JavaScript deviceready handler include the following JavaScript snippet to initialize ezAR and activate the camera on the back of the device.

        ezar.initializeLights(
            function(deviceLightInfo) {
                //do something with the info {front: true, back: true}
                },
            function(err) {
                alert('Initialization error: ' + err);
                });
        
        if (ezarHasBackLight()) {
            ezar.setBackLightOn(successCallback,errorCallback);
        }
        
        ezar.setCurrentLightOff(successCallback,errorCallback);
                    
##Additional Documentation        
See [ezartech.com](http://ezartech.com) for documentation and support.

##License
The ezAR Snapshot is licensed under a [modified MIT license](http://www.ezartech.com/ezarstartupkit-license).


Copyright (c) 2015-2016, ezAR Technologies


