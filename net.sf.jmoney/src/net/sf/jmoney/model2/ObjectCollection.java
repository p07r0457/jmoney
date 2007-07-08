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

import org.eclipse.core.runtime.Assert;

/**
 * This class is used to provide access to lists of objects
 * contained in a list property of a parent object.
 * 
 * @author Nigel Westbury
 */
public class ObjectCollection<E extends ExtendableObject> implements Collection<E> {
	
	private IListManager<E> listManager;
	ExtendableObject parent;
	ListPropertyAccessor<E> listPropertyAccessor;
	
	public ObjectCollection(IListManager<E> listManager, ExtendableObject parent, ListPropertyAccessor<E> listPropertyAccessor) {
		this.listManager = listManager;
		this.parent = parent;
		this.listPropertyAccessor = listPropertyAccessor;
	}

	/**
	 * This version of this method is called only by the end-user code, i.e. this method
	 * is not called when a transaction manager is committing its changes to the underlying
	 * data manager.
	 *  
	 * @param <F> the class of the object being created in this list
	 * @param actualPropertySet
	 * @return
	 */
	public <F extends E> F createNewElement(ExtendablePropertySet<F> actualPropertySet) {
		final F newObject = listManager.createNewElement(parent, actualPropertySet);
		
		parent.getSession().getChangeManager().processObjectCreation(parent, listPropertyAccessor, newObject);
		
		// Fire the event.
		parent.getDataManager().fireEvent(
				new ISessionChangeFirer() {
					public void fire(SessionChangeListener listener) {
						listener.objectInserted(newObject);
						listener.objectCreated(newObject);
					}
				});
		
		return newObject;
	}
	
	/**
	 * This version of this method is called only from within a transaction. The values of
	 * the scalar properties are passed so that:
	 * 
	 * - the underlying database need only do a single insert, instead of inserting with
	 *   default values and then updating each value as they are set.
	 *   
	 * - a single notification is fired, passing the object with its final property values,
	 *   rather than sending out an object with default values and then a property change
	 *   notification for each property.
	 *   
	 * This may be a top level insert or a descendent of an object that was inserted in
	 * the same transaction.  We must know the difference so we can fire the objectInserted
	 * event methods correctly. We therefore need a flag to indicate this.
	 * 
	 * @param isDescendentInsert true if this object is being inserted because its parent is
	 * 			being inserted in the same transaction, false if this object is being inserted
	 *          into a list that existed prior to this transaction
	 */
	public <F extends E> F createNewElement(ExtendablePropertySet<F> actualPropertySet, IValues values, final boolean isDescendentInsert) {
		final F newObject = listManager.createNewElement(parent, actualPropertySet, values);
		
		parent.getSession().getChangeManager().processObjectCreation(parent, listPropertyAccessor, newObject);
		
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
	
	public Iterator<E> iterator() {
		return listManager.iterator();
	}
	
	public Object[] toArray() {
		return listManager.toArray();
	}
	
	public <T> T[] toArray(T[] arg0) {
		return listManager.toArray(arg0);
	}
	
	public boolean add(E arg0) {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Removes an object from the collection.  This method ensures that listeners
	 * are notified as appropriate.
	 * 
	 * @return true if the object existed in this collection,
	 * 			false if the object was not in the collection
	 */
	public boolean remove(Object object) {
		if (!(object instanceof ExtendableObject)) {
			return false;
		}
		
		ExtendableObject extendableObject = (ExtendableObject)object;
		
		if (extendableObject.getDataManager() != parent.getDataManager()) {
    		throw new RuntimeException("Invalid call to remove.  The object passed does not belong to the data manager that is the base data manager of this collection.");
		}
		
		// TODO: We should check the containing list property too, as it
		// is possible that there is more than one list in a given parent
		// that is able to contain the object (though this is not possible
		// in the core JMoney schema).
		
		if (extendableObject.parentKey.equals(parent.objectKey)) {
			final E objectToRemove = listPropertyAccessor.getElementPropertySet().getImplementationClass().cast(extendableObject);

			/*
			 * Deletion events are fired before the object is removed from the
			 * datastore. This is necessary because listeners processing the
			 * object deletion may need to fetch information about the object
			 * from the datastore.
			 */
			parent.getDataManager().fireEvent(
					new ISessionChangeFirer() {
						public void fire(SessionChangeListener listener) {
							listener.objectRemoved(objectToRemove);
						}
					});
			
			// Notify the change manager.
			parent.getSession().getChangeManager().processObjectDeletion(parent, listPropertyAccessor, objectToRemove);
			
			boolean found = listManager.remove(object);
			Assert.isTrue(found);
			
			return true;
		}

		// Object was not in the collection
		return false;
	}
	
	public boolean containsAll(Collection<?> arg0) {
		return listManager.containsAll(arg0);
	}
	
	public boolean addAll(Collection<? extends E> arg0) {
		throw new UnsupportedOperationException();
	}
	
	public boolean removeAll(Collection<?> arg0) {
		// TODO: implement this
		throw new RuntimeException("not yet implemented");
	}
	
	public boolean retainAll(Collection<?> arg0) {
		// TODO: implement this
		throw new RuntimeException("not yet implemented");
	}
	
	public void clear() {
		// TODO: implement this
		throw new RuntimeException("not yet implemented");
	}
}
