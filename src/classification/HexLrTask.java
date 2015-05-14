package classification;

import hexgraph.Configuration;
import hexgraph.HEXGraph;
import hexgraph.HEXGraphFactory;
import hexgraph.HEXGraphMethods;
import hexgraph.JunctionTree;
import hexgraph.JunctionTreeNode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Map;
import java.util.Set;

import util.NameSpace;
import util.SparseVector;

/**
 * A HEXGraph Logistic Regression task, or HexLrTask for short runs parallel classification of
 * Logistic Regression classifiers while utilizing {@link HEXGraph} inference for weighted updates. 
 */
public class HexLrTask {
	private JunctionTree<String> mJunctionTree;
	private HEXGraphMethods mHexGraphMethods;
	private LogRegClassifier[] mClassifiers;
	private String[] mClassNames;
	private int mNumFeatures;
	private NameSpace<String> mNameSpace;
	private Map<JunctionTreeNode<String>, Set<Configuration>> mJunctionTreeStateSpace;
	
	public static final int CLASSIFICATION_TRUE = 1;
	public static final int CLASSIFICATION_FALSE = 0;
	
	/**
	 * Learning Rate, passed in to each {@link LogRegClassifier}
	 * TODO: put in a function to control the learning rate
	 */
	private final double ETA = 0.1;
	
	/**
	 * Regularization parameter, passed in to each {@link LogRegClassifier}
	 */
	private final double LAMBDA = 0.3;
	
	/**
	 * Constructs a new {@link HexLrTask} from the given parameters.
	 * 
	 * @param graphFile - the file encoding of the {@link HEXGraph} we will be using to 
	 * @param cnames - the string names of the classes
	 * @param numFeatures - the number of features each datum contains
	 * @param nameSpace - a {@link NameSpace} encoding of cnames that can be passed to the 
	 * 	{@link HEXGraph}
	 * @throws IOException if graphFile does not exist.
	 */
	public HexLrTask(File graphFile, String[] cnames, int numFeatures, NameSpace<String> nameSpace) throws IOException {
		mNameSpace = nameSpace;
		HEXGraphFactory factory = new HEXGraphFactory(nameSpace);
		factory.buildHEXGraph(graphFile.getPath());
		mHexGraphMethods = new HEXGraphMethods(factory, graphFile.getPath(), nameSpace);
		
		long startTime = System.currentTimeMillis();
		mJunctionTree = mHexGraphMethods.buildJunctionTree();
		long endTime = System.currentTimeMillis();
		System.out.println(String.format("Building junction tree took %d ms", endTime - startTime));
		mJunctionTreeStateSpace = mHexGraphMethods.getJunctionTreeStateSpaces(mJunctionTree);
		
		mClassNames = cnames;
		mNumFeatures = numFeatures;
		
		mClassifiers = new LogRegClassifier[cnames.length];
		// for each classifier, initialize the classifier to the correct number of weights
		for (int i = 0; i < mClassifiers.length; i++) {
			mClassifiers[i] = new LogRegClassifier(mNumFeatures, ETA, LAMBDA);
		}
	}
	
	/**
	 * Constructs a new {@link HexLrTask} from a model file.
	 * 
	 * @param graphFile - String filepath of the graph file
	 * @param modelFile - String filepath of the model file
	 * @throws IOException if an invalid filepath is passed in for the model file
	 */
	public HexLrTask(String graphFile, String modelFile) throws IOException {
		HEXGraphFactory factory = new HEXGraphFactory(mNameSpace);
		factory.buildHEXGraph(graphFile);
		mHexGraphMethods = new HEXGraphMethods(factory, graphFile, mNameSpace);
		mJunctionTree = mHexGraphMethods.buildJunctionTree();
		
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(modelFile));
			int numClasses = Integer.parseInt(br.readLine().trim());
			mNumFeatures = Integer.parseInt(br.readLine().trim());
			mClassifiers = new LogRegClassifier[numClasses]; 
			mClassNames = new String[numClasses];
			
			for (int i = 0; i < numClasses; i++) {
				mClassNames[i] = br.readLine();
				SparseVector weights = new SparseVector(mNumFeatures);
				for (String entry : br.readLine().trim().split(" ")) {
					String[] splitEntry = entry.split(":");
					weights.put(Integer.parseInt(splitEntry[0]), Double.parseDouble(splitEntry[1]));
				}
				mClassifiers[i] = new LogRegClassifier(weights, ETA, LAMBDA);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				br.close();
			}
		}
	}
	
	/**
	 * Runs a synchronized training procedure for all classifiers in parallel.
	 * 
	 * TODO: Calculate log loss for each classifier at the end of the iteration, and if it is less
	 * 	than a value epsilon, return
	 * 
	 * @param x_train - {@link SparseVector} array containing the training data
	 * @param y_train - {@link BitSet} array containing the training labels
	 * @param numIterations - the number of iterations over our data we will use to train
	 */
	public void train(SparseVector[] x_train, BitSet[] y_train, int numIterations) {
		// assert there are the same number of training examples and classes
		if (x_train.length != y_train.length) {
			System.err.println("Training data and classes of non-matching length");
			return;
		}
		
		System.out.println("YTRAIN LENTG" + y_train.length);
		System.out.println(y_train[113]);
		
		for (int j = 0; j < numIterations; j++) {
			for (int i = 0; i < x_train.length; i++) {
				System.out.println("Loop " + j + " instance " + i);
				double[] scores = new double[mClassifiers.length];
				double[] labels = new double[mClassifiers.length];
				
				long startTime = System.currentTimeMillis();
				for (int c = 0; c < mClassifiers.length; c++) {
					LogRegClassifier curr = mClassifiers[c];
					curr.train(x_train[i]);
					scores[c] = curr.getRecentScore();
					labels[c] = curr.getRecentLabel();
				}
				long endTime = System.currentTimeMillis();
				// System.out.println(String.format("Getting current weights %d ms", endTime - startTime));
				
				startTime = System.currentTimeMillis();
				// System.out.println(Arrays.toString(scores));
				double[] hexScores = getHexData(scores);
				endTime = System.currentTimeMillis();
				// System.out.println(String.format("Collecting hex scores %d ms", endTime - startTime));
				
				startTime = System.currentTimeMillis();
				for (int c = 0; c < mClassifiers.length; c++) {
					if (y_train[i].get(c)) System.out.println(scores[c] + " " + mNameSpace.get(c));
					mClassifiers[c].update(x_train[i], hexScores[c], y_train[i].get(c));
					if (y_train[i].get(c)) {
						mClassifiers[c].train(x_train[i]);
						System.out.println(mClassifiers[c].getRecentScore());
					}
				}
				endTime = System.currentTimeMillis();
				// System.out.println(String.format("update step took %d ms", endTime - startTime));
			}
		}
	}
	
	/**
	 * Writes the model to the provided file. This model can then be loaded at a later instance,
	 * saving valuable training time.
	 * 
	 * If the directory and file do not exist we create them.
	 * 
	 * @param directory - the directory containing the model file
	 * @param filename - the filename within that directory where we will write the model file
	 */
	public void writeModelFile(String directory, String filename) throws IOException {
		File outDir = new File(directory);
		if (!outDir.isDirectory()) {
			outDir.mkdir();
		}
		File outFile = new File(directory + filename);
		PrintWriter writer = new PrintWriter(outFile.getPath(), "UTF-8");
		writer.print(mClassNames.length);
		
		for (int i = 0; i < mClassNames.length; i++) {
			writer.println(mClassNames[i]);
			SparseVector weights = mClassifiers[i].getWeights();
			for (int idx : weights.nzindices()) {
				writer.print(String.format("%d:%f ", idx, weights.get(idx)));
			}
			writer.println();
		}
		writer.close();
	}
	
	
	/**
	 * Method to test our classification on training data consisting of x_test and y_test.
	 * Prints out information on the accuracy and precision and recall for each class.
	 * Internal note: int[] res consists of: [True_pos, True_neg, False_pos, False_neg]
	 * 
	 * @param x_test - an array of {@link SparseVector} containing the test instances
	 * @param y_test - an array of {@link BitSet} containing the gold labels for the testing data for
	 * 		each class
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
	
	/**
	 * Returns the classification precision of a given set of predictions
	 */
	private double getPrecision(int[] res) {
		return (double)(res[0]) / (res[0] + res[3]);
	}
	
	/**
	 * Returns the classification recall of a given set of predictions
	 */
	private double getRecall(int[] res) {
		return (double)(res[0]) / (res[0] + res[2]);
	}
	
	/**
	 * Returns the classification accuracy of a given set of predictions
	 */
	private double getAccuracy(int[] res) {
		return (double)(res[0] + res[1]) / getSum(res);
	}
	
	/**
	 * Helper method to return the sum of an int array
	 * 
	 * @param res - the int array we are summing
	 * @return the sum of the values in res
	 */
	private int getSum(int[] res) {
		int sum = 0;
		for (int i = 0; i < res.length; i++) {
			sum += res[i];
		}
		return sum;
	}
	
	/**
	 * Queries the {@link HEXGraph} with a vector of scores from all classifiers being trained in 
	 * parallel. The {@link HEXGraph} returns a new (presumably improved) score that we normalize 
	 * and use in our update step
	 * 
	 * @param scores - the predictions from this round of training
	 * @return a double array of updated predictions using the HEXGraph
	 */
	private double[] getHexData(double[] scores) {
		mHexGraphMethods.setScores(scores);
		
		double[] hexScores = new double[scores.length];
		long start = System.currentTimeMillis();
		Map<String, Double> results = mHexGraphMethods.exactMarginalInference(mJunctionTree, mJunctionTreeStateSpace);
		long end = System.currentTimeMillis();
		// System.out.println(String.format("marginal inference total was %d ms", end - start));
		
		double max = -1;
		for (int i = 0; i < scores.length; i++) {
			hexScores[i] = results.get(mClassNames[i]);
			if (hexScores[i] > max) max = hexScores[i];
		}
		
		// OLD REGULARIZATION
		for (int i = 0; i < hexScores.length; i++) {
			hexScores[i] /= max;
		}
		
		return hexScores;	
	}
	
}
