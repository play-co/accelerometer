import .accelerometer;

var samples = [];
var sampleCount = 0;
var lastSample = {};
var smoothedAmp = 0;
var lastCheck = 0, lastShake = 0;
var lastX = 0, lastY = 0, lastZ = 0;
var shakeHandler = null;

var accelerometerHandler = function(evt) {
	var x = evt.x, y = evt.y, z = evt.z;
	var amp = Math.sqrt(x*x + y*y + z*z);

	// Eliminate scaling issues between devices, normalized to gravity
	var samp = smoothedAmp;
	if (!samp) {
		samp = amp;
	} else {
		samp = smoothedAmp * 0.95 + amp * 0.05;
	}
	smoothedAmp = samp;

	// Normalize by smoothed amplitude
	x = x / samp;
	y = y / samp;
	z = z / samp;

	// This is not tied to any particular time interval so may
	// behave differently on different devices
	var dx = x - lastX;
	var dy = y - lastY;
	var dz = z - lastZ;
	lastX = x;
	lastY = y;
	lastZ = z;

	var sampleIndex = sampleCount++;
	var sample = samples[sampleIndex];
	if (!sample) {
		samples[sampleIndex] = {
			'x': dx,
			'y': dy,
			'z': dz,
			'amp': amp
		};
	} else {
		sample.x = dx;
		sample.y = dy;
		sample.z = dz;
		sample.amp = amp;
	}

	// Every second,
	var now = +new Date();
	if (now - lastCheck < 1000) {
		return;
	}

	// Find the most intense acceleration in the set
	var best = samples[0];
	var bestAmp = best && best.amp;
	for (var i = 1; i < sampleCount; i++) {
		var sample = samples[i];
		var amp = sample.amp;

		if (amp > bestAmp) {
			bestAmp = amp;
			best = sample;
		}
	}

	// If useful data,
	if (best) {
		var last = 0;
		var zeroCrossings = 0;

		// For each sample,
		for (var j = 0; j < sampleCount; j++) {
			var sample = samples[j];
			var dot = sample.x * best.x + sample.y * best.y + sample.z * best.z;

			// If there is some motion,
			if (Math.abs(dot) > 0.0015) {
				if ((dot < 0 && last > 0) || (dot > 0 && last < 0)) {
					++zeroCrossings;
				}

				last = dot;
			}
		}

		// If 4 shakes in ~2 seconds,
		if (zeroCrossings >= 4) {
			if (shakeHandler) {
				// If not shaken recently,
				if (now - lastShake > 1000) {
					shakeHandler();
					lastShake = now;
				}
			}
		}
	}

	lastCheck = now;

	// Keep half the data
	var half = Math.floor(sampleCount / 2);
	samples = samples.splice(half, sampleCount);
	sampleCount = half;
};

var ShakeDetect = Class(function () {
	this.start = function(handler) {
		accelerometer.start(accelerometerHandler);
		shakeHandler = handler;
	};

	this.stop = function() {
		accelerometer.stop();
	};
});

exports = new ShakeDetect();

