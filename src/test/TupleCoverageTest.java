package test;

/**
 * In order to execute the code written in this class' main-method,
 * the classes "CoverageMap" and "CoverageMap.TupleCoverage"
 * need to be made public
 * - and possibly some of its methods as well.
 */
public class TupleCoverageTest {
    public static void main(String[] args) {
        /*
        CoverageMap.TupleCoverage tupleCoverage = new CoverageMap.TupleCoverage(new int[]{0, 1, 2}, new int[]{2, 2, 2}, false);
        tupleCoverage.markAsCovered(new int[]{0, 0, 0});
        try {
            tupleCoverage.markAsCovered(new int[]{1, 1, 2});
            throw new AssertionError();
        }
        catch (Exception ignored) {
        }
        if (!tupleCoverage.isCovered(new int[]{0, 0, 0})) {
            throw new AssertionError();
        }
        if (tupleCoverage.isCovered(new int[]{0, 0, 1})) {
            throw new AssertionError();
        }
        Optional<int[]> uncoveredTuple = tupleCoverage.getUncoveredCombination();
        if (uncoveredTuple.isEmpty()) {
            throw new AssertionError();
        }
        if (!Arrays.equals(new int[]{1, 0, 0}, uncoveredTuple.get())) {
            throw new AssertionError();
        }
        tupleCoverage.markAsCovered(new int[]{1, 0, 0});
        uncoveredTuple = tupleCoverage.getUncoveredCombination();
        if (uncoveredTuple.isEmpty()) {
            throw new AssertionError();
        }
        if (!Arrays.equals(new int[]{0, 1, 0}, uncoveredTuple.get())) {
            throw new AssertionError();
        }
        if (!tupleCoverage.hasUncoveredCombinations()) {
            throw new AssertionError();
        }
        if (tupleCoverage.isCovered(new int[]{0, 0, 1})) {
            throw new AssertionError();
        }
        tupleCoverage.markAsCovered(new int[]{1, 1, 1});
        if (!tupleCoverage.isCovered(new int[]{1, 1, 1})) {
            throw new AssertionError();
        }
        if (tupleCoverage.isCovered(new int[]{0, 1, 1})) {
            throw new AssertionError();
        }
        // 000, 100, 111 are covered
        int[] gains = new int[2];
        int[] best = new int[2];  // best[0] = value with max coverage; best[1] = best coverage
        tupleCoverage.addGainsOfFixedParameter(new int[]{0, 0, 0}, gains, best);
        if (gains[0] != 0 || gains[1] != 1 || best[0] != 1 || best[1] != 1) {
            System.out.println("Arrays.toString(gains) = " + Arrays.toString(gains));
            System.out.println("Arrays.toString(best) = " + Arrays.toString(best));
            throw new AssertionError();
        }
        best = new int[2];
        gains = new int[2];
        tupleCoverage.addGainsOfFixedParameter(new int[]{0, 1, 0}, gains, best);
        if (gains[0] != 1 || gains[1] != 1 || best[0] != 0 || best[1] != 1) {
            System.out.println("Arrays.toString(gains) = " + Arrays.toString(gains));
            System.out.println("Arrays.toString(best) = " + Arrays.toString(best));
            throw new AssertionError();
        }
        tupleCoverage.markAllAsCovered();
        if (tupleCoverage.hasUncoveredCombinations()) {
            throw new AssertionError();
        }
        uncoveredTuple = tupleCoverage.getUncoveredCombination();
        if (uncoveredTuple.isPresent()) {
            throw new AssertionError();
        }
         */
    }
}