package scalar.field;

public class Frac implements Comparable<Frac> {
	final int num;
	final int den;

	public Frac(int numerator, int denominator) {
		if (denominator == 0)
			throw new ArithmeticException("Dev;ision by zero.");
		if (denominator < 0) {
			den = -denominator;
			num = -numerator;
		} else {
			den = denominator;
			num = numerator;
		}
	}

	public Frac reduction() {
		int common = gcd(num, den);
		return new Frac(num / common, den / common);
	}

	private int gcd(int x, int y) {
		return y > 0 ? gcd(y, x % y) : x;
	}

	@Override
	public String toString() {
		if (den != 1)
			return Integer.toString(num) + "/" + Integer.toString(den);
		else
			return Integer.toString(num);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Frac))
			return false;
		Frac other = (Frac) obj;
		return num * other.den == den * other.num;
	}

	@Override
	public int compareTo(Frac o) {
		return new Integer(num * o.den).compareTo(new Integer(o.num * den));
	}
}
