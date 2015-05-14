package hexgraph;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import util.NameSpace;

//TODO better integration of bitset
public class JunctionTreeNode<V> {
	private BitSet mMembers;
	private Set<JunctionTreeEdge<V>> mEdges;
	private Factor mFactor;
	private int mNumClasses;
	
	private final NameSpace<V> mNameSpace;
	
	public JunctionTreeNode(Set<V> mems, int numClasses, NameSpace<V> nameSpace) {
		mEdges = new HashSet<JunctionTreeEdge<V>>();
		mFactor = new Factor();
		mMembers = new BitSet(numClasses);
		mNameSpace = nameSpace;
		for (V v : mems) {
			mMembers.set(mNameSpace.getIndex(v));
		}
		mNumClasses = numClasses;
	}
	
	public void addEdge(JunctionTreeEdge<V> edge) {
		mEdges.add(edge);
	}
	
	public Set<JunctionTreeEdge<V>> getEdges() {
		return mEdges;
	}
	
	public void removeEdge(JunctionTreeNode<V> other) {
		Iterator<JunctionTreeEdge<V>> it = mEdges.iterator();
		while (it.hasNext()) {
			if (it.next().getOther(this).equals(other)) {
				it.remove();
			}
		}
	}
	
	public Factor getFactor() {
		return mFactor.getDeepCopy();
	}
	
	public BitSet getMembers() {
		return (BitSet) mMembers.clone();
	}
	
	public BitSet getOverlappingSet(JunctionTreeNode<V> other) {
		BitSet otherMembers = other.getMembers();
		otherMembers.and(mMembers);
		return otherMembers;
	}
	
	public int getOverlap(JunctionTreeNode<V> other) {
		return getOverlappingSet(other).cardinality();
	}
	
	public Set<JunctionTreeNode<V>> getNeighbors() {
		Set<JunctionTreeNode<V>> neighbors = new HashSet<JunctionTreeNode<V>>();
		for (JunctionTreeEdge<V> edge : mEdges) {
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
	Factor collectMessages(JunctionTreeNode<V> parent,
			Map<JunctionTreeNode<V>, Set<Configuration>> stateSpaces,
			double[] scores) {
		// base case: this node doesnt have any neighbors that arent parent (leaf)
		mFactor = new Factor(stateSpaces.get(this), getMembers(), scores);
		if (mEdges.size() == 1 && parent != null) {
			// we know that the only neighbor is parent
			return mFactor.getDeepCopy();
		} else {
			Set<Factor> separators = new HashSet<Factor>();
			for (JunctionTreeEdge<V> outEdge : getEdges()) {
				if (!outEdge.contains(parent)) {
					separators.add(outEdge.collectMessages(this, stateSpaces, scores));
				}
			}
			// Calculate scores and put them into a new factor
			for (Factor separator : separators) {
				mFactor.combineDistributionProduct(separator);
			}
			return mFactor.getDeepCopy();
		}	
	}
	
	/**
	 * Once the messages have been collected at the root, we propagate them outwards to the leaves
	 */
	void propagateMessages(JunctionTreeEdge<V> parent, Factor properFactor) {
		if (parent != null) {
			mFactor.combineDistributionProduct(properFactor);
		}
		for (JunctionTreeEdge<V> edge : getEdges()) {
			if (!edge.equals(parent)) {
				edge.propagateMessages(this);
			}
		}	
	}
	
	public String toString() {
		return mMembers.toString();
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object o) {
		if (o instanceof JunctionTreeNode && ((JunctionTreeNode<V>) o).mMembers.equals(this.mMembers)) {
			return true;
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return mMembers.hashCode();
	}
}
