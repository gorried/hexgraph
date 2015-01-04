import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

/**
 * @author dgorrie
 * Junction tree class. Each node in the junction tree will be a clique from a triangulated
 * HEXGraph, and each edge will be labeled with the intersection of the classes in adjacent
 * cliques.
 * 
 * Parameter V is the label class from the associated HEXGraph.
 */
public class JunctionTree<V> {
	private Set<JunctionTreeNode<V>> nodes;
	
	
	public JunctionTree() {
		nodes = new HashSet<JunctionTreeNode<V>>();
	}
	
	
	public void addNode(Set<V> members) {
		nodes.add(new JunctionTreeNode<V>(members));
		System.out.println("added node " + members.toString() + " to tree");
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
		// Priority queue for later
		Queue<JunctionTreeNode<V>> unseen = new LinkedList<JunctionTreeNode<V>>();
		
		for (JunctionTreeNode<V> first : nodes) {
			for (JunctionTreeNode<V> second : nodes) {
				if (!first.equals(second)) {
					int weight = first.addEdge(second);
					if (weight > 0) {
						second.addEdge(first);
					}
				}
			}
			unseen.add(first);
		}
	
		// at this point we have all the edges built, now we find the spanning tree (Prims)
		Set<DummyEdge<V>> finalEdges = new HashSet<DummyEdge<V>>();
		
		while (!unseen.isEmpty()) {
			JunctionTreeNode<V> curr = unseen.remove();
			JunctionTreeNode<V> other = curr.getPrimsNeighbor(unseen);
			if (other != null) {				
				finalEdges.add(new DummyEdge<>(curr, other, curr.getOverlap(other)));		
			}
		} 
		
		for (DummyEdge<V> edge : finalEdges) {
			System.out.println(edge.toString());
		}
		
		// get rid of all the edges that are not supposed to be there
		for (JunctionTreeNode<V> node : nodes) {
			node.neighbors.clear();
			for (DummyEdge<V> edge : finalEdges) {
				if (edge.contains(node)) {
					node.addEdge(edge.getOther(node));
				}
			}
		}
		
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
		for (JunctionTreeNode<V> child : root.getNeighbors()) {
			factors.add(child.passMessage(root, stateSpaces, scoreMap));
		}
		return null;
	}
	
	@SuppressWarnings("hiding")
	class DummyEdge<V> {
		JunctionTreeNode<V> first;
		JunctionTreeNode<V> second;
		int weight;
		
		public DummyEdge(JunctionTreeNode<V> f, JunctionTreeNode<V> s, int w) {
			first = f;
			second = s;
			weight = w;
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
	
}


class JunctionTreeNode<V> {
	private Set<V> members;
	Map<JunctionTreeNode<V>, Set<V>> neighbors;
	
	public JunctionTreeNode(Set<V> mems) {
		neighbors = new HashMap<JunctionTreeNode<V>, Set<V>>();
		// make a deep copy of mems
		members = new HashSet<V>();
		for (V v : mems) {
			members.add(v);
		}
	}
	
	/**
	 * @requires other != this
	 */
	public int addEdge(JunctionTreeNode<V> other) {
		Set<V> overlap = new HashSet<V>();
		for (V member : members) {
			if (other.members.contains(member)) {
				overlap.add(member);
			}
		}
		if (overlap.size() != 0) {
			neighbors.put(other, overlap);
		}
		return overlap.size();
	}
	
	public Set<V> getMembers() {
		return members;
	}
	
	public Set<JunctionTreeNode<V>> getNeighbors() {
		return neighbors.keySet();
	}
	
	public int getOverlap(JunctionTreeNode<V> other) {
		if (neighbors.containsKey(other)) {
			return neighbors.get(other).size();
		}
		return -1;
	}
	
	// returns null if there is no node to be added
	public JunctionTreeNode<V> getPrimsNeighbor(Queue<JunctionTreeNode<V>> unseen) {
		int maxScore = -1;
		JunctionTreeNode<V> maxNode = null;
		for (JunctionTreeNode<V> neighbor : neighbors.keySet()) {
			if (neighbors.get(neighbor).size() > maxScore && !unseen.contains(neighbor)) {
				maxScore = neighbors.get(neighbor).size();
				maxNode = neighbor;
			}
		}
		return maxNode;
	}
	
	Factor<V> passMessage(JunctionTreeNode<V> parent,
			Map<JunctionTreeNode<V>, Set<Configuration<V>>> stateSpaces,
			Map<V, Double> scoreMap) {
		// base case: this node doesnt have any neighbors that arent parent (leaf)
		if (neighbors.size() == 1) {
			// we know that the only neighbor is parent
			return new Factor<V>(stateSpaces.get(this), scoreMap);
		} else {
			Map<JunctionTreeNode<V>, Factor<V>> factors = new HashMap<JunctionTreeNode<V>, Factor<V>>();
			Map<JunctionTreeNode<V>, Factor<V>> separators = new HashMap<JunctionTreeNode<V>, Factor<V>>();
			for (JunctionTreeNode<V> neighbor : getNeighbors()) {
				if (!neighbor.equals(parent)) {
					Factor<V> neighborPhi = neighbor.passMessage(this, stateSpaces, scoreMap);
					factors.put(neighbor, neighborPhi);
					
					// build the separator node information
				}
			}
			
			
		}
		
		return null;
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
