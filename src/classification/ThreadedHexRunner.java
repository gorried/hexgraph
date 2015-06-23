package classification;

import hexgraph.Configuration;
import hexgraph.HEXGraphMethods;
import hexgraph.JunctionTree;
import hexgraph.JunctionTreeNode;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import util.NameSpace;


public class ThreadedHexRunner {
	private ExecutorService mExecutorService;
	private NameSpace<String> mNameSpace;
	
	private HEXGraphMethods[] mHexGraphMethods;
	private JunctionTree<String>[] mJunctionTrees;
	private Map<JunctionTreeNode<String>, Set<Configuration>>[] mJunctionTreeStateSpaces;
	
	private int mNextRun = 0;
	
	private final int NUM_THREADS = 15;
	
	@SuppressWarnings("unchecked")
	public ThreadedHexRunner(
			HEXGraphMethods hexGraphMethods,
			JunctionTree<String> junctionTree,
			NameSpace<String> nameSpace) {
		mExecutorService = Executors.newFixedThreadPool(NUM_THREADS);
		mNameSpace = nameSpace;
		
		mHexGraphMethods = new HEXGraphMethods[NUM_THREADS];
		mJunctionTrees = (JunctionTree<String>[]) new Object[NUM_THREADS];
		mJunctionTreeStateSpaces = (Map<JunctionTreeNode<String>, Set<Configuration>>[]) new Object[NUM_THREADS];
		
		// Make NUM_THREADS instances of the proper JunctionTree and HexGraphMethods using deep copy
		// and store them in a field
		
		for (int i = 0; i < NUM_THREADS; i++) {
			mHexGraphMethods[i] = hexGraphMethods.getDeepCopy();
			mJunctionTrees[i] = junctionTree.getDeepCopy();
			mJunctionTreeStateSpaces[i] = hexGraphMethods.getJunctionTreeStateSpaces(junctionTree);
		}
		
	}

	public double[] process(double[] scores) {
		try {
			mExecutorService.execute(new HexDelegate(mHexGraphMethods[mNextRun],
					scores, mJunctionTrees[mNextRun], mJunctionTreeStateSpaces[mNextRun], mNameSpace));
			mNextRun++;
			mNextRun %= NUM_THREADS;
		} catch (Exception e) {
			if (mExecutorService != null) {
				mExecutorService.shutdown();
			}
		}
		return scores;
	}
	
	public void shutdown() {
		mExecutorService.shutdown();
		mExecutorService = null;
	}
}

