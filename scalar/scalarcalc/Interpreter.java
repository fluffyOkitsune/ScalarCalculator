package scalar.scalarcalc;

import java.util.HashMap;

import scalar.Field;
import scalar.SContainer;

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

	// テスト
	public static void main(String args[]) {
		Parser.main(args);
	}

	public static SContainer run(String text, Field field, HashMap<String, SContainer> var) {
		Parser P = new Parser(text, field);
		P.advance();
		P.plains(var);
		return P.result;
	}
}
