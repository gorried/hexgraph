package classification;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

import util.NameSpace;
import util.SparseMatrix;


//TODO
/*
 * Incremental testing on the training set (to check if we are over fit)
 * 
 * Future:
 * cross validation for hyperparameters (each classifier independently)
 * */

public class ClassificationRunner {
	private static final String DATA_FILE_FOLDER = "src/data_files/";
	private static final String TRAINING_DATA = DATA_FILE_FOLDER + "train_small.features";
	private static final String TRAINING_LABEL_FILE_DIR = "src/data_files/labels_small/";
	private static final String GRAPH_FILE = "src/graph_files/figer/figer_new.hxg";
	private static final String NAME_SPACE_FILE = "src/data_files/namespace/type.list";
	
	private static final double TEST_SET_SIZE = 0.1;
	private static final int BATCH_SIZE = 50;
	
	// We will just say there are 100 million features. it really doesnt matter because 
	// we only iterate over the nonzero instances anyhow.
	private static final int NUM_FEATURES = 10391078;
		
	private static NameSpace<String> mNameSpace;
	
	public static void main(String[] args) throws IOException {
		setNameSpace(NAME_SPACE_FILE);
		System.out.println("Loaded class names");
		// Load the training data
		File trainingDataFile = new File(TRAINING_DATA);
		int numInstances = countLines(trainingDataFile.getPath());
		
		
		// Load in the training labels for each classifier
		File labelDir = new File(TRAINING_LABEL_FILE_DIR);
		File[] labelFiles = labelDir.listFiles();
		if (labelFiles == null) {
			System.err.println("Need to put in some training labels");
			return;
		}
		
		int numClassifiers = labelFiles.length;
		// make a HexLrTask
		File graphFile = new File(GRAPH_FILE);
		System.out.println("Loading training data");
		SparseMatrix x = loadData(trainingDataFile, numInstances);
		System.out.println("Shuffling training data");
		x.shuffleRows();
		System.out.println("Loading training labels");
		SparseMatrix y = loadClasses(labelFiles, numInstances, numClassifiers);
		System.out.println("Creating task");
		SparseHexLrTask task = new SparseHexLrTask(graphFile, NUM_FEATURES, mNameSpace);
		
		int testingCutoff = (int)Math.floor(x.getRows() * (1 - TEST_SET_SIZE));
//		System.out.println("Running cross validation");
//		task.kFoldCrossValidation(x.getSubMatrix(0, testingCutoff),y.getSubColMatrix(0, testingCutoff), 10);
		System.out.println("Starting training");
		task.train(x.getSubMatrix(0, testingCutoff),y.getSubColMatrix(0, testingCutoff), BATCH_SIZE, 0.1, 0.3);
		System.out.println("Writing model file");
		task.writeModelFile(DATA_FILE_FOLDER, "figer_hex.model");
		// test it
		System.out.println("Testing");
		task.test(x.getSubMatrix(testingCutoff, numInstances),y.getSubColMatrix(testingCutoff, numInstances));
	}
	
	/**
	 * Loads the labels for the training data
	 * 
	 * @param labelFiles - an array containing a file object for a
	 * @param numInstances - the number of training instances
	 * @param numClassifiers - the number of classifiers we are training. in the case that
	 * 	numClassifiers is less than the length of the labelFiles array, we will only look at the
	 * 	first numClassifiers files in that array
	 * @return a {@link BitSet} array containing labels for our training data
	 * @throws IOException if any of the files in labelFiles do not exist
	 */
	private static SparseMatrix loadClasses(File[] labelFiles, int numInstances, int numClassifiers) throws IOException {
		BufferedReader br = null;
		SparseMatrix data = new SparseMatrix(numInstances, numClassifiers);
		for (int c = 0; c < numClassifiers; c++) {
			try {				
				br = new BufferedReader(new FileReader(labelFiles[c].getPath()));
				int lineCount = 0;
				String line = "";
				while ((line = br.readLine()) != null && lineCount < numInstances) {
					if (Integer.parseInt(line) == 1) {
						data.put(c, lineCount, 1.0);
					}
					lineCount++;
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (br != null) {
					br.close();
				}
			}
		}
		
		return data;
	}

	
	
	private static SparseMatrix loadData(File dataFile, int numInstances) throws IOException{
		SparseMatrix data = new SparseMatrix(NUM_FEATURES, numInstances);
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(dataFile.getPath()));
			String line = "";
			int lineCount = 0;
			while ((line = br.readLine()) != null) {
				line.trim();
				String[] splitLine = line.split(" ");
				data.put(lineCount, 0, 1.0);;
				for (String entry : splitLine) {
					if (!entry.equals("")) {							
						String[] splitEntry = entry.split(":");
						data.put(
								lineCount,
								Integer.parseInt(splitEntry[0]),
								Double.parseDouble(splitEntry[1]));
					}
				}
				lineCount++;
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				br.close();
			}
		}
		
		return data;
	}
	
	public static int countLines(String filename) throws IOException {
	    InputStream is = new BufferedInputStream(new FileInputStream(filename));
	    try {
	        byte[] c = new byte[1024];
	        int count = 0;
	        int readChars = 0;
	        boolean empty = true;
	        while ((readChars = is.read(c)) != -1) {
	            empty = false;
	            for (int i = 0; i < readChars; ++i) {
	                if (c[i] == '\n') {
	                    ++count;
	                }
	            }
	        }
	        return (count == 0 && !empty) ? 1 : count;
	    } finally {
	        is.close();
	    }
	}
	
	private static void setNameSpace(String filepath) throws IOException{
		String[] names = new String[countLines(filepath)];
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(filepath));
			String line = "";
			while ((line = br.readLine()) != null) {
				line.trim();
				String[] splitLine = line.split("\\s+");
				names[Integer.parseInt(splitLine[0])] = splitLine[1];
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				br.close();
			}
		}
		mNameSpace = new NameSpace<String>(names);
	}
}
