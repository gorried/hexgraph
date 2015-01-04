import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;




public class HEXGraphMethods {
	
	private HEXGraph<String> mDenseGraph;
	private HEXGraph<String> mSparseGraph;
	
	/**
	 * Constructor that initializes the graph. The graph can be changed at a later time with the
	 * selectGraph. Null can be passed in as the parameters to this constructor to create an
	 * empty instance of this class.
	 */
	public HEXGraphMethods(HEXGraphFactory factory, String key) {
		selectGraph(factory, key);
	}
	
	
	/**
	 * Select a graph to list the state space for. We are using the maximally dense equivalent
	 * of that graph so that we maximize the number of relations we can get with minimal inference
	 * @param factory The factory that contains all the graphs
	 * @param key The lookup key to get the dense graph
	 */
	public void selectGraph(HEXGraphFactory factory, String key) {
		if (factory == null || key == null) return;
		mDenseGraph = factory.getDenseGraph(key);
		mSparseGraph = factory.getSparseGraph(key);
	}
	
	/**
	 * Returns a set of all possible configurations that the graph could be in
	 * @return a set of all possible configurations that the graph could be in
	 */
	public Set<Configuration<String>> listStateSpace() {
		// Check to make sure graph is not empty
		if (mDenseGraph.isEmpty()) {
			return new HashSet<Configuration<String>>();
		}
		
		Set<Configuration<String>> configSet = new HashSet<Configuration<String>>();
		listStateSpace(new Configuration<String>(mDenseGraph.getNodeSet()), configSet, mDenseGraph);	
		
		return configSet;
	}
	
	/**
	 * Returns a set of all possible configurations for the graph. Used for clique state space
	 * in junction tree inference
	 * @return a set of all possible configurations for the graph.
	 */
	private Set<Configuration<String>> listStateSpace(HEXGraph<String> graph) {
		// Check to make sure graph is not empty
		if (graph.isEmpty()) {
			return new HashSet<Configuration<String>>();
		}
		
		Set<Configuration<String>> configSet = new HashSet<Configuration<String>>();
		listStateSpace(new Configuration<String>(graph.getNodeSet()), configSet, graph);	
		
		return configSet;
	}
	
	/**
	 * Subroutine to assist with the listing of the state space
	 * 
	 * @param currentConfig The current configuration we are considering
	 * @param configSet A reference to the set of configurations being returned
	 * @param graph the current subgraph we are considering
	 */
	private void listStateSpace(Configuration<String> currentConfig,
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
		
		for (String excluded : mDenseGraph.getExcluded(pivot)) {
			v0.add(excluded);
		}
		for (String overlapping : mDenseGraph.getOverlapping(pivot)) {
			v0.add(overlapping);
			v1.add(overlapping);
		}
		for (String ancestor : mDenseGraph.getAncestors(pivot)) {
			v0.add(ancestor);
		}
		for (String descendant : mDenseGraph.getDescendants(pivot)) {
			v1.add(descendant);
		}
		
		Configuration<String> s0 = currentConfig.getDeepCopy();
		Configuration<String> s1 = currentConfig;
		
		// assign values from the direct relations TO HALF the graph
		s0.setValues(pivot, Configuration.CONFIG_TRUE);
		s0.setValues(mDenseGraph.getAncestors(pivot), Configuration.CONFIG_TRUE);
		s0.setValues(mDenseGraph.getExcluded(pivot), Configuration.CONFIG_FALSE);
		
		// Here is the other set of relations we need to recurse over
		s1.setValues(pivot, Configuration.CONFIG_FALSE);
		s1.setValues(mDenseGraph.getDescendants(pivot), Configuration.CONFIG_FALSE);
		
		// recursively call this method on v0 and v1. the results should automatically merge
		listStateSpace(s0, configSet, graph.getSubgraphMinus(v0));
		listStateSpace(s1, configSet, graph.getSubgraphMinus(v1));
	}
	
	public JunctionTree<String> buildJunctionTree() {
		HEXGraph<String> graph = mSparseGraph.getDeepCopy();
		graph.triangulate();
		List<String> elimOrdering = graph.getEliminationOrdering();
		
		JunctionTree<String> junctionTree = new JunctionTree<String>();
		
		for (int i = 0; i < elimOrdering.size(); i++) {
			Set<String> clique = new HashSet<String>();
			String curr = elimOrdering.get(i);
			clique.add(curr);
			Set<String> triNeighbors = graph.getTriangulatedNeighbors(curr);
			for (int j = i; j < elimOrdering.size(); j++) {
				if (triNeighbors.contains(elimOrdering.get(j))) {
					clique.add(elimOrdering.get(j));
				}
			}
			if (clique.size() != 1 || triNeighbors.size() == 1) {
				junctionTree.addNode(clique);
			}
		}
		
		junctionTree.buildEdges();
		
		return junctionTree;
	}
	
	public Factor<String> exactInference(JunctionTree<String> tree) {
		Map<JunctionTreeNode<String>, Set<Configuration<String>>> stateSpaces = getJunctionTreeStateSpaces(tree);
		return tree.exactInference(tree.getFirst(), stateSpaces, mDenseGraph.getScoreMap());
	}
	
	public Map<JunctionTreeNode<String>, Set<Configuration<String>>> getJunctionTreeStateSpaces(JunctionTree<String> tree) {
		Map<JunctionTreeNode<String>, Set<Configuration<String>>> stateSpaces = 
				new HashMap<JunctionTreeNode<String>, Set<Configuration<String>>>();
		for (JunctionTreeNode<String> node : tree.getNodeSet()) {
			Set<Configuration<String>> config = listStateSpace(mDenseGraph.getSubgraph(node.getMembers()));
			stateSpaces.put(node, config);
		}
		return stateSpaces;
	}
	
}
