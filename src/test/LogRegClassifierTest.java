package test;


import util.SparseVector;
import classification.LogRegClassifier;

public class LogRegClassifierTest {
	private static LogRegClassifier mClassifier;
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
		mClassifier = new LogRegClassifier(3, 0.1, 0.3);
		
		for (int j = 0; j < NUM_ITER; j++) {			
			for (int i = 0; i < points.length; i++) {
				SparseVector vec = new SparseVector(3);
				vec.put(0, 1);
				vec.put(1, points[i][0]);
				vec.put(2, points[i][1]);
				mClassifier.train(vec);
				mClassifier.update(vec, mClassifier.getRecentScore(), points[i][2] == 1);
			}
		}
		
		System.out.println(mClassifier.getWeights());
		
		test_points = points;
		
		for (int i = 0; i < test_points.length; i++) {
			SparseVector vec = new SparseVector(3);
			vec.put(0, 1);
			vec.put(1, test_points[i][0]);
			vec.put(2, test_points[i][1]);
			
			System.out.println(String.format("%f %f %b: %f", test_points[i][0], test_points[i][1], 
					test_points[i][2] == (mClassifier.getClassification(vec) ? 1 : 0), 
					mClassifier.getClassificationScore(vec)));
		}
	}
	
	
}
