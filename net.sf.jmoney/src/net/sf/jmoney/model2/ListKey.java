package net.sf.jmoney.model2;

import org.eclipse.core.runtime.Assert;

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
