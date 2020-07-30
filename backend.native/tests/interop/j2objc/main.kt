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
  val myInterfaceObject = implementsFooInterface()
  assertEquals(100, myObject.return100())
  assertEquals(43, myObject.returnNum(43))
  assertEquals(47, myObject.add2(16,31))

  assertEquals(13, myObject.fib(7))
  assertEquals(13, testInterface(myObject, 7))
  assertEquals(13, myInterfaceObject.fib(7))

  assertEquals(100, j2objctest.Foo.return100Static())
}
