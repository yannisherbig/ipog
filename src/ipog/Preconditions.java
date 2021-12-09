package ipog;

final class Preconditions {
    static final String STRENGTH_TOO_SMALL = "Strength must be positive!";
    static final String STRENGTH_TOO_BIG = "Strength must be less than or equal to the number of parameters!";
    static final String TOO_MANY_COMBOS = "The combination of the provided strength and number of parameters is too large!";
    static final String UNSUPPORTED_BASE_ALGORITHM = "The provided base algorithm is not supported!";
    static final String FALSE_ARRAY_LENGTH = "The provided parameter combination or value combination array has the wrong length!";
    static final String OCC_NOT_COUNTED = "The coverage map needs to count the occurrences!";
    static final String INVALID_ELEMENT_IN_KSUBSET = "Invalid element in provided k-subset!";

    private Preconditions() {
    }

    public static <T> T checkNotNull(T t) {
        return checkNotNull(t, null);
    }

    public static <T> T checkNotNull(T t, String message) {
        if (t == null) {
            throw new NullPointerException(message);
        }
        return t;
    }

    public static void checkArgument(boolean b, String message) {
        if (!b) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void checkArgument(boolean b) {
        checkArgument(b, null);
    }
}