package scalar.scalarcalc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;

//----------------------------------------------------------------------
//字句解析機
//----------------------------------------------------------------------
class Lexer {
	private static final boolean DEBUG = true;

	Lexer(String s) {
		_ISTREAM = new BufferedReader(new StringReader(s));
		ch = new ArrayList<Character>();
	}

	BufferedReader _ISTREAM;
	ArrayList<Character> ch;

	char read() {
		if (ch.isEmpty())
			try {
				int c = _ISTREAM.read();
				if (c == -1)
					throw new End_of_file();
				return (char) c;
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException();
			}
		else {
			char c = ch.get(0);
			ch.remove(0);
			return c;
		}
	}

	void unread(char c) {
		ch.add(0, c);
	}

	char lookahead() {
		try {
			char c = read();
			unread(c);
			return c;
		} catch (End_of_file e) {
			return '$';
		}
	}

	/* 整数の認識 */
	int integer(int i) {
		char c = lookahead();
		if ('0' <= c && c <= '9')
			return integer(10 * i + (read() - '0'));
		else
			return i;
	}

	/* 識別子の認識 */
	String identifier(String id) {
		char c = lookahead();
		if (('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z') || ('0' <= c && c <= '9') || c == '_')
			return identifier(id += read());
		else
			return id;
	}

	/* 字句解析本体 */
	Token native_token() {
		char c = lookahead();
		if (('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z') || c == '_') {
			String id = identifier("");
			if (id.equals("eof"))
				return new Token.EOF();
			else if (id.equals("clear"))
				return new Token.CLEAR();
			else if (id.equals("extend"))
				return new Token.EXTEND();
			else if (id.equals("invert"))
				return new Token.INVERT();
			else if (id.equals("print"))
				return new Token.PRINT();
			else
				return new Token.ID(id);
		} else if ('0' <= c && c <= '9')
			return new Token.NUM(integer(0));
		else {
			return new Token.ONE(read());
		}
	}

	/* ホワイトスペースの読み飛ばしも含む */
	Token getToken() {
		try {
			Token token = native_token();
			if (token instanceof Token.ONE) {
				if (((Token.ONE) token).one == 32)// 半角スペース
					return getToken();
				else if (((Token.ONE) token).one == 12288)// 全角スペース
					return getToken();
				else if (((Token.ONE) token).one == '\t')
					return getToken();
				else if (((Token.ONE) token).one == '\n')
					return getToken();
				else
					return token;
			} else
				return token;
		} catch (End_of_file e) {
			return new Token.EOF();
		}
	}

	/* トークンの印字 */
	void printToken(Token tok) {
		if (tok instanceof Token.ID)
			System.out.print("ID(" + ((Token.ID) tok).id + ") ");
		else if (tok instanceof Token.NUM)
			System.out.print("NUM(" + ((Token.NUM) tok).num + ") ");
		else if (tok instanceof Token.ONE)
			System.out.print("ONE(" + ((Token.ONE) tok).one + ") ");
		else if (tok instanceof Token.EOF)
			System.out.print("EOF ");
		else if (tok instanceof Token.INVERT)
			System.out.print("INVERT ");
	}

	/* お試し実行関数 */
	void run() {
		Token rft = getToken();
		if (rft instanceof Token.EOF)
			throw new End_of_file();
		if (rft instanceof Token.ONE && ((Token.ONE) rft).one == '$')
			throw new End_of_system();
		else
			run();
	}
}
