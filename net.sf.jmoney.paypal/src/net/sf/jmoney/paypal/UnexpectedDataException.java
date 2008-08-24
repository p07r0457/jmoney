package net.sf.jmoney.paypal;

public class UnexpectedDataException extends Exception {
	private static final long serialVersionUID = 1L;

	public UnexpectedDataException(String columnName, String columnValue) {
		super("An unexpected value was found. " + columnName + " found with a value of " + columnValue);
	}

	public UnexpectedDataException(String generalMessage) {
		super(generalMessage);
	}

}
