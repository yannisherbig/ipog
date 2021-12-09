package test;

import ipog.CombinatoricUtils;

public class BinomialCoefficientTest {
    public static void main(String[] args) {
        CombinatoricUtils.BinomialCoefficient combo = new CombinatoricUtils.BinomialCoefficient(256, 256);
        System.out.println(combo.choose(0, 0) == 1);
        System.out.println(combo.choose(0, 1) == 0);
        System.out.println(combo.choose(0, 2) == 0);
        System.out.println(combo.choose(1, 0) == 1);
        System.out.println(combo.choose(2, 0) == 1);
        System.out.println(combo.choose(2, 1) == 2);
        System.out.println(combo.choose(2, 2) == 1);
        System.out.println(combo.choose(2, 3) == 0);
        System.out.println(combo.choose(3, 2) == 3);
        System.out.println(combo.choose(28, 7) == 1184040);
    }
}
