package test;


import util.SparseMatrix;
import util.SparseVector;
import classification.SparseLogRegClassifier;

public class LogRegClassifierTest {
	private static SparseLogRegClassifier mClassifier;
	private static final int NUM_ITER = 50;
	
	private static double[][] points = {
		{0,0.1,1},
		{0,2,1},
		{0.1,0,0},
		{2,-1,0},
		{-4,5,1},
		{2,1,0},
		{8,7,0},
		{5,6,1},
		{-4,-5,0},
		{6,3,0},
		{-3,1,1},
		{-1,0,1},
		{-4,1,1},
		{4,-1,0},
		{4,2,0},
		{11,14,1},
		{-30,-40,0},
		{5,7,1},
		{6,5,0},
		{-3,-5,0}
	};
	
	private static double[][] test_points = {
		{-1,0,1},
		{-4,1,1},
		{4,-1,0},
		{4,2,0},
		{11,14,1},
		{-30,-40,0},
		{5,7,1},
		{6,5,0},
		{-3,-5,0}
	};
	
	public static void main(String[] args) {
		mClassifier = new SparseLogRegClassifier(3, 0.1, 0.3);
		
		SparseMatrix x_train = new SparseMatrix(3, points.length);
		SparseVector y_train = new SparseVector(points.length);
		for (int i = 0; i < points.length; i++) {
			x_train.put(i, 0, 1.0);
			x_train.put(i, 1, points[i][0]);
			x_train.put(i, 2, points[i][1]);
			y_train.put(i, points[i][2]);
		}
		for (int j = 0; j < NUM_ITER; j++) {
			double[] scores = mClassifier.train(x_train);
			System.out.println(mClassifier.getLogLoss(scores, y_train));
			mClassifier.update(x_train, scores, y_train);
		}
		
		SparseMatrix x_test = new SparseMatrix(3, test_points.length);
		SparseVector y_test = new SparseVector(test_points.length);
		for (int i = 0; i < test_points.length; i++) {
			x_test.put(i, 0, 1);
			x_test.put(i, 1, test_points[i][0]);
			x_test.put(i, 2, test_points[i][1]);
			y_test.put(i, test_points[i][2]);
		}
		
		test(x_test, y_test);
	}
	
	public static void test(SparseMatrix x_test, SparseVector y_test) {
		int[] res = new int[4];
		// for each classifier iterate over the data set
		for (int i = 0; i < x_test.getRows(); i++) {
			if (mClassifier.getClassification(x_test.getRow(i))) {
				if (y_test.get(i) == 1.0) {
					res[0]++;
				} else {
					res[2]++;
				}
			} else {
				if (y_test.get(i) == 1.0) {
					res[3]++;
				} else {
					res[1]++;
				}
			}
		}
		System.out.println("Accuracy: " + getAccuracy(res));
		System.out.println("Precision: " + getPrecision(res));
		System.out.println("Recall: " + getRecall(res));
		
	}
	
	
	/**
	 * Returns the classification precision of a given set of predictions
	 */
	private static double getPrecision(int[] res) {
		return (double)(res[0]) / (res[0] + res[3]);
	}
	
	/**
	 * Returns the classification recall of a given set of predictions
	 */
	private static double getRecall(int[] res) {
		return (double)(res[0]) / (res[0] + res[2]);
	}
	
	/**
	 * Returns the classification accuracy of a given set of predictions
	 */
	private static double getAccuracy(int[] res) {
		return (double)(res[0] + res[1]) / getSum(res);
	}
	
	/**
	 * Helper method to return the sum of an int array
	 * 
	 * @param res - the int array we are summing
	 * @return the sum of the values in res
	 */
	private static int getSum(int[] res) {
		int sum = 0;
		for (int i = 0; i < res.length; i++) {
			sum += res[i];
		}
		return sum;
	}
	
}
