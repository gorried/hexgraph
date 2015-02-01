import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

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
	
	
	public JunctionTree() {
		nodes = new HashSet<JunctionTreeNode<V>>();
		edges = new HashSet<JunctionTreeEdge<V>>();
	}
	
	private boolean nodeConsumes(JunctionTreeNode<V> node, Set<V> members) {
		for (V member : members) {
			Set<V> nodeMembers = node.getMembers();
			if (!nodeMembers.contains(member)) {
				return false;
			}
		}
		return true;
	}
	
	public void addNode(Set<V> members) {
		for (JunctionTreeNode<V> node : nodes) {
			if (nodeConsumes(node, members)) return;
		}
		nodes.add(new JunctionTreeNode<V>(members));
		System.out.println("added node " + members.toString() + " to tree");
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
					throw new IllegalStateException("Error while building edges");
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
		
		// debugging
		for (JunctionTreeEdge<V> edge : finalEdges) {
			System.out.println(edge.toString());
		}
		
		// get rid of all the edges that are not supposed to be there
		
		
		// we now have our maximal spanning junction tree built!
	}
	
	public Factor<V> exactInference(JunctionTreeNode<V> root, 
			Map<JunctionTreeNode<V>, Set<Configuration<V>>> stateSpaces,
			Map<V, Double> scoreMap) {
		// base case
		if (root.getNeighbors().size() == 0) {
			// dont know what to do here
		}
		Set<Factor<V>> factors = new HashSet<Factor<V>>();
		for (JunctionTreeEdge<V> edge : edges) {
			factors.add(edge.collectMessages(root, stateSpaces, scoreMap));
		}
		return null;
	}
	
}


class JunctionTreeNode<V> {
	private Set<V> members;
	private Set<JunctionTreeEdge<V>> edges;
	private Factor<V> factor;
	
	public JunctionTreeNode(Set<V> mems) {
		edges = new HashSet<JunctionTreeEdge<V>>();
		// make a deep copy of mems
		members = new HashSet<V>();
		for (V v : mems) {
			members.add(v);
		}
		factor = new Factor<V>();
	}
	
	public void addEdge(JunctionTreeEdge<V> edge) {
		edges.add(edge);
	}
	
	public Set<JunctionTreeEdge<V>> getEdges() {
		return edges;
	}
	
	public void removeEdge(JunctionTreeNode<V> other) {
		Iterator<JunctionTreeEdge<V>> it = edges.iterator();
		while (it.hasNext()) {
			if (it.next().getOther(this).equals(other)) {
				it.remove();
			}
		}
	}
	
	public Factor<V> getFactor() {
		return factor.getDeepCopy();
	}
	
	public Set<V> getMembers() {
		return members;
	}
	
	public Set<V> getOverlappingSet(JunctionTreeNode<V> other) {
		Set<V> overlapping = new HashSet<V>();
		for (V member : members) {
			if (other.members.contains(member)) {
				overlapping.add(member);
			}
		}
		return overlapping;
	}
	
	public int getOverlap(JunctionTreeNode<V> other) {
		int count = 0;
		for (V member : members) {
			if (other.members.contains(member)) {
				count++;
			}
		}
		return count;
	}
	
	public Set<JunctionTreeNode<V>> getNeighbors() {
		Set<JunctionTreeNode<V>> neighbors = new HashSet<JunctionTreeNode<V>>();
		for (JunctionTreeEdge<V> edge : edges) {
			neighbors.add(edge.getOther(this));
		}
		return neighbors;
	}
	
	/**
	 * We collect messages from the leaves and bring them to the root
	 * @param parent
	 * @param stateSpaces
	 * @param scoreMap
	 * @return
	 */
	Factor<V> collectMessages(JunctionTreeEdge<V> parent,
			Map<JunctionTreeNode<V>, Set<Configuration<V>>> stateSpaces,
			Map<V, Double> scoreMap) {
		// base case: this node doesnt have any neighbors that arent parent (leaf)
		if (edges.size() == 1) {
			// we know that the only neighbor is parent
			return new Factor<V>(stateSpaces.get(this), scoreMap);
		} else {
			factor = new Factor<V>(stateSpaces.get(this));
			Set<Factor<V>> separators = new HashSet<Factor<V>>();
			for (JunctionTreeEdge<V> outEdge : getEdges()) {
				if (!outEdge.equals(parent)) {
					separators.add(outEdge.collectMessages(this, stateSpaces, scoreMap));
				}
			}
			// Calculate scores and put them into a new factor
			for (Factor<V> separator : separators) {
				factor.combineDistributionProduct(separator);
			}
			
			return factor.getDeepCopy();
		}	
	}
	
	/**
	 * Once the messages have been collected at the root, we propagate them outwards to the leaves
	 */
	void propagateMessages(JunctionTreeEdge<V> parent) {
		if (parent != null) {
			factor.combineDistributionProduct(parent.getPhiStarStar());
		}
		for (JunctionTreeEdge<V> edge : getEdges()) {
			if (!edge.equals(parent)) {
				edge.propagateMessages(this);
			}
		}	
	}
	
	public String toString() {
		return members.toString();
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object o) {
		if (o instanceof JunctionTreeNode && ((JunctionTreeNode<V>) o).members.equals(this.members)) {
			return true;
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return members.hashCode();
	}
}

class JunctionTreeEdge<V> {
	JunctionTreeNode<V> first;
	JunctionTreeNode<V> second;
	int weight;
	Factor<V> phiStar;
	Factor<V> phiStarStar;
	
	public JunctionTreeEdge(JunctionTreeNode<V> f, JunctionTreeNode<V> s) {
		first = f;
		second = s;
		weight = f.getOverlap(s);
		phiStar = new Factor<V>();
		phiStarStar = new Factor<V>();
	}
	
	public Factor<V> getPhiStarStar() {
		return phiStarStar.getDeepCopy();
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
	public Factor<V> collectMessages(JunctionTreeNode<V> caller, 
			Map<JunctionTreeNode<V>, Set<Configuration<V>>> stateSpaces,
			Map<V, Double> scoreMap) {
		if (!caller.equals(first) && !caller.equals(second)) {
			throw new IllegalStateException("Illegal caller to edge in collect messages");
		}
		Factor<V> result = getOther(caller).collectMessages(this, stateSpaces, scoreMap);
		Set<V> overlapping = caller.getOverlappingSet(getOther(caller));
		phiStar = result.getSubDistribution(overlapping);
		return phiStar.getDeepCopy();
	}
	
	/**
	 * Once the messages have been collected at the root, we propagate them outwards to the leaves
	 */
	public void propagateMessages(JunctionTreeNode<V> caller) {
		if (!caller.equals(first) && !caller.equals(second)) {
			throw new IllegalStateException("Illegal caller to edge in collect messages");
		}
		Set<V> overlapping = caller.getOverlappingSet(getOther(caller));
		phiStarStar = caller.getFactor().getSubDistribution(overlapping);
		getOther(caller).propagateMessages(this);
	}
	
	public boolean contains(JunctionTreeNode<V> node) {
		return node.equals(first) || node.equals(second);
	}
	
	public JunctionTreeNode<V> getOther(JunctionTreeNode<V> node) {
		return node.equals(first) ? second : first;
	}
	
	@Override
	public String toString() {
		return first.toString() + "\n" + second.toString() + "\n" + weight;
	}

}


