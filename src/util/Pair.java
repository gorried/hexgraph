package util;

public class Pair<A, B> {
	public A first;
	public B second;
	
	public Pair() {
		first = null;
		second = null;
	}
	
	public Pair(A a, B b) {
		first = a;
		second = b;
	}
}
