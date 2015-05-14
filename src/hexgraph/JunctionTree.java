package hexgraph;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import util.NameSpace;
import util.TriState;

/**
 * @author dgorrie
 * Junction tree class. Each node in the junction tree will be a clique from a triangulated
 * HEXGraph, and each edge will be labeled with the intersection of the classes in adjacent
 * cliques.
 * 
 * Parameter V is the label class from the associated HEXGraph.
 * 
 * TODO: check for number of connected components and run message passing over each one
 */
public class JunctionTree<V> {
	private Set<JunctionTreeNode<V>> nodes;
	private Set<JunctionTreeEdge<V>> edges;
	
	private final NameSpace<V> mNameSpace;
	private final int mNumClasses;
	
	public JunctionTree(NameSpace<V> nameSpace, int numClasses) {
		nodes = new HashSet<JunctionTreeNode<V>>();
		edges = new HashSet<JunctionTreeEdge<V>>();
		mNameSpace = nameSpace;
		mNumClasses = numClasses;
	}
	
	private boolean nodeConsumes(JunctionTreeNode<V> node, Set<V> members) {
		BitSet nodeMembers = node.getMembers();
		for (V member : members) {
			if (!nodeMembers.get(mNameSpace.getIndex(member))) {
				return false;
			}
		}
		return true;
	}
	
	public void addNode(Set<V> members) {
		for (JunctionTreeNode<V> node : nodes) {
			if (nodeConsumes(node, members)) return;
		}
		nodes.add(new JunctionTreeNode<V>(members, mNumClasses, mNameSpace));
		System.out.println("added node " + members.size() + " " + members.toString());
	}
	
	private void addEdge(JunctionTreeNode<V> first, JunctionTreeNode<V> second) {
		JunctionTreeEdge<V> newEdge = new JunctionTreeEdge<V>(first, second);
		if (newEdge.weight != 0) {
			first.addEdge(newEdge);
			second.addEdge(newEdge);
		}
	}
	
	public Set<JunctionTreeNode<V>> getNodeSet() {
		return nodes;
	}
	
	public JunctionTreeNode<V> getFirst() {
		Iterator<JunctionTreeNode<V>> it = nodes.iterator();
		return it.next();
	}
	
	/**
	 * Builds a graph that with all the relations in the junction tree and runs a maximum
	 * spanning tree algorithm over that graph to find the final set of edges that should be
	 * retained
	 */
	public void buildEdges() {
		
		Set<JunctionTreeNode<V>> unseen = new HashSet<JunctionTreeNode<V>>();
		Set<JunctionTreeNode<V>> seen = new HashSet<JunctionTreeNode<V>>();
		JunctionTreeNode<V> curr = null;
		
		for (JunctionTreeNode<V> first : nodes) {
			for (JunctionTreeNode<V> second : nodes) {
				if (!first.equals(second)) {					
					addEdge(first, second);
				}
			}
			unseen.add(first);
			curr = first;
		}
	
		// at this point we have all the edges built, now we find the spanning tree (Prims)
		Set<JunctionTreeEdge<V>> finalEdges = new HashSet<JunctionTreeEdge<V>>();
		
		while (!unseen.isEmpty()) {
			int maxCount = -1;
			JunctionTreeEdge<V> bestEdge = null;
			if (finalEdges.isEmpty()) {
				for (JunctionTreeEdge<V> edge : curr.getEdges()) {
					if (edge.weight > maxCount) {
						bestEdge = edge;
						maxCount = edge.weight;
					}
				}
				if (bestEdge == null) {
					return;
				} else {
					finalEdges.add(bestEdge);
					unseen.remove(bestEdge.first);
					seen.add(bestEdge.first);
					unseen.remove(bestEdge.second);
					seen.add(bestEdge.second);
				}
			} else {
				for (JunctionTreeNode<V> node : seen) {
					for (JunctionTreeEdge<V> edge : node.getEdges()) {
						if (edge.weight > maxCount && unseen.contains(edge.getOther(node))) {
							bestEdge = edge;
							maxCount = edge.weight;
						}
					}
				}
				if (bestEdge == null) {
					throw new IllegalStateException("Error while building edges");
				} else {
					finalEdges.add(bestEdge);
					unseen.remove(bestEdge.first);
					seen.add(bestEdge.first);
					unseen.remove(bestEdge.second);
					seen.add(bestEdge.second);
				}
			}
		}
		
		for (JunctionTreeNode<V> node : getNodeSet()) {
			Iterator<JunctionTreeEdge<V>> it = node.getEdges().iterator();
			while (it.hasNext()) {
				JunctionTreeEdge<V> currEdge = it.next();
				if (!finalEdges.contains(currEdge)) {
					it.remove();
				}
			}
		}
		this.edges.addAll(finalEdges);
		
		System.out.print("EDGES: ");
		for (JunctionTreeEdge<V> edge : this.edges) {
			System.out.println(edge.weight + edge.toString());
		}
	}
	
	public Map<Configuration, Double> exactInference(Set<Configuration> graphStateSpace,
			Map<JunctionTreeNode<V>, Set<Configuration>> stateSpaces,
			double[] scores) {
		JunctionTreeNode<V> root = getFirst();
		// base case
		if (root.getNeighbors().size() == 0) {
			// dont know what to do here
			System.out.println("Root has cardinality zero....");
			printTreeStats();
		}
		root.collectMessages(null, stateSpaces, scores);
		root.propagateMessages(null, null);
		
		// All the nodes and edges are stored in a field
		Map<Configuration, Double> configScores = new HashMap<Configuration, Double>();
		for (Configuration config : graphStateSpace) {
			double numerator = 1.0;
			double denominator = 1.0;
//			System.out.println(config.toString());
//			System.out.print("Numerator: ");
			for (JunctionTreeNode<V> node : nodes) {
				numerator *= node.getFactor().getScoreIfSubsumed(config);
//				System.out.print(node.getFactor().getScoreIfSubsumed(config));
//				System.out.print(" ");
			}
//			System.out.println();
//			System.out.print("Denominator: ");
			for (JunctionTreeEdge<V> edge : edges) {
				denominator *= edge.getDivided().getScoreIfSubsumed(config);
//				System.out.println(edge.getDivided().getScoreIfSubsumed(config));
//				System.out.print(" ");
			}
//			System.out.println("-----");
			// tune for the repeated factors. We know a factor is a "super" factor if it
			// appears in an edge.
			for (int i = 0; i < config.size(); i++) {
				if (config.get(i) == TriState.TRUE) {
					for (JunctionTreeEdge<V> edge : edges) {
						if (edge.phiStar.getVariables().get(i)) {
							denominator *= Math.pow(Math.E, scores[i]);
						}
					}
				}
			}
			configScores.put(config, numerator / denominator);
		}
		
		return configScores;
	}
	
	
	public Map<V, Double> exactMarginalInference(int numClasses,
			Map<JunctionTreeNode<V>, Set<Configuration>> stateSpaces,
			double[] scores) {
		JunctionTreeNode<V> root = getFirst();
		// base case
		if (root.getNeighbors().size() == 0) {
			// dont know what to do here
			System.out.println("Root has cardinality zero....");
		}
		long startTime = System.currentTimeMillis();
		root.collectMessages(null, stateSpaces, scores);
		long endTime = System.currentTimeMillis();
		// System.out.println(String.format("Collecting messages took %d ms", endTime - startTime));
		
		startTime = System.currentTimeMillis();
		root.propagateMessages(null, null);
		endTime = System.currentTimeMillis();
		// System.out.println(String.format("Propagating messages took %d ms", endTime - startTime));
		
		startTime = System.currentTimeMillis();
		// All the nodes and edges are stored in a field
		Map<V, Double> configScores = new HashMap<V, Double>();
		double[] pNode1 = new double[numClasses];
		double[] pNode0 = new double[numClasses];
		double[] pSeparator1 = new double[numClasses];
		double[] pSeparator0 = new double[numClasses];
		for (JunctionTreeEdge<V> edge : edges) {
			Map<Configuration, Double> dist = edge.getPhiStar().getDist();
			for (Configuration c : dist.keySet()) {
				double val = dist.get(c);
				for (int i = 0; i < numClasses; i++) {
					if (c.contains(i)) {
						if (c.get(i) == TriState.TRUE) {
							pSeparator1[i] += val;
						} else {
							pSeparator0[i] += val;
						}
					}
				}
			}
		}
		for (JunctionTreeNode<V> node : nodes) {
			Map<Configuration, Double> dist = node.getFactor().getDist();
			for (Configuration c : dist.keySet()) {
				double val = dist.get(c);
				for (int i = 0; i < numClasses; i++) {
					if (c.contains(i)) {
						if (c.get(i) == TriState.TRUE) {
							pNode1[i] += val;
						} else {
							pNode0[i] += val;
						}
					}
				}
			}
		}
		for (int i = 0; i < numClasses; i++) {			
			// System.out.println(String.format("%.6f %.6f || %.6f %.6f", pNode1[i], pSeparator1[i], pNode0[i], pSeparator0[i]));
			double m1 = pNode1[i] / (pSeparator1[i] == 0.0 ? 1.0 : pSeparator1[i]);
			double m0 = pNode0[i] / (pSeparator0[i] == 0.0 ? 1.0 : pSeparator0[i]);
			// System.out.println(Math.exp(m1 / (m0 + m1)));
			configScores.put(mNameSpace.get(i), Math.exp(m1 / (m0 + m1)));
		}
		endTime = System.currentTimeMillis();
		// System.out.println(String.format("Merging results took %d ms", endTime - startTime));
		return configScores;
	}
	
	
	public void printFactors() {
		for (JunctionTreeNode<V> node : nodes) {
			node.getFactor().print("node", node.getMembers());
			
		}
		for (JunctionTreeEdge<V> edge : edges) {
			System.out.println("edge between " + edge.first.getMembers() + " and " + edge.second.getMembers());
			edge.phiStar.print("phistar");
			edge.phiStarStar.print("phistarstar");
		}
	}
	
	public void printTreeStats() {
		System.out.println(String.format("This tree contains %d nodes and %d edges", nodes.size(), edges.size()));
	}
	
}

class JunctionTreeEdge<V> {
	JunctionTreeNode<V> first;
	JunctionTreeNode<V> second;
	int weight;
	Factor phiStar;
	Factor phiStarStar;
	Factor divided;
	
	public JunctionTreeEdge(JunctionTreeNode<V> f, JunctionTreeNode<V> s) {
		first = f;
		second = s;
		weight = f.getOverlap(s);
		phiStar = new Factor();
		phiStarStar = new Factor();
		divided = new Factor();
	}
	
	public Factor getPhiStar() {
		return phiStar.getDeepCopy();
	}
	
	public Factor getPhiStarStar() {
		return phiStarStar.getDeepCopy();
	}
	
	public Factor getDivided() {
		return divided.getDeepCopy();
	}
	
	/**
	 * Collects the messages from the leaves and sends them upwards to the root
	 * 
	 * @param caller
	 * @param message
	 * @param stateSpaces
	 * @param scoreMap
	 * @return
	 */
	public Factor collectMessages(JunctionTreeNode<V> caller, 
			Map<JunctionTreeNode<V>, Set<Configuration>> stateSpaces,
			double[] scores) {
		if (!caller.equals(first) && !caller.equals(second)) {
			throw new IllegalStateException("Illegal caller to edge in collect messages");
		}
		Factor result = getOther(caller).collectMessages(caller, stateSpaces, scores);
		BitSet overlapping = caller.getOverlappingSet(getOther(caller));
		phiStar = result.getSubDistribution(overlapping);
		return getPhiStar();
	}
	
	/**
	 * Once the messages have been collected at the root, we propagate them outwards to the leaves
	 */
	public void propagateMessages(JunctionTreeNode<V> caller) {
		if (!caller.equals(first) && !caller.equals(second)) {
			throw new IllegalStateException("Illegal caller to edge in propagate messages");
		}
		BitSet overlapping = caller.getOverlappingSet(getOther(caller));
		phiStarStar = caller.getFactor().getSubDistribution(overlapping);
		divided = phiStarStar.divide(getPhiStar());
		
		getOther(caller).propagateMessages(this, phiStarStar);
	}
	
	public boolean contains(JunctionTreeNode<V> node) {
		if (node == null) return false;
		return node.equals(first) || node.equals(second);
	}
	
	public JunctionTreeNode<V> getOther(JunctionTreeNode<V> node) {
		return node.equals(first) ? second : first;
	}
	
	@Override
	public String toString() {
		return "\n" + first.toString() + " <--" + weight + "--> " + second.toString();
	}

}


