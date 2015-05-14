package hexgraph;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import util.NameSpace;



/**
 * @author Daniel Gorrie
 * 
 * This is a generic implementation of a Hierarchy and Exclusion Graph. There are two parts to
 * this implementation, the graph object, and a node object. Edges in this graph are implied
 * (Nodes store other nodes they connect to).
 * 
 * There are two types of edges in this graph. The first kind is a hierarchy edge, which
 * represents that a node is a subset of another class (for example, husky is a subset of dog). 
 * The second is an exclusion edge, which describes two classes that are exclusive to one another
 * (a mountain cannot also be an airplane)
 * 
 * This graph has a specific purpose of being used to represent relations between certain
 * classes for natural language entity recognition. We have implemented several functions specific
 * to this end, namely functions to sparsify, densify, enumerate the state space, and extract 
 * inferences between nodes in the graph.
 * 
 * For the purposes of documentation, nodes and classes are the same thing.
 */
public class HEXGraph<V> {
	
	private Map<V, GraphNode<V>> nodes;
	private NameSpace<V> mNameSpace;
	
	/**
	 * Default constructor. Constructs an empty HEXGraph
	 */
	public HEXGraph(NameSpace<V> nameSpace) {
		nodes = new HashMap<V, GraphNode<V>>();
		mNameSpace = nameSpace;
	}
	
	/**
	 * Returns a deep copy of the current graph
	 * @return a deep copy of the graph
	 */
	public HEXGraph<V> getDeepCopy() {
		HEXGraph<V> copy = new HEXGraph<V>(mNameSpace);
		Map<GraphNode<V>, GraphNode<V>> nodeMap = new HashMap<GraphNode<V>, GraphNode<V>>();
		// Initialize the new nodes, and keep a mapping from the old nodes to the new nodes
		for (V label : nodes.keySet()) {
			nodeMap.put(nodes.get(label), new GraphNode<V>(label));
		}
		
		// for each node, use the mapping between old and new to build up identical relations
		for (GraphNode<V> oldNode : nodeMap.keySet()) {
			GraphNode<V> newNode = nodeMap.get(oldNode);
			// update descendants
			for (GraphNode<V> descendant : oldNode.hierarchy) {
				newNode.hierarchy.add(nodeMap.get(descendant));
			}
			// update excluded
			for (GraphNode<V> exclude : oldNode.excluded) {
				newNode.excluded.add(nodeMap.get(exclude));
			}
			// update triangulated
			for (GraphNode<V> tri : oldNode.triangulated) {
				newNode.triangulated.add(nodeMap.get(tri));
			}
			
			newNode.score = oldNode.score;
			
			// Add the new node into the graph
			copy.nodes.put(newNode.getLabel(), newNode);
		}
		
		return copy;
	}
	
	/**
	 * Returns a subgraph containing only the specified nodes. If a node has an edge that is not
	 * part of the node subset, that edge is deleted
	 * @param nodeSubset the nodes in the new subgraph
	 * @return subgraph with only the specified nodes
	 */
	public HEXGraph<V> getSubgraph(Set<V> nodeSubset) {
		if (nodeSubset.isEmpty()) {
			return new HEXGraph<V>(mNameSpace);
		} else {			
			HEXGraph<V> subgraph = getDeepCopy();
			for (V node : subgraph.getNodeList()) {
				if (!nodeSubset.contains(node)) {
					subgraph.deleteNode(node);
				}
			}
			return subgraph;
		}
	}
	
	public HEXGraph<V> getSubgraph(BitSet nodeSubset) {
		if (nodeSubset.isEmpty()) {
			return new HEXGraph<V>(mNameSpace);
		} else {
			HEXGraph<V> subgraph = getDeepCopy();
			for (V node : subgraph.getNodeList()) {
				if (!nodeSubset.get(mNameSpace.getIndex(node))) {
					subgraph.deleteNode(node);
				}
			}
			return subgraph;
		}
	}
	
	/**
	 * Returns a subgraph containing EVERYTHING BUT the specified nodes. If a node has an edge
	 * that is past of the node subset.
	 * @param nodeSubset the nodes to not include in the new subgraph
	 * @return subgraph without the specified nodes
	 */
	public HEXGraph<V> getSubgraphMinus(Set<V> nodeSubset) {
		HEXGraph<V> subgraph = getDeepCopy();
		if (nodeSubset.isEmpty()) {
			return subgraph;
		} else {			
			for (V node : subgraph.getNodeList()) {
				if (nodeSubset.contains(node)) {
					subgraph.deleteNode(node);
				}
			}
			return subgraph;
		}
	}
	
	/**
	 * Adds a new node to the graph.
	 * 
	 * @param label The label of the node being added.
	 * @throws IllegalArgumentException if label is null.
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
	 * Adds a new hierarchy edge from parent to child.
	 * 
	 * We also add an undirected edge between the two for triangulation (no semantic meanint to
	 * this edge)
	 * 
	 * @param parent The label of the class that is the superset of child.
	 * @param child The label of the class that is the subset of parent.
	 * @return true if the edge is added successfully, false otherwise.
	 */
	public boolean addHierarchy(V parent, V child) {
		// Check to make sure that both things are actually members of the graph
		if (!(nodes.keySet().contains(parent) && nodes.keySet().contains(child))) return false;
		
		return nodes.get(parent).addHierarchyEdge(nodes.get(child)) &&
				addTriangulationRelationship(parent, child);
	}
	
	/**
	 * Adds an exclusion edge between two nodes. Order does not matter as the edge is undirected
	 * 
	 * We also add an undirected edge for triangulation. (No semantic meaning to this edge)
	 * 
	 * @param first The first node.
	 * @param second The second node.
	 * @return true if the edge is added successfully, false otherwise
	 */
	public boolean addExclusion(V first, V second) {
		// Check to make sure that both things are actually members of the graph
		if (!(nodes.keySet().contains(first) && nodes.keySet().contains(second))) return false;
		
		return nodes.get(first).addExclusionEdge(nodes.get(second)) && 
				nodes.get(second).addExclusionEdge(nodes.get(first)) &&
				addTriangulationRelationship(first, second);
	}
	
	/**
	 * Adds a triangulation edge between nodes first and second.
	 * 
	 * @param first The first node.
	 * @param second The second node.
	 * @return true if the edge is added successfully, false otherwise.
	 */
	public boolean addTriangulationRelationship(V first, V second) {
		if (!(nodes.keySet().contains(first) && nodes.keySet().contains(second))) return false;
		
		return nodes.get(first).addTriangulationEdge(nodes.get(second)) &&
				nodes.get(second).addTriangulationEdge(nodes.get(first));
	}

	/**
	 * Removes the node with the given label, as well as all edges coming from and going to it.
	 * 
	 * @param label The label of the node being removed
	 * @return True if the node and corresponding edges was removed successfully, false otherwise.
	 */
	public void deleteNode(V label) {
		GraphNode<V> toRemove = nodes.get(label);
		nodes.remove(label);
		for (V v : nodes.keySet()) {
			nodes.get(v).removeEdges(toRemove);
		}
	}

	/**
	 * Removes a hierarchy edge going from node parent to node child.
	 * 
	 * @param parent The parent node.
	 * @param child The child node.
	 * @return true if the edge was removed successfully, false otherwise.
	 */
	public boolean deleteHierarchyEdge(V parent, V child) {
		return nodes.get(parent).removeEdges(nodes.get(child));
	}
	
	/**
	 * Removes an exclusion edge between node first and node second.
	 * 
	 * @param first The first node.
	 * @param second The second node.
	 * @return true if the edge was removed successfully, false otherwise.
	 */
	public boolean deleteExclusion(V first, V second) {
		return nodes.get(first).removeEdges(nodes.get(second));
	}
	
	/**
	 * Determines if node child is a descendant of node parent
	 * 
	 * @param parent The parent class we are considering.
	 * @param child The potential child class.
	 * @throws IllegalArgumentException if either head or tail is null.
	 * @return true if child is a descendant of parent, false if it isn't.
	 */
	public boolean isDescendant(V parent, V child) {
		return nodes.get(parent).getDescendants().contains(nodes.get(child));
	}
	
	/**
	 * Determines if two classes are excluded from one another.
	 * 
	 * @param first One of the nodes we are testing
	 * @param second The other node we are testing
	 * @return true if first and second have an exclusion relationship, false otherwise.
	 */
	public boolean hasExclusion(V first, V second) {
		return nodes.get(first).getExcluded().contains(nodes.get(second));
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
			throw new IllegalArgumentException("Null passed in as node label in hasNode");
		}
		return nodes.containsKey(label);
	}
	
	/**
	 * Returns the ancestors of a node with the given label
	 * 
	 * @param label the label of the node we are finding ancestors for
	 * @return null if label is not in the graph. Otherwise returns a set of all the ancestors of
	 *  that node (in no particular order)
	 */
	public Set<V> getAncestors(V label) {
		if (hasNode(label)) {
			Set<V> set = new HashSet<V>();
			for (V node : nodes.keySet()) {
				if (nodes.get(node).getDescendants().contains(nodes.get(label))) {					
					set.add(node);
				}
			}
			return set;
		} else {
			return null;			
		}
	}
	
	/**
	 * Returns the descendants of the given node
	 * 
	 * @param label the label of the node we are finding descendants for
	 * @return null if label is not in the graph. Otherwise returns a set of all descendants of
	 * that node (in no particular order)
	 */
	public Set<V> getDescendants (V label) {
		if (hasNode(label)) {
			Set<V> set = new HashSet<V>();
			for (GraphNode<V> node : nodes.get(label).getDescendants()) {
				set.add(node.getLabel());
			}
			return set;
		} else {
			return null;
		}	
	}
	
	
	/**
	 * Returns a set of all nodes excluded from the node specified by label
	 * 
	 * @param label the node we are finding exclusions for
	 * @return a set of all classes excluded from the specified class
	 */
	public Set<V> getExcluded (V label) {
		if (hasNode(label)) {
			Set<V> exc = new HashSet<V>();
			GraphNode<V> current = nodes.get(label);
			for (GraphNode<V> node : current.getExcluded()) {
				exc.add(node.getLabel());
			}
			return exc;
		} else {
			return null;
		}
	}
	
	/**
	 * Returns a set of all nodes that the specified node has a triangulated relationship with.
	 * 
	 * @param label the node we are finding triangulated relationships for
	 * @return a set of all classes that are undirected neighbors for the specified class
	 */
	public Set<V> getTriangulatedNeighbors(V label) {
		if (hasNode(label)) {
			Set<V> tri = new HashSet<V>();
			for (GraphNode<V> node : nodes.get(label).getTriangulated()) {
				tri.add(node.getLabel());
			}
			return tri;
		} else {
			return null;
		}
	}
	
	/**
	 * Returns a set of all nodes that are not a part of a hierarchy or exclusion relation with
	 * the given node. 
	 * 
	 * @requires THIS IS ONLY GUARANTEED TO BE ACCURATE FOR A DENSIFIED GRAPH
	 * 
	 * @param label the node in question
	 * @return a set of all nodes that are not part unrelated to the given node
	 */
	public Set<V> getOverlapping(V label) {
		if (hasNode(label)) {			
			Set<V> overlapping = new HashSet<V>();
			Set<V> ancestors = getAncestors(label);
			Set<V> descendants = getDescendants(label);
			Set<V> excluded = getExcluded(label);
			
			for (V node : getNodeList()) {
				if (!ancestors.contains(node) &&
						!descendants.contains(node) &&
						!excluded.contains(node)) {
					overlapping.add(label);
				}
			}
			
			return overlapping;
		} else {
			return null;
		}
		
	}
	
	/**
	 * Sparsifies the graph. This method eliminates any redundant edges by examining hierarchy
	 * chains. For example if A is a parent of B and C, and B is a parent of C, we can say that
	 * A's ancestry of C is encoded in A->B->C. Thus the edge between A and C is deleted.
	 */
	public void sparsify() {
		for (V name : nodes.keySet()) {
			Set<V> ancestors = getAncestors(name);
			Set<V> excluded = getExcluded(name);
			for (V ancestor : ancestors) {
				// Remove unnecessary hierarchy edges
				for (V ancestorAncestor : getAncestors(ancestor)) {
					if (ancestors.contains(ancestorAncestor)) {
						deleteHierarchyEdge(ancestorAncestor, name);
					}
				}
				
				// Remove unnecessary exclusions
				for (V ancestorExcluded : getExcluded(ancestor)) {
					if (excluded.contains(ancestorExcluded)) {
						deleteExclusion(name, ancestorExcluded);
					}
				}
			}
			
		}
		checkInvariant();
	}
	
	/**
	 * This method densifies the graph. The opposite of sparsify, we look to see if there is any
	 * implied relationships in the graph, and if there are, we add edges to represent those
	 * relationships explicitly.
	 */
	public void densify() {
		for (V name : nodes.keySet()) {
			for (V ancestor : getAncestors(name)) {
				for (V ex : getExcluded(ancestor)) {
					addExclusion(name, ex);
				}
			}
			
			for (V descendant : getDescendants(name)) {
				addHierarchy(name, descendant);
			}
		}
		checkInvariant();
	}
	
	/**
	 * Triangulates the graph. This means that all cycles of size > 3 will be broken into cliques
	 * of size 3
	 */
	public void triangulate() {
		HEXGraph<V> copy = this.getDeepCopy();
		// iterate over the nodes in the copy, add triangulation edges in both graphs between all neighbors
		// that need to be triangulated and then remove the current node from the copy of the graph
		
		for (V label : copy.getNodeSet()) {
			for (V first : copy.getTriangulatedNeighbors(label)) {
				for (V second : copy.getTriangulatedNeighbors(label)) {
					if (!first.equals(second)) {
						copy.addTriangulationRelationship(first, second);
						this.addTriangulationRelationship(first, second);
					}
				}
			}
			copy.deleteNode(label);
		}
	}
	
	/**
	 * @requires that the graph has been triangulated for this to be somewhat useful
	 * @return an elimination ordering for the graph.
	 */
	public List<V> getEliminationOrdering() {
		Set<V> unmarked = getNodeSet();
		List<V> ordering = new ArrayList<V>();
		for (int i = this.size() - 1; i >= 0; i--) {
			V current = null;
			int maxMarked = -1;
			if (i == this.size()) {
				current = getNodeList().get(new Random().nextInt(this.size()));
			} else {
				for (V node : unmarked) {
					int markedNeighbors = 0;
					for (V neighbor : getTriangulatedNeighbors(node)) {
						if (!unmarked.contains(neighbor)) {
							markedNeighbors++;
						}
					}
					if (markedNeighbors > maxMarked) {
						current = node;
					}
				}
			}
			ordering.add(current);
			unmarked.remove(current);
		}
		Collections.reverse(ordering);
		return ordering;
	}

	
	/**
	 * Returns the number of nodes in the graph.
	 * @return the number of nodes in the graph.
	 */
	public int size() {
		return nodes.size();
	}
	
	/**
	 * Returns whether the graph is empty or not.
	 * @return True if the graph is empty, false if it isn't.	
	 */
	public boolean isEmpty() {
		return size() == 0;
	}
	
	/**
	 * Returns how many edges start at the given node.
	 * 
	 * @param node The label of the node that we are finding the degree of.
	 * @throws IllegalArgumentException if node is null.
	 * @return The degree of the node
	 */
	public int getDegree(V node) {
		if (node == null) {
			throw new IllegalArgumentException();
		}
		return nodes.get(node).getDegree();
	}
	
	/**
	 * Returns a list of all the nodes in the graph.
	 * @return a list of all the nodes in the graph.
	 */
	public List<V> getNodeList() {
		List<V> list = new ArrayList<V>();
		for (V n : nodes.keySet()){
			list.add(n);
		}
		return list;
	}
	
	/**
	 * Returns a set of all the nodes in the graph.
	 * @return a set of all the nodes in the graph.
	 */
	public Set<V> getNodeSet() {
		Set<V> set = new HashSet<V>();
		for (V n : nodes.keySet()) {
			set.add(n);
		}
		return set;
	}
	
	/**
	 * Returns the classification score for the given node
	 * @param node
	 * @return
	 */
	public double getScore(V node) {
		if (nodes.containsKey(node)) {			
			return nodes.get(node).getScore();
		}
		return 0.0;
	}
	
	/**
	 * Returns a double[] containing scores for this graph
	 * 
	 * TODO make the score integration more efficient
	 * 
	 * @return a double[] containing scores for this graph
	 */
	public double[] getScores() {
		double[] scores = new double[size()];
		for (int i = 0; i < size(); i++) {
			scores[i] = getScore(mNameSpace.get(i));
		}
		return scores;
	}
	
	/**
	 * Sets scores for all the nodes in the graph at once. If a node is provided in scores that is
	 * not in the graph we simply ignore it.
	 */
	public void setScores(Map<V, Double> scores) {
		for (V node : scores.keySet()) {
			if (nodes.containsKey(node)) {
				nodes.get(node).setScore(scores.get(node));
			}
		}
	}
	
	/**
	 * Sets scores for all the nodes in the graph at once. If scores contains more entries than
	 * there are nodes in the graph we ignore the extra entries.
	 * 
	 * @param scores - the double[] of scores we are assigning to the graph
	 */
	public void setScores(double[] scores) {
		for (int i = 0; i < scores.length; i++) {
			nodes.get(mNameSpace.get(i)).setScore(scores[i]);
		}
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
	 * More specifically, each GraphNode represents an entity recognition class in our uses of
	 * the HEXGraph.
	 *
	 * GraphNodes store two sets of edges, one representing their direct children in a hierarchy
	 * relationship, the other representing classes they are excluded from.
	 */
	@SuppressWarnings("hiding")
	class GraphNode<V> implements Serializable{
		
		private static final long serialVersionUID = -3158134210978319197L;
		
		/**
		 * Descriptor of the class
		 */
		private final V label;
		
		/**
		 * Score assigned to a node representing whether an entity we are considering can have
		 * this class or not
		 */
		private double score;
		
		/**
		 * Set of nodes that this node is hierarchically above (by one level)
		 */
		private Set<GraphNode<V>> hierarchy;
		
		/**
		 * Set of nodes this node is excluded from
		 */
		private Set<GraphNode<V>> excluded;
		
		/**
		 * Set of nodes this node has a triangulation relationship with (no semantic meaning)
		 */
		private Set<GraphNode<V>> triangulated;
		
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
			triangulated = new HashSet<GraphNode<V>>();
			score = 0.0;
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
		 * @return The confidence score of this node from the entity recognition processing
		 */
		public double getScore() {
			return score;
		}
		
		/**
		 * Sets the score value;
		 */
		public void setScore(double s) {
			score = s;
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
		 * Returns whether this is adjacent for the purposes of triangulation
		 */
		public boolean triangulationAdjacent(GraphNode<V> n) {
			return adjacencyHelper(n.getLabel(), triangulated);
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
		 * Returns the set of all descendants for this class (the entire subtree rooted at this
		 * class)
		 * 
		 * @return a set of all descendants of this node.
		 */
		public Set<GraphNode<V>> getDescendants() {
			Set<GraphNode<V>> subset = new HashSet<GraphNode<V>>(hierarchy);
			for (GraphNode<V> node : hierarchy) {
				subset.addAll(node.getDescendants());
			}
			return subset;
		}
		
		/**
		 * Returns a soft copy of the set of excluded nodes
		 * 
		 * @return a copy of the excluded set
		 */
		public Set<GraphNode<V>> getExcluded() {
			return new HashSet<GraphNode<V>>(excluded);
		}
		
		public Set<GraphNode<V>> getTriangulated() {
			return new HashSet<GraphNode<V>>(triangulated);
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
		
		public boolean addTriangulationEdge(GraphNode<V> head) {
			return triangulated.add(head);
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
			edgeRemovalHelper(head, triangulated);
			if (triangulated.contains(head) || hierarchy.contains(head) || excluded.contains(head)) {
				throw new IllegalStateException("Node not actually removed");
			}
			return !excluded.contains(head) && !triangulated.contains(head);
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
		 * @return The hash code for this object.
		 */
		public int hashCode() {
			return label.hashCode();
		}
	}
	
}
