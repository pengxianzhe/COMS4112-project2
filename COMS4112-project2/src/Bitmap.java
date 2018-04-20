import java.util.Arrays;

/** 
 * COMS 4112 project2
 * Xianzhe Peng, xp2155@columbia.edu
 * Siddharth Preetam, sp3567@columbia.edu
 * 
 * Bitmap representation of a set: Length of bitmap is number of possible elements.
 * The ith element of bitmap means if ith item is in the set or not.
 * Contains useful utility functions as well
 */

public class Bitmap {
	private boolean[] bitmap;
	
	/**
	 * Construct a bitmap representing 'length' possible items
	 * All values are initialized to false (no item in the set)
	 * @param length length of bitmap, which is also number of possible items
	 */
	public Bitmap(int length) {
		bitmap = new boolean[length];
	}
	
	/**
	 * Get the boolean value if the ith item is in the set
	 * @param i index of the item
	 * @return true if ith item is in the set, false if not
	 */
	public boolean get(int i) {
		return bitmap[i];
	}
	
	/**
	 * Set the boolean value for the ith item
	 * @param i index of the item
	 * @param value boolean value for the ith item
	 */
	public void set(int i, boolean value) {
		bitmap[i] = value;
	}
	
	/**
	 * Get the length of bitmap
	 * @return length of bitmap
	 */
	public int length() {
		return bitmap.length;
	}
	
	/**
	 * Convert an integer into binary number and set it to this bitmap
	 * Bitmap[0] is the least significant bit
	 * @param integer integer value to convert, must be within range(0, 2 ^ length - 1)
	 */
	public void setInt(int integer) {
		for (int i = 0; i < bitmap.length; i++) {
			bitmap[i] = (integer / (int) Math.pow(2, i)) % 2 == 1;
		}
	}
	
	/**
	 * Return the index of set for items that is in this set but not in the subset
	 * @param subset a bitmap that represents a subset of this set
	 * @return the index of set for items that is in this set but not in the subset
	 *         return -1 if the set passed in is not a subset of this set
	 */
	public int difference(Bitmap subset) {
		if (subset.length() != bitmap.length) {
			return -1;
		}
		int length = bitmap.length;
		int result = 0;
		for (int i = 0; i < length; i++) {
			boolean subvalue = subset.get(i);
			if (!bitmap[i] && subvalue) {
				return -1;
			}
			if (bitmap[i] && !subvalue) {
				result += (int) Math.pow(2, i);
			}
		}
		return result;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(bitmap);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Bitmap other = (Bitmap) obj;
		if (!Arrays.equals(bitmap, other.bitmap))
			return false;
		return true;
	}
}
