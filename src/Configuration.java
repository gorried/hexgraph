import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

class Configuration<V> implements Serializable {
		
		private static final long serialVersionUID = -6817749231285265526L;
		private Map<V, Integer> config;

		public Configuration() {
			config = new HashMap<V, Integer>();
		}
		
		public Configuration(Set<V> classes) {
			this();
			// initialize the values to -1 so we can determine whether we have put a final
			// configuration value for a given keys
			for (V item : classes) {
				config.put(item, -1);
			}
		}
		
		/**
		 * Returns a deep copy of this configuration if it is of type string.
		 * 
		 * @return a deep copy of this configuration
		 */
		public Configuration<V> getDeepCopy() {
			Configuration<V> copy = new Configuration<V>();
			for (V item : config.keySet()) {
				copy.config.put(item, config.get(item));
			}
			return copy;
		}
		
		public boolean setValues(Set<V> classes, int val) {
			for (V item : classes) {
				int curr = config.get(item);
				if (!(curr == -1 || curr == val)) {
					return false;
				} else {
					config.put(item, val);
				}
			}
			return true;
		}

		public boolean setValues(V item, int val) {
			int curr = config.get(item);
			if (!(curr == -1 || curr == val)) {
				return false;
			} else {
				config.put(item, val);
				return true;
			}
		}
		
		public String toString() {
			String s = "Configuration: \n";
			for (V node : config.keySet()) {
				s += node.toString() + ": " + config.get(node) + "\n";
			}
			return s;
		}

	}
