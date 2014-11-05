import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * @author dgorrie
 *
 * Factory class that returns a new HEX graph object based on the .hexg file passed in
 */
public class HEXGraphFactory {
	private BufferedReader mBufferedReader;
	
	private final String ENUMERATION_HEADER = "#E#";
	private final String EXCLUSION_HEADER = "#X#";
	private final String HIERARCHY_HEADER = "#H#";
	private final String INPUT_ERROR = "Invalid input formatting in %s for input '%s'";
	private final String GRAPH_ERROR = "Invalid tokens %s and %s in %s";
	private final String HIERARCHY = "hierarchy section";
	private final String EXCLUSION = "exclusion section";
	
	
	
	public HEXGraphFactory () {}
	
	public HEXGraph<String> getHEXGraph() {
		return new HEXGraph<String>();
	}
	
	public HEXGraph<String> getHexGraph(String filepath) throws IOException {
		HEXGraph<String> graph = new HEXGraph<String>();
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
					graph.addNode(line);
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
								if (!graph.addExclusion(terms[i], terms[j])) {
									throw new IllegalStateException(String.format(
											GRAPH_ERROR,
											terms[i],
											terms[j],
											EXCLUSION));
								} else {
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
							if (!graph.addHierarchy(first, terms[i])) {
								throw new IllegalStateException(String.format(
										GRAPH_ERROR,
										first,
										terms[i],
										HIERARCHY));
							} else {
								System.out.println(String.format("Added hierarchy between %s and %s", first, terms[i]));
							}
						}
					}
				}
				line = mBufferedReader.readLine();
			}
			// check to make sure the graph is valid
			graph.checkInvariant();
		} catch (Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		} finally {
			mBufferedReader.close();
		}
		
		return graph;
	}
	
}
