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

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IListManager;
import net.sf.jmoney.model2.IObjectKey;
import net.sf.jmoney.model2.ListPropertyAccessor;
import net.sf.jmoney.model2.ObjectCollection;
import net.sf.jmoney.model2.PropertyAccessor;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.ScalarPropertyAccessor;

/**
 * This is a special implementation of the IListManager interface. It is used
 * only in objects that are copies in a transaction manager of objects that have
 * been committed to the datastore.
 * <P>
 * This class implements the Collection methods by looking at the differences
 * (objects added and objects deleted) between the committed list and the list
 * in this transaction. These differences are applied to the collection of
 * committed objects returned by the underlying datastore.
 * <P>
 * This class keeps a reference to a ModifiedList object which contains the actual
 * lists of inserted and deleted objects.  All data managers must guarantee that only
 * a single instance of the same object is returned, and as DeltaListManager objects
 * are held by extendable objects, we can be sure that only a single instance of this
 * object will exist for a given list.  Furthermore, all updates are made through this
 * object, so if a ModifiedList object is created, this object will know about it.
 * 
 * @author Nigel Westbury
 */
public class DeltaListManager<E extends ExtendableObject> extends AbstractCollection<E> implements IListManager<E> {

	private TransactionManager transactionManager;
	
	/**
	 * Key, containing the parent object and the list accessor, that uniquely
	 * identifies a list. This key is used to fetch from the transaction manager
	 * the object (if any) that contains the modifications that have been made
	 * to the list.
	 */
	private ModifiedListKey<E> modifiedListKey;

	/**
	 * The modified list.
	 * Null indicates no modification yet to this list.
	 */
	private ModifiedList<E> modifiedList = null;

	/**
	 * The committed list, set by the constructor
	 */
	private ObjectCollection<E> committedList;

	/**
	 * @param committedParent the object containing the list property.  This
	 * 			object must be an uncommitted object 
	 * @param propertyAccessor the list property
	 */
	public DeltaListManager(TransactionManager transactionManager, ExtendableObject committedParent, ListPropertyAccessor<E> listProperty) {
		this.transactionManager = transactionManager;
		this.modifiedListKey = new ModifiedListKey<E>(committedParent.getObjectKey(), listProperty);
		this.committedList = committedParent.getListPropertyValue(listProperty);
	}

	/**
	 * Create a new extendable object in the list represented by this object.
	 * <P>
	 * This method does not create the object in the underlying committed list,
	 * because if it did that then other views would see the object before it is
	 * committed. Instead this method adds the object to a list maintained by
	 * the transaction manager. When the consumer iterates over the list, the objects in the
	 * 'added' list are appended to the items returned by the underlying
	 * committed list.
	 */
	public <F extends E> F createNewElement(ExtendableObject parent, PropertySet<F> propertySet) {
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
		for (Iterator iter = constructorProperties.iterator(); iter.hasNext(); iter.next()) {
			constructorParameters[index++] = new UncommittedListManager(transactionManager);
		}
		
		// We can now create the object.
		F extendableObject = propertySet.constructDefaultImplementationObject(constructorParameters);
		
		objectKey.setObject(extendableObject);

		if (modifiedList == null) {
			modifiedList = new ModifiedList<E>();
			transactionManager.putModifiedList(modifiedListKey, modifiedList);
		}
		modifiedList.add(extendableObject);
		
		return extendableObject;
	}

	/**
	 * Create a new extendable object in the list represented by this object.
	 * This version of this method takes an array of values of the properties in
	 * the object.
	 * <P>
	 * This method does not create the object in the underlying committed list,
	 * because if it did that then other views would see the object before it is
	 * committed. Instead this method adds the object to a list maintained by
	 * the transaction manager. When the consumer iterates over the list, the
	 * objects in the 'added' list are appended to the items returned by the
	 * underlying committed list.
	 * <P>
	 * This method is used only if transactions are nested. The API between the
	 * model and the application does not support a method for creating an
	 * object and setting the property values in a single call. That can only be
	 * done when using a transaction manager.
	 */
	public <F extends E> F createNewElement(ExtendableObject parent, PropertySet<F> propertySet, Object[] values) {
		Collection constructorProperties = propertySet.getConstructorProperties();
		
		JMoneyPlugin.myAssert (!propertySet.isExtension());

		int numberOfParameters = 3 + constructorProperties.size();
		Object[] constructorParameters = new Object[numberOfParameters];
		
		UncommittedObjectKey objectKey = new UncommittedObjectKey(transactionManager);
		
		Map<PropertySet, Object[]> extensionMap = new HashMap<PropertySet, Object[]>();
		
		constructorParameters[0] = objectKey;
		constructorParameters[1] = extensionMap;
		constructorParameters[2] = parent.getObjectKey();
		
		// Construct the extendable object using the 'full' constructor.
		// This constructor takes a parameter for every property in the object.
		
		// TODO: This code could be simplified by making better use
		// of getIndexIntoScalarProperties().  We only need one loop.
		
		int valuesIndex = 0;
		for (PropertyAccessor propertyAccessor: propertySet.getProperties3()) {
			
			Object value;
			if (propertyAccessor.isScalar()) {
				ScalarPropertyAccessor scalarAccessor = (ScalarPropertyAccessor)propertyAccessor;
				if (valuesIndex != scalarAccessor.getIndexIntoScalarProperties()) {
					throw new RuntimeException("index mismatch");
				}
				value = values[valuesIndex++];
			} else {
				value = new UncommittedListManager(transactionManager);
			}
			
			// Determine how this value is passed to the constructor.
			// If the property comes from an extension then we must set
			// the property into an extension, otherwise we simply set
			// the property into the constructor parameters.
			if (!propertyAccessor.getPropertySet().isExtension()) {
				constructorParameters[propertyAccessor.getIndexIntoConstructorParameters()] = value;
			} else {
				Object [] extensionConstructorParameters = extensionMap.get(propertyAccessor.getPropertySet());
				if (extensionConstructorParameters == null) {
					extensionConstructorParameters = new Object [propertyAccessor.getPropertySet().getConstructorProperties().size()];
					extensionMap.put(propertyAccessor.getPropertySet(), extensionConstructorParameters);
				}
				extensionConstructorParameters[propertyAccessor.getIndexIntoConstructorParameters()] = value;
			}
		}
		
		// We can now create the object.
		F extendableObject = propertySet.constructImplementationObject(constructorParameters);
		
		objectKey.setObject(extendableObject);

		if (modifiedList == null) {
			modifiedList = new ModifiedList<E>();
			transactionManager.putModifiedList(modifiedListKey, modifiedList);
		}
		modifiedList.add(extendableObject);
		
		return extendableObject;
	}
	
	public int size() {
		// This method is called, for example when getting the number of entries
		// in a transaction.
		
		int committedCount = committedList.size();

		if (modifiedList == null) {
			return committedCount; 
		} else {
			return committedCount + modifiedList.addedObjects.size() - modifiedList.deletedObjects.size();
		}
	}

	public Iterator<E> iterator() {
		Iterator<E> committedListIterator = committedList.iterator();

		if (modifiedList == null) {
			// We cannot simply return committedListIterator because
			// that returns materializations of the objects that are outside
			// of the transaction.  We must return objects that are versions
			// inside the transaction.
			return new DeltaListIterator<E>(transactionManager, committedListIterator, new Vector<E>(), new Vector<IObjectKey>());
		} else {
			return new DeltaListIterator<E>(transactionManager, committedListIterator, modifiedList.addedObjects, modifiedList.deletedObjects);
		}
	}

	public boolean contains(Object object) {
		IObjectKey committedObjectKey = ((UncommittedObjectKey)((ExtendableObject)object).getObjectKey()).getCommittedObjectKey();

		if (modifiedList != null) {
			if (modifiedList.addedObjects.contains(object)) {
				return true; 
			} else if (modifiedList.deletedObjects.contains(committedObjectKey)) {
				return false;
			}
		}
		
		// The object has neither been added or removed by us, so
		// pass the request on the the underlying datastore.
		return committedList.contains(committedObjectKey.getObject());
	}

	public boolean remove(Object object) {
		ExtendableObject extendableObject = (ExtendableObject)object;
		if (modifiedList == null) {
			modifiedList = new ModifiedList<E>();
			transactionManager.putModifiedList(modifiedListKey, modifiedList);
		}
		boolean isRemoved = modifiedList.delete(extendableObject);
		
		if (isRemoved) {
			IObjectKey committedObjectKey = ((UncommittedObjectKey)extendableObject.getObjectKey()).getCommittedObjectKey();
			ModifiedObject modifiedObject = transactionManager.modifiedObjects.get(committedObjectKey);
			if (modifiedObject == null) {
				modifiedObject = new ModifiedObject();
				transactionManager.modifiedObjects.put(committedObjectKey, modifiedObject);
			}
			modifiedObject.setDeleted();
		}
		
		return isRemoved;
	}
}
