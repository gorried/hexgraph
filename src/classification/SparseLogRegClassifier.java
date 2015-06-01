package classification;

import util.SparseMatrix;
import util.SparseVector;


/**
 * Logistic regression classifier for the online learning portion of the hexgraph learning pathway.
 * This classifier does stochastic gradient descent on each update received.
 */
public class SparseLogRegClassifier {
	private double[] w;
	private double eta;
	private double lambda;
	
	public SparseLogRegClassifier(int numFeatures, double e, double lam) {
		w = new double[numFeatures];
		eta = e;
		lambda = lam;
	}
	
	public SparseLogRegClassifier(double[] weights, double e, double lam) {
		w = weights;
		eta = e;
		lambda = lam;
	}
	
	public double[] train(SparseMatrix instances) {
		double[] probs = instances.dot(w);
		for (int i = 0; i < probs.length; i++) {
			probs[i] = Math.exp(probs[i]);
			probs[i] /= 1 + probs[i];
		}
		return probs;
	}

	// TODO: use an adaptive gradient
	public void update(SparseMatrix instances, double[] scores, SparseVector labels) {
		SparseVector deltas = instances.columnDot(labels.minus(scores)).scale(instances.getRows());
		// update the bias
		w[0] += eta * deltas.get(0);
		double biasSave = w[0];
		for (int i : deltas.nzindices()) {
			w[i] -= eta * (w[i] * lambda - deltas.get(i));
		}
		w[0] = biasSave;
	}
	
	public double[] getWeights() {
		return w;
	}
	
	public boolean getClassification(SparseVector instance) {
		return instance.dot(w) > 0.5;
	}
	
	public double getLogLoss(double[] scores, SparseVector labels) {
		double term_one = 0.5 * lambda * weightL2Norm();
	    double prob_sum = 0.0;
	    for (int i = 0; i < scores.length; i++) {
	    	double y_i = labels.get(i);
	    	if (y_i == -1) y_i = 0;
	        prob_sum += Math.log((1 - y_i) + (2 * y_i - 1) * scores[i]);
	    }
	    // System.out.println(String.format("term one: %f, prob_sum: %f, score: %f", term_one, prob_sum, scores[0]));
	    return term_one - prob_sum / scores.length;
	}
	
	/**
	 * Holy expensive method batman
	 */
	private double weightL2Norm() {
		double sum = 0.0;
        for (int i = 0; i < w.length; i++)
            sum += (w[i] * w[i]);
        return Math.sqrt(sum);
	}
}

