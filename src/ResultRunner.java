import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class ResultRunner {
	
	public static void main(String[] args) throws IOException, IllegalStateException{
		HEXGraphFactory factory = new HEXGraphFactory();
		String filepath = "src/test.hxg";
		factory.buildHEXGraph(filepath);
		
		HEXGraphMethods methods = new HEXGraphMethods(factory, filepath);
		
		Map<String, Double> scores = new HashMap<String, Double>();
		scores.put("A", Math.log(5));
		scores.put("B", Math.log(.2));
		scores.put("C", Math.log(.4));
		
		
		JunctionTree<String> tree = methods.buildJunctionTree();
		methods.setScores(scores);
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
