import kotlinx.cinterop.*
import kotlin.test.*

fun main(args: Array<String>) {
  autoreleasepool {
    testMethods()
  }
}

private fun testMethods() {
  val myObject = j2objctest.Foo()
  val myObject2 = j2objctest.Foo()

  assertEquals(100, myObject.return100())
  assertEquals(43, myObject.returnNum(43))
  assertEquals(47, myObject.add2(16,31))

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

  assertEquals(100, j2objctest.Foo.return100Static())
}
