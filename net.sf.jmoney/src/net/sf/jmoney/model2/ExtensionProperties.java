package net.sf.jmoney.model2;

/**
 *
 * @author Nigel Westbury
 */
public class ExtensionProperties {
	private PropertySet propertySet;
	private Object[] values;

	public ExtensionProperties(PropertySet propertySet, Object [] values) {
		this.propertySet = propertySet;
		this.values = values;
	}
	
	public void setProperties(ExtensionObject extensionObject) {
		propertySet.setProperties(extensionObject, values);
	}
}
