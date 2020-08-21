// TODO: Replace code with J2ObjC generated code once nameMangling is complete

#import <Foundation/Foundation.h>

@protocol FooInterface
@required
- (int)fib:(int)n;

@end

@interface Foo : NSObject <FooInterface> {
  int _myInt;
}

- (instancetype)init;

- (int)returnNum:(int)x;
- (int)return100;
- (int)add2:(int)x secondparam:(int)y;
+ (int)return100Static;

- (int)getMyInt;
- (void)setMyInt:(int)x;
+ (int)getMyStaticInt;
+ (void)setMyStaticInt:(int)x;

- (int)testKotlinInterface:(id<FooInterface>)i num:(int)n;

@end
