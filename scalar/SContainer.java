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
public class SContainer implements Comparator<SContainer> {
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
		if (m == null)
			return this;

		Stack<SContainer> mul = new Stack<>();
		spritM(mul);
		System.out.println("#mul" + mul.toString());

		Vector<SContainer> addedANS = new Vector<>();
		Vector<SContainer> addedSC = new Vector<>();

		SContainer ans = mul.pop();
		ans.spritA(addedANS);

		SContainer sc;

		while (!mul.isEmpty()) {
			sc = mul.pop();
			sc.spritA(addedSC);

			System.out.println("#sc" + addedSC.toString());
			System.out.println("#an" + addedANS.toString());

			Vector<SContainer> arrayTemp = new Vector<>();
			for (int i = 0; i < addedSC.size(); i++)
				for (int j = 0; j < addedANS.size(); j++)
					arrayTemp.addElement((addedSC.get(i)).multi((addedANS.get(j)), field));

			addedANS = arrayTemp;
		}

		System.out.println("#" + addedANS.toString());
		return combineA(addedANS, field);
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
		added.sort(ONE);
	}

	private static SContainer combineA(Vector<SContainer> added, Field f) {
		added.sort(ONE);
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
		multipled.sort(ONE);
	}

	private static SContainer combineM(Vector<SContainer> multipled, Field f) {
		multipled.sort(ONE);
		SContainer ans = SContainer.ZERO;
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

	// Factorのみのコンテナを正規化
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
		Collections.sort(added, sc);
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
		Collections.sort(multipled, sc);
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
		SContainer A = this.rearrange(field, null);
		if (c == null || c.equals(SContainer.ZERO)) {
			// A+0 = A
			if (DEBUG)
				debugPrint("ADD:[" + A.toString() + "] + 0");
			return A;
		}
		SContainer B = c.rearrange(field, null);
		if (A.equals(SContainer.ZERO)) {
			// 0+B = B
			if (DEBUG)
				debugPrint("ADD:N/A + [" + B.toString() + "]");
			return B;
		}
		if (B.equals(SContainer.ZERO)) {
			// A+0 = A
			if (DEBUG)
				debugPrint("ADD:[" + A.toString() + "] + 0");
			return A;
		}
		if (A.v != null && B.v != null) {
			// 2+3 = 5 or S+T
			if (DEBUG)
				debugPrint("ADD:[VAL:" + A.toString() + "] + [" + B.toString() + "]");
			return (A.v).add(B.v, field);
		}
		if (DEBUG)
			debugPrint("ADD:[" + A.toString() + "] + [" + B.toString() + "]");
		if (A.a != null) {
			Vector<SContainer> array = new Vector<>();
			spritA(array);
			SContainer last = array.remove(array.size() - 1);
			last = last.add(B, field);
			array.addElement(last);
			return combineA(array, field);
		}
		return new SContainer(A, B, null, null);
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
		SContainer A = this.rearrange();
		if (c == null || c.equals(SContainer.ONE)) {
			// A*1 = A
			if (DEBUG)
				debugPrint("MULTI:[" + A.toString() + "] * 1");
			return A;
		}
		SContainer B = c.rearrange();
		if (A.equals(SContainer.ZERO) || B.equals(SContainer.ZERO)) {
			// A*0 = 0*B = 0
			if (DEBUG)
				debugPrint("MULTI:[VAL:" + A.toString() + "] * [VAL:" + B.toString() + "] = 0");
			return SContainer.ZERO;
		}
		if (A.equals(SContainer.ONE)) {
			// 1*B = B
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
		} else {
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
			return new SContainer(A, null, B, null);
		}
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

	@Override
	public int compare(SContainer A, SContainer B) {
		if (DEBUG) {
			debugPrint("COMPARE: " + A.toString() + ", " + B.toString());
			debugPrint("  A:" + A.showStructure());
			debugPrint("  B:" + B.showStructure());
		}
		int result;
		if (A.v != null && A.v.i != null)
			if (B.v != null && B.v.i != null)
				// 1,2
				result = Integer.compare(A.v.i, B.v.i);
			else
				// 1,B
				result = -1;
		else if (B.v != null && B.v.i != null)
			// A,1
			result = 1;
		else
			result = A.showStructure().compareTo(B.showStructure());

		if (DEBUG) {
			if (result < 0)
				debugPrint("  " + A.toString() + " < " + B.toString());
			else if (result > 0)
				debugPrint("  " + A.toString() + " > " + B.toString());
			else
				debugPrint("  " + A.toString() + " = " + B.toString());
		}
		return result;
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
