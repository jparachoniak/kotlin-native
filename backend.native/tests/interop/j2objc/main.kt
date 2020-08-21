import kotlinx.cinterop.*
import kotlin.test.*

fun main(args: Array<String>) {
  autoreleasepool {
    testMethods()
  }
}

fun testInterface(i: j2objctest.FooInterface, n: Int): Int {
  return i.fib(n)
}

class implementsFooInterface: j2objctest.FooInterface, platform.darwin.NSObject() {
  override fun fib(n: Int): Int {
    if (n == 0 || n == 1)
      return n
    return fib(n-1) + fib(n-2)
  }
}

private fun testMethods() {
  val myObject = j2objctest.Foo()
  val myObject2 = j2objctest.Foo()
  val myInterfaceObject = implementsFooInterface()

  val innerClass = j2objctest.Foo_InnerClass(myObject)
  val nestedClass = j2objctest.Foo_NestedClass()

  val myExtensionObject = j2objctest.ExtendsFoo()

  assertEquals(100, myObject.return100())
  assertEquals(43, myObject.returnNum(43))
  assertEquals(47, myObject.add2(16,31))
  assertEquals("Hello world!", myObject.returnString("Hello world!"))

  assertEquals(100, myExtensionObject.return100())
  assertEquals(-10, myExtensionObject.returnNum(-10))
  assertEquals(1, myExtensionObject.add2(-9,-10))
  assertTrue(myExtensionObject.returnFoo() is j2objctest.Foo)

  assertEquals(13, myObject.fib(7))
  assertEquals(13, testInterface(myObject, 7))
  assertEquals(13, myInterfaceObject.fib(7))
  assertEquals(13, myObject.testKotlinInterface(myInterfaceObject,7))

  assertEquals(10, myObject.myInt)
  assertEquals(10, myObject2.myInt)
  myObject.myInt = 4
  assertEquals(4, myObject.myInt)
  assertEquals(10, myObject2.myInt)

  assertEquals(20, j2objctest.Foo.myStaticInt)
  assertEquals(20, j2objctest.Foo.myStaticInt)
  j2objctest.Foo.myStaticInt = 30
  assertEquals(30, j2objctest.Foo.myStaticInt)
  assertEquals(30, j2objctest.Foo.myStaticInt)


  assertEquals(6.0, innerClass.myInnerFunc(2.0, 3.0))
  assertEquals(6.0, nestedClass.myNestedFunc(3.0,2.0))
  // add2/add3 overridden to x-y/x-(y-z)
  assertEquals(-15, myExtensionObject.add2(16,31))
  assertEquals(2, myExtensionObject.add3(1,2,3))
  assertEquals(-16, myExtensionObject.add3(-12,3,-1))

  assertEquals(47, doAddTo(myObject, 16,31))
  assertEquals(-15, doAddTo(myExtensionObject, 16,31))

  assertEquals(100, j2objctest.Foo.return100Static())
}

fun doAddTo(obj: j2objctest.Foo, a: Int, b: Int): Int {
  return obj.add2(a,b)
}
