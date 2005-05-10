/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2004 Nigel Westbury <westbury@users.sourceforge.net>
 *
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */

package net.sf.jmoney.model2;

import java.util.Collection;
import java.util.Iterator;

import net.sf.jmoney.JMoneyPlugin;

/**
 * This class is used to provide access to lists of objects
 * contained in a list property of a parent object.
 * 
 * @author Nigel Westbury
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
	
	public ExtendableObject createNewElement(PropertySet actualPropertySet) {
		final ExtendableObject newObject = listManager.createNewElement(parent, actualPropertySet);
		
		parent.getSession().getChangeManager().processObjectCreation(parent, listPropertyAccessor, newObject);
		
		// Fire the event.
		parent.getSession().fireEvent(
				new ISessionChangeFirer() {
					public void fire(SessionChangeListener listener) {
						listener.objectAdded(newObject);
					}
				});
		
		
		return newObject;
	}
	
	public ExtendableObject createNewElement(PropertySet actualPropertySet, Object values[]) {
		final ExtendableObject newObject = listManager.createNewElement(parent, actualPropertySet, values);
		
		parent.getSession().getChangeManager().processObjectCreation(parent, listPropertyAccessor, newObject);
		
		// Fire the event.
		parent.getSession().fireEvent(
				new ISessionChangeFirer() {
					public void fire(SessionChangeListener listener) {
						listener.objectAdded(newObject);
					}
				});
		
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
	
	/**
	 * The object must exist in the collection.  If it does
	 * not, the results are undefined.
	 */
	public boolean remove(Object object) {
		// Deletion events are fired before the object is removed from the
		// datastore.  This is necessary because once the object has been
		// removed from the datastore, it may no longer be possible to fetch
		// from the datastore lists of objects owned by the object.
		
		// We do not bother to check that the object exists in the given list.
		// Therefore notifications of the deletion of the object will be sent
		// even if the object does not exist in the list.  This situation should
		// be considered a bug in the caller code.
		
		if (object instanceof ExtendableObject && listManager.contains(object)) {
			final ExtendableObject extendableObject = (ExtendableObject)object;
			
			parent.getSession().fireEvent(
					new ISessionChangeFirer() {
						public void fire(SessionChangeListener listener) {
							listener.objectDeleted(extendableObject);
						}
					});
			
			// Notify the change manager.
			parent.getSession().getChangeManager().processObjectDeletion(parent, listPropertyAccessor, extendableObject);
			
			boolean found = listManager.remove(object);
			JMoneyPlugin.myAssert(found);
			
			return true;
		} else {
			return false;
		}
	}
	
	public boolean containsAll(Collection arg0) {
		return listManager.containsAll(arg0);
	}
	
	public boolean addAll(Collection arg0) {
		throw new UnsupportedOperationException();
	}
	
	public boolean removeAll(Collection arg0) {
		// TODO: implement this
		throw new RuntimeException("not yet implemented");
	}
	
	public boolean retainAll(Collection arg0) {
		// TODO: implement this
		throw new RuntimeException("not yet implemented");
	}
	
	public void clear() {
		// TODO: implement this
		throw new RuntimeException("not yet implemented");
	}
	
}
