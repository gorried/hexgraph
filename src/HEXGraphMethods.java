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
	 * Returns a set of all possible configurations that the graph could be in
	 * @return a set of all possible configurations that the graph could be in
	 */
	public Set<Configuration<String>> ListStateSpace() {
		// Check to make sure graph is not empty
		if (mGraph.isEmpty()) {
			return new HashSet<Configuration<String>>();
		}
		
		Set<Configuration<String>> configSet = new HashSet<Configuration<String>>();
		ListStateSpace(new Configuration<String>(mGraph.getNodeSet()), configSet, mGraph);	
		
		return configSet;
	}
	
	/**
	 * Subroutine to assist with the listing of the state space
	 * 
	 * @param currentConfig The current configuration we are considering
	 * @param configSet A reference to the set of configurations being returned
	 * @param graph the current subgraph we are considering
	 */
	private void ListStateSpace(Configuration<String> currentConfig,
			Set<Configuration<String>> configSet,
			HEXGraph<String> graph) {
		
		// BASE CASE. If there is nothing left to do we are at a leaf and thus we add it
		if (graph.isEmpty()) {
			configSet.add(currentConfig);
			return;
		}
				
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
		
		Configuration<String> s0 = currentConfig.getDeepCopy();
		Configuration<String> s1 = currentConfig;
		
		// assign values from the direct relations TO HALF the graph
		s0.setValues(pivot, CONFIG_TRUE);
		s0.setValues(mGraph.getAncestors(pivot), CONFIG_TRUE);
		s0.setValues(mGraph.getExcluded(pivot), CONFIG_FALSE);
		
		// Here is the other set of relations we need to recurse over
		s1.setValues(pivot, CONFIG_FALSE);
		s1.setValues(mGraph.getDescendants(pivot), CONFIG_FALSE);
		
		// recursively call this method on v0 and v1. the results should automatically merge
		ListStateSpace(s0, configSet, graph.getSubgraphMinus(v0));
		ListStateSpace(s1, configSet, graph.getSubgraphMinus(v1));
	}
	
}
