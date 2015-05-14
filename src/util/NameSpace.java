package util;

import java.util.HashMap;
import java.util.Map;

public class NameSpace<V> {
	private V[] mClassNames;
	private Map<V, Integer> mNameMapping;

	public NameSpace(V[] names) {
		mClassNames = names;
		mNameMapping = new HashMap<V, Integer>();
		for (int i = 0; i < mClassNames.length; i++) {
			mNameMapping.put(mClassNames[i], i);
		}
	}
	
	public V[] getNames() {
		return mClassNames;
	}
	
	public V get(int idx) {
		return mClassNames[idx];
	}
	
	public int getIndex(V query) {
		return mNameMapping.get(query);
	}
	
	public int size() {
		return mClassNames.length;
	}
}
