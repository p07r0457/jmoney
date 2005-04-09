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
import net.sf.jmoney.model2.PropertyAccessor;

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
public class ModifiedList {

	private ExtendableObject parent;

	private PropertyAccessor listProperty;
	
	/**
	 * Element: ExtendableObject
	 */
	Vector addedObjects = new Vector();
	
	/**
	 * Element: ExtendableObject
	 */
	Vector deletedObjects = new Vector();
	
	/**
	 * 
	 */
	void add(ExtendableObject newObject) {
		addedObjects.add(newObject);
	}
	
	/**
	 * 
	 */
	boolean delete(ExtendableObject objectToDelete) {
		if (((UncommittedObjectKey)objectToDelete.getObjectKey()).isNewObject()) {
			return addedObjects.remove(objectToDelete);
		} else {
			if (deletedObjects.contains(objectToDelete)) {
				return false;
			}
			deletedObjects.add(objectToDelete);
			// TODO: following return value may not be correct.
			// However, it is expensive to see if the object
			// exists in the original list, so assume it does.
			return true;
		}
	}
	
	/**
	 * @return
	 */
	Iterator getAddedObjectIterator() {
		return addedObjects.iterator();
	}

	/**
	 * @return
	 */
	Iterator getDeletedObjectIterator() {
		return deletedObjects.iterator();
	}
}
