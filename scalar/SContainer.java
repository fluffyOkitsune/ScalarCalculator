package scalar;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Stack;
import java.util.Vector;

import scalar.scalarcalc.Interpreter;

/**
 * スカラー式の構造の表現単位 <BR>
 * m*(c^e)+a を表現する。
 */
public class SContainer {
	public static SContainer make(String formula) {
		return Interpreter.run(formula, Field.Real, null);
	}

	public static SContainer make(String formula, Field field, HashMap<String, SContainer> varName) {
		return Interpreter.run(formula, Field.Real, varName);
	}

	public static final SContainer ZERO = new SContainer(SValue.ZERO);
	public static final SContainer ONE = new SContainer(SValue.ONE);
	private static final boolean DEBUG = true;
	private static int debugIndent = 0;

	// コンテナはイミュータブルにつき、インスタンス生成後のフィールドの変更は一切不可
	/** 式を表すスカラコンテナ(container) */
	final SContainer c;
	/** cに加えられる項(added) */
	final SContainer a;
	/** cにかけられる係数(multiple) */
	final SContainer m;
	/** cの指数(exponent) */
	final SContainer e;
	/** 文字や数値などの値(value) */
	final SValue v;

	// [v] : 値だけを保持するコンテナ
	public SContainer(SValue value) {
		if (value == null)
			throw new IllegalArgumentException("\"Value\" must not be null.");
		c = null;
		a = null;
		m = null;
		e = null;
		v = value;
	}

	public SContainer(String value) {
		if (value == null)
			throw new IllegalArgumentException("\"Value\" must not be null.");
		c = null;
		a = null;
		m = null;
		e = null;
		v = new SValue(value);
	}

	public SContainer(int value) {
		c = null;
		a = null;
		m = null;
		e = null;
		v = new SValue(value);
	}

	// [(f^e)*m+a] : 式の形を表すコンテナ
	protected SContainer(SContainer container, SContainer added, SContainer multipled, SContainer exponent) {
		c = container;
		if (c == null)
			throw new IllegalArgumentException("\"Container\" must not be null.");
		a = added;
		m = multipled;
		e = exponent;
		v = null;
	}

	// ------------------------------------------------------------
	// 展開

	public SContainer extend(Field field) {
		if (a == null)
			return subExtend(field);

		Vector<SContainer> added = new Vector<>();
		spritA(added);
		for (int i = 0; i < added.size(); i++)
			added.setElementAt(added.get(i).extend(field), i);

		return combineA(added, field);
	}

	private SContainer subExtend(Field field) {
		if (m == null)
			return this;

		Stack<SContainer> mul = new Stack<>();
		spritM(mul);

		// 展開できるか確認
		boolean unableToExtend = true;
		for (SContainer sc : mul)
			if (sc.a == null)
				continue;
			else {
				unableToExtend = false;
				break;
			}
		if (unableToExtend)
			return this;

		if (DEBUG) {
			debugPrint("EXTEND[" + toString() + "] : BEGIN");
			debugIndent++;
		}

		SContainer L;
		Vector<SContainer> addedL = new Vector<>();
		SContainer R = mul.pop();
		Vector<SContainer> addedR = new Vector<>();

		R = R.extend(field);
		R.spritA(addedR);

		while (!mul.isEmpty()) {
			L = mul.pop().extend(field);
			L.spritA(addedL);

			Vector<SContainer> arrayAns = new Vector<>();

			debugPrint(addedL.toString());
			debugPrint(addedR.toString());

			for (int i = 0; i < addedL.size(); i++)
				for (int j = 0; j < addedR.size(); j++)
					arrayAns.addElement((addedL.get(i)).multi((addedR.get(j)), field));

			addedR = arrayAns;
		}

		SContainer ans = combineA(addedR, field);
		if (DEBUG) {
			debugPrint("EXTEND : END [" + ans.toString() + "]");
			debugIndent--;
		}

		return ans;
	}

	// addedの列を分割
	// A + [B+C] + D*E + F + ... -> A, [B+C], [D*E], F, ...
	private void spritA(Vector<SContainer> added) {
		added.removeAllElements();
		for (SContainer sc = this; sc != null; sc = sc.a)
			if (sc.v != null)
				added.addElement(sc);
			else
				added.addElement(new SContainer(sc.c, null, sc.m, sc.e).rearrange());
		added.sort(new OrderA());
	}

	private static SContainer combineA(Vector<SContainer> added, Field f) {
		added.sort(new OrderA());
		SContainer ans = SContainer.ZERO;
		for (int i = 0; i < added.size(); i++) {
			SContainer sc = added.get(added.size() - 1 - i);
			ans = sc.add(ans, f);
		}
		return ans;
	}

	// multipleの列を分割
	// A * [B*C] * [D^E] * F *... -> A, [B*C], [D^E], F, ...
	private void spritM(Vector<SContainer> multipled) {
		multipled.removeAllElements();
		for (SContainer sc = this; sc != null; sc = sc.m)
			if (sc.v != null)
				multipled.addElement(sc);
			else
				multipled.addElement(new SContainer(sc.c, sc.a, null, sc.e).rearrange());
		multipled.sort(new OrderM());
	}

	private static SContainer combineM(Vector<SContainer> multipled, Field f) {
		multipled.sort(new OrderM());
		SContainer ans = SContainer.ONE;
		for (int i = 0; i < multipled.size(); i++) {
			SContainer sc = multipled.get(multipled.size() - 1 - i);
			ans = sc.multi(ans, f);
		}
		return ans;
	}

	// ------------------------------------------------------------
	// コンテナの構造の整理
	private SContainer rearrange(Field field, CalcOption opt) {
		SContainer A = a;
		SContainer M = m;
		SContainer E = e;

		if (A != null)
			A = A.rearrange(field, opt);
		if (M != null)
			M = M.rearrange(field, opt);
		if (E != null)
			E = E.rearrange(field, opt);

		// 展開
		if (opt == CalcOption.Extend && M != null && M.a != null)
			return extend(field);
		else
			return rearrange();
	}

	// Containerのみのコンテナを正規化
	SContainer rearrange() {
		// 定数値コンテナ
		if (v != null)
			return this;
		else {
			// 無効な値の削除
			SContainer C;
			SContainer A;
			SContainer M;
			SContainer E;

			C = c.rearrange();

			if (m != null) {
				if (m.equals(ZERO))
					return SContainer.ZERO;
				else if (m.equals(ONE))
					M = null;
				else
					M = m.rearrange();
			} else
				M = null;

			if (e != null) {
				if (e.equals(ZERO))
					return SContainer.ONE;
				else if (e.equals(ONE))
					E = null;
				else
					E = e.rearrange();
			} else
				E = null;

			if (a != null) {
				if (a.equals(ZERO))
					A = null;
				else
					A = a.rearrange();
			} else
				A = null;

			if (A == null && M == null && E == null)
				// [[A]] -> [A]
				return C;
			else
				return new SContainer(C, A, M, E);
		}
	}

	// ルートコンテナの計算
	public SContainer calc(Field field) {
		return calc(field, null);
	}

	private SContainer calc(Field field, CalcOption opt) {
		if (v != null)
			return this;
		if (DEBUG) {
			debugPrint("CALC [" + toString() + "] : BEGIN");
			debugIndent++;
		}

		SContainer ans = calcA(field, opt);

		if (DEBUG) {
			debugIndent--;
			debugPrint("CALC : RETURN [" + ans.toString() + "]");
			debugPrint("");
		}

		return ans;
	}

	// コンテナの和の計算
	public SContainer calcA(Field field, CalcOption opt) {
		if (v != null)
			return this;

		if (DEBUG) {
			debugPrint("CALC (ADD) : BEGIN");
			debugIndent++;
		}

		// 木を分割
		Stack<SContainer> added = new Stack<SContainer>();
		spritA(added);

		Stack<SContainer> tmp = new Stack<SContainer>();
		for (SContainer sc : added) {
			tmp.addElement(sc.calcM(field, opt));
		}
		added = tmp;

		// 列をソート
		SContainer sc = SContainer.ZERO;
		Collections.sort(added, new OrderA());
		if (DEBUG) {
			debugPrint("@ A_array:" + added.toString());
			debugIndent++;
		}

		// 木の再構成
		while (!added.isEmpty())
			sc = added.pop().add(sc, field);

		if (DEBUG) {
			debugIndent--;
			debugIndent--;
			debugPrint("CALC (ADD) : END");
		}
		return sc.rearrange(field, opt);
	}

	// コンテナの積の計算
	public SContainer calcM(Field field, CalcOption opt) {
		if (v != null)
			return this;

		if (DEBUG) {
			debugPrint("CALC (MULTIPLE) : BEGIN");
			debugIndent++;
		}

		// 木を分割
		Stack<SContainer> multipled = new Stack<SContainer>();
		spritM(multipled);

		// 列をソート
		SContainer sc = SContainer.ONE;
		Collections.sort(multipled, new OrderM());
		if (DEBUG) {
			debugPrint("@ M_array:" + multipled.toString());
			debugIndent++;
		}

		// 木の再構成
		while (!multipled.isEmpty())
			sc = multipled.pop().multi(sc, field);

		if (DEBUG) {
			debugIndent--;
			debugIndent--;
			debugPrint("CALC (MULTIPLE) : END");
		}
		return sc.rearrange(field, opt);
	}

	// 足し算
	public SContainer add(SContainer c, Field field) {
		SContainer A = rearrange(field, null);

		// A+0 = A
		if (c == null) {
			if (DEBUG)
				debugPrint("ADD:[" + A.toString() + "] + 0");
			return A;
		}

		SContainer B = c.rearrange(field, null);

		// 0+B = B
		if (A.equals(SContainer.ZERO)) {
			if (DEBUG)
				debugPrint("ADD:N/A + [" + B.toString() + "]");
			return B;
		}

		// A+0 = A
		if (B.equals(SContainer.ZERO)) {
			if (DEBUG)
				debugPrint("ADD:[" + A.toString() + "] + 0");
			return A;
		}

		// valueコンテナ
		if (A.v != null && B.v != null) {
			if (DEBUG)
				debugPrint("ADD:[VAL:" + A.toString() + "] + [" + B.toString() + "]");
			return (A.v).add(B.v, field);
		}

		if (DEBUG)
			debugPrint("ADD:[" + A.toString() + "] + [" + B.toString() + "]");
		if (A.a != null) {
			return attachLastA(B, field);
		}

		return new SContainer(A, B, null, null);

	}

	private SContainer attachLastA(SContainer sc, Field field) {
		Vector<SContainer> array = new Vector<>();
		spritA(array);
		SContainer last = array.remove(array.size() - 1);
		last = last.add(sc, field);
		array.addElement(last);
		return combineA(array, field);
	}

	// 引き算
	public SContainer sub(SContainer c, Field field) {
		SContainer A = this.rearrange(field, null);
		SContainer B = c.rearrange(field, null);
		SContainer M_ONE = new SContainer(-1);
		return A.add(M_ONE.multi(B, field), field);
	}

	// 掛け算
	public SContainer multi(SContainer c, Field field) {
		SContainer A = rearrange();

		// 0*B = 0
		if (A.equals(SContainer.ZERO)) {
			if (DEBUG)
				debugPrint("MULTI:0 * [VAL:" + c.toString() + "] = 0");
			return SContainer.ZERO;
		}

		// A*1 = A
		if (c == null) {
			if (DEBUG)
				debugPrint("MULTI:[" + A.toString() + "] * 1");
			return A;
		}

		SContainer B = c.rearrange();

		// A*0 = 0
		if (B.equals(SContainer.ZERO)) {
			if (DEBUG)
				debugPrint("MULTI:[VAL:" + A.toString() + "] * 0 = 0");
			return SContainer.ZERO;
		}

		// 1*B = B
		if (A.equals(SContainer.ONE)) {
			if (DEBUG)
				debugPrint("MULTI:1 * [" + B.toString() + "]");
			return B;
		}
		if (A.v != null) {
			// 値 * コンテナ
			if (B.v != null) {
				// 2 * 3 = 6
				if (DEBUG)
					debugPrint("MULTI:[VAL:" + A.toString() + "] * [VAL:" + B.toString() + "]");
				return (A.v).multi(B.v, field);
			} else {
				// 2 * A = 6
				if (DEBUG)
					debugPrint("MULTI:[VAL:" + A.toString() + "] * [" + B.toString() + "]");
				return (A.v).multi(B, field);
			}
		}

		// コンテナ * コンテナ
		if (DEBUG)
			debugPrint("MULTI:[" + A.toString() + "] * [" + B.toString() + "]");
		if (A.a == null && B.a == null) {
			if (A.m == null) {
				if ((A.c).equals(B.c)) {
					// 指数法則
					if ((A.e) != null)
						// A^2 * [A^3 * m] = A^(2+3) * m
						return new SContainer(A, null, B.m, A.e.add(B.e, field));
					else
						// A * [A^3 * m] = A^(1+3) * m
						return new SContainer(A, null, B.m, SContainer.ONE.add(B.e, field));
				} else
					// [A^2] * [B^2*3+5]
					return new SContainer(A.c, null, B, A.e);
			} else {
				// [A^2*3] * [B^5*7]
				Vector<SContainer> arrayA = new Vector<>();
				Vector<SContainer> arrayB = new Vector<>();
				A.spritM(arrayA);
				B.spritM(arrayB);
				arrayA.addAll(arrayB);
				return combineM(arrayA, field);
			}
		}

		// [A^2*3+4] * [B^2*3+5]
		if (A.m != null) {
			return attachLastM(B, field);
		}
		return new SContainer(A, null, B, null);

	}

	private SContainer attachLastM(SContainer sc, Field field) {
		Vector<SContainer> array = new Vector<>();
		spritM(array);
		SContainer last = array.remove(array.size() - 1);
		last = last.multi(sc, field);
		array.addElement(last);
		return combineM(array, field);
	}

	// 割り算
	public SContainer div(SContainer c, Field field) {
		SContainer A = this.rearrange(field, null);
		SContainer B = c.rearrange(field, null);
		SContainer M_ONE = new SContainer(new SValue(-1));
		if (B.equals(SContainer.ZERO))
			throw new ArithmeticException("Division by zero.");
		else
			return A.multi(B.pow(M_ONE, field), field);
	}

	// べき乗
	public SContainer pow(SContainer c, Field field) {
		SContainer A = this.rearrange(field, null);
		SContainer B = c.rearrange(field, null);
		if (A.v != null)
			return (A.v).pow(B, field);
		else
			return new SContainer(this, null, null, B);
	}

	// 逆数
	public SContainer invert(Field field) {
		return new SContainer(this.rearrange(field, null), null, null, new SContainer(-1));
	}

	// 平方根
	public SContainer sqrt(Field field) {
		// sqrt(A) = A^(2^(-1))
		SContainer A = this.rearrange(field, null);
		SContainer M_ONE = new SContainer(new SValue(-1));
		SContainer TWO = new SContainer(new SValue(2));
		return new SContainer(A, null, null, new SContainer(TWO, null, null, M_ONE));
	}

	public boolean isNumber() {
		return (v != null && v.i != null);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((a == null) ? 0 : a.hashCode());
		result = prime * result + ((c == null) ? 0 : c.hashCode());
		result = prime * result + ((e == null) ? 0 : e.hashCode());
		result = prime * result + ((m == null) ? 0 : m.hashCode());
		result = prime * result + ((v == null) ? 0 : v.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof SContainer))
			return false;
		SContainer other = (SContainer) obj;
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

		// 値
		if (c.equals(new SContainer(-1)) && m != null)
			isMinus = true;
		else {
			if (c.a == null || (c.a).equals(SContainer.ZERO))
				str = c.toString();
			else
				str = "(" + c.toString() + ")";
		}

		// 指数
		if (e != null)
			if (e.equals(new SContainer(-1))) {
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

	public static Vector<SContainer> sortA(Vector<SContainer> v) {
		v.sort(new OrderA());
		return v;
	}

	public static Vector<SContainer> sortM(Vector<SContainer> v) {
		v.sort(new OrderM());
		return v;
	}

	private String showStructure() {
		if (v != null) {
			if (v.i != null)
				return "[" + v.i.toString() + "^1*1+0]";
			else
				return "[" + v.s + "^1*1+0]";
		}
		return subShowStructure();
	}

	private String subShowStructure() {
		if (v != null) {
			if (v.i != null)
				return v.i.toString();
			else if (v.s != null)
				return v.s;
			else
				throw new InternalError();
		}

		String str = "";

		// 値
		str = "[" + c.subShowStructure();

		// 指数
		if (e != null)
			str += "^" + e.subShowStructure();
		else
			str += "^1";

		// 係数
		if (m != null)
			str += "*" + m.subShowStructure();
		else
			str += "*1";

		// 係数
		if (a != null)
			str += "+" + a.subShowStructure();
		else
			str += "+0";
		str += "]";
		return str;
	}

	public void print() {
		System.out.println(toString());
	}

	public void printAll() {
		debugPrint("PRINT : " + toString());
		debugIndent++;
		debugPrint("v:" + v);

		if (c != null && c.v != null)
			debugPrint("c : [VAL : " + c + " ]");
		else
			debugPrint("c : " + c);

		if (a != null && a.v != null)
			debugPrint("a : [VAL : " + a + " ]");
		else
			debugPrint("a : " + a);

		if (m != null && m.v != null)
			debugPrint("m : [VAL : " + m + " ]");
		else
			debugPrint("m : " + m);

		if (e != null && e.v != null)
			debugPrint("e : [VAL : " + e + " ]");
		else
			debugPrint("e : " + e);
		debugIndent--;
	}

	public void printDetail() {
		debugPrint("PRINT [ " + toString() + " ]");
		debugIndent++;
		if (v != null)
			debugPrint("[VAL : " + v + " ]");

		if (c != null) {
			if (c.v != null)
				debugPrint("c : [VAL: " + c.v.toString() + " ]");
			else {
				debugPrint("c : " + c.toString());
				debugIndent++;
				c.printDetail();
				debugIndent--;
			}
		}

		if (e != null) {
			if (e.v != null)
				debugPrint("e : [VAL: " + e.v.toString() + " ]");
			else {
				debugPrint("e : " + e.toString());
				debugIndent++;
				e.printDetail();
				debugIndent--;
			}
		} else
			debugPrint("e : NULL");
		if (m != null) {
			if (m.v != null)
				debugPrint("m : [VAL: " + m.v.toString() + " ]");
			else {
				debugPrint("m : " + m.toString());
				debugIndent++;
				m.printDetail();
				debugIndent--;
			}
		} else
			debugPrint("m : NULL");
		if (a != null) {
			if (a.v != null)
				debugPrint("a : [VAL: " + a.v.toString() + " ]");
			else {
				debugPrint("a : " + a.toString());
				debugIndent++;
				a.printDetail();
				debugIndent--;
			}
		} else
			debugPrint("a : NULL");
		debugIndent--;
	}

	private static String debugIndent() {
		if (debugIndent < 0)
			debugIndent = 0;
		String str = "";
		for (int i = 0; i < debugIndent; i++)
			str += "  ";
		return str;
	}

	private static void debugPrint(String str) {
		System.out.println(debugIndent() + str);
	}
}

// 項の並び替え
class OrderA implements Comparator<SContainer> {
	@Override
	public int compare(SContainer A, SContainer B) {
		if (A == null && B == null)
			return 0;
		if (A != null && B == null)
			return -1;
		if (A == null && B != null)
			return 1;
		// e,m,aの順に優先/同じ底のべきは降べきの順。異なる底は辞書順
		// A^B > A^3 > 2*A^2 > A^2 > B^5 > 3 > 2
		if (A.v != null) {
			if (B.v != null)
				// A:VAL ? B:VAL
				return new OrderVA().compare(A.v, B.v);
			else
				// A:VAL ? B:SC
				return compare(new SContainer(A, SContainer.ZERO, SContainer.ONE, SContainer.ONE), B);
		} else {
			if (B.v != null)
				// A:SC ? B:VAL
				return compare(A, new SContainer(B, SContainer.ZERO, SContainer.ONE, SContainer.ONE));
			else {
				// A:SC ? B:SC
				SContainer coefA = null;
				SContainer coefB = null;
				if (A.c.v != null && A.c.v.i != null) {
					coefA = A.c;
					A = A.m;
				}
				if (B.c.v != null && B.c.v.i != null) {
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
class OrderM implements Comparator<SContainer> {
	@Override
	public int compare(SContainer A, SContainer B) {
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
				return new OrderVM().compare(A.v, B.v);
			else
				// A:VAL ? B:SC
				return compare(new SContainer(A, SContainer.ZERO, SContainer.ONE, SContainer.ONE), B);
		} else {
			if (B.v != null)
				// A:SC ? B:VAL
				return compare(A, new SContainer(B, SContainer.ZERO, SContainer.ONE, SContainer.ONE));
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
