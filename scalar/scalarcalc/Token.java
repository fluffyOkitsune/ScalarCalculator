package scalar.scalarcalc;

//----------------------------------------------------------------------
//トークン
//----------------------------------------------------------------------
//(*'ω'*)JAVAはtype文がないからつらいです...
interface Token {
	// CLEAR
	class CLEAR implements Cloneable, Token {
		@Override
		public CLEAR clone() {
			return new CLEAR();
		}

		@Override
		public String toString() {
			return "CLEAR";
		}

	}

	// EXTEND
	class EXTEND implements Cloneable, Token {
		@Override
		public EXTEND clone() {
			return new EXTEND();
		}

		@Override
		public String toString() {
			return "EXTEND";
		}
	}

	// EOF
	class EOF implements Cloneable, Token {
		@Override
		public EOF clone() {
			return new EOF();
		}

		@Override
		public String toString() {
			return "EOF";
		}
	}

	// PRINT
	class PRINT implements Cloneable, Token {
		@Override
		public PRINT clone() {
			return new PRINT();
		}

		@Override
		public String toString() {
			return "PRINT";
		}
	}

	// INVERT
	class INVERT implements Cloneable, Token {
		@Override
		public INVERT clone() {
			return new INVERT();
		}

		@Override
		public String toString() {
			return "INVERT";
		}
	}

	// NUM of int
	class NUM implements Cloneable, Token {
		public int num;

		NUM(int i) {
			num = i;
		}

		@Override
		public NUM clone() {
			return new NUM(num);
		}

		@Override
		public String toString() {
			return "NUM";
		}
	}

	// ID of string
	class ID implements Cloneable, Token {
		public String id;

		ID(String s) {
			id = s;
		}

		@Override
		public ID clone() {
			return new ID(id);
		}

		@Override
		public String toString() {
			return "ID";
		}
	}

	// ONE of char
	class ONE implements Token {
		public char one;

		ONE(char c) {
			one = c;
		}

		@Override
		public ONE clone() {
			return new ONE(one);
		}

		@Override
		public String toString() {
			return Character.toString(one);
		}
	}

	Token clone();

	String toString();
}
