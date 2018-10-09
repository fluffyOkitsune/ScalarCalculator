package scalar.field;

public interface Field<E extends Comparable<E>> {
	public abstract E zero();

	public abstract E one();

	public abstract E add(E a, E b);

	public abstract E multiply(E a, E b);

	public abstract E opposite(E a);

	public abstract E inverse(E a) throws NotInvertibleException;

	public abstract E pow(E a, E b) throws PowerIsUnableException;

	public abstract E makeElement(String num);

}
