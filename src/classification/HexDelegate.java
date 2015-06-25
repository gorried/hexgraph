package classification;

import hexgraph.Configuration;
import hexgraph.HEXGraphMethods;
import hexgraph.JunctionTree;
import hexgraph.JunctionTreeNode;

import java.util.Map;
import java.util.Set;

import util.NameSpace;

public class HexDelegate extends Thread{
	private HEXGraphMethods mHexGraphMethods;
	private double[] mScores;
	private JunctionTree<String> mJunctionTree;
	private Map<JunctionTreeNode<String>, Set<Configuration>> mJunctionTreeStateSpace;
	private NameSpace<String> mNameSpace;
	private ThreadedHexRunner mThreadedHexRunner;
	private int mCol;
	
	public HexDelegate(
			HEXGraphMethods hexGraphMethods,
			double[] scores,
			JunctionTree<String> junctionTree,
			Map<JunctionTreeNode<String>, Set<Configuration>> junctionTreeStateSpace,
			NameSpace<String> nameSpace,
			ThreadedHexRunner threadedHexRunner,
			int col) {
		
		mHexGraphMethods = hexGraphMethods;
		mScores = scores;
		mJunctionTree = junctionTree;
		mJunctionTreeStateSpace = junctionTreeStateSpace;
		mNameSpace = nameSpace;
		mThreadedHexRunner = threadedHexRunner;
		mCol = col;
	}
	
	@Override
	public void run() {
		Map<String, Double> results = mHexGraphMethods.exactMarginalInference(mJunctionTree, mJunctionTreeStateSpace, mScores);
		
		double[] scores = new double[mScores.length];
		double max = -1;
		for (int i = 0; i < scores.length; i++) {
			scores[i] = results.get(mNameSpace.get(i));
			if (scores[i] > max) max = scores[i];
		}
		
		// OLD REGULARIZATION
		for (int i = 0; i < scores.length; i++) {
			scores[i] = scores[i] / max;
		}
		
		mThreadedHexRunner.onThreadTerminate(mCol, scores);
	}
}
