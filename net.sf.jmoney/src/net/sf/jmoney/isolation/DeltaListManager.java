/*
*
*  JMoney - A Personal Finance Manager
*  Copyright (c) 2005 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.isolation;

import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IListManager;
import net.sf.jmoney.model2.ISessionManager;
import net.sf.jmoney.model2.PropertyAccessor;
import net.sf.jmoney.model2.PropertySet;

/**
 * This is a special implementation of the IListManager interface.
 * It is used only in objects that are copies in a transaction manager
 * of objects that have been committed to the datastore.
 * <P>
 * This class keeps the differences (objects added and objects deleted)
 * between the committed list and the list in this transaction.
 * 
 * @author Nigel Westbury
 */
public class DeltaListManager implements IListManager {

	TransactionManager transactionManager;
	
	private ISessionManager sessionManager = null;
	private IListManager committedList = null;

	// These two enable us to get the ModifiedList object,
	// they also enable us to get the collection of
	// objects in the committed list.
	ExtendableObject parent;
	PropertyAccessor listProperty;
	
	private ModifiedList modifiedList;
	
	/**
	 * @param object the object containing the list property 
	 * @param propertyAccessor the list property
	 */
	public DeltaListManager(TransactionManager transactionManager, ExtendableObject parent, PropertyAccessor listProperty) {
		this.transactionManager = transactionManager;
		this.parent = parent;
		this.listProperty = listProperty;
	}

	/**
	 * Create a new extendable object in the list represented by this object.
	 * <P>
	 * This method does not create the object in the underlying committed list,
	 * because if it did that then other views would see the object before it is
	 * committed. Instead this method adds the object to a list maintained by
	 * this object. When the consumer iterates over the list, the objects in the
	 * 'added' list are appended to the items returned by the underlying
	 * committed list.
	 */
	public ExtendableObject createNewElement(ExtendableObject parent, PropertySet propertySet/*, Object[] values, ExtensionProperties[] extensionProperties */) {
		Collection constructorProperties = propertySet.getDefaultConstructorProperties();
		
		JMoneyPlugin.myAssert (!propertySet.isExtension());

		int numberOfParameters = 3 + constructorProperties.size();
		Object[] constructorParameters = new Object[numberOfParameters];
		
		UncommittedObjectKey objectKey = new UncommittedObjectKey(transactionManager);
		
		constructorParameters[0] = objectKey;
		constructorParameters[1] = null;
		constructorParameters[2] = parent.getObjectKey();
		
		// Construct the extendable object using the 'default' constructor.
		// This constructor takes the minimum number of parameters necessary
		// to properly construct the object, setting default values for all
		// the scalar properties.  We must, however, pass objects that manage
		// any lists within the object.
		
		// Add a list manager for each list property in the object.
		int index = 3;
		for (Iterator iter = constructorProperties.iterator(); iter.hasNext(); ) {
			PropertyAccessor propertyAccessor = (PropertyAccessor)iter.next();
			constructorParameters[index++] = new UncommittedListManager(transactionManager);
		}
		
		// We can now create the object.
		ExtendableObject extendableObject = (ExtendableObject)propertySet.constructDefaultImplementationObject(constructorParameters);
		
		objectKey.setObject(extendableObject);

		if (modifiedList == null) {
			ExtendableObject committedParent = ((UncommittedObjectKey)parent.getObjectKey()).getCommittedObject();
			modifiedList = transactionManager.createModifiedList(committedParent, listProperty);
		}
		modifiedList.add(extendableObject);
		
		return extendableObject;
	}
	
	public int size() {
		// This method is called, for example when getting the number of entries
		// in a transaction.
		
		// TODO: When the data model is changed to expose Collections, not Iterators,
		// this loop can be removed.
		int count = 0;
		Iterator committedListIterator = parent.getPropertyIterator(listProperty);
		while (committedListIterator.hasNext()) {
			committedListIterator.next();
			count++;
		}
		
		if (modifiedList == null) {
			return count; 
		} else {
			return count + modifiedList.addedObjects.size() - modifiedList.deletedObjects.size();
		}
	}

	public boolean isEmpty() {
		return size() == 0;
	}

	public boolean contains(Object object) {
		throw new RuntimeException("not implemented");
	}

	public Iterator iterator() {
		Iterator committedListIterator = parent.getPropertyIterator(listProperty);
		if (modifiedList == null) {
			// We cannot simply return committedListIterator because
			// that returns materializations of the objects that are outside
			// of the transaction.  We must return objects that are versions
			// inside the transaction.
			return new DeltaListIterator(transactionManager, committedListIterator, new Vector(), new Vector());
		} else {
			return new DeltaListIterator(transactionManager, committedListIterator, modifiedList.addedObjects, modifiedList.deletedObjects);
		}
	}

	public Object[] toArray() {
		throw new RuntimeException("not implemented");
	}

	public Object[] toArray(Object[] arg0) {
		throw new RuntimeException("not implemented");
	}

	public boolean add(Object arg0) {
		throw new RuntimeException("not implemented");
	}

	public boolean remove(Object object) {
		if (modifiedList == null) {
			modifiedList = transactionManager.createModifiedList(parent, listProperty);
		}
		return modifiedList.delete((ExtendableObject)object);
	}

	public boolean containsAll(Collection arg0) {
		throw new RuntimeException("not implemented");
	}

	public boolean addAll(Collection arg0) {
		throw new RuntimeException("not implemented");
	}

	public boolean removeAll(Collection arg0) {
		throw new RuntimeException("not implemented");
	}

	public boolean retainAll(Collection arg0) {
		throw new RuntimeException("not implemented");
	}

	public void clear() {
		throw new RuntimeException("not implemented");
	}
}
