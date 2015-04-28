package hexgraph;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;


public class ResultRunner {
	public static String rawDirectory = "src/score_files/figer-hex/raw";
	public static String scoreDirectory = "src/score_files/figer-hex/scores";
	public static String outputDirectory = "src/output_files";
	public static String graphDirectory = "src/graph_files/figer";
	public static boolean MARGINAL = true;
	
	public static void main(String[] args) throws IOException, IllegalStateException {
		HEXGraphFactory factory = new HEXGraphFactory();
		
		// Check if the output directory exists, and if not, make it
		File outputDir = new File(outputDirectory);
		if (!outputDir.exists()) {
			outputDir.mkdirs();
		}
		
		File graphDir = new File(graphDirectory);
		File[] graphDirectoryListing = graphDir.listFiles();
		if (graphDirectoryListing != null) {
			for (File graphFile : graphDirectoryListing) {
				String filepath = graphFile.getPath();
				
				File outputSubDir = new File(outputDir.getPath() + "/" + graphFile.getName().split("\\.")[0]);
				if (!outputSubDir.exists()) {
					outputSubDir.mkdirs();
				}
				
				factory.buildHEXGraph(filepath);
				HEXGraphMethods methods = new HEXGraphMethods(factory, filepath);
				System.out.println("Built graph from" + filepath);
				
				JunctionTree<String> tree = methods.buildJunctionTree();
				System.out.println("Done building tree");
				
				// iterate through the score directory and 
				File scoreDir = new File(scoreDirectory);
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
					throw new IOException(scoreDirectory + " is not a directory");
				}
			}
		} else {
			throw new IOException(graphDirectory + " is not a directory");
		}
		
	}
	
	public static void runMarginalInference(HEXGraphMethods methods, File scoreFile, 
			File outputSubDir, JunctionTree<String> tree) throws IOException, IllegalStateException {
		Map<String, Double> resultMap = methods.exactMarginalInference(tree);
		
		File outputFile = new File(outputSubDir.getPath() + "/marginal_" + scoreFile.getName());
		PrintWriter writer = new PrintWriter(outputFile.getPath(), "UTF-8");
		
		Scanner sc = new Scanner(new File(rawDirectory + "/" + scoreFile.getName()));
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
	
	public static void runJointInference(HEXGraphMethods methods, File scoreFile, 
			File outputSubDir, JunctionTree<String> tree) throws IOException, IllegalStateException {
		Map<Configuration<String>, Double> scoreMap = methods.exactInference(tree);
		
		File outputFile = new File(outputSubDir.getPath() + "/joint_" + scoreFile.getName());
		PrintWriter writer = new PrintWriter(outputFile.getPath(), "UTF-8");
		
		Scanner sc = new Scanner(new File(rawDirectory + "/" + scoreFile.getName()));
		try {
			writer.println(sc.useDelimiter("\\Z").next() + "\n");
		} catch (NoSuchElementException e) {
			writer.println("Unable to copy over sentence \n");
		} finally {							
			sc.close();
		}
		
		for (Configuration<String> key : scoreMap.keySet()) {
			boolean zeroFlag = true;
			for (String s : key.getKeySet()) {
				if (key.get(s) == Configuration.CONFIG_TRUE) {
					zeroFlag = false;
					writer.println(String.format("%s: 1", s));
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
}
