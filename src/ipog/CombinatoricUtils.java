package ipog;

import java.util.Arrays;
import java.util.Iterator;

public class CombinatoricUtils {
    public static class BinomialCoefficient {
        private final int[][] nCkMemo;

        public BinomialCoefficient(int mxN, int mxK) {
            nCkMemo = new int[mxN + 1][mxK + 1];
            choose(mxN, mxK);
        }

        public int choose(int n, int k) {
            if (k < 0 || n < k) {
                return 0;
            }
            if (k == 0 || k == n) {
                return 1;
            }
            if (nCkMemo[n][k] == 0) {
                nCkMemo[n][k] = choose(n - 1, k) + choose(n - 1, k - 1);
                if (nCkMemo[n][k] < 0) {
                    throw new ArithmeticException("Integer overflow!");
                }
            }
            return nCkMemo[n][k];
        }
    }

    // https://cp-algorithms.com/combinatorics/generating_combinations.html
    public static boolean nextKCombination(int[] a, int n) {
        int k = a.length;
        for (int i = k - 1; i >= 0; i--) {
            if (a[i] < n - k + i) {
                a[i]++;
                for (int j = i + 1; j < k; j++) {
                    a[j] = a[j - 1] + 1;
                }
                return true;
            }
        }
        return false;
    }

    public static int kSubsetLexRank(int[] kElementSubset, int n, BinomialCoefficient binomCoeffs) {
        int rank = 0, k = kElementSubset.length;
        for (int i = 0; i < k; i++) {
            Preconditions.checkArgument(kElementSubset[i] >= 0 && kElementSubset[i] < n,
                    Preconditions.INVALID_ELEMENT_IN_KSUBSET);
            if ((i == 0 && kElementSubset[i] >= 1) || (i > 0 && kElementSubset[i - 1] + 1 <= kElementSubset[i] - 1)) {
                int j = i == 0 ? 0 : kElementSubset[i - 1] + 1;
                while (j < kElementSubset[i]) {
                    rank += binomCoeffs.choose(n - (j + 1), k - (i + 1));
                    j++;
                }
            }
        }
        return rank;
    }

    // https://stackoverflow.com/questions/714108/cartesian-product-of-an-arbitrary-number-of-sets
    public static class CartesianProduct implements Iterable<int[]>, Iterator<int[]> {
        private final int[] lengths;  // length[i] = alphabet size of the i'th set
        private final int[] indices;  // current combination; indices[i] = index of the value from the i'th set
        private boolean hasNext;

        public CartesianProduct(int[] lengths) {
            this.lengths = lengths;
            indices = new int[lengths.length];
            hasNext = true;
        }

        public boolean hasNext() {
            return hasNext;
        }

        public int[] next() {
            int[] result = Arrays.copyOf(indices, indices.length);
            for (int i = indices.length - 1; i >= 0; i--) {
                if (indices[i] == lengths[i] - 1) {
                    indices[i] = 0;
                    if (i == 0) {
                        hasNext = false;
                    }
                }
                else {
                    indices[i]++;
                    break;
                }
            }
            return result;
        }

        public Iterator<int[]> iterator() {
            return this;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
