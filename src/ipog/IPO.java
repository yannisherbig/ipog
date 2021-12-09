package ipog;

import java.util.Map;
import java.util.Set;

interface IPO {
    boolean extendHorizontal(CoverageMap coverageMap, int i);
    void extendVertical(CoverageMap coverageMap, Map<Integer, Set<Integer>> partitions, int numberOfParameters);
}
