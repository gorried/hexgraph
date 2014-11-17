import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;




public class HEXGraphMethods {
	
	private HEXGraph<String> mGraph;
	private final int CONFIG_FALSE = 0;
	private final int CONFIG_TRUE = 1;
	
	/**
	 * Constructor that initializes the graph. The graph can be changed at a later time with the
	 * selectGraph. Null can be passed in as the parameters to this constructor to create an
	 * empty instance of this class.
	 */
	public HEXGraphMethods(HEXGraphFactory factory, String key) {
		selectGraph(factory, key);
	}
	
	
	public HEXGraphMethods(HEXGraph<String> graph) {
		mGraph = graph;
	}
	
	/**
	 * Select a graph to list the state space for. We are using the maximally dense equivalent
	 * of that graph so that we maximize the number of relations we can get with minimal inference
	 * @param factory The factory that contains all the graphs
	 * @param key The lookup key to get the dense graph
	 */
	public void selectGraph(HEXGraphFactory factory, String key) {
		if (factory == null || key == null) return;
		mGraph = factory.getDenseGraph(key);
	}
	
	/**
	 * Returns a configuration
	 * @return
	 */
	public Configuration<String> ListStateSpace() {
		// Check to make sure graph is not empty
		if (mGraph.isEmpty()) {
			return new Configuration<String>();
		}
		
		Configuration<String> config = new Configuration<String>(mGraph.getNodeSet());
		
		ListStateSpace(config, mGraph);	
		return config;
	}
	
	private void ListStateSpace(Configuration<String> config, HEXGraph<String> graph) {
			
		// set a random pivot pivot
		String pivot = graph.getNodeList().get(new Random().nextInt(graph.size()));
		
		// Define the two node subsets we will use to get subgraphs to recurse over
		Set<String> v0 = new HashSet<String>();
		Set<String> v1 = new HashSet<String>();
		
		for (String excluded : mGraph.getExcluded(pivot)) {
			v0.add(excluded);
		}
		for (String overlapping : mGraph.getOverlapping(pivot)) {
			v0.add(overlapping);
			v1.add(overlapping);
		}
		for (String ancestor : mGraph.getAncestors(pivot)) {
			v0.add(ancestor);
		}
		for (String descendant : mGraph.getDescendants(pivot)) {
			v1.add(descendant);
		}
		
		// assign values from the direct relations
		config.setValues(mGraph.getAncestors(pivot), CONFIG_TRUE);
		config.setValues(mGraph.getExcluded(pivot), CONFIG_FALSE);
		
		// recursively call this method on v0 and v1. the results should automatically merge
		ListStateSpace(config, graph.getSubgraph(v0));
		ListStateSpace(config, graph.getSubgraph(v1));
	}
	
}
