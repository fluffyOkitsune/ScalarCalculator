package scalar.field;

@SuppressWarnings("serial")
public class NotInvertibleException extends RuntimeException {

	public NotInvertibleException() {
		super();
	}

	public NotInvertibleException(Object o) {
		super(o.toString() + " is not invertible.");
	}
}
