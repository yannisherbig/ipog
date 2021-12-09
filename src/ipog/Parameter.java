package ipog;

import java.util.Arrays;
import java.util.List;

/**
 * Also known as factor or component
 */
public class Parameter<T> {
    private final String name;
    private final List<T> values;

    public Parameter(String name, List<T> values) {
        this.name = name;
        this.values = values;
    }

    @SafeVarargs
    public Parameter(String name, T... values) {
        this.name = name;
        this.values = Arrays.asList(values);
    }

    public String getName() {
        return name;
    }

    public List<T> getValues() {
        return values;
    }

    public int size() {
        return values.size();
    }

    public void addValue(T value) {
        this.values.add(value);
    }

    @Override
    public String toString() {
        return name + ": " + values;
    }
}