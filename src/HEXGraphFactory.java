import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author dgorrie
 *
 * Factory class that returns a new HEX graph object based on the .hexg file passed in
 */
public class HEXGraphFactory {
	
	private final String ENUMERATION_HEADER = "#E#";
	private final String EXCLUSION_HEADER = "#X#";
	private final String HIERARCHY_HEADER = "#H#";
	private final String INPUT_ERROR = "Invalid input formatting in %s for input '%s'";
	private final String GRAPH_ERROR = "Invalid tokens %s and %s in %s";
	private final String HIERARCHY = "hierarchy section";
	private final String EXCLUSION = "exclusion section";
	
	private BufferedReader mBufferedReader;
	
	private Map<String, HEXGraph<String>> mLiteralGraphs;
	private Map<String, HEXGraph<String>> mSparseGraphs;
	private Map<String, HEXGraph<String>> mDenseGraphs;
	
	
	
	public HEXGraphFactory() {
		mLiteralGraphs = new HashMap<String, HEXGraph<String>>();
		mSparseGraphs = new HashMap<String, HEXGraph<String>>();
		mDenseGraphs = new HashMap<String, HEXGraph<String>>();
	}
	
	public HEXGraphFactory(String filepath) throws IOException {
		this();
		buildHEXGraph(filepath);
	}
	
	public HEXGraph<String> getEmptyHEXGraph() {
		return new HEXGraph<String>();
	}
	
	public HEXGraph<String> getSparseGraph(String key) {
		return mSparseGraphs.get(key);
	}
	
	public HEXGraph<String> getDenseGraph(String key) {
		return mDenseGraphs.get(key);
	}
	
	public HEXGraph<String> getLiteralGraph(String key) {
		return mLiteralGraphs.get(key);
	}
	
	public void buildHEXGraph(String filepath) throws IOException {
		HEXGraph<String> literalGraph = new HEXGraph<String>();
		HEXGraph<String> sparseGraph = new HEXGraph<String>();
		HEXGraph<String> denseGraph = new HEXGraph<String>();
		mBufferedReader = new BufferedReader(new FileReader(filepath));
		String line = "";
		try {
			// Read to the first line of enumeration
			while (!line.equals(ENUMERATION_HEADER)) {
				line = mBufferedReader.readLine();
			}
			line = mBufferedReader.readLine();
			
			// Add all the classes to the nodes in a graph
			while (!line.equals(EXCLUSION_HEADER)) {
				if (!line.equals("")) {
					literalGraph.addNode(line);
					sparseGraph.addNode(line);
					denseGraph.addNode(line);
					System.out.println(String.format("Added node %s", line));
				}
				line = mBufferedReader.readLine();
			}
			line = mBufferedReader.readLine();
			
			// Add the exclusions to the graph
			while (!line.equals(HIERARCHY_HEADER)) {
				if (!line.equals("")) {
					String[] terms = line.split("\\s+");
					if (terms.length == 1) {
						throw new IOException(String.format(INPUT_ERROR, EXCLUSION, line));
					} else {
						for (int i = 0; i < terms.length; i++) {
							for (int j = i + 1; j < terms.length; j++) {
								if (!literalGraph.addExclusion(terms[i], terms[j])) {
									throw new IllegalStateException(String.format(
											GRAPH_ERROR,
											terms[i],
											terms[j],
											EXCLUSION));
								} else {
									sparseGraph.addExclusion(terms[i], terms[j]);
									denseGraph.addExclusion(terms[i], terms[j]);
									System.out.println(String.format("Added exlusion between %s and %s", terms[i], terms[j]));
								}
							}
						}
					}
				}
				line = mBufferedReader.readLine();
			}
			line = mBufferedReader.readLine();
			
			// Add the hierarchy relations to the graph
			while (line != null) {
				if (!line.trim().equals("")) {
					String[] terms = line.split("\\s+");
					if (terms.length == 1) {
						throw new IOException(String.format(INPUT_ERROR, HIERARCHY, line));
					} else {
						if (terms[0].length() < 1) {
							throw new IOException(String.format(INPUT_ERROR, HIERARCHY, line));
						}
						String first = terms[0].substring(0, terms[0].length()-1);
						for (int i = 1; i < terms.length; i++) {
							if (!literalGraph.addHierarchy(first, terms[i])) {
								throw new IllegalStateException(String.format(
										GRAPH_ERROR,
										first,
										terms[i],
										HIERARCHY));
							} else {
								sparseGraph.addHierarchy(first, terms[i]);
								denseGraph.addHierarchy(first, terms[i]);
								System.out.println(String.format("Added hierarchy between %s and %s", first, terms[i]));
							}
						}
					}
				}
				line = mBufferedReader.readLine();
			}
			// check to make sure the graph is valid
			literalGraph.checkInvariant();
			
			// Sparsify the sparse graph
			sparseGraph.sparsify();
			
			// Densify the dense graph
			denseGraph.densify();
			
			// Add the graphs to their respective maps
			mLiteralGraphs.put(filepath, literalGraph);
			mSparseGraphs.put(filepath, sparseGraph);
			mDenseGraphs.put(filepath, denseGraph);
			
		} catch (Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		} finally {
			mBufferedReader.close();
		}
	}
	
}
