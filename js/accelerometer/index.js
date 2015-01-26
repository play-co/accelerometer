var hasNativeEvents = NATIVE && NATIVE.plugins && NATIVE.plugins.sendEvent;

var handleAccel = null;

if (hasNativeEvents) {
	NATIVE.events.registerHandler('accelerometerEvent', function(evt) {
		if (handleAccel) {
			// evt.x, evt.y, evt.z are unscaled x,y,z acceleration including gravity
			// iOS seems to send 1.0 = force of gravity, but other devices may
			// do it differently
			handleAccel(evt);
		}
	});
}

var Accelerometer = Class(function () {
	this.start = function(handler) {
		if (hasNativeEvents) {
			NATIVE.plugins.sendEvent("AccelerometerPlugin", "startEvents",
				JSON.stringify({}));
		}
		handleAccel = handler;
	};

	this.stop = function() {
		if (hasNativeEvents) {
			NATIVE.plugins.sendEvent("AccelerometerPlugin", "stopEvents",
				JSON.stringify({}));
		}
	};

});

exports = new Accelerometer();

