package net.sf.jmoney.oda.driver;


public abstract class Column {
	
	String name;
	String displayName;
	Class classOfValueObject;
	boolean isNullAllowed;
	
	public Column(String name, String displayName, Class classOfValueObject, boolean isNullAllowed) {
		this.name = name;
		this.displayName = displayName;
		this.classOfValueObject = classOfValueObject;
		this.isNullAllowed = isNullAllowed;
	}

	public String getName() {
		return name;
	}

	public String getDisplayName() {
		return displayName;
	}

	public Class getClassOfValueObject() {
		return classOfValueObject;
	}

	public boolean isNullAllowed() {
		return isNullAllowed;
	}
	
	abstract Object getValue();
}
