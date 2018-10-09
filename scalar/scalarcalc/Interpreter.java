package scalar.scalarcalc;

import java.util.HashMap;

import scalar.SContainer;
import scalar.field.Field;

/**
 * 数式を打ち込んだら計算してくれるエンジン
 *
 * @version 1.20
 * @since 1.20
 * @author おきつね
 *
 */
public class Interpreter {
	public final static byte NUM = 0;
	public final static byte SID = 1;
	public final static byte MID = 2;
	public final static byte OPR = 3;
	public final static byte RES = 4;

	public static <E extends Comparable<E>> SContainer<E> run(String text, Field<E> field,
			HashMap<String, SContainer<E>> var) {
		Parser<E> P = new Parser<E>(text, field);
		P.advance();
		P.plains(var);
		return P.result;
	}
}
