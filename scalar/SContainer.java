package scalar;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Stack;
import java.util.Vector;

import scalar.field.Field;
import scalar.field.PowerIsUnableException;
import scalar.scalarcalc.Interpreter;
import util.DebugPrinter;

/**
 * スカラー式の構造の表現単位 <BR>
 * m*(c^e)+a を表現する。
 */
public class SContainer<E extends Comparable<E>> {
	public static <E extends Comparable<E>> SContainer<E> make(String formula, Field<E> field) {
		DebugPrinter.debugPrint("MAKE:[" + formula + "] BEGIN", 1);

		SContainer<E> res = Interpreter.run(formula, field, null);

		DebugPrinter.debugPrint(-1, "MAKE END:[" + res.toString() + "]");

		return res;
	}

	public static <E extends Comparable<E>> SContainer<E> make(String formula, Field<E> field,
			HashMap<String, SContainer<E>> varName) {
		return Interpreter.run(formula, field, varName);
	}

	// コンテナはイミュータブルにつき、インスタンス生成後のフィールドの変更は一切不可
	/** 式を表すスカラコンテナ(container) */
	final SContainer<E> c;
	/** cに加えられる項(added) */
	final SContainer<E> a;
	/** cにかけられる係数(multiple) */
	final SContainer<E> m;
	/** cの指数(exponent) */
	final SContainer<E> e;
	/** 文字や数値などの値(value) */
	final SValue<E> v;

	// [v] : 値だけを保持するコンテナ
	SContainer(SValue<E> value) {
		if (value == null)
			throw new IllegalArgumentException("\"Value\" must not be null.");
		c = null;
		a = null;
		m = null;
		e = null;
		v = value;
	}

	// 値のコンテナ
	public SContainer(E value, Field<E> field) {
		if (value == null)
			throw new IllegalArgumentException("\"Value\" must not be null.");
		c = null;
		a = null;
		m = null;
		e = null;
		v = new SValue<E>(value, field);
	}

	// 変数名のコンテナ
	public SContainer(String value, Field<E> field) {
		if (value == null)
			throw new IllegalArgumentException("\"Value\" must not be null.");
		c = null;
		a = null;
		m = null;
		e = null;
		v = new SValue<E>(value, field);
	}

	// [(f^e)*m+a] : 式の形を表すコンテナ
	SContainer(SContainer<E> container, SContainer<E> added, SContainer<E> multipled,
			SContainer<E> exponent) {
		c = container;
		if (c == null)
			throw new IllegalArgumentException("\"Container\" must not be null.");
		a = added;
		m = multipled;
		e = exponent;
		v = null;
	}

	// ------------------------------------------------------------
	// 定数0
	public SContainer<E> zero() {
		Field<E> field = findField();
		return new SContainer<E>(field.zero(), field);
	}

	// 定数1
	public SContainer<E> one() {
		Field<E> field = findField();
		return new SContainer<E>(field.one(), field);
	}

	// マイナス
	public SContainer<E> minusOne() {
		Field<E> field = findField();
		return new SContainer<E>(field.opposite(field.one()), field);
	}

	Field<E> findField() {
		if (isValue())
			return v.f;
		else
			return c.findField();
	}

	boolean isValue() {
		return (v != null);
	}

	boolean isNumber() {
		return (isValue() && v.i != null);
	}

	boolean isCoefficient() {
		return (!isValue() && c.isNumber() && m != null && e == null);
	}

	// 足し算
	public SContainer<E> add(SContainer<E> cont) {
		return add(cont, true);
	}

	SContainer<E> add(SContainer<E> cont, boolean needsToSort) {
		// A+0 = A
		if (cont == null) {
			DebugPrinter.debugPrint("ADD(" + needsToSort + "):[" + toString() + "] + 0");
			return this;
		}

		// 0+B = B
		if (equals(zero())) {
			DebugPrinter.debugPrint("ADD(" + needsToSort + "):N/A + [" + cont.toString() + "]");
			return cont;
		}

		// A+0 = A
		if (cont.equals(zero())) {
			DebugPrinter.debugPrint("ADD(" + needsToSort + "):[" + toString() + "] + 0");
			return this;
		}

		if (isValue()) {
			if (cont.isValue()) {
				// A:VAL + B:VAL
				DebugPrinter.debugPrint(
						"ADD(" + needsToSort + "):[VAL:" + this.toString() + "] + [VAL:" + cont.toString() + "]");
				return v.add(cont.v);
			} else {
				// A:VAL + B:CON
				DebugPrinter.debugPrint(
						"ADD(" + needsToSort + "):[VAL:" + this.toString() + "] + [" + cont.toString() + "]");
				return v.add(cont, needsToSort);
			}
		} else {
			// A:CON + B:?
			DebugPrinter.debugPrint("ADD(" + needsToSort + "):[" + toString() + "] + [" + cont.toString() + "]");
			if (a == null) {
				if (isCoefficient()) {
					if (cont.isCoefficient()) {
						if (m.equals(cont.m)) {
							// A:[m*S] + B:[n*S + T] = [[m+n]*S + T]
							return new SContainer<E>(c.add(cont.c), cont.a, cont.m, null);
						}
					} else {
						if (m.equals(cont)) {
							// A:[m*S] + B:[S] = [[m+1]*S]
							return new SContainer<E>(c.add(one()), null, m, null);
						}
						if (m.equals(cont.c)) {
							// A:[m*S] + B:[S + T] = [[m+1]*S + T]
							return new SContainer<E>(c.add(one()), cont.a, m, null);
						}
					}
				} else {
					if (cont.isCoefficient()) {
						if (equals(cont.m))
							// A:[S] + B:[b*S + T] = [[1+b]*S + T]
							return new SContainer<E>(one().add(cont.c), cont.a, this, null);
					} else {
						boolean eq = c.equals(cont.c);
						eq = eq && ((e == null && cont.e == null) || (e.equals(cont.e)));
						eq = eq && ((m == null && cont.m == null) || (m.equals(cont.m)));
						if (eq)
							// A:[S*P] + B:[S*P + T] = [[1+1]*S + T]
							return new SContainer<E>((one()).add(one()), cont.a, this, null);
					}
				}
				return new SContainer<E>(c, cont, m, e);
			} else {
				Vector<SContainer<E>> arrayA = new Vector<>();
				spritA(arrayA);

				Vector<SContainer<E>> arrayB = new Vector<>();
				cont.spritA(arrayB);

				arrayA.addAll(arrayB);
				return combineA(arrayA);
			}
		}
	}

	// addedの列を分割
	// A + [B+C] + D*E + F + ... -> A, [B+C], [D*E], F, ...
	void spritA(Vector<SContainer<E>> added) {
		added.removeAllElements();
		for (SContainer<E> sc = this; sc != null; sc = sc.a)
			if (sc.isValue())
				added.addElement(sc);
			else {
				if (sc.m == null && sc.e == null)
					added.addElement(sc.c);
				else
					added.addElement(new SContainer<E>(sc.c, null, sc.m, sc.e));
			}
	}

	SContainer<E> combineA(Vector<SContainer<E>> added) {
		added.sort(new OrderA<E>());

		DebugPrinter.addDebugIndent(1);

		SContainer<E> ans = zero();
		for (int i = 0; i < added.size(); i++) {
			SContainer<E> sc = added.get(added.size() - 1 - i);
			ans = sc.add(ans, false);
		}

		DebugPrinter.addDebugIndent(-1);

		return ans;
	}

	// 引き算
	public SContainer<E> sub(SContainer<E> cont) {
		Field<E> f = findField();
		return add(new SContainer<E>(f.opposite(f.one()), f).multi(cont));
	}

	// 掛け算
	public SContainer<E> multi(SContainer<E> cont) {
		return multi(cont, true);
	}

	SContainer<E> multi(SContainer<E> cont, boolean needsToSort) {
		// A*1 = A
		if (cont == null) {
			DebugPrinter.debugPrint("MULTI:[" + toString() + "] * 1");
			return this;
		}

		// 0*B = 0
		if (equals(zero())) {
			DebugPrinter.debugPrint("MULTI:0 * [VAL:" + c.toString() + "] = 0");
			return zero();
		}

		// A*0 = 0
		if (cont.equals(zero())) {
			DebugPrinter.debugPrint("MULTI:[VAL:" + toString() + "] * 0 = 0");
			return zero();
		}

		// 1*B = B
		if (equals(one())) {
			DebugPrinter.debugPrint("MULTI:1 * [" + cont.toString() + "]");
			return cont;
		}

		if (isValue()) {
			if (cont.isValue()) {
				// A:VAL * B:VAL
				DebugPrinter.debugPrint("MULTI:[VAL:" + toString() + "] * [VAL:" + cont.toString() + "]");
				return v.multi(cont.v);
			} else {
				// A:VAL * B:CON
				DebugPrinter.debugPrint("MULTI:[VAL:" + toString() + "] * [" + cont.toString() + "]");
				return v.multi(cont, needsToSort);
			}
		} else {
			if (m == null) {
				if (cont.isValue()) {
					// A:CON * B:VAL
					DebugPrinter.debugPrint("MULTI:[" + toString() + "] * [VAL:" + cont.toString() + "]");
					if (c.equals(cont) && a == null)
						// A:[S^p * a] * B:[S] = [[S^(p+1)]*[T]]
						return new SContainer<E>(c, null, m, e.add(one()));
				} else {
					DebugPrinter.debugPrint("MULTI:[" + toString() + "] * [" + cont.toString() + "]");
					if (a == null) {
						if (cont.a == null) {
							if (c.equals(cont.c)) {
								if (e == null)
									// A:[S] * B:[S^3 * n] = [S^(1+3) * n]
									return new SContainer<E>(c, null, cont.m, one().add(cont.e));
								else
									// A:[S^2] * B:[S^3 * n] = [S^(2+3) * n]
									return new SContainer<E>(c, null, cont.m, (e).add(cont.e));
							}
						}
						// A:[A^p] * B:[B^q * n] = [S^(2+3) * n]
						return new SContainer<E>(c, null, cont, e);
					}
				}
				// A:[S^p + a] * B[T^q * n]
				return new SContainer<E>(this, null, cont, null);
			} else {
				Vector<SContainer<E>> arrayA = new Vector<>();
				spritM(arrayA);
				Vector<SContainer<E>> arrayB = new Vector<>();
				cont.spritM(arrayB);
				arrayA.addAll(arrayB);
				return combineM(arrayA);
			}
		}
	}

	// multipleの列を分割
	// A * [B*C] * [D^E] * F *... -> A, [B*C], [D^E], F, ...
	private void spritM(Vector<SContainer<E>> multipled) {
		multipled.removeAllElements();
		for (SContainer<E> sc = this; sc != null; sc = sc.m)
			if (sc.isValue())
				multipled.addElement(sc);
			else {
				if (sc.a == null && sc.e == null)
					multipled.addElement(sc.c);
				else
					multipled.addElement(new SContainer<E>(sc.c, sc.a, null, sc.e));
			}
	}

	private SContainer<E> combineM(Vector<SContainer<E>> multipled) {
		multipled.sort(new OrderM<E>());
		SContainer<E> ans = one();
		for (int i = 0; i < multipled.size(); i++) {
			SContainer<E> sc = multipled.get(multipled.size() - 1 - i);
			ans = sc.multi(ans, false);
		}
		return ans;
	}

	// 割り算
	public SContainer<E> div(SContainer<E> cont) {
		if (cont.equals(zero()))
			throw new ArithmeticException("Division by zero.");
		return multi(cont.invert());
	}

	// べき乗
	public SContainer<E> pow(SContainer<E> cont) {
		if (isValue() && cont.isValue()) {
			DebugPrinter.debugPrint("POW:[VAL: " + toString() + "] ^ [VAL:" + cont.toString() + "]");
			try {
				return v.pow(cont.v);
			} catch (PowerIsUnableException e) {
				System.err.println(cont.toString() + " is not supported exponent.");
			}
		} else
			DebugPrinter.debugPrint("POW:[" + toString() + "] ^ [" + cont.toString() + "]");
		return new SContainer<E>(this, null, null, cont);
	}

	// ------------------------------------------------------------
	// 展開
	public SContainer<E> extend() {
		if (isValue())
			return this;

		DebugPrinter.debugPrint("EXTEND[" + toString() + "] : BEGIN", 1);
		Vector<SContainer<E>> res = new Vector<>();

		Vector<SContainer<E>> added = new Vector<>();
		spritA(added);
		DebugPrinter.debugPrint("spA:" + added);

		for (SContainer<E> scA : added) {
			Stack<SContainer<E>> multipled = new Stack<>();
			scA.spritExtendM(multipled);
			DebugPrinter.debugPrint("spM:" + multipled);

			SContainer<E> scR = null;
			while (!multipled.isEmpty()) {
				SContainer<E> scL = multipled.pop();
				scL = scL.extend();
				scR = calcExtend(scL, scR);
			}
			res.addElement(scR);
		}
		SContainer<E> result = combineA(res);
		DebugPrinter.debugPrint(-1, "EXTEND : END[" + result.toString() + "]");
		return result;
	}

	private SContainer<E> calcExtend(SContainer<E> opL, SContainer<E> opR) {
		if (opR == null)
			return opL;

		Vector<SContainer<E>> res = new Vector<>();

		Vector<SContainer<E>> fcL = new Vector<>();
		opL.spritA(fcL);
		DebugPrinter.debugPrint("L:" + opL.toString() + "->" + fcL);

		Vector<SContainer<E>> fcR = new Vector<>();
		opR.spritA(fcR);
		DebugPrinter.debugPrint("R:" + opR.toString() + "->" + fcR);

		for (SContainer<E> scL : fcL)
			for (SContainer<E> scR : fcR)
				res.addElement(scL.multi(scR));

		return combineA(res);
	}

	private void spritExtendM(Vector<SContainer<E>> multipled) {
		multipled.removeAllElements();
		for (SContainer<E> sc = this; sc != null; sc = sc.m) {
			if (sc.isValue())
				multipled.addElement(sc);
			else {
				if (sc.a == null)
					if (sc.e == null)
						multipled.addElement(sc.c);
					else
						multipled.addElement(new SContainer<E>(sc.c, sc.a, null, sc.e));
				else {
					multipled.addElement(sc);
					return;
				}
			}
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof SContainer))
			return false;

		@SuppressWarnings("unchecked")
		SContainer<E> other = (SContainer<E>) obj;

		if (a == null) {
			if (other.a != null)
				return false;
		} else if (!a.equals(other.a))
			return false;
		if (e == null) {
			if (other.e != null)
				return false;
		} else if (!e.equals(other.e))
			return false;
		if (c == null) {
			if (other.c != null)
				return false;
		} else if (!c.equals(other.c))
			return false;
		if (m == null) {
			if (other.m != null)
				return false;
		} else if (!m.equals(other.m))
			return false;
		if (v == null) {
			if (other.v != null)
				return false;
		} else if (!v.equals(other.v))
			return false;
		return true;
	}

	@Override
	public String toString() {
		if (v != null)
			return v.toString();

		String str = "";
		boolean isFrac = false;
		boolean isMinus = false;

		Field<E> f = findField();
		SContainer<E> minus = new SContainer<E>(f.opposite(f.one()), f);

		// 値
		if (c.equals(minus) && m != null)
			isMinus = true;
		else {
			if (c.a == null || (c.a).equals(zero()))
				str = c.toString();
			else
				str = "(" + c.toString() + ")";
		}

		// 指数
		if (e != null)
			if (e.equals(minus)) {
				// 分数
				isFrac = true;
				str = "/" + str;
			} else if (e.a == null && e.m == null && e.e == null)
				str += "^" + e.toString();
			else
				str += "^(" + e.toString() + ")";

		// 係数
		if (m != null)
			if (m.a == null)
				if (isFrac && isMinus)
					str = "-";
				else if (isFrac)
					str = m.toString() + str;
				else if (isMinus)
					str += "-" + m.toString();
				else
					str += "*" + m.toString();
			else if (isFrac && isMinus)
				str = "-";
			else if (isFrac)
				str = "(" + m.toString() + ")" + str;
			else if (isMinus)
				str += "-(" + m.toString() + ")";
			else
				str += "*(" + m.toString() + ")";
		else if (isFrac)
			str = "1" + str;

		// 加える項
		if (a != null)
			str += "+" + a.toString();
		return str;
	}

	public Vector<SContainer<E>> sortA(Vector<SContainer<E>> v) {
		v.sort(new OrderA<E>());
		return v;
	}

	public Vector<SContainer<E>> sortM(Vector<SContainer<E>> v) {
		v.sort(new OrderM<E>());
		return v;
	}

	public void print() {
		System.out.println(toString());
	}

	public void printAll() {
		DebugPrinter.debugPrint("PRINT : " + toString());
		DebugPrinter.addDebugIndent(1);
		DebugPrinter.debugPrint("v:" + v);

		if (c != null && c.v != null)
			DebugPrinter.debugPrint("c : [VAL : " + c + " ]");
		else
			DebugPrinter.debugPrint("c : " + c);

		if (a != null && a.v != null)
			DebugPrinter.debugPrint("a : [VAL : " + a + " ]");
		else
			DebugPrinter.debugPrint("a : " + a);

		if (m != null && m.v != null)
			DebugPrinter.debugPrint("m : [VAL : " + m + " ]");
		else
			DebugPrinter.debugPrint("m : " + m);

		if (e != null && e.v != null)
			DebugPrinter.debugPrint("e : [VAL : " + e + " ]");
		else
			DebugPrinter.debugPrint("e : " + e);
		DebugPrinter.addDebugIndent(-1);
	}

	public void printDetail() {
		DebugPrinter.debugPrint("PRINT(DETAIL) [" + toString() + "] : BEGIN");
		DebugPrinter.addDebugIndent(1);
		if (v != null)
			DebugPrinter.debugPrint("[VAL : " + v + " ]");

		if (c != null) {
			if (c.v != null)
				DebugPrinter.debugPrint("c : [VAL: " + c.v.toString() + " ]");
			else {
				DebugPrinter.debugPrint("c : " + c.toString());
				DebugPrinter.addDebugIndent(1);
				c.printDetail();
				DebugPrinter.addDebugIndent(-1);
			}
		}

		if (e != null) {
			if (e.v != null)
				DebugPrinter.debugPrint("e : [VAL: " + e.v.toString() + " ]");
			else {
				DebugPrinter.debugPrint("e : " + e.toString());
				DebugPrinter.addDebugIndent(1);
				e.printDetail();
				DebugPrinter.addDebugIndent(-1);
			}
		} else
			DebugPrinter.debugPrint("e : NULL");
		if (m != null) {
			if (m.v != null)
				DebugPrinter.debugPrint("m : [VAL: " + m.v.toString() + " ]");
			else {
				DebugPrinter.debugPrint("m : " + m.toString());
				DebugPrinter.addDebugIndent(1);
				m.printDetail();
				DebugPrinter.addDebugIndent(-1);
			}
		} else
			DebugPrinter.debugPrint("m : NULL");
		if (a != null) {
			if (a.v != null)
				DebugPrinter.debugPrint("a : [VAL: " + a.v.toString() + " ]");
			else {
				DebugPrinter.debugPrint("a : " + a.toString());
				DebugPrinter.addDebugIndent(1);
				a.printDetail();
				DebugPrinter.addDebugIndent(-1);
			}
		} else
			DebugPrinter.debugPrint("a : NULL");
		DebugPrinter.addDebugIndent(-1);
		DebugPrinter.debugPrint("PRINT(DETAIL) : END");
	}

	public SContainer<E> invert() {
		Field<E> f = findField();
		if (isNumber())
			return new SContainer<E>(f.inverse(v.i), f);
		else
			return pow(minusOne());
	}
}

// 項の並び替え
class OrderA<E extends Comparable<E>> implements Comparator<SContainer<E>> {
	@Override
	public int compare(SContainer<E> A, SContainer<E> B) {
		if (A == null && B == null)
			return 0;
		if (A != null && B == null)
			return -1;
		if (A == null && B != null)
			return 1;
		// e,m,aの順に優先/同じ底のべきは降べきの順。異なる底は辞書順
		// A^B > A^3 > 2*A^2 > A^2 > B^5 > 3 > 2
		if (A.isValue()) {
			if (B.isValue())
				// A:VAL ? B:VAL
				return new OrderVA<E>().compare(A.v, B.v);
			else
				// A:VAL ? B:SC
				return compare(new SContainer<E>(A, A.zero(), A.one(), A.one()), B);
		} else {
			if (B.isValue())
				// A:SC ? B:VAL
				return compare(A, new SContainer<E>(B, A.zero(), A.one(), A.one()));
			else {
				// A:SC ? B:SC
				SContainer<E> coefA = null;
				SContainer<E> coefB = null;
				if (A.m != null && A.c.v != null && A.c.v.i != null) {
					coefA = A.c;
					A = A.m;
				}
				if (B.m != null && B.c.v != null && B.c.v.i != null) {
					coefB = B.c;
					B = B.m;
				}

				int ret = compare(A.c, B.c);
				if (ret == 0)
					ret = compare(A.e, B.e);
				if (ret == 0)
					ret = compare(A.m, B.m);
				if (ret == 0)
					ret = compare(A.a, B.a);
				if (ret == 0)
					ret = compare(coefA, coefB);
				return ret;

			}
		}
	}
}

// 因数の並び替え
class OrderM<E extends Comparable<E>> implements Comparator<SContainer<E>> {
	@Override
	public int compare(SContainer<E> A, SContainer<E> B) {
		// 係数は数字が前
		if (A == null && B == null)
			return 0;
		if (A != null && B == null)
			return 1;
		if (A == null && B != null)
			return -1;
		// e,aの順に優先/同じ底のべきは降べきの順。異なる底は辞書順
		// 2 * A^3 * B * (B + A) * (B + C) * C
		if (A.v != null) {
			if (B.v != null)
				// A:VAL ? B:VAL
				return new OrderVM<E>().compare(A.v, B.v);
			else
				// A:VAL ? B:SC
				return compare(new SContainer<E>(A, A.zero(), A.one(), A.one()), B);
		} else {
			if (B.v != null)
				// A:SC ? B:VAL
				return compare(A, new SContainer<E>(B, A.zero(), A.one(), A.one()));
			else {
				// A:SC ? B:SC
				int ret = compare(A.c, B.c);
				if (ret == 0)
					ret = compare(A.e, B.e);
				if (ret == 0)
					ret = compare(A.a, B.a);
				return ret;
			}
		}
	}
}
