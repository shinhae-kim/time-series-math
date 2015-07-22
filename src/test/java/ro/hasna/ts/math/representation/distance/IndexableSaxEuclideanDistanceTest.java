package ro.hasna.ts.math.representation.distance;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ro.hasna.ts.math.representation.IndexableSymbolicAggregateApproximation;
import ro.hasna.ts.math.type.SaxPair;
import ro.hasna.ts.math.util.TimeSeriesPrecision;

import java.util.Random;

/**
 * @since 1.0
 */
public class IndexableSaxEuclideanDistanceTest {
    private IndexableSaxEuclideanDistance distance;

    @Before
    public void setUp() throws Exception {
        distance = new IndexableSaxEuclideanDistance(new IndexableSymbolicAggregateApproximation(8, new int[]{2, 2, 2, 4, 2, 2, 2, 2}));
    }

    @After
    public void tearDown() throws Exception {
        distance = null;
    }

    @Test
    public void testCompute1() throws Exception {
        int n = 128;
        double a[] = new double[n];
        double b[] = new double[n];
        double c[] = new double[n];

        for (int i = 0; i < n; i++) {
            a[i] = i;
            b[i] = n - i;
            c[i] = i * i;
        }

        double ab = distance.compute(a, b);
        double ba = distance.compute(b, a);
        double bc = distance.compute(b, c);
        double ac = distance.compute(a, c);

        Assert.assertEquals(ab, ba, TimeSeriesPrecision.EPSILON);
        Assert.assertTrue(ab + bc >= ac);
    }

    @Test
    public void testCompute2() throws Exception {
        int n = 128;
        double a[] = new double[n];
        double b[] = new double[n];
        Random random = new Random();
        for (int i = 0; i < n; i++) {
            if (i >= 48 && i < 64) {
                a[i] = 10 + i;
                b[i] = 300 + i + random.nextDouble();
            } else {
                a[i] = i;
                b[i] = 100 + i + random.nextDouble();
            }
        }

        double result = distance.compute(a, b);

        Assert.assertEquals(0, result, TimeSeriesPrecision.EPSILON);
    }

    @Test
    public void testCompute3() throws Exception {
        SaxPair[] a = new SaxPair[4];
        a[0] = new SaxPair(0,8);
        a[1] = new SaxPair(4,8);
        a[2] = new SaxPair(5,8);
        a[3] = new SaxPair(6,8);

        SaxPair[] b = new SaxPair[4];
        b[0] = new SaxPair(0,8);
        b[1] = new SaxPair(2,4);
        b[2] = new SaxPair(5,8);
        b[3] = new SaxPair(1,2);

        double result = distance.compute(a, b, 128);

        Assert.assertEquals(0, result, TimeSeriesPrecision.EPSILON);
    }
}