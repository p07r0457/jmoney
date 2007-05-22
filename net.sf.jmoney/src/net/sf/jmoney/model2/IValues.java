package net.sf.jmoney.model2;

import java.util.Collection;

public interface IValues {
	<V> V getScalarValue(ScalarPropertyAccessor<V> propertyAccessor);
	IObjectKey getReferencedObjectKey(ScalarPropertyAccessor<? extends ExtendableObject> propertyAccessor);
	<E extends ExtendableObject> IListManager<E> getListManager(IObjectKey listOwnerKey, ListPropertyAccessor<E> listAccessor);

	/**
	 * This method can be used to get the set of extensions that contain
	 * non-default values.  By using this method, one can avoid creating
	 * extensions that contain only default property values.
	 * 
	 * @return
	 */
	Collection<ExtensionPropertySet<?>> getNonDefaultExtensions();
}
