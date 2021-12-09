package ipog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParameterConfigurationParser {
    /**
     *
     * @param parameterConfiguration the parameter configuration in exponential notation
     */
    public static List<Parameter<?>> generate(String parameterConfiguration) {
        parameterConfiguration = parameterConfiguration.trim();
        Map<Integer, Integer> configMap = new HashMap<>();
        int i = 0;
        while (i < parameterConfiguration.length()) {
            StringBuilder currentNumber = new StringBuilder();
            char c = parameterConfiguration.charAt(i);
            while (Character.isDigit(c)) {
                currentNumber.append(c);
                if (i < parameterConfiguration.length() - 1) {
                    c = parameterConfiguration.charAt(++i);
                }
                else {
                    break;
                }
            }
            if (currentNumber.length() == 0) {
                throw new IllegalArgumentException("domain size expected, but not found! input string has wrong format!");
            }
            int domainSize = Integer.parseInt(currentNumber.toString());
            if (domainSize <= 0) {
                throw new IllegalArgumentException("domain size needs to be > 0! input string has wrong format!");
            }
            if (configMap.containsKey(domainSize)) {
                throw new IllegalArgumentException("each domain size can only be included once! input string has wrong format!");
            }
            if (c != '^') {
                throw new IllegalArgumentException("caret '^' is missed! input string has wrong format.");
            }
            i++;  // skip '^'
            c = parameterConfiguration.charAt(i);
            currentNumber = new StringBuilder();
            while (Character.isDigit(c)) {
                currentNumber.append(c);
                if (i < parameterConfiguration.length() - 1) {
                    c = parameterConfiguration.charAt(++i);
                }
                else {
                    break;
                }
            }
            if (currentNumber.length() == 0) {
                throw new IllegalArgumentException("exponent expected, but not found! input string has wrong format!");
            }
            int n = Integer.parseInt(currentNumber.toString());
            if (n <= 0) {
                throw new IllegalArgumentException("n needs to be > 0! input string has wrong format!");
            }
            configMap.put(domainSize, n);
            i++;  // skip ',' or go all the way to the end of string
            while (i < parameterConfiguration.length() && parameterConfiguration.charAt(i) == ' ') {
                i++;
            }
        }
        return generate(configMap);
    }

    private static List<Parameter<?>> generate(Map<Integer, Integer> parameters) {
        List<Parameter<?>> runConfiguration = new ArrayList<>();
        int paramName = 0;
        for (Map.Entry<Integer, Integer> parameter : parameters.entrySet()) {
            int d = parameter.getKey(), n = parameter.getValue();  // domain size and how many of this domain size
            for (int i = 0; i < n; i++) {
                List<String> values = new ArrayList<>();
                for (int j = 0; j < d; j++) {
                    values.add(String.valueOf(j));
                }
                runConfiguration.add(new Parameter<>("p" + paramName, values));
                paramName++;
            }
        }
        return runConfiguration;
    }
}
