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
		
	}
