# Game Closure Devkit Plugin : Accelerometer

This module provides preliminary support for the built-in accelerometer on iOS and Android devices.

Installation instructions:

Run this command:

~~~
basil install accelerometer
~~~

Example usage from inside a JS game:

~~~
import plugins.accelerometer.accelerometer as accelerometer;
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

The Accelerometer plugin also includes a ShakeDetect JS module.

Example usage:

~~~
import plugins.accelerometer.ShakeDetect as ShakeDetect;
~~~

~~~
ShakeDetect.start(function() {
		logger.log("User shook the phone").
});

...

ShakeDetect.stop();
~~~

Be sure to call .stop() when done handling events.  Also, only one event handler
can be specified of either type at a time.

