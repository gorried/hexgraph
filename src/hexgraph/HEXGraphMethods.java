package hexgraph;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import util.NameSpace;
import util.Pair;
import util.TriState;

/**
 * A class containing methods that utilize {@link HEXGraph} and {@link JunctionTree} properties to
 * infer about the information stored within them.
 */
public class HEXGraphMethods {
	private HEXGraph<String> mDenseGraph;
	private HEXGraph<String> mSparseGraph;
	
	private Map<HEXGraph<String>, Set<Configuration>> mStateSpaceMapping;
	
	private NameSpace<String> mNameSpace;
	
	/**
	 * Constructor that initializes the graph. The graph can be changed at a later time with the
	 * selectGraph. Null can be passed in as the parameters to this constructor to create an
	 * empty instance of this class.
	 */
	public HEXGraphMethods(HEXGraphFactory factory, String key, NameSpace<String> nameSpace) {
		selectGraph(factory, key);
		mStateSpaceMapping = new HashMap<HEXGraph<String>, Set<Configuration>>();
		mNameSpace = nameSpace;
	}
	
	private HEXGraphMethods(HEXGraph<String> denseGraph, HEXGraph<String> sparseGraph, NameSpace<String> nameSpace) {
		mDenseGraph = denseGraph;
		mSparseGraph = sparseGraph;
		mNameSpace = nameSpace;
	}
	
	public HEXGraphMethods getDeepCopy() {
		return new HEXGraphMethods(
				mDenseGraph.getDeepCopy(),
				mSparseGraph.getDeepCopy(),
				mNameSpace);
	}
	
	/**
	 * Simple call to set the scores in the graph 
	 */
	private void setScores(Map<String, Double> scores){
		mDenseGraph.setScores(scores);
	}
	
	/**
	 * Simple call to set the scores in the graph 
	 */
	private void setScores(double[] scores) {
		mDenseGraph.setScores(scores);
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
	 * Builds a new {@link JunctionTree} using the information encoded in our sparse {@link HEXGraph}
	 * @return a new JunctionTree with the information from our sparse graph
	 */
	public JunctionTree<String> buildJunctionTree() {
		HEXGraph<String> graph = mSparseGraph.getDeepCopy();
		graph.triangulate();
		List<String> elimOrdering = graph.getEliminationOrdering();
		
		JunctionTree<String> junctionTree = new JunctionTree<String>(mNameSpace, mDenseGraph.size());
		
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
			junctionTree.addNode(clique);
		}
		
		junctionTree.buildEdges();
		
		return junctionTree;
	}
	
	/**
	 * Treats the sparse graph as a generic CRF and runs message passing over the junction tree
	 * built from the graph to determine the most likely single configuration of classes to match
	 * a certain instance
	 * 
	 * @param tree - the junction tree built from our {@link HEXGraph}
	 * @return a map of {@link Configuration} to score containing the top two configurations and 
	 * 	their scores
	 */
	public Map<Configuration, Double> exactInference(JunctionTree<String> tree) {
		Map<JunctionTreeNode<String>, Set<Configuration>> stateSpaces = getJunctionTreeStateSpaces(tree);
		Map<Configuration, Double> scores = 
				tree.exactInference(listStateSpace(), stateSpaces, mDenseGraph.getScores());
		Configuration bestConfig = null;
		double bestScore = -1;
		for (Configuration config : scores.keySet()) {
			if (scores.get(config) > bestScore) {
				bestScore = scores.get(config);
				bestConfig = config;
			}
		}
		Map<Configuration, Double> finalMap = new HashMap<Configuration, Double>();
		finalMap.put(bestConfig, bestScore);
		if (Math.abs(bestScore - 1.0) < 0.005) {
			scores.put(bestConfig, -1.0);
			bestConfig = null;
			bestScore = -1;
			for (Configuration config : scores.keySet()) {
				if (scores.get(config) > bestScore) {
					bestScore = scores.get(config);
					bestConfig = config;
				}
			}
			finalMap.put(bestConfig, bestScore);
		}
		return finalMap;
	}
	
	/**
	 * An alternative to exactInference, in which the top result gets returned in the form of a
	 * {@link Pair}, which is nice and lightweight.
	 * 
	 * Treats the sparse graph as a generic CRF and runs message passing over the junction tree
	 * built from the graph to determine the most likely single configuration of classes to match
	 * a certain instance.
	 * 
	 * @param tree - the junction tree built from our {@link HEXGraph}
	 * @return a {@link Pair} with a of {@link Configuration} and a double score that represents 
	 * 	the top configuration its score.
	 */
	public Pair<Configuration, Double> exactPairInference(JunctionTree<String> tree) {
		Map<Configuration, Double> scores = 
				tree.exactInference(listStateSpace(), getJunctionTreeStateSpaces(tree), mDenseGraph.getScores());
		Configuration bestConfig = null;
		double bestScore = -1;
		for (Configuration config : scores.keySet()) {
			if (scores.get(config) > bestScore) {
				bestScore = scores.get(config);
				bestConfig = config;
			}
		}
		return new Pair<Configuration, Double>(bestConfig, bestScore);
	}
	
	/**
	 * Treats the sparse graph as a generic CRF and runs message passing over the junction tree
	 * built from the graph. After message passing, merges the results to get the marginal
	 * likelihood of each class for the given score setting.
	 * @param tree - the junction tree built from our {@link HEXGraph}
	 * @return a Map of String to double containing the class name and its marginal likelihood
	 */
	public synchronized Map<String, Double> exactMarginalInference(
			JunctionTree<String> tree,
			double[] scores) {
		setScores(scores);
		Map<JunctionTreeNode<String>, Set<Configuration>> stateSpaces = getJunctionTreeStateSpaces(tree);
		return tree.exactMarginalInference(mDenseGraph.size(), stateSpaces, mDenseGraph.getScores());
	}
	
	/**
	 * Treats the sparse graph as a generic CRF and runs message passing over the junction tree
	 * built from the graph. After message passing, merges the results to get the marginal
	 * likelihood of each class for the given score setting.
	 * @param tree - the junction tree built from our {@link HEXGraph}
	 * @param stateSpaces - the junction tree state space
	 * @return a Map of String to double containing the class name and its marginal likelihood
	 */
	public synchronized Map<String, Double> exactMarginalInference(
			JunctionTree<String> tree, 
			Map<JunctionTreeNode<String>, Set<Configuration>> stateSpaces,
			double[] scores) {
		setScores(scores);
		return tree.exactMarginalInference(mDenseGraph.size(), stateSpaces, mDenseGraph.getScores());
	}
	
	/**
	 * Returns a set of all possible configurations that the graph could be in
	 * @return a set of all possible configurations that the graph could be in
	 */
	public Set<Configuration> listStateSpace() {
		// Check to make sure graph is not empty
		if (mDenseGraph.isEmpty()) {
			return new HashSet<Configuration>();
		} else if (mStateSpaceMapping.containsKey(mDenseGraph)) {
			return mStateSpaceMapping.get(mDenseGraph);
		} else {
			Set<Configuration> configSet = new HashSet<Configuration>();
			listStateSpace(new Configuration(mNameSpace.size()), configSet, mDenseGraph);	
			
			mStateSpaceMapping.put(mDenseGraph, configSet);
			return configSet;
		}
	}
	
	/**
	 * Helper method to get the state spaces for a {@link JunctionTree}
	 * @return the state spaces of JunctionTree tree
	 */
	public Map<JunctionTreeNode<String>, Set<Configuration>> getJunctionTreeStateSpaces(JunctionTree<String> tree) {
		Map<JunctionTreeNode<String>, Set<Configuration>> stateSpaces = 
				new HashMap<JunctionTreeNode<String>, Set<Configuration>>();
		for (JunctionTreeNode<String> node : tree.getNodeSet()) {
			Set<Configuration> config = listStateSpace(mDenseGraph.getSubgraph(node.getMembers()));
			stateSpaces.put(node, config);
		}
		return stateSpaces;
	}
	
	/**
	 * Returns a set of all possible configurations for the graph. Used for clique state space
	 * in junction tree inference
	 * @return a set of all possible configurations for the graph.
	 */
	private Set<Configuration> listStateSpace(HEXGraph<String> graph) {
		// Check to make sure graph is not empty
		if (graph.isEmpty()) {
			return new HashSet<Configuration>();
		} else if (mStateSpaceMapping.containsKey(graph)) {
			return mStateSpaceMapping.get(graph);
		} else {
			Set<Configuration> configSet = new HashSet<Configuration>();
			listStateSpace(new Configuration(mNameSpace.size()), configSet, graph);
			
			mStateSpaceMapping.put(graph, configSet);
			return configSet;
		}
		
	}
	
	/**
	 * Subroutine to assist with the listing of the state space
	 * 
	 * @param currentConfig The current configuration we are considering
	 * @param configSet A reference to the set of configurations being returned
	 * @param graph the current subgraph we are considering
	 */
	private void listStateSpace(Configuration currentConfig,
			Set<Configuration> configSet,
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
		
		for (String excluded : graph.getExcluded(pivot)) {
			v0.add(excluded);
		}
		for (String overlapping : graph.getOverlapping(pivot)) {
			v0.add(overlapping);
			v1.add(overlapping);
		}
		for (String ancestor : graph.getAncestors(pivot)) {
			v0.add(ancestor);
		}
		for (String descendant : graph.getDescendants(pivot)) {
			v1.add(descendant);
		}
		
		Configuration s0 = currentConfig.getDeepCopy();
		Configuration s1 = currentConfig;
		
		// assign values from the direct relations TO HALF the graph
		s0.setValue(mNameSpace.getIndex(pivot), TriState.TRUE);
		s0.setValues(getSetIndices(graph.getAncestors(pivot)), TriState.TRUE);
		s0.setValues(getSetIndices(graph.getExcluded(pivot)), TriState.FALSE);
		
		// Here is the other set of relations we need to recurse over
		s1.setValue(mNameSpace.getIndex(pivot), TriState.FALSE);
		s1.setValues(getSetIndices(graph.getDescendants(pivot)), TriState.FALSE);
		
		// recursively call this method on v0 and v1. the results should automatically merge
		listStateSpace(s0, configSet, graph.getSubgraphMinus(v0));
		listStateSpace(s1, configSet, graph.getSubgraphMinus(v1));
	}
	
	/**
	 * Helper method to convert from a set of strings to a set of indices using our {@link NameSpace}
	 * TODO: replace this with better integration into hexgraph itself
	 */
	private Set<Integer> getSetIndices(Set<String> set) {
		Set<Integer> indices = new HashSet<Integer>();
		for (String ancestor : set) {
			indices.add(mNameSpace.getIndex(ancestor));
		}
		return indices;
	}
	
}
