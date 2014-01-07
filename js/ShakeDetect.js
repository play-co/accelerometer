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

	var sampleIndex = sampleCount++;
	var sample = samples[sampleIndex];
	if (!sample) {
		samples[sampleIndex] = {
			'x': x,
			'y': y,
			'z': z,
			'amp': amp
		};
	} else {
		sample.x = x;
		sample.y = y;
		sample.z = z;
		sample.amp = amp;
	}

	// Every second,
	var now = +new Date();
	if (now - lastCheck < 500) {
		return;
	}

	var zeroCrossingsX = 0, zeroCrossingsY = 0, zeroCrossingsZ = 0;

	// For each sample,
	var lsx = 0, lsy = 0, lsz = 0;
	for (var j = 0; j < sampleCount; j++) {
		var sample = samples[j];

		var sx = sample.x;
		var sy = sample.y;
		var sz = sample.z;

		logger.log(sx, sy, sz);

		// If there is some motion,
		if (Math.abs(sx) > 0.15) {
			if ((sx < 0 && lsx > 0) || (sx > 0 && lsx < 0)) {
				++zeroCrossingsX;
			}
			lsx = sx;
		}
		if (Math.abs(sy) > 0.15) {
			if ((sy < 0 && lsy > 0) || (sy > 0 && lsy < 0)) {
				++zeroCrossingsY;
			}
			lsy = sy;
		}
		if (Math.abs(sz) > 0.15) {
			if ((sz < 0 && lsz > 0) || (sz > 0 && lsz < 0)) {
				++zeroCrossingsZ;
			}
			lsz = sz;
		}
	}

	// If 4 shakes in ~2 seconds,
	if (zeroCrossingsX >= 4 || zeroCrossingsY >= 4 || zeroCrossingsZ >= 4) {
		if (shakeHandler) {
			// If not shaken recently,
			if (now - lastShake > 2000) {
				shakeHandler();
				lastShake = now;
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

