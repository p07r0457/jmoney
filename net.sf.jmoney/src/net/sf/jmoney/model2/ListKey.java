package net.sf.jmoney.model2;

import org.eclipse.core.runtime.Assert;

/**
 * This class acts as a key to a list of objects.  It allows an object
 * to contain a reference to its parent without the need to instantiate
 * the parent.
 * <P>
 * If the accounts are stored in a serialized file such as an XML file then
 * all objects are instantiated anyway, but if the accounts are kept in
 * a database then we don't always instantiate objects unless they are needed.
 *
 * @param <E> the class of objects kept in the list
 */
public class ListKey<E extends ExtendableObject> {
	private IObjectKey parentKey;
	private ListPropertyAccessor<E> listProperty;

	public ListKey(IObjectKey parentKey, ListPropertyAccessor<E> listProperty) {
		this.parentKey = parentKey;
		this.listProperty = listProperty;
	}

	public IObjectKey getParentKey() {
		return parentKey;
	}
	
	public ListPropertyAccessor<E> getListPropertyAccessor() {
		return listProperty;
	}
	
	@Override
	public boolean equals(Object other) {
		Assert.isTrue(other instanceof ListKey);
		ListKey otherListKey = (ListKey)other;
		return parentKey.equals(otherListKey.parentKey)
			&& listProperty == otherListKey.listProperty;
	}
}
