package net.sf.jmoney.model2;

/**
 *
 * @author Nigel Westbury
 */
public class ExtensionProperties {
	private PropertySet propertySet;
	private Object[] oldValues;
	private Object[] newValues;

	public ExtensionProperties(PropertySet propertySet, Object [] oldValues, Object [] newValues) {
		this.propertySet = propertySet;
		this.oldValues = oldValues;
		this.newValues = newValues;
	}
	
	public void setProperties(ExtensionObject extensionObject) {
		// TODO: complete this
	}
}
