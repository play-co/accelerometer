import device;
import lib.PubSub;

module.exports = new lib.PubSub();

var hasNativeEvents = NATIVE && NATIVE.plugins && NATIVE.plugins.sendEvent;
var hasBrowserEvents = window.DeviceOrientationEvent;

if (hasNativeEvents && !device.isSimulator) {
  var win = "import devkit.native.Window";
  var nativeWindow = jsio(win);
  NATIVE.events.registerHandler('DeviceMotionEvent', function (evt) {
    module.exports.emit('devicemotion', evt);
    nativeWindow.emit('devicemotion', evt);
  });

  var startEvents = function (type, cb) {
    if (type == 'devicemotion') {
      NATIVE.plugins.sendEvent("AccelerometerPlugin", "startEvents", "{}");
    }
  };

  nativeWindow.on('newListener', startEvents);
  module.exports.on('newListener', startEvents);
} else if (device.isMobileBrowser && device.isIOS) {
  // mobile safari returns inverted coordinates
  window.addEventListener('devicemotion', function (evt) {

    var fixedEvent = {
      acceleration: {
        x: -evt.acceleration.x,
        y: -evt.acceleration.y,
        z: -evt.acceleration.z
      },
      accelerationIncludingGravity: {
        x: -evt.accelerationIncludingGravity.x,
        y: -evt.accelerationIncludingGravity.y,
        z: -evt.accelerationIncludingGravity.z
      },
      rotationRate: {
        alpha: evt.rotationRate.gamma,
        beta: evt.rotationRate.alpha,
        gamma: evt.rotationRate.beta
      }
    };

    fixedEvent.__proto__ = evt;
    module.exports.emit('devicemotion', fixedEvent);
  });
} else if (device.isMobileBrowser && device.isAndroid) {
  // android chrome returns radians, not degrees
  var RAD_TO_DEGREES = 180 / Math.PI;
  window.addEventListener('devicemotion', function (evt) {
    var fixedEvent = {
      rotationRate: {
        alpha: evt.rotationRate.gamma * RAD_TO_DEGREES,
        beta: evt.rotationRate.alpha * RAD_TO_DEGREES,
        gamma: evt.rotationRate.beta * RAD_TO_DEGREES
      }
    };

    fixedEvent.__proto__ = evt;
    module.exports.emit('devicemotion', fixedEvent);
  });
} else {
  window.addEventListener('devicemotion', function (evt) {
    // desktop browsers sometimes send null events
    if (evt.acceleration.x == null) { return; }

    module.exports.emit('devicemotion', evt);
  });
}

module.exports.pauseNativeEvents = function () {
  hasNativeEvents && NATIVE.plugins.sendEvent("AccelerometerPlugin", "stopEvents", "{}");
}

module.exports.resumeNativeEvents = function () {
  hasNativeEvents && NATIVE.plugins.sendEvent("AccelerometerPlugin", "startEvents", "{}");
}
