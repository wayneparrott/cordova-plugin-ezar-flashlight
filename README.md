# ezAR Flashlight Cordova Plugin
Control the on/off state of the light on a mobile device. This plugin works with 
or without the VideoOverlay plugin. 

## Notes
* The plugin must be initialized before setting the light state.
* On Android devices, setting the light on will not take effect immediately when 
the VideoOverlay plugin is installed. The light will only turn on when video 
preview is started for the camera on the same side of the device as the light. 
This is due to the tight coupling between camera and light functionality of many 
Android devices. In such cases the light will automatically stop when the camera 
preview is stopped.

## Supported Platforms
- iOS 7, 8 & 9
- Android 4.2 and greater 

## Getting Started
Add the flashlight plugin to your Corodva project the Cordova CLI.

        cordova plugin add cordova-plugin-ezar-flashlight

Next initialize the Flashlight plugin and turn the light on.

        ezar.initializeLights(
            function(deviceLightInfo) {
                //do something with the info {front: true, back: true}
                if (ezar.hasLight()) {
                   ezar.setLightOn(successCallback,errorCallback);
                }
            },
            function(err) {
                alert('Initialization error: ' + err);
            });
        
        
When the light is no longer needed turn it off as follows:
        
        ezar.setCurrentLightOff(successCallback,errorCallback);
                    

## Additional Documentation        
See [ezartech.com](http://ezartech.com) for documentation and support.

## License
The ezAR Snapshot is licensed under a [modified MIT license](http://www.ezartech.com/ezarstartupkit-license).


Copyright (c) 2015-2017, ezAR Technologies


