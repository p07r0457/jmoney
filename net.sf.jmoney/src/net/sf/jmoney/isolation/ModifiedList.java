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

import java.util.Iterator;
import java.util.Vector;

import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IObjectKey;

/**
 * An instance of this class contains the changes that have
 * been made to the set of objects in a list property.
 * These changes consist of new objects added to the list
 * and objects deleted from the list.
 * <P>
 * Changes to properties of objects in the list are not stored
 * in instances of this class.
 * <P>
 * Instances of this class are kept in a map in the transaction
 * manager.  This map maps lists (pair of parent, list property accessor)
 * to objects of this class.
 * 
 * @author Nigel Westbury
 */
public class ModifiedList<E extends ExtendableObject> {

	/**
	 * The uncommitted versions of the objects that have been added
	 */
	Vector<E> addedObjects = new Vector<E>();
	
	/**
	 * The keys to the committed versions of the objects that have been deleted
	 */
	Vector<IObjectKey> deletedObjects = new Vector<IObjectKey>();
	
	/**
	 * @param newObject an uncommitted version of an object
	 * 			being added to the list
	 */
	void add(E newObject) {
		addedObjects.add(newObject);
	}
	
	/**
	 * @param objectToDelete an uncommitted version of an object
	 * 			to be deleted from the list
	 */
	boolean delete(ExtendableObject objectToDelete) {
		UncommittedObjectKey uncommittedKey = (UncommittedObjectKey)objectToDelete.getObjectKey();
		if (uncommittedKey.isNewObject()) {
			return addedObjects.remove(objectToDelete);
		} else {
			if (deletedObjects.contains(uncommittedKey.getCommittedObjectKey())) {
				return false;
			}
			deletedObjects.add(uncommittedKey.getCommittedObjectKey());
			
			// TODO: following return value may not be correct.
			// However, it is expensive to see if the object
			// exists in the original list, so assume it does.
			return true;
		}
	}
	
	/**
	 * Return the collection of objects in the list that do not exist in the committed
	 * datastore but which are being added by this transaction.
	 * 
	 * @return an iterator which iterates over elements of
	 * 			type ExtendableObject, being the uncommitted versions
	 * 			of the objects being added
	 */
	Iterator getAddedObjectIterator() {
		return addedObjects.iterator();
	}

	/**
	 * Return the collection of objects in the list that exist in the committed
	 * datastore but which are being deleted by this transaction.
	 * 
	 * @return an iterator which iterates over elements of
	 * 			type IObjectKey, being the committed keys
	 * 			of the objects being deleted
	 */
	Iterator getDeletedObjectIterator() {
		return deletedObjects.iterator();
	}
}
