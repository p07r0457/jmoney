/*
 * Created on Apr 12, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.sf.jmoney.model2;

import java.util.Collection;
import java.util.Iterator;

import net.sf.jmoney.fields.SessionInfo;

/**
 * @author Nigel
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class ObjectCollection implements Collection {

	private IListManager listManager;
	ExtendableObject parent;
	PropertyAccessor listPropertyAccessor;
	
	ObjectCollection(IListManager listManager, ExtendableObject parent, PropertyAccessor listPropertyAccessor) {
		this.listManager = listManager;
		this.parent = parent;
		this.listPropertyAccessor = listPropertyAccessor;
	}
	
    public ExtendableObject createNewElement(PropertySet actualPropertySet, Object values[]) {
		ExtendableObject newObject = listManager.createNewElement(parent, actualPropertySet, values);

		parent.processObjectAddition(listPropertyAccessor, newObject);
		
		return newObject;
    }
    
	public int size() {
		return listManager.size();
	}

	public boolean isEmpty() {
		return listManager.isEmpty();
	}

	public boolean contains(Object arg0) {
		return listManager.contains(arg0);
	}

	public Iterator iterator() {
		return listManager.iterator();
	}

	public Object[] toArray() {
		return listManager.toArray();
	}

	public Object[] toArray(Object[] arg0) {
		return listManager.toArray(arg0);
	}

	public boolean add(Object arg0) {
		throw new UnsupportedOperationException();
	}

	public boolean remove(Object arg0) {
		return listManager.remove(arg0);
	}

	public boolean containsAll(Collection arg0) {
		return listManager.containsAll(arg0);
	}

	public boolean addAll(Collection arg0) {
		throw new UnsupportedOperationException();
	}

	public boolean removeAll(Collection arg0) {
		return listManager.removeAll(arg0);
	}

	public boolean retainAll(Collection arg0) {
		return listManager.retainAll(arg0);
	}

	public void clear() {
		listManager.clear();
	}

}
