package classification;

import hexgraph.HEXGraphFactory;
import hexgraph.HEXGraphMethods;
import hexgraph.JunctionTree;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

import util.SparseVector;


public class HexLrTask {
	private JunctionTree<String> mJunctionTree;
	private HEXGraphMethods mHexGraphMethods;
	private LogRegClassifier[] mClassifiers;
	private String[] mClassNames;
	
	public static final int CLASSIFICATION_TRUE = 1;
	public static final int CLASSIFICATION_FALSE = 0;
	
	private final double ETA = 0.3;
	private final double LAMBDA = 0.3;
	
	public HexLrTask(File graphFile, int numClassifiers, String[] cnames, int numFeatures) throws IOException {
		HEXGraphFactory factory = new HEXGraphFactory();
		factory.buildHEXGraph(graphFile.getPath());
		mHexGraphMethods = new HEXGraphMethods(factory, graphFile.getPath());
		mJunctionTree = mHexGraphMethods.buildJunctionTree();
		mClassNames = cnames;
		
		mClassifiers = new LogRegClassifier[numClassifiers];
		// for each classifier, initialize the classifier to the correct number of weights
		for (int i = 0; i < mClassifiers.length; i++) {
			mClassifiers[i] = new LogRegClassifier(numFeatures, ETA, LAMBDA);
		}
	}
	
	public HexLrTask(File graphFile, File modelFile) {
		
	}
	
	public void train(SparseVector[] x_train, BitSet[] y_train, int numIterations) {
		// assert there are the same number of training examples and classes
		if (x_train.length != y_train.length) {
			System.err.println("Training data and classes of non-matching length");
			return;
		}
		
		for (int _ = 0; _ < numIterations; _++) {
			for (int i = 0; i < x_train.length; i++) {
				double[] scores = new double[mClassifiers.length];
				double[] labels = new double[mClassifiers.length];
				
				for (int c = 0; c < mClassifiers.length; c++) {
					LogRegClassifier curr = mClassifiers[c];
					curr.train(x_train[i]);
					scores[c] = curr.getRecentScore();
					labels[c] = curr.getRecentLabel();
				}
				
				// will return an array of labels
				// if hex suggests that there is no class to assign, we do the same update as previous
				double[] hexScores = getHexData(scores);
				
				for (int c = 0; c < mClassifiers.length; c++) {
					mClassifiers[i].update(x_train[i], hexScores[c], y_train[c].get(i));
				}
			}
		}
	}
	
	/**
	 * Must the directory must exist
	 */
	public void writeModelFile(String directory, String filename) throws IOException {
		File outDir = new File(directory);
		if (!outDir.isDirectory()) {
			outDir.mkdir();
		}
		File outFile = new File(directory + filename);
		PrintWriter writer = new PrintWriter(outFile.getPath(), "UTF-8");
		
		for (int i = 0; i < mClassNames.length; i++) {
			writer.println(mClassNames[i]);
			writer.print(mClassifiers[i].getBias() + ",");
			SparseVector weights = mClassifiers[i].getWeights();
			for (int j = 0; j < weights.size(); j++) {
				writer.print(weights.get(i) + ",");
			}
			writer.println();
		}
		writer.close();
		
	}
	
	/**
	 * For all the following below, int[] res consists of:
	 * [True_pos, True_neg, False_pos, False_neg]
	 */
	
	public void test(SparseVector[] x_test, BitSet[] y_test) {
		// iterate over the classifiers
		for (int j = 0; j < mClassifiers.length; j++) {
			int[] res = new int[4];
			// for each classifier iterate over the data set
			for (int i = 0; i < x_test.length; i++) {
				if (mClassifiers[j].getClassification(x_test[i])) {
					if (y_test[i].get(j)) {
						res[0]++;
					} else {
						res[2]++;
					}
				} else {
					if (y_test[i].get(j)) {
						res[3]++;
					} else {
						res[1]++;
					}
				}
			}
			System.out.println("Results for" + mClassNames[j]);
			System.out.println("Accuracy: " + getAccuracy(res));
			System.out.println("Precision: " + getPrecision(res));
			System.out.println("Recall: " + getRecall(res));
		}
		
	}
	
	private double getPrecision(int[] res) {
		return (double)(res[0]) / (res[0] + res[3]);
	}
	
	private double getRecall(int[] res) {
		return (double)(res[0]) / (res[0] + res[2]);
	}
	
	private double getAccuracy(int[] res) {
		return (double)(res[0] + res[1]) / getSum(res);
	}
	
	private int getSum(int[] res) {
		int sum = 0;
		for (int i = 0; i < res.length; i++) {
			sum += res[i];
		}
		return sum;
	}
	
	private double[] getHexData(double[] scores) {
		// turn the score data into the map format
		Map<String, Double> scoreMap = new HashMap<String, Double>();
		for (int i = 0; i < scores.length; i++) {
			scoreMap.put(mClassNames[i], scores[i]);
		}
		mHexGraphMethods.setScores(scoreMap);
		
		double[] hexScores = new double[scores.length];
		Map<String, Double> results = mHexGraphMethods.exactMarginalInference(mJunctionTree);
		double sum = 0.0;
		for (int i = 0; i < scores.length; i++) {
			hexScores[i] = results.get(mClassNames[i]);
			sum += results.get(mClassNames[i]);
		}
		
		// regularize the scores and return
		for (int i = 0; i < hexScores.length; i++) {
			hexScores[i] /= sum;
		}
		
		return hexScores;	
	}
	
}
