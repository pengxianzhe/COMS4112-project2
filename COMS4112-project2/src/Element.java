/** 
 * COMS 4112 project2
 * Xianzhe Peng, xp2155@columbia.edu
 * Siddharth Preetam, sp3567@columbia.edu
 * 
 * Array element object for the algorithm
 * Stores bitmap representation and all other information for a particular subset of S
 */

public class Element {
	private int n; // number of basic terms
	private double p; // product of selectivity of all terms
	private boolean b; // whether no-branch optimization was used
	private double c; // current best cost to subset
	private int l; // integer representation of left child of subplan
	private int r; // integer representation of right child of subplan
	private Bitmap bitmap; // bitmap of this element
	
	/**
	 * Construct an element object that stores bitmap representation 
	 * and varies information about this subset of S
	 * Also compute the number of basic terms and product of selectivity of all those terms
	 * @param pArray selectivity array of the query
	 * @param k number of basic terms in query
	 * @param integer integer representation of the bitmap, it is equal to the index of this element
	 */
	public Element(double[] pArray, int k, int integer) {
		bitmap = new Bitmap(k);
		bitmap.setInt(integer);
		
		n = 0;
		p = 1;
		for (int i = 0; i < k; i++) {
			if (bitmap.get(i)) {
				n++;
				p = p * pArray[i];
			}
		}
		
		b = false;
		c = Double.MAX_VALUE;
		l = 0;
		r = 0;
	}
	
	/**
	 * Return the index of set for items that is in this set but not in the subset
	 * @param subset the element object that represents a subset of this set
	 * @return the index of set for items that is in this set but not in the subset
	 *         return -1 if the set passed in is not a subset of this set
	 */
	public int difference(Element subset) {
		if (n < subset.getN()) {
			return -1;
		}
		return bitmap.difference(subset.getBitmap());
	}

	public boolean isB() {
		return b;
	}

	public void setB(boolean b) {
		this.b = b;
	}

	public double getC() {
		return c;
	}

	public void setC(double c) {
		this.c = c;
	}

	public int getL() {
		return l;
	}

	public void setL(int l) {
		this.l = l;
	}

	public int getR() {
		return r;
	}

	public void setR(int r) {
		this.r = r;
	}

	public int getN() {
		return n;
	}

	public double getP() {
		return p;
	}

	public Bitmap getBitmap() {
		return bitmap;
	}
}
