package scalar.field;

import java.math.BigInteger;

public class GaloisField implements Field<BigInteger> {
	private final BigInteger mod;

	public GaloisField(int modular) {
		mod = new BigInteger(Integer.toString(modular));
	}

	public GaloisField(BigInteger modular) {
		mod = modular;
	}

	@Override
	public BigInteger zero() {
		return BigInteger.ZERO;
	}

	@Override
	public BigInteger one() {
		return BigInteger.ONE;
	}

	@Override
	public BigInteger add(BigInteger a, BigInteger b) {
		return a.add(b).mod(mod);
	}

	@Override
	public BigInteger multiply(BigInteger a, BigInteger b) {
		return a.multiply(b).mod(mod);
	}

	@Override
	public BigInteger pow(BigInteger a, BigInteger b) throws PowerIsUnableException {
		try {
			return a.pow(b.intValue()).mod(mod);
		} catch (ArithmeticException e) {
			throw new PowerIsUnableException(b.toString() + " is nagative.");
		}
	}

	@Override
	public BigInteger opposite(BigInteger a) {
		return a.negate().mod(mod);
	}

	@Override
	public BigInteger inverse(BigInteger a) throws NotInvertibleException {
		try {
			return a.modInverse(mod);
		} catch (ArithmeticException e) {
			throw new NotInvertibleException(a.toString() + "is not invertible");
		}
	}

	@Override
	public BigInteger makeElement(String num) {
		return new BigInteger(num);
	}
}
