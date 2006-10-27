package net.sf.jmoney.model2;

import java.lang.reflect.InvocationTargetException;

public class ListPropertyAccessor<E extends ExtendableObject> extends PropertyAccessor {

	/**
	 * 
	 * the property set for the properties that are contained in the
	 * elements of this list.  Note that elements containing derived property
	 * sets may be added to the list.
	 */
	private ExtendablePropertySet<E> elementPropertySet;

	public ListPropertyAccessor(PropertySet parentPropertySet, String localName, String displayName, ExtendablePropertySet<E> elementPropertySet, IPropertyDependency propertyDependency) {
		super(parentPropertySet, localName, displayName, elementPropertySet.classOfObject, propertyDependency);

		this.elementPropertySet = elementPropertySet;

		// Use introspection on the interface to find the getXxxCollection method.
		theGetMethod = findMethod("get", localName + "Collection", null);

		if (!ObjectCollection.class.isAssignableFrom(theGetMethod.getReturnType())) { 		
			throw new MalformedPluginException("Method '" + theGetMethod.getName() + "' in '" + parentPropertySet.getImplementationClass().getName() + "' must return an ObjectSet type.");
		}
	}

	/**
	 * Indicates if the property is a list of objects.
	 */
	public boolean isList() {
		return true;
	}

	/**
	 * Returns the class for the values in the lists. This is the class of
	 * the items contained in the collections returned by the getter method
	 * 
	 * @return
	 */
	public ExtendablePropertySet<E> getElementPropertySet() {
		return elementPropertySet;
	}

	/**
	 */
	public ObjectCollection<E> invokeGetMethod(Object invocationTarget) {
		try {
			return (ObjectCollection<E>)theGetMethod.invoke(invocationTarget, (Object [])null);
		} catch (InvocationTargetException e) {
			// TODO Process this properly
			e.printStackTrace();
			throw new RuntimeException("Plugin error");
		} catch (Exception e) {
			// IllegalAccessException and IllegalArgumentException exceptions should
			// not be possible here because the method was checked
			// for correct access rights and parameters during initialization.
			// Therefore throw a runtime exception.
			e.printStackTrace();
			throw new RuntimeException("internal error");
		}
	}

}
