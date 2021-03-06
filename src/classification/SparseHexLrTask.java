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
import util.SparseMatrix;
import util.SparseVector;

/**
 * A HEXGraph Logistic Regression task, or HexLrTask for short runs parallel classification of
 * Logistic Regression classifiers while utilizing {@link HEXGraph} inference for weighted updates. 
 * 
 * 
 * TODO: Cross validation on small set -> parameters for big set
 * 		For testing with hexgraph: put in the inference on the testing side
 */
public class SparseHexLrTask {
	private JunctionTree<String> mJunctionTree;
	private HEXGraphMethods mHexGraphMethods;
	private SparseLogRegClassifier[] mClassifiers;
	private ThreadedHexRunner mThreadedHexRunner;
	private int mNumFeatures;
	private NameSpace<String> mNameSpace;
	private Map<JunctionTreeNode<String>, Set<Configuration>> mJunctionTreeStateSpace;
	
	private double eta = 0.0001;
	private double lambda = 0.1;
	
	public static final int CLASSIFICATION_TRUE = 1;
	public static final int CLASSIFICATION_FALSE = 0;
	
	private static final boolean USING_HEX = false;
	private static final boolean USING_THREADED = true;
	
	private final int NUM_ITERATIONS = 50;
	
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
	public SparseHexLrTask(File graphFile, int numFeatures, NameSpace<String> nameSpace) throws IOException {
		mNameSpace = nameSpace;
		HEXGraphFactory factory = new HEXGraphFactory(nameSpace);
		factory.buildHEXGraph(graphFile.getPath());
		mHexGraphMethods = new HEXGraphMethods(factory, graphFile.getPath(), nameSpace);
		
		long startTime = System.currentTimeMillis();
		mJunctionTree = mHexGraphMethods.buildJunctionTree();
		long endTime = System.currentTimeMillis();
		System.out.println(String.format("Building junction tree took %d ms", endTime - startTime));
		mJunctionTreeStateSpace = mHexGraphMethods.getJunctionTreeStateSpaces(mJunctionTree);
		
		mNumFeatures = numFeatures;
		mThreadedHexRunner = new ThreadedHexRunner(mHexGraphMethods, mJunctionTree, mNameSpace, mNameSpace.size());
		mClassifiers = new SparseLogRegClassifier[nameSpace.size()];
		
	}
	
	/**
	 * Constructs a new {@link HexLrTask} from a model file.
	 * 
	 * @param graphFile - String filepath of the graph file
	 * @param modelFile - String filepath of the model file
	 * @throws IOException if an invalid filepath is passed in for the model file
	 */
	public SparseHexLrTask(String graphFile, String modelFile) throws IOException {
		HEXGraphFactory factory = new HEXGraphFactory(mNameSpace);
		factory.buildHEXGraph(graphFile);
		mHexGraphMethods = new HEXGraphMethods(factory, graphFile, mNameSpace);
		mJunctionTree = mHexGraphMethods.buildJunctionTree();
		
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(modelFile));
			int numClasses = Integer.parseInt(br.readLine().trim());
			mNumFeatures = Integer.parseInt(br.readLine().trim());
			mClassifiers = new SparseLogRegClassifier[numClasses]; 
			String[] names = new String[mClassifiers.length];
			
			for (int i = 0; i < numClasses; i++) {
				names[i] = br.readLine();
				double[] weights = new double[mNumFeatures];
				for (String entry : br.readLine().trim().split(" ")) {
					String[] splitEntry = entry.split(":");
					weights[Integer.parseInt(splitEntry[0])] = Double.parseDouble(splitEntry[1]);
				}
				mClassifiers[i] = new SparseLogRegClassifier(weights, 0.1, 0.3);
			}
			mNameSpace = new NameSpace<String>(names);
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				br.close();
			}
		}
	}
	
	/**
	 * Runs a synchronized training procedure for all classifiers in parallel. This method takes
	 * in the entire training set, and then processes it over a certain number of iterations in
	 * micro batches
	 * 
	 * TODO: Calculate log loss for each classifier at the end of the iteration, and if it is less
	 * 	than a value epsilon, return
	 * 
	 * @param x_train - {@link SparseMatrix} containing the training data
	 * @param y_train - {@link SparseMatrix} where each row is the training labels for the given
	 * 	class. Will have row length numClassifiers
	 * @param numIterations - the number of iterations over our data we will use to train
	 */
	public void train(SparseMatrix x_train, SparseMatrix y_train, int batchSize, double eta, double lambda) {
		// for each classifier, initialize the classifier to the correct number of weights
		for (int i = 0; i < mClassifiers.length; i++) {
			mClassifiers[i] = new SparseLogRegClassifier(mNumFeatures, this.eta, this.lambda);
		}
		// assert there are the same number of training examples and classes
		if (x_train.getCols() != mNumFeatures) {
			throw new IllegalStateException(
					"x_train has wrong number of columns: " + x_train.getCols());
		}
		if (y_train.getCols() != x_train.getRows()) {
			throw new IllegalStateException(
					"y_train has wrong number of columns: " + y_train.getCols());
		}
		if (y_train.getRows() != mClassifiers.length) {
			throw new IllegalStateException(
					"y_train has wrong number of rows: " + y_train.getRows());
		}
		System.out.println("Starting microbatching " + (USING_THREADED ? "" : "not ") + "using threads");
		// run the micro-batching
		for (int x = 0; x < NUM_ITERATIONS; x++) {
			// chop the training and testing set into batches and run them in succession
			long startTime = System.currentTimeMillis();
			int numInstances = x_train.getRows();
			for (int i = 0; i < numInstances; i += batchSize) {
				// long batchStartTime = System.currentTimeMillis();
				if (numInstances - i > batchSize) {
					microbatch(x_train.getSubMatrix(i, i + batchSize), y_train, i, i + batchSize);
				} else {
					microbatch(x_train.getSubMatrix(i, numInstances), y_train, i, numInstances);
				}
				// long batchEndTime = System.currentTimeMillis();
				// System.out.println(String.format("Iteration %d batch %d: %d", x, i, batchEndTime - batchStartTime));
			}
			long endTime = System.currentTimeMillis();
			System.out.println(String.format("Single iteration of microbatch took %d ms", endTime - startTime));
			for (int i = 0; i < mClassifiers.length; i++) {
				SparseLogRegClassifier c = mClassifiers[i];
				System.out.println(String.format("Log loss for %s: %f %f", mNameSpace.get(i), c.getLogLoss(c.train(x_train), y_train.getRow(i)), c.weightL2Norm()));
			}
		}
	}
	
	private void microbatch(SparseMatrix x_batch, SparseMatrix y_train, int lowData, int hiData) {
		double[][] scores = new double[mClassifiers.length][];
		for (int c = 0; c < mClassifiers.length; c++) {
			scores[c] = mClassifiers[c].train(x_batch);
		}
		// get the hex scores
		double[][] updatedScores = scores;
		if (USING_HEX) {
			if (USING_THREADED) {	
				updatedScores = mThreadedHexRunner.process(scores);
			} else {
				for (int i = 0; i < x_batch.getRows(); i++) {
					double[] instanceScore = new double[mClassifiers.length];
					for (int c = 0; c < mClassifiers.length; c++) {
						instanceScore[c] = scores[c][i];
					}
					instanceScore = getHexData(instanceScore);
					for (int c = 0; c < mClassifiers.length; c++) {
						updatedScores[c][i] = instanceScore[c];
					}
				}
			}
		}
		// Run the update step
		for (int c = 0; c < mClassifiers.length; c++) {
			mClassifiers[c].update(x_batch, updatedScores[c], y_train.subVector(c, lowData, hiData));
		}
	}
	
	public void receiveThreadedUpdateData(double[][] scores, SparseMatrix x_batch, SparseMatrix y_train, int lowData, int hiData) {
		for (int c = 0; c < mClassifiers.length; c++) {
			mClassifiers[c].update(x_batch, scores[c], y_train.subVector(c, lowData, hiData));
		}
	}
	
	/**
	 * Runs independent cross validation to tune hyperparameters of each classifier
	 * 
	 * @param x_train
	 * @param y_train
	 * @param numIterations
	 */
	public void kFoldCrossValidation(SparseMatrix x_train, SparseMatrix y_train, int numIterations) {
		double bestLambda = -1.0;
		double bestEta = -1.0;
		double bestAccuracy = 0.0;
		
		int k = 20;
		double[] etas = {0.001, 0.01, 0.1, 1};
		double[] lambdas = {0.1, 0.2, 0.3, 0.4, 0.5};
		// split the test and training data into k parts
		
		// iterate over the parameters (learning rate and reg param)
		for (double eta : etas) {
			for (double lambda : lambdas) {
				if (crossValidationInstance(x_train, y_train, k, eta, lambda, numIterations) > bestAccuracy) {
					bestEta = eta;
					bestLambda = lambda;
				}
			}
		}
		System.out.println("Best lambda is " + bestLambda);
		System.out.println("Best eta is " + bestEta);
		
		this.eta = bestEta;
		this.lambda = bestLambda;
	}
	
	private double crossValidationInstance(
			SparseMatrix x_train,
			SparseMatrix y_train,
			int k,
			double eta,
			double lambda,
			int numIterations) {
		int batchSize = 10;
		// initialize mClassifers to the proper settings
		for (int i = 0; i < mClassifiers.length; i++) {
			mClassifiers[i] = new SparseLogRegClassifier(mNumFeatures, eta, lambda);
		}
		double[] accuracies = new double[k];
		
		// iterate over the parts, selecting one as the test set
		for (int i = 0; i < k; i++) {
			long iterationStartTime = System.currentTimeMillis();
			// i is the bucket of the held out set
			System.out.println(x_train.getRows());
			int testRegionStart = (int) (1.0 * i / k * x_train.getRows());
			int testRegionEnd = (int) (1.0 * (i + 1) / k * x_train.getRows());
			System.out.println("Test region: " + testRegionStart + " - " + testRegionEnd);
			for (int j = 0; j < k; j++) {
				if (j != i) {
					// train on j
					int trainRegionStart = (int) (1.0 * j / k * x_train.getRows());
					int trainRegionEnd = (int) (1.0 * (j + 1) / k * x_train.getRows());
					System.out.println("Train region: " + trainRegionStart + " - " + trainRegionEnd);
					// segment into micro-batches of the proper size
					for (int n = trainRegionStart; n < trainRegionEnd; n += batchSize) {
//						long batchStartTime = System.currentTimeMillis();
						if (trainRegionEnd - n > batchSize) {
							microbatch(x_train.getSubMatrix(n, n + batchSize), y_train, n, n + batchSize);
						} else {
							microbatch(x_train.getSubMatrix(n, trainRegionEnd), y_train, n, trainRegionEnd);
						}
//						long batchEndTime = System.currentTimeMillis();
//						System.out.println(String.format("Batch took %d ms", batchEndTime - batchStartTime));
					}
				}
			}
			// make a new SparseMatrix with the labels for each classifier in row order
			SparseMatrix y_test = y_train.getSubColMatrix(testRegionStart, testRegionEnd);
			
			// test on bucket i and record the accuracy in the array 
			accuracies[i] = test(x_train.getSubMatrix(testRegionStart, testRegionEnd), y_test);
			
			long iterationEndTime = System.currentTimeMillis();
			System.out.println(String.format("Iteration took %d ms", iterationEndTime - iterationStartTime));
		}
		
		return getSum(accuracies) / accuracies.length;
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
		final PrintWriter writer = new PrintWriter(outFile.getPath(), "UTF-8");
		writer.print(mNameSpace.size());
		
		for (int i = 0; i < mNameSpace.size(); i++) {
			writer.println(mNameSpace.get(i));
			double[] weights = mClassifiers[i].getWeights();
			for (int j = 0; j < weights.length; j++) {
				if (weights[j] != 0.0) {
					writer.print(String.format("%d:%f ", j, weights[j]));
				}
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
	public double test(SparseMatrix x_test, SparseMatrix y_test) {
		double[] accuracies = new double[mClassifiers.length];
		double[] precisions = new double[mClassifiers.length];
		double[] recalls = new double[mClassifiers.length];
		if (USING_HEX) {
			int[][] results = new int[mClassifiers.length][4];
			double[][] scores = new double[mClassifiers.length][];
			for (int c = 0; c < mClassifiers.length; c++) {
				scores[c] = mClassifiers[c].train(x_test);
			}
			// get the score for every instance
			for (int i = 0; i < x_test.getRows(); i++) {
				double[] instanceScore = new double[mClassifiers.length];
				for (int c = 0; c < mClassifiers.length; c++) {
					instanceScore[c] = scores[c][i];
				}
				instanceScore = getHexData(instanceScore);
				// System.out.println(Arrays.toString(instanceScore));
				for (int c = 0; c < mClassifiers.length; c++) {
					if (instanceScore[c] > 0.9) {
						if (y_test.get(c, i) == 1.0) {
							results[c][0]++;
						} else {
							results[c][2]++;
						}
					} else {
						if (y_test.get(c, i) == 1.0) {
							results[c][3]++;
						} else {
							results[c][1]++;
						}
					}
				}
			}
			
			for (int c = 0; c < mClassifiers.length; c++) {
				int[] res = results[c];
				accuracies[c] = getAccuracy(res);
				precisions[c] = getPrecision(res);
				recalls[c] = getRecall(res);
				System.out.println("Results for " + mNameSpace.get(c) + " using HEX");
				System.out.println(Arrays.toString(res));
				System.out.println("Accuracy: " + getAccuracy(res));
				System.out.println("Precision: " + getPrecision(res));
				System.out.println("Recall: " + getRecall(res));
				System.out.println();
			}
			
			
		} else {
			// iterate over the classifiers
			for (int j = 0; j < mClassifiers.length; j++) {
				int[] res = new int[4];
				// for each classifier iterate over the data set	
					for (int i = 0; i < x_test.getRows(); i++) {
						if (mClassifiers[j].getClassification(x_test.getRow(i))) {
							if (y_test.get(j, i) == 1.0) {
								res[0]++;
							} else {
								res[2]++;
							}
						} else {
							if (y_test.get(j, i) == 1.0) {
								res[3]++;
							} else {
								res[1]++;
							}
						}
					}
				accuracies[j] = getAccuracy(res);
				precisions[j] = getPrecision(res);
				recalls[j] = getRecall(res);
				System.out.println("Results for " + mNameSpace.get(j));
				System.out.println(Arrays.toString(res));
				System.out.println("Accuracy: " + getAccuracy(res));
				System.out.println("Precision: " + getPrecision(res));
				System.out.println("Recall: " + getRecall(res));
				System.out.println();
			}
		}
		System.out.println("Overall Performance: " + getSum(accuracies) / accuracies.length);
		return getSum(accuracies) / accuracies.length;
	}
	
	/**
	 * Returns the classification precision of a given set of predictions
	 */
	private double getPrecision(int[] res) {
		if (res[0] + res[3] == 0) return 0.0;
		else return (double)(res[0]) / (res[0] + res[3]);
	}
	
	/**
	 * Returns the classification recall of a given set of predictions
	 */
	private double getRecall(int[] res) {
		if (res[0] + res[2] == 0) return 0.0;
		else return (double)(res[0]) / (res[0] + res[2]);
	}
	
	/**
	 * Returns the classification accuracy of a given set of predictions
	 */
	private double getAccuracy(int[] res) {
		return (double)(res[0] + res[1]) / getSum(res);
	}
	
	/**
	 * Helper methods to return the sum of an array
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
	
	private double getSum(double[] res) {
		double sum = 0;
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
		double[] hexScores = new double[scores.length];
		Map<String, Double> results = mHexGraphMethods.exactMarginalInference(
				mJunctionTree, mJunctionTreeStateSpace, scores);
		
		double max = -1;
		for (int i = 0; i < scores.length; i++) {
			hexScores[i] = results.get(mNameSpace.get(i));
			if (hexScores[i] > max) max = hexScores[i];
		}
		
		// OLD REGULARIZATION
		for (int i = 0; i < hexScores.length; i++) {
			hexScores[i] /= max;
		}
		
		return hexScores;	
	}
	
}

