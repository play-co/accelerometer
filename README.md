# Game Closure Devkit Plugin : Accelerometer (PRE-RELEASE)

This module provides preliminary support for the built-in accelerometer on iOS and Android devices.

This is a pre-release module because we have not had the time to go back and wrap the NATIVE functions in JS objects.

Installation instructions:

Run this command:

~~~
basil install accelerometer
~~~

Example usage from inside a JS game:

~~~
			NATIVE.events.registerHandler('deviceGravity', bind(this, function(evt) {
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
~~~
