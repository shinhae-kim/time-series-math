/**
 * Copyright (C) 2015 Octavian Hasna
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ro.hasna.ts.math.ml.distance;

import org.apache.commons.math3.util.FastMath;
import ro.hasna.ts.math.normalization.Normalizer;
import ro.hasna.ts.math.normalization.ZNormalizer;

import java.util.Deque;
import java.util.LinkedList;

/**
 * Calculates the distance between two vectors using Dynamic Time Warping.
 * <p>
 * Reference:
 * Wikipedia https://en.wikipedia.org/wiki/Dynamic_time_warping
 * </p>
 *
 * @since 1.0
 */
public class DynamicTimeWarpingDistance implements GenericDistanceMeasure<double[]> {
    private static final long serialVersionUID = 1154818905340336905L;
    private final double radiusPercentage;
    private final Normalizer normalizer;

    public DynamicTimeWarpingDistance() {
        this(0.05, new ZNormalizer());
    }

    /**
     * Creates a new instance of this class with
     *
     * @param radiusPercentage Sakoe-Chiba Band width used to constraint the warping window
     * @param normalizer       the normalizer (it can be null if the values were normalized)
     */
    public DynamicTimeWarpingDistance(double radiusPercentage, Normalizer normalizer) {
        this.radiusPercentage = radiusPercentage;
        this.normalizer = normalizer;
    }

    @Override
    public double compute(double[] a, double[] b) {
        return compute(a, b, Double.POSITIVE_INFINITY);
    }

    @Override
    public double compute(double[] a, double[] b, int n, double cutOffValue) {
        return compute(a, b, Double.POSITIVE_INFINITY);
    }

    @Override
    public double compute(double[] a, double[] b, double cutOffValue) {
        if (normalizer != null) {
            a = normalizer.normalize(a);
            b = normalizer.normalize(b);
        }
        int n = a.length;
        int radius = (int) (n * radiusPercentage);
        double transformedCutOff = distance(cutOffValue, 0);

        if (computeModifiedKimLowerBound(a, b, n, transformedCutOff) == Double.POSITIVE_INFINITY) {
            return Double.POSITIVE_INFINITY;
        }

        LowerBound keoghLowerBound = computeKeoghLowerBound(a, b, n, radius, transformedCutOff);
        if (keoghLowerBound.value == Double.POSITIVE_INFINITY) {
            return Double.POSITIVE_INFINITY;
        }

        LowerBound lemireLowerBound = computeLemireLowerBound(b, keoghLowerBound, n, radius, transformedCutOff);
        if (lemireLowerBound.value == Double.POSITIVE_INFINITY) {
            return Double.POSITIVE_INFINITY;
        }

        double[] cumulativeLowerBound = lemireLowerBound.distances;
        for (int i = cumulativeLowerBound.length - 2; i >= 0; i--) {
            cumulativeLowerBound[i] += cumulativeLowerBound[i + 1];
        }

        return computeDtw(a, b, n, radius, cumulativeLowerBound, transformedCutOff);
    }

    /**
     * <p>
     * References:
     * Kim Sang-Wook, Sanghyun Park and Wesley W. Chu (2001)
     * <i>An index-based approach for similarity search supporting time warping in large sequence databases</i>
     * Rakthanmanon Thanawin, et al. (2013)
     * <i>Addressing big data time series: Mining trillions of time series subsequences under dynamic time warping</i>
     * </p>
     *
     * @return modified LB_Kim value
     */
    protected double computeModifiedKimLowerBound(double[] a, double[] b, int n, double transformedCutOff) {
        double d = distance(a[0], b[0]) + distance(a[n - 1], b[n - 1]);
        if (d >= transformedCutOff) {
            return Double.POSITIVE_INFINITY;
        }

        d += FastMath.min(distance(a[1], b[0]), FastMath.min(distance(a[0], b[1]), distance(a[1], b[1])));
        if (d >= transformedCutOff) {
            return Double.POSITIVE_INFINITY;
        }

        d += FastMath.min(distance(a[n - 1], b[n - 2]), FastMath.min(distance(a[n - 2], b[n - 1]), distance(a[n - 2], b[n - 2])));
        if (d >= transformedCutOff) {
            return Double.POSITIVE_INFINITY;
        }

        double m1 = FastMath.min(distance(a[0], b[2]), distance(a[1], b[2]));
        double m2 = FastMath.min(distance(a[2], b[0]), distance(a[2], b[1]));
        d += FastMath.min(distance(a[2], b[2]), FastMath.min(m1, m2));
        if (d >= transformedCutOff) {
            return Double.POSITIVE_INFINITY;
        }

        m1 = FastMath.min(distance(a[n - 1], b[n - 3]), distance(a[n - 2], b[n - 3]));
        m2 = FastMath.min(distance(a[n - 3], b[n - 1]), distance(a[n - 3], b[n - 2]));
        d += FastMath.min(distance(a[n - 3], b[n - 3]), FastMath.min(m1, m2));
        if (d >= transformedCutOff) {
            return Double.POSITIVE_INFINITY;
        }

        return d;
    }

    /**
     * <p>
     * Reference:
     * Daniel Lemire (2008)
     * <i>Faster Sequential Search with a Two-Pass Dynamic-Time-Warping Lower Bound</i>
     * </p>
     *
     * @return LB_Improved value
     */
    protected LowerBound computeLemireLowerBound(double[] b, LowerBound keoghLowerBound, int n, int radius, double transformedCutOff) {
        LowerBound improvedLowerBound = computeKeoghLowerBound(b, keoghLowerBound.projection, n, radius, transformedCutOff - keoghLowerBound.value);
        if (improvedLowerBound.value != Double.POSITIVE_INFINITY) {
            for (int i = 0; i < improvedLowerBound.distances.length; i++) {
                improvedLowerBound.distances[i] += keoghLowerBound.distances[i];
            }
            improvedLowerBound.value += keoghLowerBound.value;
        }
        return improvedLowerBound;
    }

    /**
     * <p>
     * Reference:
     * Eamonn Keogh and Chotirat Ann Ratanamahatana (2004)
     * <i>Exact indexing of dynamic time warping</i>
     * </p>
     *
     * @return LB_Keogh value and projection
     */
    protected LowerBound computeKeoghLowerBound(double[] a, double[] b, int n, int radius, double transformedCutOff) {
        int i;
        double value = 0, min, max, d;
        double[] projection = new double[n];
        double[] distances = new double[n];

        Envelope envelope = computeLemireEnvelope(b, n, radius);

        for (i = 0; i < n; i++) {
            min = envelope.lower[i];
            max = envelope.upper[i];
            d = 0;
            if (a[i] > max) {
                d = distance(a[i], max);
                projection[i] = max;
            } else if (a[i] < min) {
                d = distance(a[i], min);
                projection[i] = min;
            } else {
                projection[i] = a[i];
            }
            distances[i] = d;
            value += d;

            if (value >= transformedCutOff) {
                return new LowerBound(Double.POSITIVE_INFINITY, projection, distances);
            }
        }

        return new LowerBound(value, projection, distances);
    }

    protected Envelope computeLemireEnvelope(double[] v, int n, int radius) {
        int w = 2 * radius + 1;
        double[] upper = new double[n];
        double[] lower = new double[n];
        int i;

        Deque<Integer> upperList = new LinkedList<>();
        Deque<Integer> lowerList = new LinkedList<>();
        upperList.addLast(0);
        lowerList.addLast(0);
        for (i = 1; i < n; i++) {
            if (i >= w) {
                upper[i - w] = v[upperList.getFirst()];
                lower[i - w] = v[lowerList.getFirst()];
            }
            if (v[i] > v[i - 1]) {
                upperList.removeLast();
                while (!upperList.isEmpty() && v[i] > v[upperList.getLast()]) {
                    upperList.removeLast();
                }
            } else {
                lowerList.removeLast();
                while (!lowerList.isEmpty() && v[i] < v[lowerList.getLast()]) {
                    lowerList.removeLast();
                }
            }
            upperList.addLast(i);
            lowerList.addLast(i);
            if (i == 2 * w + upperList.getFirst()) {
                upperList.removeFirst();
            } else if (i == 2 * w + lowerList.getFirst()) {
                lowerList.removeFirst();
            }
        }

        for (i = n; i < n + w; i++) {
            upper[i - w] = v[upperList.getFirst()];
            lower[i - w] = v[lowerList.getFirst()];
            if (i - upperList.getFirst() >= 2 * w) {
                upperList.removeFirst();
            }
            if (i - lowerList.getFirst() >= 2 * w) {
                lowerList.removeFirst();
            }
        }

        return new Envelope(lower, upper);
    }

    protected double computeDtw(double[] a, double[] b, int n, int radius, double[] cumulativeLowerBound, double transformedCutOff) {
        int w = 2 * radius + 1;
        double[] cost = new double[w];
        double[] prevCost = new double[w];
        int i, j, k = 0, start, end;
        double min, x, y, z;
        for (i = 0; i < w; i++) {
            cost[i] = Double.POSITIVE_INFINITY;
            prevCost[i] = Double.POSITIVE_INFINITY;
        }

        for (i = 0; i < n; i++) {
            k = FastMath.max(0, radius - i);
            min = Double.POSITIVE_INFINITY;

            start = FastMath.max(0, i - radius);
            end = FastMath.min(n - 1, i + radius);
            for (j = start; j <= end; j++, k++) {
                if (i == 0 && j == 0) {
                    cost[k] = distance(a[0], b[0]);
                    min = cost[k];
                    continue;
                }

                if (j - 1 < 0 || k - 1 < 0) y = Double.POSITIVE_INFINITY;
                else y = cost[k - 1];

                if (i - 1 < 0 || k + 1 > 2 * radius) x = Double.POSITIVE_INFINITY;
                else x = prevCost[k + 1];

                if (i - 1 < 0 || j - 1 < 0) z = Double.POSITIVE_INFINITY;
                else z = prevCost[k];

                // classic DTW calculation
                cost[k] = distance(a[i], b[j]) + FastMath.min(x, FastMath.min(y, z));

                // minimum cost in row for early abandoning
                if (cost[k] < min) {
                    min = cost[k];
                }
            }

            // abandon early if the current cumulative distance with lower bound together are larger than cutOff
            if (i + radius < n - 1 && min + cumulativeLowerBound[i + radius + 1] >= transformedCutOff) {
                return Double.POSITIVE_INFINITY;
            }

            System.arraycopy(cost, 0, prevCost, 0, w);
        }

        return postProcessDistance(prevCost[k - 1]);
    }

    protected double distance(double a, double b) {
        return (a - b) * (a - b);
    }

    protected double postProcessDistance(double d) {
        return FastMath.sqrt(d);
    }

    protected static class LowerBound {
        double value;
        double[] projection;
        double[] distances;

        public LowerBound(double value, double[] projection, double[] distances) {
            this.value = value;
            this.projection = projection;
            this.distances = distances;
        }
    }

    protected static class Envelope {
        double[] lower;
        double[] upper;

        public Envelope(double[] lower, double[] upper) {
            this.lower = lower;
            this.upper = upper;
        }
    }
}
