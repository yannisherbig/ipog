package ipog;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ipog.CoveringArray.DONT_CARE_VALUE;

public class IpogRunner {
    private final int strength;
    private final List<Parameter<?>> parameters;
    private final BaseAlgorithm baseAlgorithm;
    private final int numberOfParameters;
    private final List<int[]> coveringArray;
    private final int[] originalOrder;
    private final int[] alphabetSizes;
    private final CombinatoricUtils.BinomialCoefficient binomialCoefficient;
    private final boolean enhanceHorizontal, fullHorizontal, adaptVertical;

    public IpogRunner(RunConfiguration runConfiguration) throws IllegalArgumentException {
        strength = runConfiguration.getStrength();
        Preconditions.checkArgument(strength > 0, Preconditions.STRENGTH_TOO_SMALL);
        parameters = runConfiguration.getParameters();
        numberOfParameters = parameters.size();
        Preconditions.checkArgument(strength <= numberOfParameters, Preconditions.STRENGTH_TOO_BIG);
        baseAlgorithm = runConfiguration.getBaseAlgorithm();
        enhanceHorizontal = runConfiguration.isEnhanceHorizontal();
        fullHorizontal = runConfiguration.isFullHorizontal();
        adaptVertical = runConfiguration.isAdaptVertical();
        originalOrder = IntStream.range(0, numberOfParameters).boxed()
                .sorted((a, b) -> Integer.compare(parameters.get(b).size(), parameters.get(a).size()))
                .mapToInt(Integer::intValue).toArray();
        alphabetSizes = new int[numberOfParameters];
        for (int i = 0; i < numberOfParameters; i++) {
            alphabetSizes[i] = parameters.get(originalOrder[i]).size();
        }
        try {
            binomialCoefficient =
                    new CombinatoricUtils.BinomialCoefficient(numberOfParameters - 1, strength - 1);
        }
        catch (Exception exception) {
            throw new IllegalArgumentException(Preconditions.TOO_MANY_COMBOS);
        }
        coveringArray = new ArrayList<>();
    }

    public CoveringArray generate() throws OutOfMemoryError {
        Map<Integer, Integer> dontCareValuesPerRowCount =
                coverTheFirstColumnTuple();
        if (strength == numberOfParameters) {
            return new CoveringArray(parameters, coveringArray);
        }
        IPO ipoStrategy = getIpoStrategy(dontCareValuesPerRowCount);
        SIPO sipo = null;
        if (enhanceHorizontal) {
            sipo = new SIPO();
        }
        SmallestLastOrder slo = null;
        if (adaptVertical) {
            slo = new SmallestLastOrder();
        }
        for (int i = strength; i < numberOfParameters; i++) {
            CoverageMap coverageMap =
                    new CoverageMap(Arrays.copyOf(alphabetSizes, i + 1),
                            strength, binomialCoefficient,
                            originalOrder, enhanceHorizontal);
            boolean mayHaveMoreUncoveredCombinations =
                    ipoStrategy.extendHorizontal(coverageMap, i);
            if (!mayHaveMoreUncoveredCombinations) {
                continue;
            }
            if (enhanceHorizontal) {
                sipo.enhanceHorizontal(coverageMap, i,
                        dontCareValuesPerRowCount);
                if (!coverageMap.mayHaveUncoveredCombinations()) {
                    continue;
                }
            }
            Map<Integer, Set<Integer>> partitions =
                    createPartitions(coveringArray, originalOrder, i);
            if (adaptVertical) {
                slo.extendVerticallyWithGraphColoring(coverageMap,
                        partitions, dontCareValuesPerRowCount);
            }
            else {
                ipoStrategy.extendVertical(coverageMap, partitions,
                        numberOfParameters);
            }
        }
        return new CoveringArray(parameters, coveringArray);
    }

    private IPO getIpoStrategy(
            Map<Integer, Integer> dontCareValuesPerRowCount) {
        IPO ipoStrategy;
        switch (baseAlgorithm) {
            case IPOG:
                ipoStrategy = new IPOG(coveringArray, strength,
                        originalOrder);
                break;
            case IPOG_F:
                ipoStrategy = new IPOGF(coveringArray, strength,
                        originalOrder, alphabetSizes,
                        numberOfParameters, dontCareValuesPerRowCount,
                        binomialCoefficient, false);
                break;
            case IPOG_F2:
                ipoStrategy = new IPOGF(coveringArray, strength,
                        originalOrder, alphabetSizes,
                        numberOfParameters, dontCareValuesPerRowCount,
                        binomialCoefficient, true);
                break;
            default:
                throw new IllegalStateException(
                        Preconditions.UNSUPPORTED_BASE_ALGORITHM);
        }
        return ipoStrategy;
    }

    private Map<Integer, Integer> coverTheFirstColumnTuple() {
        int[] tupleAlphabetSizes = new int[strength];
        System.arraycopy(alphabetSizes, 0, tupleAlphabetSizes, 0, strength);
        Map<Integer, Integer> starValuesPerRowCount = null;
        if (baseAlgorithm == BaseAlgorithm.IPOG_F || baseAlgorithm == BaseAlgorithm.IPOG_F2) {
            starValuesPerRowCount = new HashMap<>();
        }
        for (int[] valueCombination : new CombinatoricUtils.CartesianProduct(tupleAlphabetSizes)) {
            int[] nextCombination = new int[numberOfParameters];
            Arrays.fill(nextCombination, DONT_CARE_VALUE);
            for (int i = 0; i < strength; i++) {
                nextCombination[originalOrder[i]] = valueCombination[i];
            }
            coveringArray.add(nextCombination);
            if (starValuesPerRowCount != null) {
                starValuesPerRowCount.put(coveringArray.size() - 1, numberOfParameters - strength);
            }
        }
        return starValuesPerRowCount;
    }

    // Based on paper "An Efficient Design and Implementation of the In-Parameter-Order Algorithm"
    private static Map<Integer, Set<Integer>> createPartitions(List<int[]> coveringArray,
                                                               int[] originalOrder, int i) {
        Map<Integer, Set<Integer>> partitions = new HashMap<>();
        for (int rowIndex = 0; rowIndex < coveringArray.size(); rowIndex++) {
            partitions.putIfAbsent(coveringArray.get(rowIndex)[originalOrder[i]], new HashSet<>());
            for (int columnIndex = 0; columnIndex < i; columnIndex++) {
                if (coveringArray.get(rowIndex)[originalOrder[columnIndex]] == DONT_CARE_VALUE) {
                    partitions.get(coveringArray.get(rowIndex)[originalOrder[i]]).add(rowIndex);
                    break;
                }
            }
        }
        return partitions;
    }

    /**
     * Based on the paper "Heuristically enhanced IPO algorithms for covering array generation".
     */
    private class SIPO {
        private void enhanceHorizontal(CoverageMap coverageMap, int i,
                                       Map<Integer, Integer> dontCareValuesPerRowCount) {
            Preconditions.checkArgument(coverageMap.areOccurrencesCounted(), Preconditions.OCC_NOT_COUNTED);
            final double finalTemp = 0.1, initialTemp = 5.0;
            int numberOfBaseIterations = 1000;
            if (fullHorizontal) {
                numberOfBaseIterations = 10_000;
            }
            List<int[]> modifiableEntries = getModifiableEntries(i);
            final int numberOfIterations = numberOfBaseIterations * (strength - 1) * (i + 1);
            // adjust cooling factor so that we loop numberOfIterations times
            final double coolingFactor = 1.0 + (100.0 *
                    ((Math.pow(finalTemp / initialTemp, 1.0 / numberOfIterations)) - 1)) / 100.0;
            for (double currentTemp = initialTemp; currentTemp > finalTemp; currentTemp *= coolingFactor) {
                int randomIndex = ThreadLocalRandom.current().nextInt(0, modifiableEntries.size());
                int[] modifiableEntry = modifiableEntries.get(randomIndex);
                int chosenRow = modifiableEntry[0], chosenColumn = modifiableEntry[1];
                int beforeValue = coveringArray.get(chosenRow)[originalOrder[chosenColumn]];
                int newValue;  // random value that we try to make a move to
                do {
                    newValue = ThreadLocalRandom.current().nextInt(-1, alphabetSizes[chosenColumn]);
                }
                while (newValue == beforeValue);
                int numberOfCombinationsCoveredBefore = coverageMap.totalCoveredValueCombinationsCount();
                coveringArray.get(chosenRow)[originalOrder[chosenColumn]] = newValue;
                coverageMap.update(coveringArray, chosenRow, chosenColumn, beforeValue);
                int numberOfCombinationsCoveredAfter = coverageMap.totalCoveredValueCombinationsCount();
                int incentive = 0;
                if (newValue == DONT_CARE_VALUE) {  // if true, then beforeValue != DONT_CARE_VALUE
                    if (baseAlgorithm == BaseAlgorithm.IPOG_F || baseAlgorithm == BaseAlgorithm.IPOG_F2) {
                        dontCareValuesPerRowCount.put(chosenRow, dontCareValuesPerRowCount.get(chosenRow) + 1);
                    }
                    incentive = -1;
                }
                else if (beforeValue == DONT_CARE_VALUE) {  // newValue != DONT_CARE_VALUE
                    if (baseAlgorithm == BaseAlgorithm.IPOG_F || baseAlgorithm == BaseAlgorithm.IPOG_F2) {
                        dontCareValuesPerRowCount.put(chosenRow, dontCareValuesPerRowCount.get(chosenRow) - 1);
                    }
                    incentive = 1;
                }
                int objectiveValue = numberOfCombinationsCoveredBefore - numberOfCombinationsCoveredAfter
                        + incentive;
                if (objectiveValue > 0 && Math.exp(-objectiveValue / currentTemp) < ThreadLocalRandom.current().nextDouble()) {
                    // move not accepted; revert changes
                    coveringArray.get(chosenRow)[originalOrder[chosenColumn]] = beforeValue;
                    coverageMap.update(coveringArray, chosenRow, chosenColumn, newValue);
                    if (baseAlgorithm == BaseAlgorithm.IPOG_F || baseAlgorithm == BaseAlgorithm.IPOG_F2) {
                        if (incentive == -1) {
                            dontCareValuesPerRowCount.put(chosenRow, dontCareValuesPerRowCount.get(chosenRow) - 1);
                        }
                        else if (incentive == 1) {
                            dontCareValuesPerRowCount.put(chosenRow, dontCareValuesPerRowCount.get(chosenRow) + 1);
                        }
                    }
                }
                currentTemp *= coolingFactor;
            }
        }

        private List<int[]> getModifiableEntries(int i) {
            List<int[]> modifiableEntries = new ArrayList<>();
            if (fullHorizontal) {  // enhance type is fullHorizontal
                for (int row = 0; row < coveringArray.size(); row++) {
                    for (int col = 0; col <= i; col++) {
                        if (coveringArray.get(row)[originalOrder[col]] == DONT_CARE_VALUE
                                || col == i) {
                            modifiableEntries.add(new int[]{row, col});
                        }
                    }
                }
            }
            else {  // enhance type is newColumn
                for (int row = 0; row < coveringArray.size(); row++) {
                    modifiableEntries.add(new int[]{row, i});
                }
            }
            return modifiableEntries;
        }
    }

    /**
     * Based on the paper "Improving IPOG’s Vertical Growth Based on a Graph Coloring Scheme"
     */
    private class SmallestLastOrder {
        private void extendVerticallyWithGraphColoring(CoverageMap coverageMap, Map<Integer, Set<Integer>> partitions,
                                                       Map<Integer, Integer> dontCareValuesPerRowCount) {
            // map the tuples by value of the new parameter
            Map<Integer, List<int[][]>> missingTuplesByValue = new HashMap<>();
            Map<List<Integer>, Integer> nonConflictsCountsForMissingTuples = new HashMap<>();
            readInMissingTuplesByValue(coverageMap, missingTuplesByValue, nonConflictsCountsForMissingTuples);
            Map<List<Integer>, Set<List<Integer>>> adjacencyList = new HashMap<>();
            int maxDegree = buildConflictGraph(partitions, missingTuplesByValue,
                    nonConflictsCountsForMissingTuples, adjacencyList);
            List<int[][]> missingTuples = missingTuplesByValue.values().stream().flatMap(List::stream)
                    .collect(Collectors.toList());
            int[][][] smallestLastOrder = computeSmallestLastOrdering(nonConflictsCountsForMissingTuples,
                    adjacencyList, maxDegree, missingTuples);
            extendVerticallyByOrder(coverageMap, partitions, smallestLastOrder, dontCareValuesPerRowCount);
        }

        private void extendVerticallyByOrder(CoverageMap coverageMap, Map<Integer, Set<Integer>> partitions,
                                             int[][][] smallestLastOrder, Map<Integer, Integer> dontCareValuesPerRowCount) {
            for (int j = 0; j < smallestLastOrder.length; j++) {
                while (j > 0 && j < smallestLastOrder.length && coverageMap.isCovered(Arrays.copyOf(smallestLastOrder[j][0], strength - 1),
                        Arrays.copyOf(smallestLastOrder[j][1], strength - 1),
                        smallestLastOrder[j][1][strength - 1])) {
                    j++;
                }
                if (j >= smallestLastOrder.length) {
                    break;
                }
                int[][] missingTuple = smallestLastOrder[j];
                int[] parameterCombination = missingTuple[0],
                        valueCombination = missingTuple[1];
                int valueOfNewColumnInCurrentUncoveredTuple = valueCombination[strength - 1];
                Set<Integer> partition = partitions.getOrDefault(valueOfNewColumnInCurrentUncoveredTuple, null);
                Integer goodRow = null;
                if (partition != null) {
                    for (Integer row : partition) {
                        int[] candidateRow = coveringArray.get(row);
                        boolean rowIsGood = true;
                        for (int k = 0; k < strength - 1; k++) {
                            int curCellValue = candidateRow[originalOrder[parameterCombination[k]]];
                            if (curCellValue != DONT_CARE_VALUE && curCellValue != valueCombination[k]) {
                                rowIsGood = false;
                                break;
                            }
                        }
                        if (!rowIsGood) {
                            continue;
                        }
                        goodRow = row;
                        for (int k = 0; k < strength - 1; k++) {
                            if ((baseAlgorithm == BaseAlgorithm.IPOG_F || baseAlgorithm == BaseAlgorithm.IPOG_F2)
                                    && candidateRow[originalOrder[parameterCombination[k]]] == DONT_CARE_VALUE) {
                                dontCareValuesPerRowCount.put(goodRow, dontCareValuesPerRowCount.get(goodRow) - 1);
                            }
                            candidateRow[originalOrder[parameterCombination[k]]] = valueCombination[k];
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
                        for (int k = 0; k < strength; k++) {
                            int curCellValue = candidateRow[originalOrder[parameterCombination[k]]];
                            if (curCellValue != DONT_CARE_VALUE && curCellValue != valueCombination[k]) {
                                rowIsGood = false;
                                break;
                            }
                        }
                        if (!rowIsGood) {
                            continue;
                        }
                        goodRow = row;
                        for (int k = 0; k < strength; k++) {
                            if ((baseAlgorithm == BaseAlgorithm.IPOG_F || baseAlgorithm == BaseAlgorithm.IPOG_F2)
                                    && candidateRow[originalOrder[parameterCombination[k]]] == DONT_CARE_VALUE) {
                                dontCareValuesPerRowCount.put(goodRow, dontCareValuesPerRowCount.get(goodRow) - 1);
                            }
                            candidateRow[originalOrder[parameterCombination[k]]] = valueCombination[k];
                        }
                        coverageMap.markAsCovered(candidateRow);
                        break;
                    }
                }
                if (goodRow == null) {
                    int[] newRow = new int[numberOfParameters];
                    Arrays.fill(newRow, DONT_CARE_VALUE);
                    for (int k = 0; k < strength; k++) {
                        newRow[originalOrder[parameterCombination[k]]] = valueCombination[k];
                    }
                    coveringArray.add(newRow);
                    int indexOfNewRow = coveringArray.size() - 1;
                    if (baseAlgorithm == BaseAlgorithm.IPOG_F || baseAlgorithm == BaseAlgorithm.IPOG_F2) {
                        dontCareValuesPerRowCount.put(indexOfNewRow, numberOfParameters - strength);
                    }
                    coverageMap.markAsCovered(Arrays.copyOf(parameterCombination, strength - 1),
                            Arrays.copyOf(valueCombination, valueCombination.length - 1),
                            valueCombination[valueCombination.length - 1]);
                    partitions.get(valueOfNewColumnInCurrentUncoveredTuple).add(indexOfNewRow);  // row always has at least one star value (because strength < numberOfParameters)
                }
            }
        }

        /**
         * compute sl-ordering in O(|E|+|V|) time
         */
        private int[][][] computeSmallestLastOrdering(Map<List<Integer>, Integer> nonConflictsCountsForMissingTuples,
                                                             Map<List<Integer>, Set<List<Integer>>> adjacencyList,
                                                             int maxDegree, List<int[][]> missingTuples) {
            int[][][] smallestLastOrder = new int[missingTuples.size()][][];
            Map<List<Integer>, int[][]> missingTupleMap = new HashMap<>();
            @SuppressWarnings("unchecked")
            LinkedHashSet<List<Integer>>[] buckets = new LinkedHashSet[maxDegree + 1];
            for (int j = 0; j < buckets.length; j++) {
                buckets[j] = new LinkedHashSet<>();
            }
            // construct initial degree structure:
            for (int[][] missingTuple : missingTuples) {
                int parameterCombinationRank = missingTuple[2][0], valueCombinationRank = missingTuple[2][1];
                List<Integer> key = List.of(parameterCombinationRank, valueCombinationRank);
                buckets[nonConflictsCountsForMissingTuples.get(key)].add(key);
                missingTupleMap.put(key, missingTuple);
            }
            int assignmentsCount = 0;
            for (int degree = maxDegree; degree >= 0; degree--) {  // max degree in non-conflict graph = min degree in conflict graph
                while (buckets[degree].size() > 0) {
                    List<Integer> cur = buckets[degree].iterator().next();
                    buckets[degree].remove(cur);
                    smallestLastOrder[smallestLastOrder.length - assignmentsCount++ - 1]
                            = missingTupleMap.get(cur);
                    for (List<Integer> nei : adjacencyList.getOrDefault(cur, new HashSet<>())) {
                        adjacencyList.get(nei).remove(cur);
                        int currentDegreeOfNeighbour = nonConflictsCountsForMissingTuples.get(nei);
                        buckets[currentDegreeOfNeighbour].remove(nei);
                        int updatedDegreeOfNeighbour = currentDegreeOfNeighbour - 1;
                        buckets[updatedDegreeOfNeighbour].add(nei);
                        nonConflictsCountsForMissingTuples.put(nei, updatedDegreeOfNeighbour);
                    }
                    adjacencyList.remove(cur);
                    nonConflictsCountsForMissingTuples.remove(cur);
                }
            }
            return smallestLastOrder;
        }

        private int buildConflictGraph(Map<Integer, Set<Integer>> partitions,
                                       Map<Integer, List<int[][]>> missingTuplesByValue,
                                       Map<List<Integer>, Integer> nonConflictsCountsForMissingTuples,
                                       Map<List<Integer>, Set<List<Integer>>> adjacencyList) {
            int maxDegree = 0;
            for (Map.Entry<Integer, List<int[][]>> integerListEntry : missingTuplesByValue.entrySet()) {
                Integer valueOfNewParameter = integerListEntry.getKey();
                List<int[][]> missingTuplesForCurrentValue = integerListEntry.getValue();
                // look for compatible tuples in other missing tuples (those that have the same value in the new column)
                for (int j = 0; j < missingTuplesForCurrentValue.size(); j++) {
                    int[][] missingTupleA = missingTuplesForCurrentValue.get(j);
                    int[] parameterCombinationA = missingTupleA[0], valueCombinationA = missingTupleA[1];
                    Map<Integer, Integer> parameterToValueInTupleA = new HashMap<>();
                    for (int l = 0; l < strength - 1; l++) {
                        parameterToValueInTupleA.put(parameterCombinationA[l], valueCombinationA[l]);
                    }
                    tupleB_loop:
                    for (int k = j + 1; k < missingTuplesForCurrentValue.size(); k++) {
                        int[][] missingTupleB = missingTuplesForCurrentValue.get(k);
                        int[] parameterCombinationB = missingTupleB[0],
                                valueCombinationB = missingTupleB[1];
                        for (int l = 0; l < strength - 1; l++) {
                            if (parameterToValueInTupleA.containsKey(parameterCombinationB[l])
                                    && parameterToValueInTupleA.get(parameterCombinationB[l]) != valueCombinationB[l]) {
                                // found a conflict between tuple A and tuple B
                                // this means we cannot build an edge between tuples A and B in the non-conflict graph
                                continue tupleB_loop;
                            }
                        }
                        // build an edge between tuples A and B in the non-conflict graph
                        int parameterCombinationRankA = missingTupleA[2][0], parameterCombinationRankB = missingTupleB[2][0],
                                valueCombinationRankA = missingTupleA[2][1], valueCombinationRankB = missingTupleB[2][1];
                        List<Integer> keyA = List.of(parameterCombinationRankA, valueCombinationRankA),
                                keyB = List.of(parameterCombinationRankB, valueCombinationRankB);
                        int newDegreeA = nonConflictsCountsForMissingTuples.get(keyA) + 1;
                        nonConflictsCountsForMissingTuples.put(keyA, newDegreeA);
                        maxDegree = Math.max(maxDegree, newDegreeA);
                        int newDegreeB = nonConflictsCountsForMissingTuples.get(keyB) + 1;
                        nonConflictsCountsForMissingTuples.put(keyB, newDegreeB);
                        maxDegree = Math.max(maxDegree, newDegreeB);
                        adjacencyList.putIfAbsent(keyA, new HashSet<>());
                        adjacencyList.putIfAbsent(keyB, new HashSet<>());
                        adjacencyList.get(keyA).add(keyB);
                        adjacencyList.get(keyB).add(keyA);
                    }
                }
                // count non-conflicts with existing rows that have star values in it
                Set<Integer> sameValuePartition = partitions.getOrDefault(valueOfNewParameter, null),
                        starValuePartition = partitions.getOrDefault(DONT_CARE_VALUE, null);
                for (int[][] missingTuple : missingTuplesForCurrentValue) {
                    int[] parameterCombination = missingTuple[0],
                            valueCombination = missingTuple[1];
                    int parameterCombinationRank = missingTuple[2][0], valueCombinationRank = missingTuple[2][1];
                    List<Integer> key = List.of(parameterCombinationRank, valueCombinationRank);
                    maxDegree = updateNonConflictCountForPartition(nonConflictsCountsForMissingTuples,
                            maxDegree, sameValuePartition, parameterCombination, valueCombination, key);
                    maxDegree = updateNonConflictCountForPartition(nonConflictsCountsForMissingTuples,
                            maxDegree, starValuePartition, parameterCombination, valueCombination, key);
                }
            }
            return maxDegree;
        }

        // Look for non-conflicts with existing rows
        private int updateNonConflictCountForPartition(Map<List<Integer>, Integer> nonConflictsCountsForMissingTuples,
                                                       int maxDegree, Set<Integer> sameValuePartition,
                                                       int[] parameterCombination, int[] valueCombination,
                                                       List<Integer> key) {
            if (sameValuePartition != null) {
                row_loop:
                for (Integer row : sameValuePartition) {
                    int[] currentRow = coveringArray.get(row);
                    for (int j = 0; j < strength - 1; j++) {
                        int parameterValueInRow = currentRow[originalOrder[parameterCombination[j]]];
                        if (valueCombination[j] != parameterValueInRow
                                && parameterValueInRow != DONT_CARE_VALUE) {
                            continue row_loop;
                        }
                    }
                    int newDegree = nonConflictsCountsForMissingTuples.get(key) + 1;
                    nonConflictsCountsForMissingTuples.put(key, newDegree);
                    maxDegree = Math.max(maxDegree, newDegree);
                }
            }
            return maxDegree;
        }

        private void readInMissingTuplesByValue(CoverageMap coverageMap,
                                                Map<Integer, List<int[][]>> missingTuplesByValue,
                                                Map<List<Integer>, Integer> nonConflictsCountsForMissingTuples) {
            int coveredParameterCombinationsUntilRank = 0, coveredValueCombinationsUntilRank = 0;
            Optional<int[][]> uncoveredCombination = coverageMap.getUncoveredCombination(
                    coveredParameterCombinationsUntilRank, coveredValueCombinationsUntilRank);
            while (uncoveredCombination.isPresent()) {
                int[] parameterCombination = uncoveredCombination.get()[0],
                        valueCombination = uncoveredCombination.get()[1];
                coveredParameterCombinationsUntilRank = uncoveredCombination.get()[2][0];
                coveredValueCombinationsUntilRank = uncoveredCombination.get()[2][1];
                nonConflictsCountsForMissingTuples.put(List.of(coveredParameterCombinationsUntilRank,
                        coveredValueCombinationsUntilRank), 0);
                int valueOfNewColumnInCurrentUncoveredTuple = valueCombination[strength - 1];
                if (!missingTuplesByValue.containsKey(valueOfNewColumnInCurrentUncoveredTuple)) {
                    missingTuplesByValue.put(valueOfNewColumnInCurrentUncoveredTuple, new ArrayList<>());
                }
                missingTuplesByValue.get(valueOfNewColumnInCurrentUncoveredTuple)
                        .add(new int[][]{parameterCombination, valueCombination,
                                new int[]{coveredParameterCombinationsUntilRank, coveredValueCombinationsUntilRank}});
                coveredValueCombinationsUntilRank++;
                uncoveredCombination = coverageMap.getUncoveredCombination(
                        coveredParameterCombinationsUntilRank, coveredValueCombinationsUntilRank);
            }
        }
    }
}