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
	
	public void addConfiguration(Configuration<V> config, double score) {
		if (variables == null) {
			variables = config.getKeySet();
		}
		// verify that this configuration is over the proper variables.
		if (config.getKeySet().equals(variables)) {
			distribution.put(config, score);
		}
	}
	
	public Set<V> getOverlap(Set<V> other) {
		Set<V> overlap = new HashSet<V>();
		for (V variable : variables) {
			if (other.contains(variable)) {
				overlap.add(variable);
			}
		}
		return overlap;
	}
	
	public double getSumOfSet(V variable) {
		double sum = 0.0;
		if (variables.contains(variable)) {
			for (Configuration<V> config : distribution.keySet()) {
				if (config.isSet(variable)) {
					sum += distribution.get(variable);
				}
			}			
		}
		return sum;
	}
	
	public double getSumOfUnset(V variable) {
		double sum = 0.0;
		if (variables.contains(variable)) {
			for (Configuration<V> config : distribution.keySet()) {
				if (!config.isSet(variable)) {
					sum += distribution.get(variable);
				}
			}			
		}
		return sum;
	}
}
