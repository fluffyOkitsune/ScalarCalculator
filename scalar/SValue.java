package scalar;

public class SValue {
	public static final SValue ZERO = new SValue(0);
	public static final SValue ONE = new SValue(1);
	final String s;
	final Integer i;

	public SValue(String varName) {
		for (int k = 0; k < varName.length(); k++) {
			char c = varName.charAt(k);
			if ('0' <= c && c <= '9' || c == '+' || c == '-')
				;
			else {
				s = varName;
				i = null;
				return;
			}
		}
		s = null;
		i = Integer.parseInt(varName);
	}

	public SValue(int value) {
		s = null;
		i = value;
	}

	public SContainer add(SValue v, Field f) {
		SValue A = this;
		SValue B = v;
		if (B == null)
			return new SContainer(A);
		else if (B.equals(SValue.ZERO))
			return new SContainer(A);
		else if (A.i != null)
			if (B.i != null)
				// 1 + 2
				if (f == Field.Galois)
					return new SContainer((A.i + B.i) % f.getOrder());
				else
					return new SContainer(A.i + B.i);
			else if (B.s != null)
				// 1 + A = A+1
				return new SContainer(new SContainer(B), new SContainer(A), null, null);
			else
				throw new InternalError();
		else if (A.s != null)
			if (B.i != null)
				// A + 1
				return new SContainer(new SContainer(A), new SContainer(B), null, null);
			else if (B.s != null) {
				// A + B
				int l = (A.s.length() < B.s.length()) ? A.s.length() : B.s.length();
				for (int i = 0; i < l; i++)
					if (A.s.charAt(i) < B.s.charAt(i))
						return new SContainer(new SContainer(A), new SContainer(B), null, null);
					else if (B.s.charAt(i) < A.s.charAt(i))
						return new SContainer(new SContainer(B), new SContainer(A), null, null);
					else
						continue;
				// A + A = 2*A
				return new SContainer(new SContainer(2), null, new SContainer(A), null);
			} else
				throw new InternalError();
		else
			throw new InternalError();
	}

	public SContainer add(SContainer c, Field field) {
		if (c == null)
			return new SContainer(this);
		if (c.equals(SContainer.ZERO))
			return new SContainer(this);
		SValue A = this;
		SContainer B = c.rearrange();
		if (B.v != null)
			return A.add(c.v, field);
		if (B.c.v != null && new SContainer(A).equals(B.c.m))
			// A + [2*A]
			return SValue.ONE.add(B.c.v, field).multi(new SContainer(A), field);
		return new SContainer(new SContainer(A), B, null, null);
	}

	public SContainer multi(SValue v, Field f) {
		SValue A = this;
		SValue B = v;
		if (v == null)
			return new SContainer(A);
		else if (v.equals(SValue.ONE))
			return new SContainer(A);
		else if (A.i != null)
			if (B.i != null)
				// 2 * 3 = 6
				if (f == Field.Galois)
					return new SContainer((A.i * B.i) % f.getOrder());
				else
					return new SContainer(A.i * B.i);
			else if (B.s != null)
				// 2 * A
				return new SContainer(new SContainer(A), null, new SContainer(B), null);
			else
				throw new InternalError();
		else if (A.s != null)
			if (B.i != null)
				// A * 2 = 2*A
				return new SContainer(new SContainer(B), null, new SContainer(A), null);
			else if (B.s != null)
				if (A.s.equals(B.s))
					// A * A = A^2
					return new SContainer(new SContainer(A), null, null, new SContainer(2));
				else
					// A * B
					return new SContainer(new SContainer(A), null, new SContainer(B), null);
			else
				throw new InternalError();
		else
			throw new InternalError();
	}

	public SContainer multi(SContainer c, Field f) {
		SValue A = this;
		if (c == null)
			return new SContainer(A);
		SContainer B = c.rearrange();
		if (B.equals(SContainer.ONE))
			return new SContainer(A);
		if (B.v != null)
			return A.multi(B.v, f);
		if (B.a == null) {
			if (A.i != null) {
				if (B.c.v != null && B.e == null)
					// 2*[3*A] = 6*A
					return new SContainer(A.multi(B.c.v, f), null, B.m, null);
			} else if (A.equals(B.c.v)) {
				if (B.e == null)
					// S*S = S^2
					return new SContainer(new SContainer(A), null, B.m, new SContainer(new SValue(2)));
				else
					// S*(S^2) = S^3
					return new SContainer(new SContainer(A), null, B.m, SValue.ONE.add(B.e, f));
			}
		}
		return new SContainer(new SContainer(A), null, B, null);

	}

	public SContainer pow(SValue v, Field f) {
		SValue A = this;
		SValue B = v;
		if (v == null)
			return new SContainer(this);
		else if (v.equals(SValue.ONE))
			return new SContainer(this);
		else if (v.equals(SValue.ZERO))
			return SContainer.ONE;
		else if (A.i != null)
			if (B.i != null)
				// 2 ^ 3 = 8
				if (f == Field.Galois)
					return new SContainer(((int) Math.pow(A.i, B.i)) % f.getOrder());
				else
					return new SContainer((int) Math.pow(A.i, B.i));
			else if (B.s != null)
				// 2 ^ A
				return new SContainer(new SContainer(A), null, null, new SContainer(B));
			else
				throw new InternalError();
		else if (A.s != null)
			if (B.i != null)
				// A ^ 2
				return new SContainer(new SContainer(A), null, null, new SContainer(B));
			else if (B.s != null)
				// A ^ B
				return new SContainer(new SContainer(A), null, null, new SContainer(B));
			else
				throw new InternalError();
		else
			throw new InternalError();
	}

	public SContainer pow(SContainer c, Field f) {
		SValue A = this;
		if (c == null)
			return new SContainer(this);
		SContainer B = c.rearrange();
		if (B.equals(SContainer.ZERO))
			return SContainer.ONE;
		if (B.equals(SContainer.ONE))
			return new SContainer(this);
		if (B.v != null)
			return A.pow(B.v, f);
		if (i != null && c.v.i != null)
			// TODO : 有理数べき乗
			return new SContainer(new SValue((int) Math.pow(i, c.v.i)));
		return new SContainer(new SContainer(A), null, null, c);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((i == null) ? 0 : i.hashCode());
		result = prime * result + ((s == null) ? 0 : s.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof SValue))
			return false;
		SValue other = (SValue) obj;
		if (i == null) {
			if (other.i != null)
				return false;
		} else if (!i.equals(other.i))
			return false;
		if (s == null) {
			if (other.s != null)
				return false;
		} else if (!s.equals(other.s))
			return false;
		return true;
	}

	@Override
	public String toString() {
		if (s != null)
			return s;
		else
			return i.toString();
	}
}
