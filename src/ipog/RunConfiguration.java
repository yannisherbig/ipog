package ipog;

import java.util.*;

public class RunConfiguration {
    private final List<Parameter<?>> parameters;
    private final int strength;
    private final BaseAlgorithm baseAlgorithm;
    private final boolean enhanceHorizontal, adaptVertical, fullHorizontal;

    public static Builder builder(List<Parameter<?>> parameters, int strength, BaseAlgorithm baseAlgorithm) {
        return new Builder(parameters, strength, baseAlgorithm);
    }

    private RunConfiguration(Builder builder) {
        this.parameters = builder.parameters;
        this.strength = builder.strength;
        this.baseAlgorithm = builder.baseAlgorithm;
        this.enhanceHorizontal = builder.enhanceHorizontal;
        this.fullHorizontal = builder.fullHorizontal;
        this.adaptVertical = builder.adaptVertical;
    }

    public List<Parameter<?>> getParameters() {
        return parameters;
    }

    public int getStrength() {
        return strength;
    }

    public BaseAlgorithm getBaseAlgorithm() {
        return baseAlgorithm;
    }

    public boolean isEnhanceHorizontal() {
        return enhanceHorizontal;
    }

    public boolean isAdaptVertical() {
        return adaptVertical;
    }

    public boolean isFullHorizontal() {
        return fullHorizontal;
    }

    public static class Builder {
        private final List<Parameter<?>> parameters;
        private final int strength;
        private final BaseAlgorithm baseAlgorithm;
        private boolean enhanceHorizontal, adaptVertical, fullHorizontal;

        private Builder(List<Parameter<?>> parameters, int strength, BaseAlgorithm baseAlgorithm) {
            this.parameters = parameters;
            this.strength = strength;
            this.baseAlgorithm = baseAlgorithm;
        }

        /**
         * Heuristically enhance the horizontal extension as described in the SIPO algorithm.
         * Has potential for smaller covering arrays in exchange for a longer runtime.
         * @param fullHorizontal extends the search space beyond the newly added column:
         *                       if set to true, it also includes the entries with
         *                       don't-care values from earlier added columns.
         */
        public Builder enhanceHorizontal(boolean fullHorizontal) {
            enhanceHorizontal = true;
            this.fullHorizontal = fullHorizontal;
            return this;
        }

        /**
         * Uses graph coloring to improve the vertical growth.
         * Has potential for smaller covering arrays in exchange for a longer runtime.
         */
        public Builder adaptVertical() {
            adaptVertical = true;
            return this;
        }

        public RunConfiguration build() {
            return new RunConfiguration(this);
        }
    }
}