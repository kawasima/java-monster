package net.unit8.javamonster;

public class Generatorics {
    public long factorial(long n) {
        long ans = 1;
        for (; n > 0; ans *= n--);
        return ans;
    }
}
