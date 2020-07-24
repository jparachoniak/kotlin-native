#import "ObjcTest.h"

@implementation Foo

- (instancetype)init {
  _myInt = 10;
  return self;
}

- (int)returnNum:(int)x {
    return x;
}

- (int)return100 {
    return 100;
}

- (int)add2:(int)x secondparam:(int)y {
    return x + y;
}

+ (int)return100Static {
    return 100;
}

- (int)getMyInt {
    return _myInt;
}

- (void)setMyInt:(int)x {
    _myInt = x;
}
@end
