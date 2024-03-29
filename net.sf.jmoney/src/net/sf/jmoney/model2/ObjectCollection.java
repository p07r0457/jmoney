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
	ListKey<E> listKey;
	
	public ObjectCollection(IListManager<E> listManager, ExtendableObject parent, ListPropertyAccessor<E> listPropertyAccessor) {
		this.listManager = listManager;
		this.listKey = new ListKey<E>(parent.getObjectKey(), listPropertyAccessor);
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
		final F newObject = listManager.createNewElement(actualPropertySet);
		
		listKey.getParentKey().getSession().getChangeManager().processObjectCreation(listKey, newObject);
		
		// Fire the event.
		listKey.getParentKey().getDataManager().fireEvent(
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
	 * This may be a top level insert or a descendant of an object that was inserted in
	 * the same transaction.  We must know the difference so we can fire the objectInserted
	 * event methods correctly. We therefore need a flag to indicate this.
	 * 
	 * @param isDescendentInsert true if this object is being inserted because its parent is
	 * 			being inserted in the same transaction, false if this object is being inserted
	 *          into a list that existed prior to this transaction
	 */
	public <F extends E> F createNewElement(ExtendablePropertySet<F> actualPropertySet, IValues values, final boolean isDescendentInsert) {
		final F newObject = listManager.createNewElement(actualPropertySet, values);
		
		listKey.getParentKey().getSession().getChangeManager().processObjectCreation(listKey, newObject);
		
		return newObject;
	}
	
	/**
	 * Moves the given object into this collection, removing it from its
	 * current parent.
	 */
	@SuppressWarnings("unchecked")
	public void moveElement(final E extendableObject) {
		Assert.isTrue(listKey.getParentKey().getDataManager() == extendableObject.getDataManager());
		
		final ListKey originalListKey = extendableObject.getParentListKey();
		
		/*
		 * Note that if the parent object is not materialized (meaning that the
		 * getObject method in the following line needs to materialize the
		 * parent) then we really don't need to do anything to remove the object
		 * from the parent's list. However, there is no API for this, and the
		 * extra code for such a small optimization is not worth it.
		 */
		ObjectCollection originalCollection = originalListKey.getParentKey().getObject().getListPropertyValue(originalListKey.getListPropertyAccessor());
		IListManager originalListManager = originalCollection.listManager;

		// Move in the underlying datastore.
		listManager.moveElement(extendableObject, originalListManager);

		listManager.add(extendableObject);
		originalListManager.remove(extendableObject);
		extendableObject.parentKey = listKey;

		listKey.getParentKey().getDataManager().fireEvent(
				new ISessionChangeFirer() {
					public void fire(SessionChangeListener listener) {
						listener.objectMoved(
								extendableObject, 
								originalListKey.getParentKey().getObject(),
								listKey.getParentKey().getObject(),
								originalListKey.getListPropertyAccessor(),
								listKey.getListPropertyAccessor()
						);
					}
				});
		
		listKey.getParentKey().getSession().getChangeManager().processObjectMove(extendableObject, originalListKey);
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
		/*
		 * The Collection methods that mutate the collection should not be
		 * used.  Use instead createNewElement, deleteElement, and moveElement.
		 */
		throw new UnsupportedOperationException();
	}
	
	public boolean remove(Object object) {
		/*
		 * The Collection methods that mutate the collection should not be
		 * used.  Use instead createNewElement, deleteElement, and moveElement.
		 */
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Removes an object from the collection. Because this collection 'owns' the
	 * object, this results in the object being deleted. This method ensures
	 * that listeners are notified as appropriate.
	 * 
	 * @return true if the object was deleted, false if the object had
	 *         references to it and so the object could not be deleted
	 * @throws ReferenceViolationException
	 *             if the object cannot be deleted because there are
	 *             references to it
	 * @throws RuntimeException
	 *             if the object does not exist in the collection
	 */
	public void deleteElement(E extendableObject) throws ReferenceViolationException {
		if (extendableObject.getDataManager() != listKey.getParentKey().getDataManager()) {
    		throw new RuntimeException("Invalid call to remove.  The object passed does not belong to the data manager that is the base data manager of this collection."); //$NON-NLS-1$
		}
		
		/*
		 * Check that the object is in the list.  It is in this list if the parent
		 * object is the same and the list property is the same.
		 */
		if (!extendableObject.parentKey.equals(listKey)) {
			throw new RuntimeException("Passed object is not in the list.");
		}
		
		final E objectToRemove = listKey.getListPropertyAccessor().getElementPropertySet().getImplementationClass().cast(extendableObject);

		/*
		 * Deletion events are fired before the object is removed from the
		 * datastore. This is necessary because listeners processing the
		 * object deletion may need to fetch information about the object
		 * from the datastore.
		 */
		listKey.getParentKey().getDataManager().fireEvent(
				new ISessionChangeFirer() {
					public void fire(SessionChangeListener listener) {
						listener.objectRemoved(objectToRemove);
					}
				});

		// Notify the change manager.
		listKey.getParentKey().getSession().getChangeManager().processObjectDeletion(listKey.getParentKey().getObject(), listKey.getListPropertyAccessor(), objectToRemove);

		listManager.deleteElement(objectToRemove);
	}
	
	public boolean containsAll(Collection<?> arg0) {
		return listManager.containsAll(arg0);
	}
	
	public boolean addAll(Collection<? extends E> arg0) {
		/*
		 * The Collection methods that mutate the collection should not be
		 * used.  Use instead createNewElement, deleteElement, and moveElement.
		 */
		throw new UnsupportedOperationException();
	}
	
	public boolean removeAll(Collection<?> arg0) {
		/*
		 * The Collection methods that mutate the collection should not be
		 * used.  Use instead createNewElement, deleteElement, and moveElement.
		 */
		throw new UnsupportedOperationException();
	}
	
	public boolean retainAll(Collection<?> arg0) {
		/*
		 * The Collection methods that mutate the collection should not be
		 * used.  Use instead createNewElement, deleteElement, and moveElement.
		 */
		throw new UnsupportedOperationException();
	}
	
	public void clear() {
		/*
		 * The Collection methods that mutate the collection should not be
		 * used.  Use instead createNewElement, deleteElement, and moveElement.
		 */
		throw new UnsupportedOperationException();
	}
}
