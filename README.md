# Game Closure Devkit Plugin : Accelerometer

This module provides preliminary support for the built-in accelerometer on iOS
and Android devices, including a helper library for simply detecting if the user
is shaking the device.


## Demo
Check out [the demo
application](https://github.com/gameclosure/demoAccelerometer)


## Installation
Install the module using using the standard devkit install process:

~~~
devkit install https://github.com/gameclosure/accelerometer#v2.0.0
~~~


## Usage
Example usage from inside a JS game:

~~~
import accelerometer;
~~~

~~~
var pi = Math.PI;
var abs = Math.abs;
var atan2 = Math.atan2;

accelerometer.start(bind(this, function(evt) {
    var x = -evt.x;
    var y = -evt.y;
    var z = -evt.z;
    var forwardTilt = atan2(z, y);
    var tilt = atan2(x, y);
    var twist = atan2(x, z);
    var t = abs(forwardTilt / (pi / 2));
    if (t > 1) {
        t = 2 - t;
    }
    if (twist > pi / 2) {
        twist = (pi - twist);
    }
    if (twist < -pi / 2) {
        twist = -(pi + twist);
    }

    var interpolatedTilt = tilt * (1-t) + twist * t;

    this.controller.tilt = -interpolatedTilt;
}));

...

accelerometer.stop();
~~~

The Accelerometer plugin also includes a ShakeDetect JS module inside the util
folder.

Example usage:

~~~
import ui.TextView as TextView;
import accelerometer.util.shakedetect as shakedetect;


SHAKE_COOLDOWN = 2000;

exports = Class(GC.Application, function () {

  this.initUI = function () {
    this.shakeText = new TextView({
      superview: this.view,
      text: "...",
      color: "white",
      x: 0,
      y: 150,
      width: this.view.style.width,
      height: 100
    });

    // turn off shakedetect on hide, on on show
    GC.on('hide', function () { shakedetect.stop(); });
    GC.on('show', bind(this, function () { this.startshakedetect(); }));

    // start listening for shakes
    this.shakeCooldown = 0;
    this.startshakedetect();
  };

  this.startshakedetect = function () {
    shakedetect.start(bind(this, function() {
      logger.log("User shook the phone!");
      this.shakeText.setText("Don't shake me bro!");
      this.shakeCooldown = SHAKE_COOLDOWN;
    }));
  };

  this.tick = function (dt) {
    if (this.shakeCooldown > 0) {
      this.shakeCooldown -= dt;

      if (this.shakeCooldown <= 0) {
        this.shakeText.setText('...');
      }
    }
  };
});
~~~

Be sure to call .stop() when done handling events.  Also, only one event handler
can be specified of either type at a time.

