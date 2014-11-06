
import java.util.*;


// TODO: check all the methods that return collections to make sure they are returning COPIES
// of that collection

// TODO: Implement caching

/**
 * @author Daniel Gorrie
 * 
 *This is a generic implementation of a graph with a node type and an edge type. 
 * All interaction with the graph at this level is in terms of the generic types specified.
 * 
 */
public class HEXGraph<V>{
	private Map<V, GraphNode<V>> nodes;
	
	/**
	 * Constructs a new HEXGraph instance.
	 */
	public HEXGraph(){
		nodes = new HashMap<V, GraphNode<V>>();
	}
	
	/**
	 * Adds a new node to the graph with a label label.
	 * 
	 * @param label The label of the node being added.
	 * @throws IllegalArgumentException if label is null.
	 * @modifies this
	 * @effects Adds a new node to the graph.
	 * @return True if the node was added successfully, false otherwise.
	 */
	public boolean addNode(V label) {
		if(!hasNode(label)){
			nodes.put(label, new GraphNode<V>(label));
			return true;
		}
		return false;
	}

	/**
	 * Adds a new edge between the nodes represented by labels tail and head with the label label.
	 * 
	 * @param tail The label of the node that will be the tail of the new edge.
	 * @param head The label of the node that will be the head of the new edge.
	 * @return True if the edge is added successfully, false otherwise.
	 */
	public boolean addHierarchy(V tail, V head) {
		// Check to make sure that both things are actually members of the graph
		if (!(nodes.keySet().contains(tail) && nodes.keySet().contains(head))) return false;
		
		return nodes.get(tail).addHierarchyEdge(nodes.get(head));
	}
	
	
	public boolean addExclusion(V first, V second) {
		// Check to make sure that both things are actually members of the graph
		if (!(nodes.keySet().contains(first) && nodes.keySet().contains(second))) return false;
		
		return nodes.get(first).addExclusionEdge(nodes.get(second)) && 
				nodes.get(second).addExclusionEdge(nodes.get(first));
	}

	/**
	 * Removes the node with the given label, as well as all edges coming from and going to it.
	 * 
	 * @param label The label of the node being removed
	 * @modifies this
	 * @effects removes a node from nodes.
	 * @return True if the node and corresponding edges was removed successfully,
	 * 		false otherwise.
	 */
	public void deleteNode(V label) {
		for (V v : nodes.keySet()) {
			nodes.get(v).removeEdges(nodes.get(label));
		}
		nodes.remove(label);
	}

	/**
	 * Removes an edge going from node tail to node head with label label. 
	 * 
	 * @param tail The label of the tail of the edge
	 * @param head The label of the head of the edge
	 * @param label The label of the edge being removed
	 * @modifies this, tail
	 * @effects removes an edge from the graph, more specifically from tail.
	 * @return True if the edge was removed successfully, false otherwise.
	 */
	public boolean deleteHierarchyEdge(V tail, V head) {
		return nodes.get(tail).removeEdges(nodes.get(head));
	}
	
	// under current API same as above
	public boolean deleteExclusion(V first, V second) {
		return nodes.get(first).removeEdges(nodes.get(second));
	}
	
	/**
	 * Determines if there is an edge with a connecting node a to node b.
	 * 
	 * @param tail The label of the tail.
	 * @param head The label of the head.
	 * @throws IllegalArgumentException if either head or tail is null.
	 * @return True if there is an edge from node a to node b, false if there is not.
	 */
	public boolean hasHierarchyEdge(V tail, V head) {
		return nodes.get(tail).getHierarchySubset().contains(nodes.get(head));
	}
	
	/**
	 * @param first One of the nodes we are testing
	 * @param second The other node we are testing
	 * @return Whether first and second have an exclusion relationship
	 */
	public boolean hasExclusion(V first, V second) {
		return nodes.get(first).getExcluded().contains(nodes.get(second));
	}
	
	/**
	 * Returns the relationship between the node with the label desired and the node with the
	 * label other
	 * 
	 * @param desired
	 * @param other
	 * @return the relationship between desired and other
	 */
	public Relationship getRelationship(V desired, V other) {
		return nodes.get(desired).getRelationship(nodes.get(other));
	}
	
	/**
	 * Checks to see if the specified node exists in the graph.
	 * 
	 * @param label The label of the node being searched for.
	 * @throws IllegalArgumentException if label is null.
	 * @return True if that node exists, false otherwise.
	 */
	public boolean hasNode(V label){
		if(label == null){
			System.err.println("Null passed in as node label in hasNode");
			throw new IllegalArgumentException();
		}
		return nodes.containsKey(label);
	}
	
	/**
	 * Returns the hierarchical relationships involving that node
	 * @param label
	 * @return
	 */
	public List<V> getHierarchySuperset(V label) {
		List<V> list = new ArrayList<V>();
		for (GraphNode<V> node : nodes.get(label).getHierarchySuperset()) {
			list.add(node.getLabel());
		}
		return list;
	}
	
	/**
	 * 
	 * 
	 * @param label
	 * @return
	 */
	public List<V> getHierarchySubset (V label) {
		List<V> list = new ArrayList<V>();
		for (GraphNode<V> node : nodes.get(label).getHierarchySubset()) {
			list.add(node.getLabel());
		}
		return list;
	}
	
	
	/**
	 * Transforming to list to make iteration easier? its O(n) anyhow
	 * @param label
	 * @return
	 */
	public Set<V> getExcluded (V label) {
		Set<V> exc = new HashSet<V>();
		GraphNode<V> current = nodes.get(label);
		for (GraphNode<V> node : current.getExcluded()) {
			exc.add(node.getLabel());
		}
		return exc;
	}
	
	
	/*
	 * SUPER IMPORTANT METHODS BELOW
	 */
	// TODO: Implement these methods
	/**
	 * Returns a copy of this graph that is sparsified
	 */
	public void sparsify() {
		
	}
	
	/**
	 * Densifies the graph. Preferably this method should be called on a copy of an original graph
	 * 
	 * @modifies everything about the graph
	 */
	public void densify() {
		for (V name : nodes.keySet()) {
			for (V supermem : getHierarchySuperset(name)) {
				for (V ex : getExcluded(supermem)) {
					addExclusion(name, ex);
				}
			}
			
			for (V submem : getHierarchySubset(name)) {
				addHierarchy(name, submem);
			}
		}
		checkInvariant();
	}

	
	/**
	 * Returns the number of nodes in the graph.
	 * 
	 * @return the number of nodes in the graph.
	 */
	public int size() {
		return nodes.size();
	}
	
	/**
	 * Returns whether the graph is empty or not.
	 * 
	 * @return True if the graph is empty, false if it isn't.	
	 */
	public boolean isEmpty(){
		return size() == 0;
	}
	
	/**
	 * Returns how many edges start at the given node.
	 * 
	 * @param node The label of the node that we are finding the degree of.
	 * @throws IllegalArgumentException if node is null.
	 * @return The degree of the node
	 */
	public int getDegree(V node){
		if(node == null){
			throw new IllegalArgumentException();
		}
		return nodes.get(node).getDegree();
	}
	
	/**
	 * Returns a list of all the nodes in the graph.
	 * 
	 * @return a list of all the nodes in the graph.
	 */
	public List<V> getNodes(){
		ArrayList<V> list = new ArrayList<V>();
		for(V n : nodes.keySet()){
			list.add(n);
		}
		return list;
	}
	
	/**
	 * Checks to make sure that exclusions are properly recognized by both partners that share the
	 * exclusion edge.
	 * 
	 * @throws IllegalStateException if the invariant is violated
	 */
	public void checkInvariant() {
		for (V label : nodes.keySet()) {
			nodes.get(label).checkInvariant();
		}
	}

	/**
	 * @author Daniel Gorrie
	 * 
	 * Implementation of a node to be used in a directed graph. Each GraphNode has a label that cannot
	 * be modified once the GraphNode is created, as well as a set of edges originating at the given
	 * GraphNode. There are no duplicate edges allowed.
	 * 
	 * Class invariant: label is never null, and that edges contains only valid GraphEdges.
	 * 
	 * Abstraction function: Nodes are entities that hold information and that are connected to
	 * each other by edges.
	 *
	 */
	@SuppressWarnings("hiding")
	class GraphNode<V> {
		private final V label;
		private float score;
		
		/**
		 * Set of nodes that this node is hierarchically above
		 */
		private Set<GraphNode<V>> hierarchy;
		
		/**
		 * Set of nodes this node is excluded from
		 */
		private Set<GraphNode<V>> excluded;
		
		/**
		 * Constructs a new GraphNode with the label passed in.
		 * 
		 * @param l The label of the new GraphNode
		 * @throws IllegalArgumentException if l is null.
		 */
		public GraphNode(V l) {
			if(l == null){
				throw new IllegalArgumentException();
			}
			this.label = l;
			hierarchy = new HashSet<GraphNode<V>>();
			excluded = new HashSet<GraphNode<V>>();
		}
		
		/**
		 * Returns the label of this.
		 * 
		 * @return The label of this.
		 */
		public V getLabel() {
			return label;
		}
		
		/**
		 * Returns whether this is one layer hierarchically above the given GraphNode
		 */
		public boolean hierarchyAdjacent(GraphNode<V> n) {
			return adjacencyHelper(n.getLabel(), hierarchy);
		}
		
		/**
		 * Returns whether this is excluded from the given GraphNode.
		 */
		public boolean exclusionAdjacent(GraphNode<V> n) {
			return adjacencyHelper(n.getLabel(), excluded);
		}
		
		/**
		 * Helper method for the exclusionAdjacent and hierarchyAdjacent methods.
		 */
		private boolean adjacencyHelper(V nLabel, Set<GraphNode<V>> selected) {
			for (GraphNode<V> v : selected) {
				if (v.getLabel().equals(nLabel)) return true;
			}
			return false;
		}
		
		/**
		 * Returns the relationship between this and the given node
		 * @return Relationship.{NONE, HIERARCHY_SUPER, EXCLUSION}
		 */
		public Relationship getRelationship(GraphNode<V> n) {
			if (getHierarchySuperset().contains(n)) {
				return Relationship.HIERARCHY_SUPER;
			} else if (getHierarchySubset().contains(n)) {
				return Relationship.HIERARCHY_SUB;
			} else if (excluded.contains(n)) {
				return Relationship.EXCLUSION;
			} else {
				return Relationship.OVERLAPPING;
			}
		}
		
		@SuppressWarnings("unchecked")
		public Set<GraphNode<V>> getHierarchySuperset() {
			Set<GraphNode<V>> set = new HashSet<GraphNode<V>>();
			try {
				for (V v : (Set<V>) nodes.keySet()) {
					if (nodes.get(v).getHierarchySubset().contains(this)) {
						set.add((GraphNode<V>)nodes.get(v));
					}
				}
			} catch (ClassCastException e) {
				System.err.println("Class catch exception caught in getHierarchySuperset");
				System.err.println(e.getMessage());
			} 
			return set;
		}
		
		
		public Set<GraphNode<V>> getHierarchySubset() {
			Set<GraphNode<V>> subset = new HashSet<GraphNode<V>>(hierarchy);
			for (GraphNode<V> node : hierarchy) {
				subset.addAll(node.getHierarchySubset());
			}
			return subset;
		}
		
		/**
		 * @return a copy of the excluded set
		 */
		public Set<GraphNode<V>> getExcluded() {
			return new HashSet<GraphNode<V>>(excluded);
		}
		
		/** 
		 * Adds an edge between this and node head with label label
		 * 
		 * @param head The node representing the head of the new edge.
		 * @return True if the edge was added successfully, false if the edge already existed.
		 */
		public boolean addHierarchyEdge(GraphNode<V> head) {
			return hierarchy.add(head);
		}
		
		public boolean addExclusionEdge(GraphNode<V> head) {
			return excluded.add(head);
		}
		
		/**
		 * Removes all edges going from this to node head.
		 * 
		 * @param head The node that is the head of all edges being removed.
		 * @return 
		 * @modifies this
		 * @effects Removes a set of edges.
		 */
		public boolean removeEdges(GraphNode<V> head) {
			edgeRemovalHelper(head, hierarchy);
			edgeRemovalHelper(head, excluded);
			return !excluded.contains(head) && !excluded.contains(head);
		}
		
		/**
		 * Helper method to remove edges from the two sets
		 * @param head
		 * @param selected
		 */
		private void edgeRemovalHelper(GraphNode<V> head, Set<GraphNode<V>> selected) {
			Iterator<GraphNode<V>> i = selected.iterator();
			while (i.hasNext()) {
				GraphNode<V> curr = i.next();
				if (curr.equals(head)) {
					i.remove();
				}
			}
		}
		
				
		/**
		 * Returns the number of edges that have this as their tail.
		 * 
		 * @return the number of edges that have this as their tail.
		 */
		public int getDegree(){
			return hierarchy.size() + excluded.size();
		}
		
		/**
		 * Check the invariant that no node can be in both excluded and hierarchy
		 * 
		 * @throws IllegalStateException if the invariant is violated
		 */
		public void checkInvariant() {
			for (GraphNode<V> node : hierarchy) {
				if (excluded.contains(node)) {
					throw new IllegalStateException(
							String.format("Invariant violated in node %s", getLabel()));
				}
			}
		}
		
		/**
		 * Returns whether this is equal to another GraphNode
		 * 
		 * @param o The node being compared against
		 * @return True if the node are equivalent, false otherwise
		 */
		public boolean equals(Object o) {
			if(o instanceof HEXGraph<?>.GraphNode<?>){
				HEXGraph<?>.GraphNode<?> node = (HEXGraph<?>.GraphNode<?>) o;
				return label.equals(node.label);
			}
			return false;
		}
		
		/**
		 * Returns the hash code for this object.
		 * 
		 * @return The hash code for this object.
		 */
		public int hashCode() {
			return label.hashCode();
		}
	}
	
	public enum Relationship {
		HIERARCHY,
		HIERARCHY_SUB,
		HIERARCHY_SUPER,
		EXCLUSION,
		OVERLAPPING;
	}
}
