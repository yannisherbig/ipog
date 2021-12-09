package ipog;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Based on "EfficientCoverageMap" from the combinatorial test framework "coffee4j"
 * which in turn is based on the coverage map as described in in section 4.1 of the paper
 * "An Efficient Design and Implementation of the In-Parameter-Order Algorithm".
 * In this implementation, instead of storing them in a hash map,
 * we store the parameter combinations in a single array:
 * and we index them using their k-subset rank.
 * For each possible parameter combination we store which of the respective
 * value combinations are covered.
 */
class CoverageMap {
    private final TupleCoverage[] columnSelections;
    private final int numberOfParameters;
    private final int fixedParameter;
    private final int[] parameterSizes;
    private final CombinatoricUtils.BinomialCoefficient binomCoeffs;
    private final int strength;
    private final int[] originalOrder;
    private final boolean countOccurrences;
    private int coveredCombinationsCount;

    CoverageMap(int[] parameterSizes, int strength, CombinatoricUtils.BinomialCoefficient binomCoeffs,
                int[] originalOrder, boolean countOccurrences) {
        Preconditions.checkNotNull(parameterSizes);
        Preconditions.checkNotNull(binomCoeffs);
        Preconditions.checkNotNull(originalOrder);
        Preconditions.checkArgument(strength > 0 && strength < parameterSizes.length);
        this.originalOrder = originalOrder;
        this.parameterSizes = parameterSizes;
        this.strength = strength;
        this.binomCoeffs = binomCoeffs;
        this.countOccurrences = countOccurrences;
        coveredCombinationsCount = 0;
        numberOfParameters = parameterSizes.length;  // includes the fixed parameter
        fixedParameter = parameterSizes.length - 1;  // zero-based-indexing for fixedParameter
        columnSelections = new TupleCoverage[
                binomCoeffs.choose(numberOfParameters - 1, strength - 1)];
        int[] parameterCombination = new int[strength - 1];
        for (int parameter = 0; parameter < strength - 1; parameter++) {
            parameterCombination[parameter] = parameter;
        }
        int kCombinationRank = 0;
        do {
            int[] alphabetSizes = new int[strength];
            alphabetSizes[strength - 1] = parameterSizes[fixedParameter];
            for (int tupleParameter = 0; tupleParameter < strength - 1; tupleParameter++) {
                alphabetSizes[tupleParameter] = parameterSizes[parameterCombination[tupleParameter]];
            }
            int[] columnSelection = new int[strength];
            System.arraycopy(parameterCombination, 0, columnSelection, 0, parameterCombination.length);
            columnSelection[columnSelection.length - 1] = fixedParameter;
            columnSelections[kCombinationRank] = new TupleCoverage(columnSelection, alphabetSizes, countOccurrences);
            kCombinationRank++;
        }
        while (CombinatoricUtils.nextKCombination(parameterCombination, numberOfParameters - 1));
    }

    boolean mayHaveUncoveredCombinations() {
        for (TupleCoverage combinationCoverage : columnSelections) {
            if (combinationCoverage.hasUncoveredCombinations()) {
                return true;
            }
        }
        return false;
    }

    int occurrenceCount(int[] parameterCombination, int[] valueCombination) {
        if (!countOccurrences) {
            throw new IllegalStateException(Preconditions.OCC_NOT_COUNTED);
        }
        return getRelevantCombinationCoverages(parameterCombination)
                .occurrenceCount(valueCombination);
    }

    boolean isCovered(int[] parameterCombination,
                      int[] valueCombination, int fixedParameterValue) {
        Preconditions.checkArgument(parameterCombination.length == strength - 1,
                Preconditions.FALSE_ARRAY_LENGTH);
        Preconditions.checkArgument(valueCombination.length == strength - 1,
                Preconditions.FALSE_ARRAY_LENGTH);
        int[] valueCombinationIncludingTheFixedParameterValue = new int[strength];
        valueCombinationIncludingTheFixedParameterValue[strength - 1] = fixedParameterValue;
        System.arraycopy(valueCombination, 0,
                valueCombinationIncludingTheFixedParameterValue, 0, valueCombination.length);
        return getRelevantCombinationCoverages(parameterCombination)
                .isCovered(valueCombinationIncludingTheFixedParameterValue);
    }

    void markAsCovered(int[] parameterCombination, int[] valueCombination,
                       int fixedParameterValue) {
        Preconditions.checkArgument(parameterCombination.length == strength - 1,
                Preconditions.FALSE_ARRAY_LENGTH);
        Preconditions.checkArgument(valueCombination.length == strength - 1,
                Preconditions.FALSE_ARRAY_LENGTH);
        int[] valueCombinationIncludingTheFixedParameterValue = new int[strength];
        valueCombinationIncludingTheFixedParameterValue[strength - 1] = fixedParameterValue;
        System.arraycopy(valueCombination, 0,
                valueCombinationIncludingTheFixedParameterValue, 0, valueCombination.length);
        TupleCoverage tupleCoverage = getRelevantCombinationCoverages(parameterCombination);
        int before = tupleCoverage.getNumberOfCoveredCombinations();
        tupleCoverage.markAsCovered(valueCombinationIncludingTheFixedParameterValue);
        int after = tupleCoverage.getNumberOfCoveredCombinations();
        coveredCombinationsCount += (after - before);
    }

    private void markAsCovered(int[] parameterCombination, int[] valueCombination) {
        Preconditions.checkArgument(parameterCombination.length == strength - 1
                && valueCombination.length == strength);
        TupleCoverage tupleCoverage = getRelevantCombinationCoverages(parameterCombination);
        int before = tupleCoverage.getNumberOfCoveredCombinations();
        tupleCoverage.markAsCovered(valueCombination);
        int after = tupleCoverage.getNumberOfCoveredCombinations();
        coveredCombinationsCount += (after - before);
    }

    void markAsCovered(int[] row) {
        int[] valueCombination = new int[strength];
        valueCombination[strength - 1] = row[originalOrder[fixedParameter]];
        k_subset_loop:
        for (TupleCoverage columnSelection : columnSelections) {
            for (int i = 0; i < columnSelection.parameterCombination.length; i++) {
                valueCombination[i] = row[originalOrder[columnSelection.parameterCombination[i]]];
                if (valueCombination[i] == -1) {
                    continue k_subset_loop;
                }
            }
            markAsCovered(Arrays.copyOf(columnSelection.parameterCombination,
                    strength - 1), valueCombination);
        }
    }

    void markAsUncovered(int[] parameterCombination, int[] valueCombination) {
        TupleCoverage tupleCoverage = getRelevantCombinationCoverages(parameterCombination);
        // we need to measure the delta and cannot just decrement coveredCombinationsCount by one,
        // because it might has been uncovered already
        int before = tupleCoverage.getNumberOfCoveredCombinations();
        tupleCoverage.markAsUncovered(valueCombination);
        int after = tupleCoverage.getNumberOfCoveredCombinations();
        coveredCombinationsCount += (after - before);
    }

    /**
     * Keeps track of the changes when having changed a certain value to another
     * @param coveringArray the covering array where we have changed a value
     * @param chosenRow the row in which we have made the change
     * @param chosenColumn the column in which we have made the change
     * @param beforeValue the value that used to be in the cell before the current one
     */
    void update(List<int[]> coveringArray, int chosenRow, int chosenColumn,
                int beforeValue) {
        if (beforeValue != CoveringArray.DONT_CARE_VALUE || chosenColumn != fixedParameter) {
            // check which tuples we have to mark as uncovered
            // (we also might need to mark tuples as uncovered, because otherwise we overcount them)
            k_subset_loop:
            for (TupleCoverage columnSelection : columnSelections) {
                int[] parameterCombination = columnSelection.parameterCombination;
                int[] valueCombination = new int[strength];
                boolean isValidColumnSelection = false;  // we need to have the chosen column in it
                for (int k = 0; k < strength; k++) {
                    if (parameterCombination[k] == chosenColumn) {
                        valueCombination[k] = beforeValue;
                        isValidColumnSelection = true;
                    }
                    else {
                        valueCombination[k] = coveringArray.get(chosenRow)[originalOrder[parameterCombination[k]]];
                    }
                    if (valueCombination[k] == CoveringArray.DONT_CARE_VALUE) {
                        continue k_subset_loop;
                    }
                }
                if (!isValidColumnSelection && coveringArray.get(chosenRow)[originalOrder[chosenColumn]]
                        == CoveringArray.DONT_CARE_VALUE) {
                    continue;  // the current tuple will stay covered anyway
                }
                int[] paramCombinationWithoutFixedParam = Arrays.copyOf(parameterCombination, strength - 1);
                markAsUncovered(paramCombinationWithoutFixedParam, valueCombination);
            }
        }
        if (coveringArray.get(chosenRow)[originalOrder[chosenColumn]] == CoveringArray.DONT_CARE_VALUE) {
            return;  // there won't be any newly covered tuples
        }
        markAsCovered(coveringArray.get(chosenRow));
    }

    private int getIndex(int[] parameterCombination, boolean inputIsSorted) {
        assert parameterCombination.length == strength - 1;
        if (!inputIsSorted) {
            Arrays.sort(parameterCombination);
        }
        return CombinatoricUtils.kSubsetLexRank(parameterCombination, numberOfParameters - 1, binomCoeffs);
    }

    // rank the parameter combination to get the index in the array,
    // where all the respective value combinations are stored
    private TupleCoverage getRelevantCombinationCoverages(int[] parameterCombination) {
        return columnSelections[getIndex(parameterCombination, true)];
    }

    int[] computeGainsOfFixedParameter(int[] row, int[] best) {
        Preconditions.checkArgument(best.length == 2);
        int[] gains = new int[parameterSizes[fixedParameter]];
        int[] valueCombination = new int[strength];
        valueCombination[strength - 1] = 0;  // first value of the new parameter needs to be 0
        outer:
        for (TupleCoverage columnSelection : columnSelections) {
            if (!columnSelection.hasUncoveredCombinations()) {  // t-column selection level search pruning
                continue;
            }
            for (int i = 0; i < strength - 1; i++) {
                valueCombination[i] = row[originalOrder[columnSelection.parameterCombination[i]]];
                if (valueCombination[i] == -1) {
                    continue outer;
                }
            }
            getRelevantCombinationCoverages(Arrays.copyOf(columnSelection.parameterCombination, strength - 1))
                    .addGainsOfFixedParameter(valueCombination, gains, best);
        }
        return gains;
    }

    Optional<int[][]> getUncoveredCombination() {
        for (TupleCoverage columnSelection : columnSelections) {
            if (columnSelection.hasUncoveredCombinations()) {
                final Optional<int[]> uncoveredCombination = columnSelection.getUncoveredCombination();
                if (uncoveredCombination.isPresent()) {
                    return Optional.of(new int[][]{columnSelection.parameterCombination, uncoveredCombination.get()});
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Get the next uncovered combination.
     * @param startingRankOfParameterCombination specifies that we only want to look for
     *                     uncovered parameter combinations from this index onwards
     * @param startingRankOfValueCombination specifies that we only want to look for
     *                     uncovered value combinations from this index onwards
     * @return {column combination, value combination, {the rank of the column combination, rank of value combination}}
     */
    Optional<int[][]> getUncoveredCombination(int startingRankOfParameterCombination, int startingRankOfValueCombination) {
        for (int i = startingRankOfParameterCombination; i < columnSelections.length; i++) {
            TupleCoverage columnSelection = columnSelections[i];
            if (columnSelection.hasUncoveredCombinations(startingRankOfValueCombination)) {
                final Optional<int[][]> uncoveredCombination = columnSelection.getUncoveredCombination(startingRankOfValueCombination);
                if (uncoveredCombination.isPresent()) {
                    return Optional.of(new int[][]{columnSelection.parameterCombination, uncoveredCombination.get()[0], new int[]{i, uncoveredCombination.get()[1][0]}});
                }
            }
            startingRankOfValueCombination = 0;
        }
        return Optional.empty();
    }

    int totalCoveredValueCombinationsCount() {
        return coveredCombinationsCount;
    }

    boolean areOccurrencesCounted() {
        return countOccurrences;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (TupleCoverage columnSelection : columnSelections) {
            sb.append(Arrays.toString(columnSelection.parameterCombination)).append("\n");
            String bitsetAsBinaryString = IntStream.range(0, columnSelection.numberOfCombinations)
                    .mapToObj(b -> String.valueOf(columnSelection.coverageMap.get(b) ? 1 : 0))
                    .collect(Collectors.joining());
            sb.append(bitsetAsBinaryString).append("\n");
            sb.append(Arrays.toString(columnSelection.valueCombinationOccurrencesCount)).append("\n");
        }
        return sb.toString();
    }

    /**
     * Reused most of "ParameterCombinationCoverageMap.java" from the coffee4j framework.
     * In this implementation we can also keep track of the
     * number of covered combinations and occurrence counts.
     */
    private static final class TupleCoverage {
        private final int numberOfCombinations;
        private final int[] parameterCombination;
        private final int[] parameterSizes;
        private final int[] parameterMultipliers;
        private int numberOfCoveredCombinations;
        private final boolean countOccurrences;
        private int[] valueCombinationOccurrencesCount;
        private final BitSet coverageMap;

        private TupleCoverage(int[] parameterCombination, int[] parameterSizes,
                              boolean countOccurrences) {
            assert parameterCombination.length == parameterSizes.length;
            this.parameterCombination = parameterCombination;
            this.parameterSizes = parameterSizes;
            this.countOccurrences = countOccurrences;
            numberOfCoveredCombinations = 0;
            parameterMultipliers = buildParameterMultipliersAsArray();
            numberOfCombinations = calculateNumberOfCombinations();
            if (countOccurrences) {
                valueCombinationOccurrencesCount = new int[numberOfCombinations];
            }
            coverageMap = new BitSet(numberOfCombinations);
        }

        private int[] buildParameterMultipliersAsArray() {
            int[] parameterMultipliersAsArray = new int[parameterSizes.length];
            int currentMultiplier = 1;
            for (int i = 0; i < parameterSizes.length; i++) {
                parameterMultipliersAsArray[i] = currentMultiplier;
                currentMultiplier *= parameterSizes[i];
            }
            return parameterMultipliersAsArray;
        }

        private int calculateNumberOfCombinations() {
            int count = 1;
            for (int alphabetSize : parameterSizes) {
                Preconditions.checkArgument(((long) count * (long) alphabetSize) <= (long) Integer.MAX_VALUE);
                count *= alphabetSize;
            }
            return count;
        }

        private boolean hasUncoveredCombinations() {
            return numberOfCoveredCombinations < numberOfCombinations;
        }

        private boolean hasUncoveredCombinations(int fromIndex) {
            if (fromIndex >= numberOfCombinations) {
                return false;
            }
            return coverageMap.nextClearBit(fromIndex) < numberOfCombinations;
        }

        private int getNumberOfCoveredCombinations() {
            return numberOfCoveredCombinations;
        }

        private boolean isCovered(int index) {
            return coverageMap.get(index);
        }

        private boolean isCovered(int[] valueCombination) {
            return isCovered(getIndex(valueCombination));
        }

        private void markAsCovered(int[] valueCombination) {
            int index = getIndex(valueCombination);
            if (countOccurrences) {
                valueCombinationOccurrencesCount[index]++;
            }
            if (coverageMap.get(index)) {
                return;
            }
            coverageMap.set(index);
            numberOfCoveredCombinations++;
        }

        private void markAllAsCovered() {
            coverageMap.set(0, numberOfCombinations);
            numberOfCoveredCombinations = numberOfCombinations;
        }

        private void markAsUncovered(int[] valueCombination) {
            int index = getIndex(valueCombination);
            if (!coverageMap.get(index)) {
                return;
            }
            if (!countOccurrences || --valueCombinationOccurrencesCount[index] <= 0) {
                coverageMap.clear(index);
                numberOfCoveredCombinations--;
            }
        }

        private int getIndex(int[] valueCombination) {
            assert valueCombination.length == parameterCombination.length;
            int index = 0;
            for (int i = 0; i < valueCombination.length; i++) {
                if (valueCombination[i] >= parameterSizes[i] || valueCombination[i] < 0) {
                    throw new IllegalArgumentException();
                }
                index += valueCombination[i] * parameterMultipliers[i];
            }
            return index;
        }

        private int occurrenceCount(int[] valueCombination) {
            if (!countOccurrences) {
                throw new IllegalStateException(Preconditions.OCC_NOT_COUNTED);
            }
            return valueCombinationOccurrencesCount[getIndex(valueCombination)];
        }

        private Optional<int[]> getUncoveredCombination() {
            if (!hasUncoveredCombinations()) {
                return Optional.empty();
            }
            final int index = coverageMap.nextClearBit(0);
            assert index < numberOfCombinations;
            return Optional.of(getCombination(index));
        }

        /**
         *
         * @param fromIndex the starting index from which we want to look
         *                  for uncovered combinations
         * @return {{value combination}, {index (the rank) of value combination}}
         */
        private Optional<int[][]> getUncoveredCombination(int fromIndex) {
            if (!hasUncoveredCombinations()) {
                return Optional.empty();
            }
            final int index = coverageMap.nextClearBit(fromIndex);
            if (index >= numberOfCombinations) {
                throw new IllegalStateException("corrupt invariant");
            }
            return Optional.of(new int[][]{getCombination(index), new int[]{index}});
        }

        private int[] getCombination(int index) {
            int[] valueCombination = new int[this.parameterCombination.length];
            Arrays.fill(valueCombination, -1);
            for (int i = parameterCombination.length - 1; i >= 0; i--) {
                int parameterIndexPart = (index - (index % parameterMultipliers[i]));
                int value = parameterIndexPart / parameterMultipliers[i];
                valueCombination[i] = value;
                index -= parameterIndexPart;
            }
            return valueCombination;
        }

        /**
         *
         * @param valueCombination initial value combination for this.parameterCombination (this column selection)
         * @param gains gains[i] = coverage of value i
         * @param best best[0] = value with max coverage so far; best[1] = current max coverage
         */
        private void addGainsOfFixedParameter(int[] valueCombination, int[] gains, int[] best) {
            int fixedParameterIndex = parameterCombination.length - 1;
            if (!hasUncoveredCombinations()) {
                return;
            }
            int baseIndex = getIndex(valueCombination);
            for (int value = 0; value < gains.length; value++) {
                int index = baseIndex + value * parameterMultipliers[fixedParameterIndex];
                if (!isCovered(index)) {
                    gains[value]++;
                    if (gains[value] > best[1] || gains[value] == best[1] && value < best[0]) {
                        best[1] = gains[value];
                        best[0] = value;
                    }
                }
            }
        }
    }
}