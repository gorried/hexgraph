import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
	
	public Factor(Set<Configuration<V>> configSet) {
		this();
		for (Configuration<V> config : configSet) {
			if (variables == null) {
				variables = config.getKeySet();
			}
			distribution.put(config, 1.0);
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
		Map<Configuration<V>, Double> newDist = new HashMap<Configuration<V>, Double>();
		Set<V> newVars = new HashSet<V>();
		for (V var : variables) {
			newVars.add(var);
		}
		for (Configuration<V> config : distribution.keySet()) {
			newDist.put(config, distribution.get(config));
		}
		return new Factor<V>(newDist, newVars);
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
	public Factor<V> getSubDistribution(Set<V> vars) {
		// Assert variables are in this factor
		for (V var : vars) {
			if (!variables.contains(var)) {
				throw new IllegalArgumentException("Illegal variables in factor subDistribution");
			}
		}
		// create a smaller factor over the possibilities for variables
		Factor<V> newFactor = getDeepCopy();
		newFactor.variables = vars;
		
		for (Configuration<V> config : newFactor.distribution.keySet()) {
			config.trim(vars);
		}
		
		// for each possible outcome, add the score
		Iterator<Configuration<V>> it = newFactor.distribution.keySet().iterator();
		while (it.hasNext()) {
			Configuration<V> curr = it.next();
			for (Configuration<V> other : newFactor.distribution.keySet()) {
				if (curr.hasSameEntries(other) && !curr.equals(other)) {
					newFactor.distribution.put(other, newFactor.distribution.get(other) + newFactor.distribution.get(curr));
					it.remove();
				}
			}
		}
		
		return newFactor;
	}
	
	public void combineDistributionProduct(Factor<V> separator) {
		for (Configuration<V> config : this.distribution.keySet()) {
			for (Configuration<V> other : separator.distribution.keySet()) {
				// other will be the smaller factor than config
				if (other.isSubsumed(config)) {
					distribution.put(config, this.distribution.get(config) * separator.distribution.get(other));
				}
			}
		}
	}
}
