package classification;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.BitSet;

import util.SparseVector;


//TODO
/*
 * Sanity check on the classifier (10 point dataset)
 * Incremental testing on the training set (to check if we are over fit)
 * Put in the right names in the namespace
 * Put in random sampling for the dataset
 * do precision and recall on a class by class basis
 * 
 * Future:
 * cross validation for hyperparameters (each classifier independently)
 * */

public class ClassificationRunner {
	private static final String TRAINING_DATA = "src/data_files/train.features";
	private static final String TRAINING_LABEL_FILE_DIR = "src/data_files/labels/";
	private static final String GRAPH_FILE = "src/graph_files/figer/figer_new.hxg";
	
	private static final double TEST_SET_SIZE = 0.1;
	
	// We will just say there are 100 million features. it really doesnt matter because 
	// we only iterate over the nonzero instances anyhow.
	private static final int NUM_FEATURES = 10391078;
	
	private static final boolean TEST_MODE = true;
	
	public static void main(String[] args) throws IOException {
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
		// Array that will hold the string names
		String[] classNames = new String[numClassifiers];
		for (int i = 0; i < numClassifiers; i++) {
			String currFileName = labelFiles[i].getName();
			classNames[i] = currFileName.substring(0, currFileName.lastIndexOf('.'));
		}
	
		// make a HexLrTask
		File graphFile = new File(GRAPH_FILE);
		SparseVector[] x = loadData(trainingDataFile, numInstances);
		BitSet[] y = loadClasses(labelFiles, numInstances, numClassifiers);
		HexLrTask task = new HexLrTask(graphFile, numInstances, classNames, numInstances);
		
		int testingCutoff = (int)Math.floor(x.length * (1 - TEST_SET_SIZE));
		
		// run it baby
		task.train(Arrays.copyOfRange(x, 0, testingCutoff), Arrays.copyOfRange(y, 0, testingCutoff), 100);
		
		// test it
		task.test(Arrays.copyOfRange(x, testingCutoff + 1, x.length), Arrays.copyOfRange(y, testingCutoff + 1, y.length));
	}
	
	private static BitSet[] loadClasses(File[] labelFiles, int numInstances, int numClassifiers) throws IOException {
		BufferedReader br = null;
		BitSet[] data = new BitSet[numClassifiers];
		for (int i = 0; i < numClassifiers; i++) {
			data[i] = new BitSet(numInstances);
			
			try {				
				br = new BufferedReader(new FileReader(labelFiles[i].getPath()));
				int lineCount = 0;
				String line = "";
				while ((line = br.readLine()) != null) {
					if (Integer.parseInt(line) == 1) {
						data[i].set(lineCount);
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
	
	
	private static SparseVector[] loadData(File dataFile, int numInstances) throws IOException{
		SparseVector[] data = new SparseVector[numInstances];
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(dataFile.getPath()));
			String line = "";
			int lineCount = 0;
			while ((line = br.readLine()) != null) {
				line.trim();
				String[] splitLine = line.split(" ");
				data[lineCount] = new SparseVector(NUM_FEATURES);
				data[lineCount].put(0, 1.0);
				for (String entry : splitLine) {
					if (!entry.equals("")) {							
						String[] splitEntry = entry.split(":");
						data[lineCount].put(Integer.parseInt(splitEntry[0]), Double.parseDouble(splitEntry[1]));
					}
				}
				if (lineCount % 1000 == 0) System.out.println(lineCount);
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
}