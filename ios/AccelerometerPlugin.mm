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
	motionManager = [[CMMotionManager alloc] init];
	[motionManager stopDeviceMotionUpdates];
	[motionManager setDeviceMotionUpdateInterval: 1/60.0];
	[motionManager startDeviceMotionUpdatesToQueue:[[NSOperationQueue alloc] init]
	  withHandler: ^(CMDeviceMotion *motion, NSError *error) {
		  CGFloat angle =  -atan2( -motion.gravity.x, -motion.gravity.y );
		  [[PluginManager get] dispatchJSEvent:@{
			  @"name": @"deviceorientation",
				  @"alpha": @0,
				  @"beta": @0,
				  @"gamma": @(angle)
		  }];
		  [[PluginManager get] dispatchJSEvent:@{
			  @"name": @"deviceGravity",
				  @"x": @(motion.gravity.x),
				  @"y": @(motion.gravity.y),
				  @"z": @(motion.gravity.z)
		  }];

	 }];
	return self;
}

@end
