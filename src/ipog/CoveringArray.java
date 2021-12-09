package ipog;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class CoveringArray implements Iterable<LinkedHashMap<String, Object>> {
    private final List<Parameter<?>> parameters;
    private final List<int[]> coveringArray;
    private final int numberOfColumns;  // number of parameters (k)
    private final int numberOfRows;  // number of tests (N)

    public static final int DONT_CARE_VALUE = -1;
    public static final char DONT_CARE_SYMBOL = '*';

    private Map<String, Integer> parameterNameToColumnIndexMap;

    CoveringArray(List<Parameter<?>> parameters, List<int[]> coveringArray) {
        Preconditions.checkNotNull(parameters);
        Preconditions.checkNotNull(coveringArray);
        Preconditions.checkArgument(coveringArray.size() > 0);
        Preconditions.checkArgument(parameters.size() > 0);
        Preconditions.checkArgument(parameters.size() == coveringArray.get(0).length);
        this.numberOfRows = coveringArray.size();
        this.numberOfColumns = coveringArray.get(0).length;
        this.parameters = parameters;
        this.coveringArray = coveringArray;
        fillParameterNameToColumnIndexMap(parameters);
    }

    private void fillParameterNameToColumnIndexMap(List<Parameter<?>> parameters) {
        this.parameterNameToColumnIndexMap = new HashMap<>();
        for (int i = 0; i < parameters.size(); i++) {
            parameterNameToColumnIndexMap.put(parameters.get(i).getName(), i);
        }
    }

    public List<LinkedHashMap<String, Object>> getCoveringArrayAsLinkedHashMap(boolean randomizeDontCares) {
        List<LinkedHashMap<String, Object>> ca = new ArrayList<>(numberOfRows);
        for (int row = 0; row < coveringArray.size(); row++) {
            LinkedHashMap<String, Object> nextRow = new LinkedHashMap<>(numberOfColumns);
            for (int column = 0; column < numberOfColumns; column++) {
                nextRow.put(getParameterName(column), getValue(column, row, randomizeDontCares));
            }
            ca.add(nextRow);
        }
        return ca;
    }

    public Object getValue(int column, int row, boolean randomizeDontCares) {
        Object value;
        int valueIndex = coveringArray.get(row)[column];
        if (valueIndex != DONT_CARE_VALUE) {
            value = parameters.get(column).getValues().get(valueIndex);
        }
        else if (randomizeDontCares) {
            int randomVal = ThreadLocalRandom.current()
                    .nextInt(0, parameters.get(column).size());
            value = parameters.get(column).getValues().get(randomVal);
        }
        else {
            value = DONT_CARE_SYMBOL;
        }
        return value;
    }

    public Object getValue(String parameterName, int row, boolean randomizeDontCares) {
        return getValue(parameterNameToColumnIndexMap.get(parameterName), row, randomizeDontCares);
    }

    public Parameter<?> getParameter(int columnIndex) {
        return parameters.get(columnIndex);
    }

    public String getParameterName(int columnIndex) {
        return parameters.get(columnIndex).getName();
    }

    public List<Parameter<?>> getParameters() {
        return parameters;
    }

    public List<int[]> getTable() {
        return coveringArray;
    }

    public int numberOfColumns() {
        return numberOfColumns;
    }

    public int numberOfRows() {
        return numberOfRows;
    }

    @Override
    public String toString() {
        StringBuilder caAsString = new StringBuilder();
        caAsString.append("[");
        for (int row = 0; row < numberOfRows(); row++) {
            caAsString.append("[");
            for (int column = 0; column < numberOfColumns(); column++) {
                caAsString.append("{\"").append(getParameter(column).getName())
                        .append("\"=\"").append(getValue(column, row, false)).append("\"}")
                        .append(column < numberOfColumns() - 1 ? ", " : "");
            }
            caAsString.append("]").append(row < numberOfRows() - 1 ? ",\n" : "");
        }
        caAsString.append("]");
        return caAsString.toString();
    }

    public String toCsv(boolean randomizeDontCares) {
        StringBuilder table = new StringBuilder();
        for (int parameter = 0; parameter < numberOfColumns(); parameter++) {
            table.append(parameters.get(parameter).getName())
                    .append(parameter < numberOfColumns() - 1 ? "," : "");
        }
        table.append("\n");
        for (int test = 0; test < numberOfRows(); test++) {
            for (int parameter = 0; parameter < numberOfColumns(); parameter++) {
                table.append(getValue(parameter, test, randomizeDontCares));
                table.append(parameter < numberOfColumns() - 1 ? "," : "");
            }
            table.append("\n");
        }
        return table.toString();
    }

    @Override
    public Iterator<LinkedHashMap<String, Object>> iterator() {
        return getCoveringArrayAsLinkedHashMap(true).iterator();
    }
}
