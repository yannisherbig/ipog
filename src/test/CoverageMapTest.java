package test;

/**
 * In order to execute the code written in this class' main-method,
 * the class "CoverageMap" needs to be made public
 * - and possibly some of its methods as well.
 */
class CoverageMapTest {
    public static void main(String[] args) {
        /*
        // make class CoverageMap public to test it
        final int numberOfColumns = 4;
        int strength = 2;
        int[] originalOrder = IntStream.range(0, numberOfColumns).toArray();
        System.out.println("t=2, k=4, v=2");
        CombinatoricUtils.BinomialCoefficient binomialCoefficient =
                new CombinatoricUtils.BinomialCoefficient(numberOfColumns - 1, strength - 1);
        CoverageMap coverageMap = new CoverageMap(new int[]{2, 2, 2, 2}, strength, binomialCoefficient, originalOrder, false);
        Optional<int[][]> uncoveredCombination = coverageMap.getUncoveredCombination();
        if (uncoveredCombination.isEmpty()) {
            throw new AssertionError();
        }
        System.out.println("uncoveredCombination = " + Arrays.deepToString(uncoveredCombination.get()));
        if (!Arrays.deepEquals(uncoveredCombination.get(), new int[][]{{0, 3}, {0, 0}})) {
            throw new AssertionError();
        }
        try {
            coverageMap.markAsCovered(new int[]{0, 1}, new int[]{0, 0}, 0);
            throw new AssertionError();
        }
        catch (Exception ignored) {
            // good
        }
        try {
            coverageMap.markAsCovered(new int[]{3}, new int[]{0}, 0);
            throw new AssertionError();
        }
        catch (Exception ignored) {
            // good
        }
        coverageMap.markAsCovered(new int[]{0}, new int[]{0}, 0);
        if (!coverageMap.mayHaveUncoveredCombinations()) {
            throw new AssertionError();
        }
        int[] best = new int[2];  // best[0] = value with max coverage; best[1] = best coverage
        int[] gains = coverageMap.computeGainsOfFixedParameter(new int[]{0, 0, 0, 0, 0}, best);
        if (!Arrays.equals(gains, new int[]{2, 3}) || best[0] != 1 || best[1] != 3) {
            System.out.println("gains = " + Arrays.toString(gains));
            System.out.println("best = " + Arrays.toString(best));
            throw new AssertionError();
        }
        int[][] prevUncovered = null;
        while (coverageMap.mayHaveUncoveredCombinations()) {
            uncoveredCombination = coverageMap.getUncoveredCombination();
            if (uncoveredCombination.isEmpty()) {
                throw new AssertionError();
            }
            int[][] uncovered = uncoveredCombination.get();
            if (prevUncovered != null && Arrays.deepEquals(uncovered, prevUncovered)) {
                System.out.println("Encountered same uncovered combo twice");
                break;
            }
            prevUncovered = uncovered;
            int[] paramCombo = uncovered[0], valueCombo = uncovered[1];
            System.out.println("Arrays.deepToString(uncoveredCombination1.get()) = " + Arrays.deepToString(uncoveredCombination.get()));
            System.out.println("Arrays.toString(Arrays.copyOf(paramCombo, paramCombo.length - 1)) = " + Arrays.toString(Arrays.copyOf(paramCombo, paramCombo.length - 1)));
            System.out.println("Arrays.toString(Arrays.copyOf(valueCombo, valueCombo.length - 1)) = " + Arrays.toString(Arrays.copyOf(valueCombo, valueCombo.length - 1)));
            coverageMap.markAsCovered(Arrays.copyOf(paramCombo, paramCombo.length - 1),
                    Arrays.copyOf(valueCombo, valueCombo.length - 1), valueCombo[valueCombo.length - 1]);
        }
        System.out.println();
        System.out.println(coverageMap);

        // different strength:
        strength = 3;
        System.out.println("t=3, k=4, v=2");
        binomialCoefficient =
                new CombinatoricUtils.BinomialCoefficient(numberOfColumns - 1, strength - 1);
        coverageMap = new CoverageMap(new int[]{2, 2, 2, 2}, strength, binomialCoefficient, originalOrder, false);
        uncoveredCombination = coverageMap.getUncoveredCombination();
        if (uncoveredCombination.isEmpty()) {
            throw new AssertionError();
        }
        System.out.println("uncoveredCombination = " + Arrays.deepToString(uncoveredCombination.get()));
        if (!Arrays.deepEquals(uncoveredCombination.get(), new int[][]{{0, 1, 3}, {0, 0, 0}})) {
            throw new AssertionError();
        }
        try {
            coverageMap.markAsCovered(new int[]{0, 1, 2}, new int[]{0, 0, 0}, 0);
            throw new AssertionError();
        }
        catch (Exception ignored) {
            // good
        }
        try {
            coverageMap.markAsCovered(new int[]{0, 3}, new int[]{0, 0}, 0);
            throw new AssertionError();
        }
        catch (Exception ignored) {
            // good
        }
        try {
            coverageMap.markAsCovered(new int[]{0, 1}, new int[]{0, 2}, 0);
            throw new AssertionError();
        }
        catch (Exception ignored) {
            // good
        }
        coverageMap.markAsCovered(new int[]{0, 1}, new int[]{0, 0}, 0);
        if (!coverageMap.mayHaveUncoveredCombinations()) {
            throw new AssertionError();
        }
        best = new int[2];  // best[0] = value with max coverage; best[1] = best coverage
        gains = coverageMap.computeGainsOfFixedParameter(new int[]{0, 0, 0, 0, 0}, best);
        System.out.println("gains = " + Arrays.toString(gains));
        if (!Arrays.equals(gains, new int[]{2, 3}) || best[0] != 1 || best[1] != 3) {
            System.out.println("gains = " + Arrays.toString(gains));
            System.out.println("best = " + Arrays.toString(best));
            throw new AssertionError();
        }
        prevUncovered = null;
        while (coverageMap.mayHaveUncoveredCombinations()) {
            uncoveredCombination = coverageMap.getUncoveredCombination();
            if (uncoveredCombination.isEmpty()) {
                throw new AssertionError();
            }
            int[][] uncovered = uncoveredCombination.get();
            if (prevUncovered != null && Arrays.deepEquals(uncovered, prevUncovered)) {
                break;
            }
            prevUncovered = uncovered;
            int[] paramCombo = uncovered[0], valueCombo = uncovered[1];
            System.out.println("Arrays.deepToString(uncoveredCombination1.get()) = " + Arrays.deepToString(uncoveredCombination.get()));
            System.out.println("Arrays.toString(Arrays.copyOf(paramCombo, paramCombo.length - 1)) = " + Arrays.toString(Arrays.copyOf(paramCombo, paramCombo.length - 1)));
            System.out.println("Arrays.toString(Arrays.copyOf(valueCombo, valueCombo.length - 1)) = " + Arrays.toString(Arrays.copyOf(valueCombo, valueCombo.length - 1)));
            coverageMap.markAsCovered(Arrays.copyOf(paramCombo, paramCombo.length - 1),
                    Arrays.copyOf(valueCombo, valueCombo.length - 1), valueCombo[valueCombo.length - 1]);
        }
        System.out.println(coverageMap);
         */
    }
}