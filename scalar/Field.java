package scalar;

public enum Field {
	Real, Complex, Galois;

	private int order;

	public final void setOrder(int i) {
		order = i;
	}

	public final int getOrder() {
		return order;
	}
}
