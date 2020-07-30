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

private fun testMethods() {
  val myObject = j2objctest.Foo()
  assertEquals(100, myObject.return100())
  assertEquals(43, myObject.returnNum(43))
  assertEquals(47, myObject.add2(16,31))

  assertEquals(13, myObject.fib(7))
  assertEquals(13, testInterface(myObject, 7))

  assertEquals(100, j2objctest.Foo.return100Static())
}
