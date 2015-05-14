package hexgraph;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import util.TriState;


public class Factor {
	private Map<Configuration, Double> mDistribution;
	private BitSet mMemberClasses;
	
	private final double DEFAULT_SCORE = 1.0;
	
	// scores must be the length of the total number of classes
	public Factor(Set<Configuration> configSet, BitSet memberClasses, double[] scores) {
		this();
		mMemberClasses = memberClasses;
		for (Configuration config : configSet) {
			if (!config.getMembers().equals(mMemberClasses)) {
				throw new IllegalStateException("Configuration not over the proper variables");
			}
			
			double score = 1.0;
			for (int i = 0; i < scores.length; i++) {
				score += (config.get(i) == TriState.TRUE ? 1 : 0) * scores[i];
			}
			mDistribution.put(config, score);
		}
	}
	
	public Factor(Set<Configuration> configSet, BitSet memberClasses) {
		this();
		mMemberClasses = memberClasses;
		for (Configuration config : configSet) {
			if (!config.getMembers().equals(mMemberClasses)) {
				throw new IllegalStateException("Configuration not over the proper variables");
			}
			mDistribution.put(config, DEFAULT_SCORE);
		}
	}
	
	public Factor() {
		mDistribution = new HashMap<Configuration, Double>();
		mMemberClasses = new BitSet();
	}
	
	private Factor(Map<Configuration, Double> dist, BitSet members) {
		mDistribution = dist;
		mMemberClasses = members;
	}
	
	public Map<Configuration, Double> getDist() {
		return mDistribution;
	}
	
	public Factor getDeepCopy() {
		Map<Configuration, Double> newDist = new HashMap<Configuration, Double>();
		BitSet newMembers = (BitSet) mMemberClasses.clone();
		for (Configuration config : mDistribution.keySet()) {
			newDist.put(config.getDeepCopy(), mDistribution.get(config));
		}
		return new Factor(newDist, newMembers);
	}
	
	public void addConfiguration(Configuration config, double score) {
		if (mMemberClasses.isEmpty()) {
			mMemberClasses = config.getMembers();
		} else if (!mMemberClasses.equals(config.getMembers())) {
			throw new IllegalArgumentException("New config is over the wrong variables");
		}
		mDistribution.put(config, score);
	}
	
	public BitSet getVariables() {
		return (BitSet) mMemberClasses.clone();
	}
	
	/**
	 * Runs a sum over the certain subdistribtution
	 * @param vars
	 * @param scoreMap
	 * @return
	 */
	public Factor getSubDistribution(BitSet indices) {
		// Assert variables are in this factor
		BitSet copyIndices = (BitSet) indices.clone();
		copyIndices.andNot(mMemberClasses);
		if (!copyIndices.isEmpty()) {
			throw new IllegalArgumentException("Subdistribution over unset variables");
		}
		// create a smaller factor over the possibilities for variables
		Factor newFactor = new Factor();
		newFactor.mMemberClasses = indices;
		
		
		for (Configuration config : mDistribution.keySet()) {
			double d = mDistribution.get(config);
			Configuration trimmed = config.trimTo(indices);
			if (newFactor.mDistribution.containsKey(trimmed)) {
				newFactor.mDistribution.put(trimmed, logSumOfExponentials(d, newFactor.mDistribution.get(trimmed)));
			} else {	
				newFactor.mDistribution.put(trimmed, d);
			}
		}
		return newFactor;
	}
	
	public void combineDistributionProduct(Factor separator) {
		for (Configuration config : this.mDistribution.keySet()) {
			for (Configuration other : separator.mDistribution.keySet()) {
				// other will be the smaller factor than config
				if (other.isSubsumed(config)) {
					mDistribution.put(config, this.mDistribution.get(config) + separator.mDistribution.get(other));
				}
			}
		}
	}
	
	public Factor divide(Factor other) {
		Factor newFactor = getDeepCopy();
		for (Configuration config : newFactor.mDistribution.keySet()) {
			for (Configuration otherConfig : other.mDistribution.keySet()) {
				if (config.hasSameEntries(otherConfig)) {
					newFactor.mDistribution.put(config, newFactor.mDistribution.get(config) - other.mDistribution.get(otherConfig));
				}
			}
		}
		return newFactor;
	}
	
	public double getScoreIfSubsumed(Configuration query) {
		double sum = 0.0;
		for (Configuration config : mDistribution.keySet()) {
			if (query.isSubsumed(config)) {
				sum += mDistribution.get(config);
			}
		}
		return sum == 0.0 ? 1 : sum;
	}
	
	 public static double logSumOfExponentials(double a, double b) {
        double max = a > b ? a : b;
        double sum = 0.0;
        sum += Math.exp(a - max);
        sum += Math.exp(b - max);
        return max + java.lang.Math.log(sum);
    }

	
	public void print(String name) {
		System.out.println("PRINTING FACTOR "+ name + ":");
		for (Configuration config : mDistribution.keySet()) {
			System.out.println(config.toString() + ": " + mDistribution.get(config));
		}
		System.out.println("------");
	}
	
	public void print(String name, BitSet members) {
		System.out.println("PRINTING FACTOR "+ name + " with members " + members.toString() + ":");
		for (Configuration config : mDistribution.keySet()) {
			System.out.println(config.toString() + ": " + mDistribution.get(config));
		}
		System.out.println("------");
	}
}
