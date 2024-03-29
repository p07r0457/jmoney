package net.sf.jmoney.model2;

import java.util.Comparator;

/**
 * This class provides a default implementation of some of the methods
 * in the IPropertyControlFactory interface.  Plug-ins do not have to use
 * this class but can instead implement the interface directly.
 * <P>
 * This class, like the IPropertyControlFactory interface, is parameterized
 * with the class of values of the property.  However, this class further
 * requires that the class implements the Comparable interface.  This allows
 * this class to provide a default implementation of the getComparator method.
 * If the values of the property are of a class that does not implement Comparable
 * then this helper class cannot be used.
 *  
 * @author Nigel Westbury
 *
 */
public abstract class PropertyControlFactory<V extends Comparable<? super V>> implements IPropertyControlFactory<V> {

	public String formatValueForTable(ExtendableObject extendableObject, ScalarPropertyAccessor<? extends V> propertyAccessor) {
		V value = extendableObject.getPropertyValue(propertyAccessor);
		return value.toString();
	}

	public String formatValueForMessage(ExtendableObject extendableObject, ScalarPropertyAccessor<? extends V> propertyAccessor) {
		V value = extendableObject.getPropertyValue(propertyAccessor);
		return value.toString();
	}

	public Comparator<V> getComparator() {
		return new Comparator<V>() {
			public int compare(V value1, V value2) {
				return value1.compareTo(value2);
			}
		};
	}
}
