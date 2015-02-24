import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class ResultRunner {
	
	public static void main(String[] args) throws IOException, IllegalStateException{
		HEXGraphFactory factory = new HEXGraphFactory();
		String filepath = "src/graph_files/figer.hxg";
		factory.buildHEXGraph(filepath);
		
		HEXGraphMethods methods = new HEXGraphMethods(factory, filepath);
		
		Map<String, Double> scores = factory.getScores("src/score_files/predictions2.txt");
		System.out.println("Done loading scores \n");
		
		JunctionTree<String> tree = methods.buildJunctionTree();
		System.out.println("Done building tree \n");
		methods.setScores(scores);
		System.out.println("Now exacting inference \n");
		methods.exactInference(tree);
		
	}
	
	public static void printGraph(HEXGraph<String> graph, String name) {
		System.out.println(name + "\n");
		for (String s : graph.getNodeList()) {
			System.out.println(s);
			System.out.println(String.format("Descendants: %s", graph.getDescendants(s).toString()));
			System.out.println(String.format("Exclusion: %s", graph.getExcluded(s).toString()));
		}
		System.out.println("\n");
	}
	
	
	public static void printTriangulated(HEXGraph<String> graph, String name) {
		System.out.println(name + "\n");
		for (String s : graph.getNodeList()) {
			System.out.println(s);
			System.out.println(String.format("Neighbors: %s", graph.getTriangulatedNeighbors(s).toString()));
		}
		System.out.println("\n");
	}
	
	public static void printOrdering(List<String> ordering) {
		System.out.println(ordering.toString());
	}
}
