package scalar.field;

public class RationalField implements Field<Frac> {

	@Override
	public Frac zero() {
		return new Frac(0, 1);
	}

	@Override
	public Frac one() {
		return new Frac(1, 1);
	}

	@Override
	public Frac add(Frac a, Frac b) {
		return new Frac(a.num * b.den + b.num * a.den, a.den * b.den).reduction();
	}

	@Override
	public Frac multiply(Frac a, Frac b) {
		return new Frac(a.num * b.num, a.den * b.den).reduction();
	}

	@Override
	public Frac opposite(Frac a) {
		return new Frac(-a.num, a.den);
	}

	@Override
	public Frac inverse(Frac a) throws NotInvertibleException {
		if (a.num == 0)
			throw new NotInvertibleException(a.num);
		else
			return new Frac(a.den, a.num).reduction();
	}

	@Override
	public Frac pow(Frac a, Frac b) throws PowerIsUnableException {
		return null;
	}

	@Override
	public Frac makeElement(String num) {
		return new Frac(Integer.parseInt(num), 1);
	}
}
