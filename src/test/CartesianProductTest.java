package test;

import ipog.CombinatoricUtils;

import java.util.*;

public class CartesianProductTest {
    public static void main(String[] args) {
        threebyThreeTest();
        threebyThreebyFourbyTwobyFourbyFourbyThree();
    }

    static void threebyThreeTest() {
        Set<List<Integer>> expected = new HashSet<>(), actual = new HashSet<>();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                for (int k = 0; k < 3; k++) {
                    expected.add(List.of(i, j, k));
                }
            }
        }
        System.out.println("expected = " + expected);
        for (int[] tuple : new CombinatoricUtils.CartesianProduct(new int[]{3, 3, 3})) {
            List<Integer> tupleAsList = new ArrayList<>();
            for (int i : tuple) {
                tupleAsList.add(i);
            }
            actual.add(tupleAsList);
        }
        System.out.println("actual = " + actual);
        if (!actual.equals(expected)) {
            throw new AssertionError();
        }
        System.out.println("Test passed!");
    }

    static void threebyThreebyFourbyTwobyFourbyFourbyThree() {
        Set<List<Integer>> expected = new HashSet<>(), actual = new HashSet<>();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                for (int k = 0; k < 4; k++) {
                    for (int l = 0; l < 2; l++) {
                        for (int m = 0; m < 4; m++) {
                            for (int n = 0; n < 4; n++) {
                                for (int o = 0; o < 3; o++) {
                                    expected.add(List.of(i, j, k, l, m, n, o));
                                }
                            }
                        }
                    }
                }
            }
        }
        System.out.println("expected = " + expected);
        for (int[] tuple : new CombinatoricUtils.CartesianProduct(new int[]{3, 3, 4, 2, 4, 4, 3})) {
            List<Integer> tupleAsList = new ArrayList<>();
            for (int i : tuple) {
                tupleAsList.add(i);
            }
            actual.add(tupleAsList);
        }
        System.out.println("actual = " + actual);
        if (!actual.equals(expected)) {
            throw new AssertionError();
        }
        System.out.println("Test passed!");
    }
}