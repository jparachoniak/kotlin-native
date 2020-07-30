public class Foo implements FooInterface {
    public int add2(int firstparam, int secondparam) {
        return firstparam + secondparam;
    }
    public int returnNum(int x) {
        return x;
    }
    public int return100() {
        return 100;
    }
    static public int return100Static() {
        return 100;
    }
    public int fib(int x) {
        if (x == 0 || x == 1)
            return x;
        return fib(x-1) + fib(x-2);
    }
}
