package util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Sparse vector class, which stores information on nonzero indices. The sparse vector assumes
 * type double so that math can be performed on it
 *
 */
public class SparseVector {
	private int N; // length
	private Map<Integer, Double> map;
	
	public SparseVector(int length) {
		this.N = length;
		this.map = new HashMap<Integer, Double>();
	}
	
	public SparseVector subVector(int from, int to) {
		SparseVector res = new SparseVector(to - from);
		if (nnz() < from - to) {
			for (int i : nzindices()) {
				if (i >= from && i < to) {
					res.put(i - from, get(i));
				}
			}
		} else {			
			for (int i = from; i < to; i++) {
				res.put(i - from, get(i));
			}
		}
		return res;
	}
	
	// put st[i] = value
    public void put(int i, double value) {
        if (i < 0 || i >= N) throw new RuntimeException("Illegal index");
        if (value == 0.0) map.remove(i);
        else              map.put(i, value);
    }

    // return st[i]
    public double get(int i) {
        if (i < 0 || i >= N) throw new RuntimeException("Illegal index");
        if (map.containsKey(i)) return map.get(i);
        else                return 0.0;
    }

    // return the number of nonzero entries
    public int nnz() {
        return map.size();
    }

    // return the size of the vector
    public int size() {
        return N;
    }

    // return the dot product of this vector a with b
    public double dot(SparseVector b) {
        SparseVector a = this;
        if (a.N != b.N) throw new RuntimeException("Vector lengths disagree: " + a.N + " " + b.N);
        double sum = 0.0;

        // iterate over the vector with the fewest nonzeros
        if (a.map.size() <= b.map.size()) {
            for (int i : a.map.keySet())
                if (b.map.containsKey(i)) sum += a.get(i) * b.get(i);
        }
        else  {
            for (int i : b.map.keySet())
                if (a.map.containsKey(i)) sum += a.get(i) * b.get(i);
        }
        return sum;
    }
    
    public double dot(double[] b) {
    	SparseVector a = this;
        if (a.N != b.length) throw new RuntimeException("Vector lengths disagree: " + a.N + " " + b.length);
        double sum = 0.0;
        for (int i : a.map.keySet())
            sum += a.get(i) * b[i];
        return sum;
    }
    
    public Set<Integer> nzindices() {
    	return map.keySet();
    }

    // return the 2-norm
    public double norm() {
        SparseVector a = this;
        return Math.sqrt(a.dot(a));
    }

    // return alpha * a
    public SparseVector scale(double alpha) {
        SparseVector a = this;
        SparseVector c = new SparseVector(N);
        for (int i : a.map.keySet()) c.put(i, alpha * a.get(i));
        return c;
    }

    // return a + b
    public SparseVector plus(SparseVector b) {
        SparseVector a = this;
        if (a.N != b.N) throw new RuntimeException("Vector lengths disagree");
        SparseVector c = new SparseVector(N);
        for (int i : a.map.keySet()) c.put(i, a.get(i));                // c = a
        for (int i : b.map.keySet()) c.put(i, b.get(i) + c.get(i));     // c = c + b
        return c;
    }
    
 // return a - b
    public SparseVector minus(SparseVector b) {
        SparseVector a = this;
        if (a.N != b.N) throw new RuntimeException("Vector lengths disagree");
        SparseVector c = new SparseVector(N);
        for (int i : a.map.keySet()) c.put(i, a.get(i));                // c = a
        for (int i : b.map.keySet()) c.put(i, c.get(i) - b.get(i));     // c = c - b
        return c;
    }
    
    public SparseVector minus(double[] b) {
        SparseVector a = this;
        if (a.N != b.length) throw new RuntimeException("Vector lengths disagree");
        SparseVector c = new SparseVector(N);
        for (int i : a.map.keySet()) c.put(i, a.get(i));                // c = a
        for (int i = 0; i < b.length; i++) c.put(i, c.get(i) - b[i]);     // c = c - b
        return c;
    }

    // return a string representation
    public String toString() {
        String s = "";
        for (int i : map.keySet()) {
            s += "(" + i + ", " + map.get(i) + ") ";
        }
        return s;
    }
}
