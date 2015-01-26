import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class Factor<V> {
	private Map<Configuration<V>, Double> distribution;
	private Set<V> variables;
	
	
	public Factor(Set<Configuration<V>> configSet, Map<V, Double> scoreMap) {
		this();
		for (Configuration<V> config : configSet) {
			if (variables == null) {
				variables = config.getKeySet();
			}
			double score = 1.0;
			for (V node : config.getKeySet()) {
				score *= Math.pow(Math.E, (config.get(node) == 1 ? 1 : 0) * scoreMap.get(node));
			}
			distribution.put(config, score);
		}
	}
	
	public Factor() {
		distribution = new HashMap<Configuration<V>, Double>();
		variables = null;
	}
	
	private Factor(Map<Configuration<V>, Double> dist, Set<V> vars) {
		distribution = dist;
		variables = vars;
	}
	
	public Factor<V> getDeepCopy() {
		
		return null;
	}
	
	public void addConfiguration(Configuration<V> config, double score) {
		if (variables == null) {
			variables = config.getKeySet();
		}
		// verify that this configuration is over the proper variables.
		if (config.getKeySet().equals(variables)) {
			distribution.put(config, score);
		}
	}
	
	/**
	 * Runs a sum over the certain subdistribtution
	 * @param vars
	 * @param scoreMap
	 * @return
	 */
	public Factor<V> getSubDistribution(Set<V> vars, Map<V, Double> scoreMap) {
		// Assert variables are in this factor
		for (V var : vars) {
			if (!variables.contains(var)) {
				throw new IllegalArgumentException("Illegal variables in factor subDistribution");
			}
		}
		// create a smaller factor over the possibilities for variables
		Factor<V> f = new Factor();
		// for each possible outcome, add the score
		
		return null;
	}
	
	public Factor<V> getDistributionProduct(Factor<V> separator) {
		
		return null;
	}
}
