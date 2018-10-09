package scalar.field;

public class DoubleField implements Field<Double> {

	@Override
	public Double zero() {
		return new Double(0.0d);
	}

	@Override
	public Double one() {
		return new Double(1.0d);
	}

	@Override
	public Double add(Double a, Double b) {
		return a + b;
	}

	@Override
	public Double multiply(Double a, Double b) {
		return a * b;
	}

	@Override
	public Double opposite(Double a) {
		return -a;
	}

	@Override
	public Double inverse(Double a) throws NotInvertibleException {
		if (a != 0)
			return 1.0d / a;
		else
			throw new NotInvertibleException("division by zero.");
	}

	@Override
	public Double pow(Double a, Double b) throws PowerIsUnableException {
		return Math.pow(a, b);
	}

	@Override
	public Double makeElement(String num) {
		return Double.parseDouble(num);
	}

}
