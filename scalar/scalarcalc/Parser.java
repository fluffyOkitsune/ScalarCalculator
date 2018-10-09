package scalar.scalarcalc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Stack;

import scalar.SContainer;
import scalar.field.Field;

//----------------------------------------------------------------------
//構文解析機
//----------------------------------------------------------------------
public class Parser<E extends Comparable<E>> {
	private static final boolean DEBUG = false;

	public SContainer<E> result;
	private Field<E> field;
	Lexer L;

	Parser(String text, Field<E> f) {
		field = f;
		L = new Lexer(text);
	}

	private Token tok = new Token.ONE(' ');
	private Stack<Token> revTok = new Stack<Token>();

	Token getToken() {
		if (revTok.isEmpty())
			return L.getToken();
		else
			return revTok.pop();
	}

	void revToken(Token t) {
		revTok.push(tok);
		tok = t;
	}

	void advance() {
		tok = getToken();
	}

	/* 特定のトークンが来ているかチェックする */
	void check(Token t) {
		if (DEBUG)
			L.printToken(tok);
		if (tok instanceof Token.ID)
			if (t instanceof Token.ID)
				;// OK
			else
				throw new SyntaxErrorException(tok.getClass().toGenericString());
		else if (tok instanceof Token.NUM)
			if (t instanceof Token.NUM)
				;// OK
			else
				throw new SyntaxErrorException(tok, t);
		else if (tok instanceof Token.ONE)
			if (t instanceof Token.ONE)
				if (((Token.ONE) tok).one == ((Token.ONE) t).one)
					;// OK
				else
					throw new SyntaxErrorException(tok, t);
			else
				throw new SyntaxErrorException(tok, t);
		else if (tok.getClass().equals(t.getClass()))
			;// OK
		else
			throw new SyntaxErrorException(tok, t);
	}

	void eat(Token t) {
		check(t);
		advance();
	}

	/* 文法チェック */
	// 複数の構文
	void plains(HashMap<String, SContainer<E>> var) {
		if (DEBUG)
			System.out.print("plains:");
		plain(var);
		if (tok instanceof Token.ONE) {
			if (((Token.ONE) tok).one == ',') {
				// Plains -> Plain , Plains
				eat(new Token.ONE(','));
				plains(var);
			} else if (((Token.ONE) tok).one == ';') {
				// Plains -> Plain ; Plains
				eat(new Token.ONE(';'));
				plains(var);
			}
		}
		// Plains -> Plain EOF
		eat(new Token.EOF());
	}

	void plain(HashMap<String, SContainer<E>> var) {
		if (DEBUG)
			System.out.print("plain:");
		// Plain -> Print
		if (tok instanceof Token.PRINT) {
			print(var);
		}
		Token tmp = tok;
		advance();
		if (tok instanceof Token.ONE) {
			if (((Token.ONE) tok).one == '=') {
				// Plain -> Sub
				revToken(tmp);
				sub(var);
			}
		}
		// Plain -> Expr
		revToken(tmp);
		result = expr(var);
	}

	// 印字
	void print(HashMap<String, SContainer<E>> var) {
		if (DEBUG)
			System.out.print("print:");
		// Print -> PRINT ( expr )
		eat(new Token.PRINT());
		eat(new Token.ONE('('));
		SContainer<E> sc = expr(var);
		eat(new Token.ONE(')'));
		sc.print();
	}

	// 消去
	void clear(HashMap<String, SContainer<E>> var) {
		if (DEBUG)
			System.out.print("clear:");
		// clear -> CLEAR ( id )
		eat(new Token.CLEAR());
		eat(new Token.ONE('('));
		check(new Token.ID(""));
		String varName = ((Token.ID) tok).id;
		advance();
		var.remove(varName);
		eat(new Token.ONE(')'));
	}

	// 代入
	void sub(HashMap<String, SContainer<E>> var) {
		if (DEBUG)
			System.out.print("sub:");
		// Sub -> ID = Expr
		check(new Token.ID(""));
		String id = ((Token.ID) tok).id;
		advance();
		eat(new Token.ONE('='));
		enqueue(id, expr(var), var);

	}

	// 式
	SContainer<E> expr(HashMap<String, SContainer<E>> var) {
		if (DEBUG)
			System.out.print("expr:");
		// Expr -> Term Expr'
		SContainer<E> A = term(var);
		SContainer<E> B = expr_tail(A, var);
		return B;
	}

	SContainer<E> expr_tail(SContainer<E> A, HashMap<String, SContainer<E>> var) {
		if (DEBUG)
			System.out.print("expr':");
		if (tok instanceof Token.ONE) {
			if (((Token.ONE) tok).one == '+') {
				// Expr' -> + Term Expr'
				eat(new Token.ONE('+'));
				SContainer<E> sc = term(var);
				SContainer<E> B = expr_tail(sc, var);
				return A.add(B);
			} else if (((Token.ONE) tok).one == '-') {
				// Expr' -> - Term Expr'
				eat(new Token.ONE('-'));
				SContainer<E> sc = term(var);
				SContainer<E> B = expr_tail(sc, var);
				return A.sub(B);
			}
		}
		// Expr' -> _
		return A;
	}

	// 項
	SContainer<E> term(HashMap<String, SContainer<E>> var) {
		if (DEBUG)
			System.out.print("term:");
		// Term -> Pow Term'
		SContainer<E> A = pow(var);
		SContainer<E> B = term_tail(A, var);
		return B;
	}

	SContainer<E> term_tail(SContainer<E> A, HashMap<String, SContainer<E>> var) {
		if (DEBUG)
			System.out.print("term':");
		if (tok instanceof Token.ONE)
			if (((Token.ONE) tok).one == '*') {
				// Term' -> '*' Pow Term'
				eat(new Token.ONE('*'));
				SContainer<E> sc = pow(var);
				SContainer<E> B = term_tail(sc, var);
				return A.multi(B);
			} else if (((Token.ONE) tok).one == '/') {
				// Term' -> '*' Pow Term'
				eat(new Token.ONE('/'));
				SContainer<E> sc = pow(var);
				SContainer<E> B = term_tail(sc, var);
				return A.div(B);
			}
		// Term' -> _
		return A;
	}

	// 因数
	SContainer<E> pow(HashMap<String, SContainer<E>> var) {
		if (DEBUG)
			System.out.print("pow:");
		// Pow -> Fact Pow'
		SContainer<E> A = fact(var);
		SContainer<E> B = pow_tail(A, var);
		return B;
	}

	SContainer<E> pow_tail(SContainer<E> A, HashMap<String, SContainer<E>> var) {
		if (DEBUG)
			System.out.print("pow':");
		if (tok instanceof Token.ONE)
			if (((Token.ONE) tok).one == '^') {
				// Pow' -> '^' Pow Fact'
				eat(new Token.ONE('^'));
				SContainer<E> sc = fact(var);
				SContainer<E> B = pow_tail(sc, var);
				return A.pow(B);
			}
		// Term' -> _
		return A;
	}

	// 数
	SContainer<E> fact(HashMap<String, SContainer<E>> var) {
		if (DEBUG)
			System.out.print("fact:");
		if (tok instanceof Token.EXTEND) {
			// Fact -> EXTEND '(' expr ')'
			eat(new Token.EXTEND());
			eat(new Token.ONE('('));
			SContainer<E> tmp = expr(var).extend();
			eat(new Token.ONE(')'));
			return tmp;
		} else if (tok instanceof Token.INVERT) {
			// Fact -> INVERT(expr)
			eat(new Token.INVERT());
			eat(new Token.ONE('('));
			SContainer<E> tmp = expr(var).invert();
			eat(new Token.ONE(')'));
			return tmp;
		} else if (tok instanceof Token.ONE) {
			if (((Token.ONE) tok).one == '(') {
				// Fact -> '(' expr ')'
				eat(new Token.ONE('('));
				SContainer<E> e = expr(var);
				eat(new Token.ONE(')'));
				return e;
			} else if (((Token.ONE) tok).one == '-') {
				// Fact -> '-' Fact
				eat(new Token.ONE('-'));
				SContainer<E> f = fact(var);
				return new SContainer<E>("", field).minusOne().multi(f);
			} else
				throw new SyntaxErrorException(tok.getClass().toString() + "'" + ((Token.ONE) tok).one + "'");
		} else if (tok instanceof Token.NUM) {
			// Fact -> NUM
			return scalar(var);
		} else if (tok instanceof Token.ID) {
			// Fact -> VAR
			return dequeue(id(), var);
		} else
			throw new SyntaxErrorException(tok.getClass().toString());
	}

	SContainer<E> scalar(HashMap<String, SContainer<E>> var) {
		if (DEBUG)
			System.out.print("scalar:");
		E n = field.makeElement(num());
		if (tok instanceof Token.ID) {
			// scalar -> NUM ID
			SContainer<E> elem = dequeue(id(), var);
			return new SContainer<E>(n, field).multi(elem);
		} else
			// scalar -> NUM
			return new SContainer<E>(n, field);
	}

	// 数字
	String num() {
		if (DEBUG)
			System.out.print("num:");
		check(new Token.NUM(0));
		// TODO:Long値対応
		String tmp = Integer.toString(((Token.NUM) tok).num);
		advance();
		return tmp;
	}

	// 識別子(変数)
	String id() {
		if (DEBUG)
			System.out.print("id:");
		check(new Token.ID(""));
		String tmp = ((Token.ID) tok).id;
		advance();
		return tmp;
	}

	void enqueue(String varName, SContainer<E> element, HashMap<String, SContainer<E>> var) {
		if (var != null)
			var.put(varName, element);
	}

	SContainer<E> dequeue(String varName, HashMap<String, SContainer<E>> var) {
		if (var != null && var.containsKey(varName))
			return var.get(varName);
		return new SContainer<E>(varName, field);
	}

	void run() {
		HashMap<String, SContainer<E>> var = new HashMap<>();
		advance();
		plains(var);
		if (tok instanceof Token.ONE && ((Token.ONE) tok).one == '$')
			throw new End_of_system();
		try {
			System.out.flush();
			if (result != null)
				System.out.println("\nanswer = " + result.toString());
			else
				result = null;
		} catch (SyntaxErrorException e) {
			e.printStackTrace();
		} catch (VarNotExistException e) {
			e.printStackTrace();
		}
	}
}

/* エラー出力 */
@SuppressWarnings("serial")
class SyntaxErrorException extends RuntimeException {
	SyntaxErrorException(Token token, Token check) {
		super("\'" + check + "\' is expected. (" + token + ")");
	}

	SyntaxErrorException(String s) {
		super(s);
	}
}

@SuppressWarnings("serial")
class VarNotExistException extends RuntimeException {
	VarNotExistException() {
		super();
	}

	VarNotExistException(String s) {
		super(s);
	}
}

@SuppressWarnings("serial")
class End_of_file extends RuntimeException {

}

@SuppressWarnings("serial")
class End_of_system extends RuntimeException {

}
