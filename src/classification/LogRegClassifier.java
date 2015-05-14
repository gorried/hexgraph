package classification;

import java.util.HashSet;
import java.util.Set;

import util.SparseVector;

/**
 * Logistic regression classifier for the online learning portion of the hexgraph learning pathway.
 * This classifier does stochastic gradient descent on each update received.
 */
public class LogRegClassifier {
	private SparseVector w;
	private double eta;
	private double lambda;
	
	private double recent_score;

	
	public LogRegClassifier(int numFeatures, double e, double lam) {
		w = new SparseVector(numFeatures);
		eta = e;
		lambda = lam;
	}
	
	public LogRegClassifier(SparseVector weights, double e, double lam) {
		w = weights;
		eta = e;
		lambda = lam;
	}
	
	public void train(SparseVector instance) {
		// compute P(y = 1|w,x)
		double prob = w.dot(instance);
		prob = Math.exp(prob);
		prob /= (1 + prob);
		recent_score = prob;
	}
	
	public double getRecentScore() {
		return recent_score;
	}
	
	public double getRecentLabel() {
		if (recent_score > 0) {
			return 1;
		} else {
			return 0;
		}
	}

	
	// Pass in the gradient to update on
	// TODO: use an adaptive gradient
	public void update(SparseVector instance, double score, boolean lab) {
		int label = lab ? 1 : 0;
		SparseVector deltas = instance.scale(label - score);
		double w0Overwrite = w.get(0) + eta * deltas.get(0);
		
		Set<Integer> iterSet = new HashSet<Integer>();
		iterSet.addAll(w.nzindices());
		iterSet.addAll(deltas.nzindices());
		for (int i : iterSet) {
			double curr = w.get(i);
			w.put(i, curr - eta * (curr * lambda - deltas.get(i)));
		}
		w.put(0, w0Overwrite);
	}
	
	public double getBias() {
		return w.get(0);
	}
	
	public SparseVector getWeights() {
		return w;
	}
	
	public boolean getClassification(SparseVector test) {
		return (w.dot(test)) > 0;
	}
	
	public double getClassificationScore(SparseVector test) {
		return w.dot(test);
	}
	
	public double getLogLoss() {
		return 1;
	}
	
}
