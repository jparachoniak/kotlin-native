public class Foo {
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
    static public int return100Static() {
        return 100;
    }
}
