package scalar.field;

public class IntegerField implements Field<Integer> {
	@Override
	public Integer zero() {
		return new Integer(0);
	}

	@Override
	public Integer one() {
		return new Integer(1);
	}

	@Override
	public Integer add(Integer a, Integer b) {
		return new Integer(a.intValue() + b.intValue());
	}

	@Override
	public Integer multiply(Integer a, Integer b) {
		return new Integer(a.intValue() * b.intValue());
	}

	@Override
	public Integer pow(Integer a, Integer b) throws PowerIsUnableException {
		return new Integer((int) Math.pow(a.intValue(), b.intValue()));
	}

	@Override
	public Integer opposite(Integer a) {
		return new Integer(-(a.intValue()));
	}

	@Override
	public Integer inverse(Integer a) throws NotInvertibleException {
		if (a == 1)
			return new Integer(1);
		else
			throw new NotInvertibleException();
	}

	@Override
	public Integer makeElement(String num) {
		return Integer.parseInt(num);
	}
}
