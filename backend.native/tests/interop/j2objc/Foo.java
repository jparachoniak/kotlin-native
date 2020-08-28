package com.test;

public class Foo implements FooInterface{

    public int myInt = 10;
    public static int myStaticInt = 20;

    public void setMyInt(int x) {
        myInt = x;
    }
    public int getMyInt() {
        return myInt;
    }

    public static void setMyStaticInt(int x) {
        myStaticInt = x;
    }
    public static int getMyStaticInt() {
        return myStaticInt;
    }
    public int add2(int firstparam, int secondparam) {
        return firstparam + secondparam;
    }
    public int returnNum(int x) {
        return x;
    }
    public int return100() {
        return 100;
    }
    public String returnString(String s) {
        return s;
    }
    static public int return100Static() {
        return 100;
    }

    public int fib(int x) {
        if (x == 0 || x == 1)
            return x;
        return fib(x-1) + fib(x-2);
    }
    public int testKotlinInterface(FooInterface i, int num) {
        return i.fib(num);
    }

    // TODO: Add fields to this test after PR is merged
    class InnerClass{
        double myInnerFunc(double a, double b) {
            return a * b;
        }
    }

    static class NestedClass {
        double myNestedFunc(double a, double b) {
            return a * b;
        }
    }
}
