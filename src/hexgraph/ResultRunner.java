package hexgraph;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.BitSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;

import util.NameSpace;


public class ResultRunner {
	private static String mRawDirectory = "src/score_files/figer-hex/raw";
	private static String mScoreDirectory = "src/score_files/figer-hex/scores";
	private static String mOutputDirectory = "src/output_files";
	private static String mGraphDirectory = "src/graph_files/figer";
	private static String mNameSpaceFile = "src/data_files/namespace/type.list";
	private static boolean MARGINAL = true;
	
	private static NameSpace<String> mNameSpace;
	
	public static void main(String[] args) throws IOException, IllegalStateException {
		setNameSpace(mNameSpaceFile);
		HEXGraphFactory factory = new HEXGraphFactory(mNameSpace);
		
		// Check if the output directory exists, and if not, make it
		File outputDir = new File(mOutputDirectory);
		if (!outputDir.exists()) {
			outputDir.mkdirs();
		}
		
		File graphDir = new File(mGraphDirectory);
		File[] graphDirectoryListing = graphDir.listFiles();
		if (graphDirectoryListing != null) {
			for (File graphFile : graphDirectoryListing) {
				String filepath = graphFile.getPath();
				
				File outputSubDir = new File(outputDir.getPath() + "/" + graphFile.getName().split("\\.")[0]);
				if (!outputSubDir.exists()) {
					outputSubDir.mkdirs();
				}
				
				factory.buildHEXGraph(filepath);
				HEXGraphMethods methods = new HEXGraphMethods(factory, filepath, mNameSpace);
				System.out.println("Built graph from" + filepath);
				
				JunctionTree<String> tree = methods.buildJunctionTree();
				System.out.println("Done building tree");
				
				// iterate through the score directory and 
				File scoreDir = new File(mScoreDirectory);
				File[] scoreDirectoryListing = scoreDir.listFiles();
				if (scoreDirectoryListing != null) {
					for (File scoreFile : scoreDirectoryListing) {
						Map<String, Double> scores = factory.getScores(scoreFile.getPath());
						methods.setScores(scores);
						System.out.println(String.format("Now exacting inference on %s", scoreFile.getName()));
						tree.printTreeStats();
						if (MARGINAL) {
							runMarginalInference(methods, scoreFile, outputSubDir, tree);
						} else {
							runJointInference(methods, scoreFile, outputSubDir, tree);
						}
					}
				} else {
					throw new IOException(mScoreDirectory + " is not a directory");
				}
			}
		} else {
			throw new IOException(mGraphDirectory + " is not a directory");
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
	
	private static void runMarginalInference(HEXGraphMethods methods, File scoreFile, 
			File outputSubDir, JunctionTree<String> tree) throws IOException, IllegalStateException {
		Map<String, Double> resultMap = methods.exactMarginalInference(tree);
		
		File outputFile = new File(outputSubDir.getPath() + "/new_marginal_" + scoreFile.getName());
		PrintWriter writer = new PrintWriter(outputFile.getPath(), "UTF-8");
		
		Scanner sc = new Scanner(new File(mRawDirectory + "/" + scoreFile.getName()));
		try {
			writer.println(sc.useDelimiter("\\Z").next() + "\n");
		} catch (NoSuchElementException e) {
			writer.println("Unable to copy over sentence \n");
		} finally {							
			sc.close();
		}
		for (String s : resultMap.keySet()) {
			// System.out.println(s + ": " + resultMap.get(s));
			writer.println(String.format("%s: %f", s, resultMap.get(s)));
		}
		writer.close();
	}
	
	private static void runJointInference(HEXGraphMethods methods, File scoreFile, 
			File outputSubDir, JunctionTree<String> tree) throws IOException, IllegalStateException {
		Map<Configuration, Double> scoreMap = methods.exactInference(tree);
		
		File outputFile = new File(outputSubDir.getPath() + "/new_joint_" + scoreFile.getName());
		PrintWriter writer = new PrintWriter(outputFile.getPath(), "UTF-8");
		
		Scanner sc = new Scanner(new File(mRawDirectory + "/" + scoreFile.getName()));
		try {
			writer.println(sc.useDelimiter("\\Z").next() + "\n");
		} catch (NoSuchElementException e) {
			writer.println("Unable to copy over sentence \n");
		} finally {							
			sc.close();
		}
		
		for (Configuration key : scoreMap.keySet()) {
			boolean zeroFlag = true;
			BitSet setting = key.getBitwiseConfig();
			for (int i = 0; i < setting.length(); i++) {
				if (setting.get(i)) {
					zeroFlag = false;
					writer.println(String.format("%s: 1", mNameSpace.get(i)));
				}
			}
			if (zeroFlag) {
				writer.println("No positive assignments");
			}
			writer.println();
			writer.println(scoreMap.get(key));
			writer.println();
			writer.println("----------");
			writer.println();
			
		}
		writer.close();
	}
	
	/**
	 * Counts and returns the number of lines in a given file
	 */
	private static int countLines(String filename) throws IOException {
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
