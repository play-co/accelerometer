#import "AccelerometerPlugin.h"
#import <CoreMotion/CoreMotion.h>

CMMotionManager* motionManager;

const double G = -9.80665f;

@implementation AccelerometerPlugin

- (void) dealloc {
	[super dealloc];
}

- (id) init {
	self = [super init];
	if (!self) {
		return nil;
	}

	NSLog(@"{accelerometer} Initializing");

	motionManager = [[CMMotionManager alloc] init];
	[motionManager stopDeviceMotionUpdates];

	return self;
}

- (void) startEvents:(NSDictionary *)jsonObject {
	@try {
		NSLog(@"{accelerometer} Starting");
		[motionManager stopDeviceMotionUpdates];
		[motionManager setDeviceMotionUpdateInterval: 1/40.0];
		[motionManager startDeviceMotionUpdatesToQueue:[[NSOperationQueue alloc] init]
			withHandler: ^(CMDeviceMotion *motion, NSError *error) {
				[[PluginManager get] dispatchJSEvent:@{
					@"name": @"DeviceMotionEvent",
					@"acceleration": @{
						@"x": @(motion.userAcceleration.x * G),
						@"y": @(motion.userAcceleration.y * G),
						@"z": @(motion.userAcceleration.z * G),
					},
					@"accelerationIncludingGravity": @{
						@"x": @((motion.userAcceleration.x + motion.gravity.x) * G),
						@"y": @((motion.userAcceleration.y + motion.gravity.y) * G),
						@"z": @((motion.userAcceleration.z + motion.gravity.z) * G),
					},
					@"rotationRate": @{
						@"alpha": @(motion.rotationRate.z),
						@"beta": @(motion.rotationRate.x),
						@"gamma": @(motion.rotationRate.y),
					}
				}];
			}];
	}
	@catch (NSException *exception) {
		NSLog(@"{accelerometer} WARNING: Unable to start events: %@", exception);
	}
}

- (void) stopEvents:(NSDictionary *)jsonObject {
	@try {
		[motionManager stopDeviceMotionUpdates];
		NSLog(@"{accelerometer} Stopped");
	}
	@catch (NSException *exception) {
		NSLog(@"{accelerometer} WARNING: Unable to stop events: %@", exception);
	}
}

@end

