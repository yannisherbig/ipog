package ipog;

import java.util.*;

public class CoveringArrayUtils {
    public static boolean isMixedLevel(List<Parameter<?>> parameters) {
        Set<Integer> domainSizes = new HashSet<>();
        for (Parameter<?> parameter : parameters) {
            domainSizes.add(parameter.size());
        }
        return domainSizes.size() > 1;
    }

    public static int computeStrength(CoveringArray coveringArray) {
        int tLow = 1, tHigh = coveringArray.numberOfColumns();
        while (tLow < tHigh) {
            int tMid = tLow + (tHigh - tLow + 1) / 2;
            if (isStrengthCovered(coveringArray, tMid)) {
                tLow = tMid;
            }
            else {
                tHigh = tMid - 1;
            }
        }
        return tLow;
    }

    // might be better option in case that the number of parameters is very large
    public static int computeStrengthLinearly(CoveringArray coveringArray) {
        int strength = 1;
        while (isStrengthCovered(coveringArray, strength)) {
            strength++;
        }
        return strength - 1;
    }

    public static boolean isStrengthCovered(CoveringArray coveringArray, int strength) {
        List<? extends Parameter<?>> parameters = coveringArray.getParameters();
        Set<Set<Map.Entry<Integer, Integer>>> actualCoveredTuples = new HashSet<>();
        int numberOfParameters = parameters.size();
        for (int row = 0; row < coveringArray.numberOfRows(); row++) {
            int[] parameterCombination = new int[strength];
            for (int i = 0; i < strength; i++) {
                parameterCombination[i] = i;
            }
            do {
                Set<Map.Entry<Integer, Integer>> currentTuple = new HashSet<>(strength);
                for (int tupleIndex = 0; tupleIndex < strength; tupleIndex++) {
                    int parameter = parameterCombination[tupleIndex];
                    Map.Entry<Integer, Integer> entry =
                            new AbstractMap.SimpleEntry<>(parameter, coveringArray.getTable().get(row)[parameter]);
                    currentTuple.add(entry);
                }
                actualCoveredTuples.add(currentTuple);
            }
            while (CombinatoricUtils.nextKCombination(parameterCombination, numberOfParameters));
        }
        // go over the all the tuples which are expected to be covered (mandatory that all those t-way tuples are covered)
        int[] allAlphabetSizes = new int[numberOfParameters];
        for (int i = 0; i < numberOfParameters; i++) {
            allAlphabetSizes[i] = parameters.get(i).size();
        }
        int[] alphabetSizes = new int[strength];
        int[] parameterCombination = new int[strength];
        for (int i = 0; i < strength; i++) {
            parameterCombination[i] = i;
        }
        do {
            for (int i = 0; i < strength; i++) {
                alphabetSizes[i] = allAlphabetSizes[parameterCombination[i]];
            }
            for (int[] tuple : new CombinatoricUtils.CartesianProduct(alphabetSizes)) {
                Set<Map.Entry<Integer, Integer>> valueCombination = new HashSet<>(strength);
                for (int j = 0; j < strength; j++) {
                    valueCombination.add(new AbstractMap.SimpleEntry<>(parameterCombination[j], tuple[j]));
                }
                if (!actualCoveredTuples.contains(valueCombination)) {
                    System.out.println("tuple " + valueCombination + " is not covered");
                    return false;
                }
            }
        }
        while (CombinatoricUtils.nextKCombination(parameterCombination, numberOfParameters));
        return true;
    }

    public static boolean areAllTuplesCovered(CoveringArray coveringArray, Set<Set<Map.Entry<Integer, Integer>>> expectedTuplesCovered) {
        if (expectedTuplesCovered.size() == 0 || expectedTuplesCovered.iterator().next().size() == 0) {
            throw new IllegalArgumentException();
        }
        int strength = expectedTuplesCovered.iterator().next().size();
        List<? extends Parameter<?>> parameters = coveringArray.getParameters();
        Set<Set<Map.Entry<Integer, Integer>>> actualCoveredTuples = new HashSet<>();
        int numberOfParameters = parameters.size();
        for (int row = 0; row < coveringArray.numberOfRows(); row++) {
            int[] parameterCombination = new int[strength];
            for (int i = 0; i < strength; i++) {
                parameterCombination[i] = i;
            }
            k_subset_loop:
            do {
                Set<Map.Entry<Integer, Integer>> currentTuple = new HashSet<>(strength);
                for (int tupleIndex = 0; tupleIndex < strength; tupleIndex++) {
                    int parameter = parameterCombination[tupleIndex];
                    int value = coveringArray.getTable().get(row)[parameter];
                    if (value == CoveringArray.DONT_CARE_VALUE) {
                        continue k_subset_loop;
                    }
                    Map.Entry<Integer, Integer> entry =
                            new AbstractMap.SimpleEntry<>(parameter, value);
                    currentTuple.add(entry);
                }
                actualCoveredTuples.add(currentTuple);
            }
            while (CombinatoricUtils.nextKCombination(parameterCombination, numberOfParameters));
        }
        for (Set<Map.Entry<Integer, Integer>> tuple : expectedTuplesCovered) {
            if (!actualCoveredTuples.contains(tuple)) {
                System.out.println("tuple " + tuple + " is not covered");
                return false;
            }
        }
        return true;
    }
}
