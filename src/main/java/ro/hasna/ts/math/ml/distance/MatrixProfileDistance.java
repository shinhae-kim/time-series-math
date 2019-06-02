package ro.hasna.ts.math.ml.distance;

import ro.hasna.ts.math.representation.mp.MatrixProfileTransformer;
import ro.hasna.ts.math.type.FullMatrixProfile;

import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * Calculates the k<sup>th</sup> smallest value in the full join matrix profile (Mab and Mba).
 * <p>
 * Reference:
 * Gharghabi S., Imani S., Bagnall A., Darvishzadeh A., Keogh E. (2018)
 * <i>An Ultra-Fast Time Series Distance Measure to allow Data Mining in more Complex Real-World Deployments</i>
 * </p>
 */
public class MatrixProfileDistance implements GenericDistanceMeasure<double[]> {
    private static final long serialVersionUID = -2290780320746907899L;
    private final MatrixProfileTransformer matrixProfileTransformer;
    private final double kPercentage;

    public MatrixProfileDistance(int window) {
        this(new MatrixProfileTransformer(window), 0.05);
    }

    public MatrixProfileDistance(MatrixProfileTransformer matrixProfileTransformer, double kPercentage) {
        this.matrixProfileTransformer = matrixProfileTransformer;
        this.kPercentage = kPercentage;
    }

    @Override
    public double compute(double[] a, double[] b) {
        return compute(a, b, Double.POSITIVE_INFINITY);
    }

    @Override
    public double compute(double[] a, double[] b, double cutOffValue) {
        FullMatrixProfile fmp = matrixProfileTransformer.fullJoinTransform(a, b);
        double[] leftProfile = fmp.getLeftMatrixProfile().getProfile();
        double[] rightProfile = fmp.getRightMatrixProfile().getProfile();
        int k = Math.max(1, (int) (kPercentage * (leftProfile.length + rightProfile.length)));

        PriorityQueue<Double> maxHeap = new PriorityQueue<>(Comparator.reverseOrder());
        updateMaxHeap(leftProfile, k, maxHeap);
        updateMaxHeap(rightProfile, k, maxHeap);
        return maxHeap.peek();
    }

    private void updateMaxHeap(double[] profile, int k, PriorityQueue<Double> maxHeap) {
        for (double v : profile) {
            if (maxHeap.size() < k) {
                maxHeap.add(v);
            } else if (v < maxHeap.peek()) {
                maxHeap.poll();
                maxHeap.add(v);
            }
        }
    }
}
