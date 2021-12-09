package ipog;

import java.util.*;

import static ipog.CoveringArray.DONT_CARE_VALUE;

/**
 * Based on the paper "Refining the In-Parameter-Order Strategy for Constructing Covering Arrays"
 */
class IPOGF implements IPO {
    private final int strength;
    private final int numberOfParameters;
    private final List<int[]> coveringArray;
    private final int[] originalOrder;
    private final int[] alphabetSizes;
    private final Map<Integer, Integer> dontCareValuesPerRowCount;
    private final CombinatoricUtils.BinomialCoefficient binomialCoefficient;
    private final boolean heuristicHorizontal;

    IPOGF(List<int[]> coveringArray, int strength, int[] originalOrder,
          int[] alphabetSizes, int numberOfParameters,
          Map<Integer, Integer> dontCareValuesPerRowCount,
          CombinatoricUtils.BinomialCoefficient binomialCoefficient,
          boolean heuristicHorizontal) {
        this.coveringArray = coveringArray;
        this.strength = strength;
        this.originalOrder = originalOrder;
        this.alphabetSizes = alphabetSizes;
        this.numberOfParameters = numberOfParameters;
        this.dontCareValuesPerRowCount = dontCareValuesPerRowCount;
        this.binomialCoefficient = binomialCoefficient;
        this.heuristicHorizontal = heuristicHorizontal;
    }

    @Override
    public boolean extendHorizontal(CoverageMap coverageMap, int i) {
        boolean mayHaveMoreUncoveredCombinations = true;
        Set<Integer> unassignedRows = new HashSet<>();
        for (int row = 0; row < coveringArray.size(); row++) {
            unassignedRows.add(row);
        }
        int[][] alreadyCoveredCount = new int[unassignedRows.size()][alphabetSizes[i]];  // tc for every row,value-pair
        while (unassignedRows.size() > 0) {
            int bestRow = -1, bestValue = -1;
            int maxNumberOfInteractionsForCurrentExtension = binomialCoefficient.choose(i, strength - 1);
            int maxCoverage = -1;
            outer:
            for (int row : unassignedRows) {
                int numberOfDontCareValuesInRowUntilThisColumn = dontCareValuesPerRowCount.get(row)
                        - (numberOfParameters - i);
                int numberOfInteractionsForCurrentExtensionExcludingDontCareValues =
                        binomialCoefficient.choose(i - numberOfDontCareValuesInRowUntilThisColumn, strength - 1);
                for (int value = 0; value < alphabetSizes[i]; value++) {
                    int wouldBeCoveredNew =
                            numberOfInteractionsForCurrentExtensionExcludingDontCareValues
                                    - alreadyCoveredCount[row][value];
                    assert alreadyCoveredCount[row][value] + wouldBeCoveredNew
                            == numberOfInteractionsForCurrentExtensionExcludingDontCareValues;
                    if (wouldBeCoveredNew > maxCoverage) {
                        maxCoverage = wouldBeCoveredNew;
                        bestValue = value;
                        bestRow = row;
                        if (maxCoverage == maxNumberOfInteractionsForCurrentExtension) {
                            break outer;  // we don't need to look any further (we don't care about ties)
                        }
                    }
                }
            }
            if (maxCoverage <= 0) {
                break;
            }
            coveringArray.get(bestRow)[originalOrder[i]] = bestValue;
            dontCareValuesPerRowCount.put(bestRow, dontCareValuesPerRowCount.get(bestRow) - 1);
            unassignedRows.remove(bestRow);
            for (int unassignedRow : unassignedRows) {
                List<Integer> setOfColumnsWhereValsInBestRowAndRowJmatch = new ArrayList<>();
                for (int k = 0; k < i; k++) {
                    if (coveringArray.get(unassignedRow)[originalOrder[k]]
                            == coveringArray.get(bestRow)[originalOrder[k]]
                            && coveringArray.get(unassignedRow)[originalOrder[k]] != DONT_CARE_VALUE) {
                        setOfColumnsWhereValsInBestRowAndRowJmatch.add(k);
                    }
                }
                if (setOfColumnsWhereValsInBestRowAndRowJmatch.size() < strength - 1) {
                    continue;
                }
                if (!heuristicHorizontal) {
                    int[] parameterCombination = new int[strength - 1],
                            valueCombination = new int[strength - 1];
                    for (int k = 0; k < strength - 1; k++) {
                        parameterCombination[k] = k;
                    }
                    int[] actualColumnIndices = new int[strength - 1];
                    do {
                        for (int k = 0; k < strength - 1; k++) {
                            valueCombination[k] = coveringArray.get(unassignedRow)[
                                    originalOrder[setOfColumnsWhereValsInBestRowAndRowJmatch.get(parameterCombination[k])]];
                            actualColumnIndices[k] = setOfColumnsWhereValsInBestRowAndRowJmatch.get(parameterCombination[k]);
                        }
                        if (!coverageMap.isCovered(actualColumnIndices, valueCombination, bestValue)) {
                            alreadyCoveredCount[unassignedRow][bestValue]++;
                        }
                    }
                    while (CombinatoricUtils.nextKCombination(parameterCombination,
                            setOfColumnsWhereValsInBestRowAndRowJmatch.size()));
                }
                else {
                    alreadyCoveredCount[unassignedRow][bestValue] +=
                            binomialCoefficient.choose(setOfColumnsWhereValsInBestRowAndRowJmatch.size(),
                                    strength - 1);
                }
            }
            coverageMap.markAsCovered(coveringArray.get(bestRow));
            if (!coverageMap.mayHaveUncoveredCombinations()) {
                mayHaveMoreUncoveredCombinations = false;
                break;
            }
        }
        return mayHaveMoreUncoveredCombinations;
    }

    @Override
    public void extendVertical(CoverageMap coverageMap,
                               Map<Integer, Set<Integer>> partitions, int numberOfParameters) {
        int coveredParameterCombinationsUntilRank = 0, coveredValueCombinationsUntilRank = 0;
        Optional<int[][]> uncoveredCombination = coverageMap.getUncoveredCombination(
                coveredParameterCombinationsUntilRank, coveredValueCombinationsUntilRank);
        while (uncoveredCombination.isPresent()) {
            int[] parameterCombination = uncoveredCombination.get()[0],
                    valueCombination = uncoveredCombination.get()[1];
            coveredParameterCombinationsUntilRank = uncoveredCombination.get()[2][0];
            coveredValueCombinationsUntilRank = uncoveredCombination.get()[2][1];
            int valueOfNewColumnInCurrentUncoveredTuple = valueCombination[strength - 1];
            Set<Integer> partition = partitions.getOrDefault(valueOfNewColumnInCurrentUncoveredTuple, null);
            Integer goodRow = null;
            if (partition != null) {
                for (Integer row : partition) {
                    int[] candidateRow = coveringArray.get(row);
                    boolean rowIsGood = true;
                    for (int j = 0; j < strength - 1; j++) {
                        int curCellValue = candidateRow[originalOrder[parameterCombination[j]]];
                        if (curCellValue != DONT_CARE_VALUE && curCellValue != valueCombination[j]) {
                            rowIsGood = false;
                            break;
                        }
                    }
                    if (!rowIsGood) {
                        continue;
                    }
                    goodRow = row;
                    for (int j = 0; j < strength - 1; j++) {
                        if (candidateRow[originalOrder[parameterCombination[j]]] == DONT_CARE_VALUE) {
                            dontCareValuesPerRowCount.put(goodRow, dontCareValuesPerRowCount.get(goodRow) - 1);
                        }
                        candidateRow[originalOrder[parameterCombination[j]]] = valueCombination[j];
                    }
                    coverageMap.markAsCovered(candidateRow);
                    break;
                }
            }
            partition = partitions.getOrDefault(DONT_CARE_VALUE, null);
            if (goodRow == null && partition != null) {
                for (Integer row : partition) {
                    int[] candidateRow = coveringArray.get(row);
                    boolean rowIsGood = true;
                    // check up until including value in last column because it
                    // might no longer be a star value if it was set before:
                    for (int j = 0; j < strength; j++) {
                        int curCellValue = candidateRow[originalOrder[parameterCombination[j]]];
                        if (curCellValue != DONT_CARE_VALUE && curCellValue != valueCombination[j]) {
                            rowIsGood = false;
                            break;
                        }
                    }
                    if (!rowIsGood) {
                        continue;
                    }
                    goodRow = row;
                    for (int j = 0; j < strength; j++) {
                        if (candidateRow[originalOrder[parameterCombination[j]]] == DONT_CARE_VALUE) {
                            dontCareValuesPerRowCount.put(goodRow, dontCareValuesPerRowCount.get(goodRow) - 1);
                        }
                        candidateRow[originalOrder[parameterCombination[j]]] = valueCombination[j];
                    }
                    coverageMap.markAsCovered(candidateRow);
                    break;
                }
            }
            if (goodRow == null) {
                int[] newRow = new int[numberOfParameters];
                Arrays.fill(newRow, DONT_CARE_VALUE);
                for (int j = 0; j < strength; j++) {
                    newRow[originalOrder[parameterCombination[j]]] = valueCombination[j];
                }
                coveringArray.add(newRow);
                int indexOfNewRow = coveringArray.size() - 1;
                dontCareValuesPerRowCount.put(indexOfNewRow, numberOfParameters - strength);
                coverageMap.markAsCovered(Arrays.copyOf(parameterCombination, strength - 1),
                        Arrays.copyOf(valueCombination, valueCombination.length - 1),
                        valueCombination[valueCombination.length - 1]);
                // new row always has at least one star value
                partitions.putIfAbsent(valueOfNewColumnInCurrentUncoveredTuple, new HashSet<>());
                partitions.get(valueOfNewColumnInCurrentUncoveredTuple).add(indexOfNewRow);
            }
            else if (dontCareValuesPerRowCount.get(goodRow) <= 0) {
                partitions.remove(goodRow);
            }
            coveredValueCombinationsUntilRank++;
            uncoveredCombination = coverageMap.getUncoveredCombination(
                    coveredParameterCombinationsUntilRank, coveredValueCombinationsUntilRank);
        }
    }
}