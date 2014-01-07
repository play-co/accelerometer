#import "AccelerometerPlugin.h"
#import <CoreMotion/CoreMotion.h>

CMMotionManager* motionManager;

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
					@"name": @"accelerometerEvent",
						@"x": @(motion.gravity.x),
						@"y": @(motion.gravity.y),
						@"z": @(motion.gravity.z)
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

