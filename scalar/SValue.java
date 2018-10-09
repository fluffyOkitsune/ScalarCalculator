package scalar;

import java.util.Comparator;
import java.util.Vector;

import scalar.field.Field;
import scalar.field.PowerIsUnableException;

/**
 * @param F
 *            Field
 * @param E
 *            Type of Element
 */
class SValue<E extends Comparable<E>> {
	final String s;
	final E i;
	final Field<E> f;

	SValue(String varName, Field<E> field) {
		s = varName;
		i = null;
		f = field;
	}

	SValue(E value, Field<E> field) {
		s = null;
		i = value;
		f = field;
	}

	SValue<E> zero() {
		return new SValue<E>(f.zero(), f);
	}

	SValue<E> one() {
		return new SValue<E>(f.one(), f);
	}

	boolean isNumber() {
		return i != null;
	}

	boolean isVarName() {
		return s != null;
	}

	SContainer<E> add(SValue<E> val) {
		if (val == null)
			return new SContainer<E>(this);
		if (val.equals(zero()))
			return new SContainer<E>(this);

		if (isNumber())
			if (val.isNumber())
				// 1 + 2
				return new SContainer<E>(new SValue<E>(f.add(i, val.i), f));
			else if (val.isVarName())
				// 1 + A = A+1
				return new SContainer<E>(new SContainer<E>(val), new SContainer<E>(this), null, null);
			else
				throw new InternalError();
		else if (isVarName())
			if (val.isNumber())
				// A + 1
				return new SContainer<E>(new SContainer<E>(this), new SContainer<E>(val), null, null);
			else if (val.isVarName()) {
				if (s.equals(val.s))
					// A + A = 2*A
					return new SContainer<E>(new SContainer<E>(f.add(f.one(), f.one()), f), null,
							new SContainer<E>(this), null);
				// A + B
				if (s.compareTo(val.s) > 0)
					return new SContainer<E>(new SContainer<E>(val), new SContainer<E>(this), null, null);
				else
					return new SContainer<E>(new SContainer<E>(this), new SContainer<E>(val), null, null);

			} else
				throw new InternalError();
		else
			throw new InternalError();
	}

	SContainer<E> add(SContainer<E> cont, boolean needsToSort) {
		if (cont == null)
			return new SContainer<E>(this);

		// A:VAL * B:VAL
		if (cont.isValue()) {
			if ((cont.v).equals(zero()))
				return new SContainer<E>(this);
			else
				return this.add(cont.v);
		}
		// A:VAL * B:CONT
		else if (cont.isCoefficient()) {
			if (new SContainer<E>(this).equals(cont.m))
				// A[S] + B[2*S + T] = [(1+2)*S + T]
				return new SContainer<E>(one().add(cont.c, true), cont.a, cont.m, null);
		} else {
			if (new SContainer<E>(this).equals(cont.c) && cont.e == null && cont.m == null)
				// A:[S] + B:[S + T] = [[1+1]*S + T]
				return new SContainer<E>(one().add(one()), cont.a, cont.c, null);
		}

		if (cont.a == null)
			if (cont.e == null && cont.m == null)
				return new SContainer<E>(new SContainer<E>(this), cont.c, null, null);

		SContainer<E> res = new SContainer<E>(new SContainer<E>(this), cont, null, null);
		if (needsToSort) {
			Vector<SContainer<E>> array = new Vector<>();
			return sortA(array, res);
		} else
			return res;
	}

	private SContainer<E> sortA(Vector<SContainer<E>> added, SContainer<E> res) {
		added.removeAllElements();
		for (SContainer<E> sc = res; sc != null; sc = sc.a)
			if (sc.isValue())
				added.addElement(sc);
			else {
				if (sc.m == null && sc.e == null)
					added.addElement(sc.c);
				else
					added.addElement(new SContainer<E>(sc.c, null, sc.m, sc.e));
			}
		added.sort(new OrderA<E>());
		SContainer<E> ans = added.get(0).zero();
		for (int i = 0; i < added.size(); i++) {
			SContainer<E> sc = added.get(added.size() - 1 - i);
			ans = sc.add(ans, false);
		}
		return ans;
	}

	SContainer<E> multi(SValue<E> val) {
		if (val == null)
			return new SContainer<E>(this);
		if (val.equals(one()))
			return new SContainer<E>(this);

		if (isNumber())
			if (val.isNumber())
				// 2 * 3 = 6
				return new SContainer<E>(new SValue<E>(f.multiply(i, val.i), f));
			else if (val.isVarName())
				// 2 * A
				return new SContainer<E>(new SContainer<E>(this), null, new SContainer<E>(val), null);
			else
				throw new InternalError();
		else if (isVarName())
			if (val.i != null)
				// A * 2 = 2*A
				return new SContainer<E>(new SContainer<E>(val), null, new SContainer<E>(this), null);
			else if (val.s != null)
				if (s.equals(val.s))
					// A * A = A^2
					return new SContainer<E>(new SContainer<E>(this), null, null, one().add(one()));
				else
				// A * B
				if (s.compareTo(val.s) > 0)
					return new SContainer<E>(new SContainer<E>(val), null, new SContainer<E>(this), null);
				else
					return new SContainer<E>(new SContainer<E>(this), null, new SContainer<E>(val), null);
			else
				throw new InternalError();
		else
			throw new InternalError();
	}

	SContainer<E> multi(SContainer<E> cont, boolean needsToSort) {
		if (cont == null)
			return new SContainer<E>(this);

		// A:VAL * B:VAL
		if (cont.isValue()) {
			if ((cont.v).equals(one()))
				return new SContainer<E>(this);
			else
				return multi(cont.v);
		}

		// A:VAL * B:CONT
		if (cont.a == null) {
			if (this.isNumber()) {
				if (cont.isCoefficient())
					// A:[2] * B:[3*A] = [6*A]
					return new SContainer<E>(multi(cont.c, true), null, cont.m, null);
			} else {
				if (new SContainer<E>(this).equals(cont.c)) {
					if (cont.e == null)
						// A:[S] * B:[S] = [S^2]
						return new SContainer<E>(new SContainer<E>(this), null, cont.m, one().add(one()));
					else
						// A:[S] * B:[S^2] = [S^3]
						return new SContainer<E>(new SContainer<E>(this), null, cont.m, one().add(cont.e, true));
				}
			}
		}
		if (cont.m == null)
			if (cont.e == null)
				return new SContainer<E>(new SContainer<E>(this), null, cont.c, null);
			else
				return new SContainer<E>(new SContainer<E>(this), null, cont, null);

		SContainer<E> res = new SContainer<E>(new SContainer<E>(this), null, cont, null);
		if (needsToSort) {
			Vector<SContainer<E>> array = new Vector<>();
			return sortM(array, res);
		} else
			return res;
	}

	private SContainer<E> sortM(Vector<SContainer<E>> multipled, SContainer<E> res) {
		multipled.removeAllElements();
		for (SContainer<E> sc = res; sc != null; sc = sc.m)
			if (sc.isValue())
				multipled.addElement(sc);
			else {
				if (sc.a == null && sc.e == null)
					multipled.addElement(sc.c);
				else
					multipled.addElement(new SContainer<E>(sc.c, sc.a, null, sc.e));
			}
		multipled.sort(new OrderM<E>());
		SContainer<E> ans = multipled.get(0).one();
		for (int i = 0; i < multipled.size(); i++) {
			SContainer<E> sc = multipled.get(multipled.size() - 1 - i);
			ans = sc.multi(ans, false);
		}
		return ans;
	}

	public SContainer<E> pow(SValue<E> val) throws PowerIsUnableException {
		if (val == null)
			return new SContainer<E>(this);
		else if (val.equals(one()))
			return new SContainer<E>(this);
		else if (val.equals(zero()))
			return new SContainer<E>(one());

		else if (isNumber())
			if (val.isNumber())
				// 2 ^ 3 = 8
				return new SContainer<E>(f.pow(i, val.i), f);
			else if (val.isVarName())
				// 2 ^ A
				return new SContainer<E>(new SContainer<E>(this), null, null, new SContainer<E>(val));
			else
				throw new InternalError();
		else if (isVarName())
			// A ^ 2
			return new SContainer<E>(new SContainer<E>(this), null, null, new SContainer<E>(val));
		else
			throw new InternalError();
	}

	public SContainer<E> pow(SContainer<E> cont) throws PowerIsUnableException {
		if (cont == null)
			return new SContainer<E>(this);
		if (cont.equals(new SContainer<E>(zero())))
			return new SContainer<E>(one());
		if (cont.equals(new SContainer<E>(one())))
			return new SContainer<E>(this);

		if (cont.isValue())
			return pow(cont.v);
		else
			return new SContainer<E>(new SContainer<E>(this), null, null, cont);
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

		@SuppressWarnings("unchecked")
		SValue<E> other = (SValue<E>) obj;

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

class OrderVA<E extends Comparable<E>> implements Comparator<SValue<E>> {
	// 項を A+B+2 の順に並び替え
	@Override
	public int compare(SValue<E> A, SValue<E> B) {
		if (A.isNumber()) {
			if (B.i != null)
				// A:NUM ? B:NUM
				return (A.i).compareTo(B.i);
			else
				// A:NUM : B:VAL -> B,A
				return 1;
		} else {
			if (B.i != null)
				// A:VAL : B:NUM -> A,B
				return -1;
			else
				// A:VAL : B:VAL
				return (A.s).compareTo(B.s);
		}
	}
}

class OrderVM<E extends Comparable<E>> implements Comparator<SValue<E>> {
	// 因数を 2*A*B の順に並び替え
	@Override
	public int compare(SValue<E> A, SValue<E> B) {
		if (A.i != null) {
			if (B.i != null)
				// A:NUM ? B:NUM
				return (A.i).compareTo(B.i);
			else
				// A:NUM < B:VAL
				return -1;
		} else {
			if (B.i != null)
				// A:VAL > B:NUM
				return 1;
			else
				// A:VAL ? B:VAL
				return (A.s).compareTo(B.s);
		}
	}
}
