package classification;

import hexgraph.Configuration;
import hexgraph.HEXGraphMethods;
import hexgraph.JunctionTree;
import hexgraph.JunctionTreeNode;

import java.util.ArrayList;
import java.util.List;
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
	
	private double[][] mScores;
	
	private int mNextRun = 0;
	
	private final int NUM_THREADS = 10;
	
	@SuppressWarnings("unchecked")
	public ThreadedHexRunner(
			HEXGraphMethods hexGraphMethods,
			JunctionTree<String> junctionTree,
			NameSpace<String> nameSpace,
			int numClassifiers) {
		
		mExecutorService = Executors.newFixedThreadPool(NUM_THREADS);
		mNameSpace = nameSpace;
		
		mHexGraphMethods = new HEXGraphMethods[NUM_THREADS];
		mJunctionTrees = (JunctionTree<String>[]) new JunctionTree[NUM_THREADS];
		mJunctionTreeStateSpaces = (Map<JunctionTreeNode<String>, Set<Configuration>>[]) new Map[NUM_THREADS];
		
		// Make NUM_THREADS instances of the proper JunctionTree and HexGraphMethods using deep copy
		// and store them in a field
		
		for (int i = 0; i < NUM_THREADS; i++) {
			mHexGraphMethods[i] = hexGraphMethods.getDeepCopy();
			mJunctionTrees[i] = junctionTree.getDeepCopy();
			mJunctionTreeStateSpaces[i] = hexGraphMethods.getJunctionTreeStateSpaces(junctionTree);
		}
		
	}

	/**
	 * 
	 * @param scores an array of dims [mClassifiers.length][mbatch_size]
	 * @return
	 */
	public double[][] process(double[][] scores) {
		List<Thread> threads = new ArrayList<Thread>(scores[0].length);
		mScores = scores;
		for (int i = 0; i < scores[0].length; i++) {			
			try {
				double[] instanceScore = new double[scores.length];
				for (int c = 0; c < scores.length; c++) {
					instanceScore[c] = scores[c][i];
				}
				Thread task = new HexDelegate(
						mHexGraphMethods[mNextRun],
						instanceScore,
						mJunctionTrees[mNextRun],
						mJunctionTreeStateSpaces[mNextRun],
						mNameSpace,
						this,
						i);
				threads.add(task);
				mExecutorService.execute(task);
				mNextRun++;
				mNextRun %= NUM_THREADS;
				// System.out.println("Next run: " + mNextRun);
			} catch (Exception e) {
				e.printStackTrace();
				if (mExecutorService != null) {
					mExecutorService.shutdown();
				}
			}
		}
		for (Thread thread : threads) {
			try {				
				thread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return mScores;
	}
	
	public void onThreadTerminate(int col, double[] scores) {
		for (int c = 0; c < scores.length; c++) {
			mScores[c][col] = scores[c];
		}
	}
	
	public void shutdown() {
		mExecutorService.shutdown();
		mExecutorService = null;
	}
}

