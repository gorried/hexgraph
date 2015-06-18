package util;

import java.util.Arrays;
import java.util.Random;
import java.util.Set;

public class SparseMatrix {
    private final int N;          
    private SparseVector[] rows;   // the rows, each row is a sparse vector

    public SparseMatrix(int width, int height) {
        this.N  = width;
        rows = new SparseVector[height];
        for (int i = 0; i < height; i++) rows[i] = new SparseVector(N);
    }
    
    public SparseMatrix(SparseVector[] arr) {
    	N = arr[0].size();
    	rows = arr;
    }
    
    public SparseMatrix getSubMatrix(int rowStart, int rowEnd) {
    	return new SparseMatrix(Arrays.copyOfRange(rows, rowStart, rowEnd));
    }
    
    public SparseMatrix getSubColMatrix(int colStart, int colEnd) {
    	SparseMatrix res = new SparseMatrix(colEnd - colStart, this.rows.length);
    	for (int i = 0; i < rows.length; i++) {
    		res.rows[i] = rows[i].subVector(colStart, colEnd);
    	}
    	return res;
    }
    
    public SparseVector subVector(int row, int from, int to) {
    	return rows[row].subVector(from, to);
    }
    
    public SparseVector getRow(int row) {
    	return rows[row];
    }
    
    public int getRows() {
    	return rows.length;
    }
    
    public int getCols() {
    	return N;
    }
    

    // put A[i][j] = value
    public void put(int i, int j, double value) {
        if (i < 0 || i >= rows.length) throw new RuntimeException("Illegal index");
        if (j < 0 || j >= N) throw new RuntimeException("Illegal index");
        rows[i].put(j, value);
    }
    
    public Set<Integer> getRowNZ(int i) {
    	if (i < 0 || i >= rows.length) throw new RuntimeException("Illegal index");
    	return rows[i].nzindices();
    }

    // return A[i][j]
    public double get(int i, int j) {
        if (i < 0 || i >= rows.length) throw new RuntimeException("Illegal index");
        if (j < 0 || j >= N) throw new RuntimeException("Illegal index");
        return rows[i].get(j);
    }

    // return the number of nonzero entries (not the most efficient implementation)
    public int nnz() { 
        int sum = 0;
        for (int i = 0; i < rows.length; i++)
            sum += rows[i].nnz();
        return sum;
    }

    // return the matrix-vector product b = Ax
    public SparseVector dot(SparseVector x) {
        SparseMatrix A = this;
        if (N != x.size()) throw new RuntimeException("Dimensions disagree");
        SparseVector b = new SparseVector(rows.length);
        for (int i = 0; i < rows.length; i++)
            b.put(i, A.rows[i].dot(x));
        return b;
    }
    
    // return the matrix-vector product b = Ax
    public double[] dot(double[] x) {
    	SparseMatrix A = this;
    	if (N != x.length) throw new RuntimeException("Dimensions disagree");
    	double[] b = new double[rows.length];
    	for (int i = 0; i < rows.length; i++)
            b[i] = A.rows[i].dot(x);
        return b;
    }
    
    // return the vector matrix product b = xA
    public SparseVector columnDot(SparseVector x) {
    	if (x.size() != getRows()) throw new RuntimeException("dimensions disagree");
    	SparseVector res = new SparseVector(getCols());
    	// scale and merge
    	for (int i = 0; i < x.size(); i++) {
    		SparseVector curr = rows[i].scale(x.get(i));
    		res = res.plus(curr);
    	}
    	return res;
    }

    // return C = A + B
    public SparseMatrix plus(SparseMatrix B) {
        SparseMatrix A = this;
        if (A.N != B.N || A.getRows() != B.getRows()) throw new RuntimeException("Dimensions disagree");
        SparseMatrix C = new SparseMatrix(N, getRows());
        for (int i = 0; i < rows.length; i++)
            C.rows[i] = A.rows[i].plus(B.rows[i]);
        return C;
    }


    // return a string representation
    public String toString() {
        String s = "N = " + N + ", nonzeros = " + nnz() + "\n";
        for (int i = 0; i < rows.length; i++) {
            s += i + ": " + rows[i] + "\n";
        }
        return s;
    }
    
    public void shuffleRows() {
	    int index;
	    SparseVector temp;
	    Random random = new Random();
	    for (int i = rows.length - 1; i > 0; i--) {
	        index = random.nextInt(i + 1);
	        temp = rows[index];
	        rows[index] = rows[i];
	        rows[i] = temp;
	    }
	}
}