#import "ObjcTest.h"

@implementation Foo

- (instancetype)init {
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

- (int)fib:(int)n {
  if (n == 0 || n == 1){
    return n;
  }
  return [self fib:(n-1)] + [self fib:(n-2)];
}
@end
