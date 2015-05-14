package hexgraph;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Map;
import java.util.Set;

import util.TriState;

/**
 * The Configuration class represents a single configuration within a probabilistic distribution.
 * 
 * This specific implementation relies on a global name-space to map between string class name and
 * an integer index that we will use to specify whether that class is part of the distribution,
 * and if so, whether it is set to true or false. 
 *
 */
public class Configuration {
	public static final int CONFIG_FALSE = 0;
	public static final int CONFIG_TRUE = 1;
	public final int CONFIG_UNSET = 2;
	
	private TriState[] mConfig;
	private BitSet mMemberList;
	private BitSet mBitwiseConfig;

	public Configuration() {
		mConfig = new TriState[10];
		mMemberList = new BitSet();
		mBitwiseConfig = new BitSet();
	}
	
	public Configuration(int numClasses) {
		mConfig = new TriState[numClasses];
		for (int i = 0; i < mConfig.length; i++) {
			mConfig[i] = TriState.UNSET;
		}
		mMemberList = new BitSet(numClasses);
		mBitwiseConfig = new BitSet(numClasses);
	}
	
	public Configuration(int numClasses, int[] set) {
		this(numClasses);
		setValues(set, TriState.TRUE);
	}
	
	public Configuration(TriState[] config) {
		mConfig = config;
		mMemberList = new BitSet(config.length);
		mBitwiseConfig = new BitSet(config.length);
		for (int i = 0; i < config.length; i++) {
			switch (config[i]) {
			case TRUE:
				mBitwiseConfig.set(i);
			case FALSE:
				mMemberList.set(i);
				break;
			default: break;
			}
		}
	}
	
	public boolean contains(int idx) {
		return mConfig[idx] != TriState.UNSET;
	}
	
	public boolean isSet(int idx) {
		return mConfig[idx] == TriState.TRUE;
	}
	
	public TriState get(int idx) {
		return mConfig[idx];
	}
	
	public BitSet getMembers() {
		return (BitSet) mMemberList.clone();
	}
	
	public BitSet getBitwiseConfig() {
		return (BitSet) mBitwiseConfig.clone();
	}
	
	public Configuration getDeepCopy() {
		return new Configuration(Arrays.copyOf(mConfig, mConfig.length));
	}
	
	public int size() {
		return mConfig.length;
	}
	
	public void setValues(int[] indices, TriState val) {
		for (int i : indices) {
			setValue(i, val);
		}
	}
	
	public void setValues(Set<Integer> indices, TriState val) {
		for (int i : indices) {
			setValue(i, val);
		}
	}
	
	public void setValue(int idx, TriState val) {
		mConfig[idx] = val;
		switch (val) {
		case TRUE:
			mBitwiseConfig.set(idx);
			mMemberList.set(idx);
			break;
		case FALSE:
			mBitwiseConfig.clear(idx);
			mMemberList.set(idx);
			break;
		case UNSET:
			mBitwiseConfig.clear(idx);
			mMemberList.clear(idx);
			break;
		}
	}
	
	/**
	 * Not a good method to call on this implementation
	 */
	public boolean containsMapping(Map<Integer, TriState> mapping) {
		for (Integer idx : mapping.keySet()) {
			if (mapping.get(idx) != mConfig[idx]) return false;
		}
		return true;
	}
	
	/**
	 * Checks to see if the two configurations have the same entries AND that the entries are 
	 * set to the same value
	 */
	public boolean hasSameEntries(Configuration other) {
		BitSet checkConfig = other.getBitwiseConfig();
		checkConfig.xor(mBitwiseConfig);
		
		BitSet checkMembers = other.getMembers();
		checkMembers.xor(mMemberList);
		
		return checkConfig.isEmpty() && checkMembers.isEmpty();
	}
	
	/**
	 * Returns a new configuration that is trimmed such that the only indices in the configuration
	 * are in the indices passed in.
	 */
	public Configuration trimTo(BitSet indices) {
		Configuration newConfig = getDeepCopy();
		for (int i = 0; i < mConfig.length; i++) {
			if (!indices.get(i)) newConfig.setValue(i, TriState.UNSET);
		}
		return newConfig;
	}
	
	/**
	 * Returns true if this configuration is a complete subset of configuration other. To be a
	 * complete subset, for each enabled index in this, other must have that index enabled, and
	 * with the same value. 
	 */
	public boolean isSubsumed(Configuration other) {
		BitSet checkConfig = getBitwiseConfig();
		checkConfig.andNot(other.getBitwiseConfig());
		
		BitSet checkMembers = getMembers();
		checkMembers.andNot(other.getMembers());
		
		return checkConfig.isEmpty() && checkMembers.isEmpty();
	}
	
	@Override
	public boolean equals(Object o) {
		return (o instanceof Configuration && Arrays.equals(((Configuration) o).mConfig, mConfig));
	}
	
	@Override
	public int hashCode() {
		return Arrays.hashCode(mConfig);
	}
	
	public String toString() {
		String s = String.format("Configuration (%d): ", size());
		for (int i = 0; i < mConfig.length; i++) {
			switch (mConfig[i]) {
			case TRUE:
				s += CONFIG_TRUE;
				break;
			case FALSE:
				s += CONFIG_FALSE;
				break;
			case UNSET:
				s += CONFIG_UNSET;
				break;
			}
		}
		return s;
	}
}
