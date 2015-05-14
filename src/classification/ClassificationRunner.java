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
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import util.NameSpace;
import util.SparseVector;


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
	private static final String TRAINING_LABEL_FILE_DIR = "src/data_files/labels/";
	private static final String GRAPH_FILE = "src/graph_files/figer/figer_new.hxg";
	private static final String NAME_SPACE_FILE = "src/data_files/namespace/type.list";
	
	private static final double TEST_SET_SIZE = 0.1;
	
	// We will just say there are 100 million features. it really doesnt matter because 
	// we only iterate over the nonzero instances anyhow.
	private static final int NUM_FEATURES = 10391078;
		
	private static NameSpace<String> mNameSpace;
	
	public static void main(String[] args) throws IOException {
		setNameSpace(NAME_SPACE_FILE);
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
			classNames[i] = currFileName.substring(currFileName.indexOf('.') + 1, currFileName.lastIndexOf('.'));
		}
		
		Map<String, String> nameMapping = loadVerboseClassNames(NAME_SPACE_FILE);
		for (int i = 0; i < numClassifiers; i++) {
			classNames[i] = nameMapping.get(classNames[i]);
		}
		
		System.out.println("Loaded class names");
			
		// make a HexLrTask
		File graphFile = new File(GRAPH_FILE);
		System.out.println("Loading training data");
		SparseVector[] x = loadData(trainingDataFile, numInstances);
		System.out.println("Shuffling training data");
		shuffleArray(x);
		System.out.println("Loading training labels");
		BitSet[] y = loadClasses(labelFiles, numInstances, numClassifiers);
		HexLrTask task = new HexLrTask(graphFile, classNames, NUM_FEATURES, mNameSpace);
		
		int testingCutoff = (int)Math.floor(x.length * (1 - TEST_SET_SIZE));
		
		System.out.println("Starting training");
		// run it baby
		task.train(Arrays.copyOfRange(x, 0, testingCutoff), Arrays.copyOfRange(y, 0, testingCutoff), 15);
		System.out.println("Writing model file");
		task.writeModelFile(DATA_FILE_FOLDER, "figer_hex.model");
		// test it
		System.out.println("Testing");
		task.test(Arrays.copyOfRange(x, testingCutoff + 1, x.length), Arrays.copyOfRange(y, testingCutoff + 1, y.length));
	}
	
	private static BitSet[] loadClasses(File[] labelFiles, int numInstances, int numClassifiers) throws IOException {
		BufferedReader br = null;
		BitSet[] data = new BitSet[numInstances];
		for (int i = 0; i < numInstances; i++) {
			data[i] = new BitSet(numClassifiers);
		}
		for (int c = 0; c < numClassifiers; c++) {
			try {				
				br = new BufferedReader(new FileReader(labelFiles[c].getPath()));
				int lineCount = 0;
				String line = "";
				while ((line = br.readLine()) != null && lineCount < numInstances) {
					if (Integer.parseInt(line) == 1) {
						data[lineCount].set(c);
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
	
	private static Map<String, String> loadVerboseClassNames(String filepath) throws IOException{
		Map<String, String> mapping = new HashMap<String, String>();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(filepath));
			String line = "";
			while ((line = br.readLine()) != null) {
				line.trim();
				String[] splitLine = line.split("\\s+");
				mapping.put(splitLine[0], splitLine[1]);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				br.close();
			}
		}
		return mapping;
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
	
	private static void shuffleArray(SparseVector[] array) {
	    int index;
	    SparseVector temp;
	    Random random = new Random();
	    for (int i = array.length - 1; i > 0; i--) {
	        index = random.nextInt(i + 1);
	        temp = array[index];
	        array[index] = array[i];
	        array[i] = temp;
	    }
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
