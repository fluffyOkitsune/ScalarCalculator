package scalar.scalarcalc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Stack;

import scalar.Field;
import scalar.SContainer;
import scalar.SValue;

//----------------------------------------------------------------------
//構文解析機
//----------------------------------------------------------------------
public class Parser {
	private static final boolean DEBUG = true;

	public static void main(String args[]) {
		System.out.println("構文解析テスト");
		while (true) {
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in), 1);
			try {
				new Parser(br.readLine(), Field.Real).run();
			} catch (End_of_file e) {
				System.out.println("-------------------------");
				continue;
			} catch (End_of_system e) {
				break;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		System.out.println("おわり");
	}

	public SContainer result;
	private Field field;
	Lexer L;

	Parser(String text, Field f) {
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
				throw new Syntax_error(tok.getClass().toGenericString());
		else if (tok instanceof Token.NUM)
			if (t instanceof Token.NUM)
				;// OK
			else
				throw new Syntax_error(tok, t);
		else if (tok instanceof Token.ONE)
			if (t instanceof Token.ONE)
				if (((Token.ONE) tok).one == ((Token.ONE) t).one)
					;// OK
				else
					throw new Syntax_error(tok, t);
			else
				throw new Syntax_error(tok, t);
		else if (tok.getClass().equals(t.getClass()))
			;// OK
		else
			throw new Syntax_error(tok, t);
	}

	void eat(Token t) {
		check(t);
		advance();
	}

	/* 文法チェック */
	// 複数の構文
	void plains(HashMap<String, SContainer> var) {
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

	void plain(HashMap<String, SContainer> var) {
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
	void print(HashMap<String, SContainer> var) {
		if (DEBUG)
			System.out.print("print:");
		// Print -> PRINT ( expr )
		eat(new Token.PRINT());
		eat(new Token.ONE('('));
		SContainer sc = expr(var);
		eat(new Token.ONE(')'));
		sc.print();
	}

	// 消去
	void clear(HashMap<String, SContainer> var) {
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
	void sub(HashMap<String, SContainer> var) {
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
	SContainer expr(HashMap<String, SContainer> var) {
		if (DEBUG)
			System.out.print("expr:");
		// Expr -> Term Expr'
		SContainer A = term(var);
		SContainer B = expr_tail(A, var);
		return B;
	}

	SContainer expr_tail(SContainer A, HashMap<String, SContainer> var) {
		if (DEBUG)
			System.out.print("expr':");
		if (tok instanceof Token.ONE) {
			if (((Token.ONE) tok).one == '+') {
				// Expr' -> + Term Expr'
				eat(new Token.ONE('+'));
				SContainer sc = term(var);
				SContainer B = expr_tail(sc, var);
				return A.add(B, field);
			} else if (((Token.ONE) tok).one == '-') {
				// Expr' -> - Term Expr'
				eat(new Token.ONE('-'));
				SContainer sc = term(var);
				SContainer B = expr_tail(sc, var);
				return A.sub(B, field);
			}
		}
		// Expr' -> _
		return A;
	}

	// 項
	SContainer term(HashMap<String, SContainer> var) {
		if (DEBUG)
			System.out.print("term:");
		// Term -> Pow Term'
		SContainer A = pow(var);
		SContainer B = term_tail(A, var);
		return B;
	}

	SContainer term_tail(SContainer A, HashMap<String, SContainer> var) {
		if (DEBUG)
			System.out.print("term':");
		if (tok instanceof Token.ONE)
			if (((Token.ONE) tok).one == '*') {
				// Term' -> '*' Pow Term'
				eat(new Token.ONE('*'));
				SContainer sc = pow(var);
				SContainer B = term_tail(sc, var);
				return A.multi(B, field);
			}
		// Term' -> _
		return A;
	}

	// 因数
	SContainer pow(HashMap<String, SContainer> var) {
		if (DEBUG)
			System.out.print("pow:");
		// Pow -> Fact '^' Fact
		Token tmp = tok.clone();
		advance();
		if (tok instanceof Token.ONE) {
			if (((Token.ONE) tok).one == '^') {
				revToken(tmp);
				SContainer A = fact(var);
				eat(new Token.ONE('^'));
				SContainer B = fact(var);
				return A.pow(B, field);
			}
		}
		// Pow -> Fact
		revToken(tmp);
		return fact(var);
	}

	// 数
	SContainer fact(HashMap<String, SContainer> var) {
		if (DEBUG)
			System.out.print("fact:");
		if (tok instanceof Token.EXTEND) {
			// Fact -> EXTEND '(' expr ')'
			eat(new Token.EXTEND());
			eat(new Token.ONE('('));
			SContainer tmp = expr(var).extend(field);
			eat(new Token.ONE(')'));
			return tmp;
		} else if (tok instanceof Token.INVERT) {
			// Fact -> INVERT(expr)
			eat(new Token.INVERT());
			eat(new Token.ONE('('));
			SContainer tmp = expr(var).invert(field);
			eat(new Token.ONE(')'));
			return tmp;
		} else if (tok instanceof Token.ONE) {
			if (((Token.ONE) tok).one == '(') {
				// Fact -> '(' expr ')'
				eat(new Token.ONE('('));
				SContainer e = expr(var);
				eat(new Token.ONE(')'));
				return e;
			} else if (((Token.ONE) tok).one == '-') {
				// Fact -> '-' Fact
				eat(new Token.ONE('-'));
				SContainer f = fact(var);
				return new SContainer(-1).multi(f, field);
			} else
				throw new Syntax_error(tok.getClass().toString() + "'" + ((Token.ONE) tok).one + "'");
		} else if (tok instanceof Token.NUM) {
			// Fact -> NUM
			return scalar(var);
		} else if (tok instanceof Token.ID) {
			// Fact -> VAR
			return dequeue(id(), var);
		} else
			throw new Syntax_error(tok.getClass().toString());
	}

	SContainer scalar(HashMap<String, SContainer> var) {
		if (DEBUG)
			System.out.print("scalar:");
		int n = num();
		if (tok instanceof Token.ID) {
			// scalar -> NUM ID
			SContainer elem = dequeue(id(), var);
			return new SContainer(n).multi(elem, field);
		} else
			// scalar -> NUM
			return new SContainer(n);
	}

	// 数字
	int num() {
		if (DEBUG)
			System.out.print("num:");
		check(new Token.NUM(0));
		int tmp = ((Token.NUM) tok).num;
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

	void enqueue(String varName, SContainer element, HashMap<String, SContainer> var) {
		if (var != null)
			var.put(varName, element);
	}

	SContainer dequeue(String varName, HashMap<String, SContainer> var) {
		if (var != null)
			if (var.containsKey(varName))
				return var.get(varName);
			else
				return new SContainer(varName);
		else
			return new SContainer(varName);
	}

	void run() {
		HashMap<String, SContainer> var = new HashMap<>();
		advance();
		plains(var);
		if (tok instanceof Token.ONE && ((Token.ONE) tok).one == '$')
			throw new End_of_system();
		try {
			System.out.flush();
			if (result != null)
				System.out.println("\nanswer = " + result.calc(field).toString());
			else
				result = null;
		} catch (Syntax_error e) {
			e.printStackTrace();
		} catch (Var_not_exist_error e) {
			e.printStackTrace();
		}
	}
}

/* エラー出力 */
@SuppressWarnings("serial")
class Syntax_error extends RuntimeException {
	Syntax_error(Token token, Token check) {
		super("\'" + check + "\' is expected. (" + token + ")");
	}

	Syntax_error(String s) {
		super(s);
	}
}

@SuppressWarnings("serial")
class Var_not_exist_error extends RuntimeException {
	Var_not_exist_error() {
		super();
	}

	Var_not_exist_error(String s) {
		super(s);
	}
}

@SuppressWarnings("serial")
class End_of_file extends RuntimeException {

}

@SuppressWarnings("serial")
class End_of_system extends RuntimeException {

}
