package classification;

import hexgraph.Configuration;
import hexgraph.HEXGraphMethods;
import hexgraph.JunctionTree;
import hexgraph.JunctionTreeNode;

import java.util.Map;
import java.util.Set;

import util.NameSpace;

public class HexDelegate implements Runnable{
	private HEXGraphMethods mHexGraphMethods;
	private double[] mScores;
	private JunctionTree<String> mJunctionTree;
	private Map<JunctionTreeNode<String>, Set<Configuration>> mJunctionTreeStateSpace;
	private NameSpace<String> mNameSpace;
	
	public HexDelegate(
			HEXGraphMethods hexGraphMethods,
			double[] scores, // scores will be returned here
			JunctionTree<String> junctionTree,
			Map<JunctionTreeNode<String>, Set<Configuration>> junctionTreeStateSpace,
			NameSpace<String> nameSpace) {
		
		mHexGraphMethods = hexGraphMethods;
		mScores = scores;
		mJunctionTree = junctionTree;
		mJunctionTreeStateSpace = junctionTreeStateSpace;
		mNameSpace = nameSpace;
	}
	
	public void run() {
		Map<String, Double> results = mHexGraphMethods.exactMarginalInference(mJunctionTree, mJunctionTreeStateSpace, mScores);
		
		double max = -1;
		for (int i = 0; i < mScores.length; i++) {
			mScores[i] = results.get(mNameSpace.get(i));
			if (mScores[i] > max) max = mScores[i];
		}
		
		// OLD REGULARIZATION
		for (int i = 0; i < mScores.length; i++) {
			mScores[i] = mScores[i] / max;
		}
		
	}
}
