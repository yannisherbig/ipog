package ipog;

import java.util.*;

import static ipog.CoveringArray.DONT_CARE_VALUE;

/**
 * Based on paper "An Efficient Design and Implementation of the In-Parameter-Order Algorithm"
 */
class IPOG implements IPO {
    private final int strength;
    private final List<int[]> coveringArray;
    private final int[] originalOrder;

    IPOG(List<int[]> coveringArray, int strength, int[] originalOrder) {
        this.coveringArray = coveringArray;
        this.strength = strength;
        this.originalOrder = originalOrder;
    }

    @Override
    public boolean extendHorizontal(CoverageMap coverageMap, int i) {
        boolean mayHaveMoreUncoveredCombinations = true;
        for (int[] row : coveringArray) {  // for every already generated row
            // best[0] = value with max coverage; best[1] = max coverage
            int[] best = new int[2];
            coverageMap.computeGainsOfFixedParameter(row, best);
            if (best[1] == 0) {
                continue;
            }
            row[originalOrder[i]] = best[0];
            coverageMap.markAsCovered(row);
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
        // allows us to only search to the right of a certain index in the coverage map:
        int coveredParameterCombinationsUntilRank = 0, coveredValueCombinationsUntilRank = 0;
        Optional<int[][]> uncoveredCombination = coverageMap.getUncoveredCombination(
                coveredParameterCombinationsUntilRank, coveredValueCombinationsUntilRank);
        while (uncoveredCombination.isPresent()) {
            int[] parameterCombination = uncoveredCombination.get()[0],
                    valueCombination = uncoveredCombination.get()[1];
            coveredParameterCombinationsUntilRank = uncoveredCombination.get()[2][0];
            coveredValueCombinationsUntilRank = uncoveredCombination.get()[2][1];
            int valueOfNewColumnInCurrentUncoveredTuple = valueCombination[strength - 1];
            Set<Integer> partition = partitions.getOrDefault(valueOfNewColumnInCurrentUncoveredTuple,
                    null);
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
                coverageMap.markAsCovered(Arrays.copyOf(parameterCombination, strength - 1),
                        Arrays.copyOf(valueCombination, valueCombination.length - 1),
                        valueCombination[valueCombination.length - 1]);
                // new row always has at least one star value
                partitions.putIfAbsent(valueOfNewColumnInCurrentUncoveredTuple, new HashSet<>());
                partitions.get(valueOfNewColumnInCurrentUncoveredTuple).add(coveringArray.size() - 1);
            }
            coveredValueCombinationsUntilRank++;
            uncoveredCombination = coverageMap.getUncoveredCombination(
                    coveredParameterCombinationsUntilRank, coveredValueCombinationsUntilRank);
        }
    }
}
