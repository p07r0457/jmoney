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

package net.sf.jmoney.jdbcdatastore;

import java.util.Map;
import java.util.Vector;

import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.ExtendablePropertySet;
import net.sf.jmoney.model2.IListManager;
import net.sf.jmoney.model2.ListPropertyAccessor;

/**
 * Every datastore implementation must provide an implementation
 * of the IListManager interface.  This implementation simply
 * uses the Vector class to keep a list of objects.
 *
 * @author Nigel Westbury
 */
public class ListManagerCached<E extends ExtendableObject> extends Vector<E> implements IListManager<E> {

	private static final long serialVersionUID = 867883048050895954L;

	private SessionManager sessionManager;
	private ListPropertyAccessor listProperty;
	
	public ListManagerCached(SessionManager sessionManager, ListPropertyAccessor listProperty) {
		this.sessionManager = sessionManager;
		this.listProperty = listProperty;
	}

	public <F extends E> F createNewElement(ExtendableObject parent, ExtendablePropertySet<F> propertySet) {
		// We must create the object before we persist it to the database.
		// The reason why we must do this, and not simply write the
		// default values, is that the constructor only uses the
		// default values array as a guide.  For example, the constructor
		// may replace a null timestamp with the current time, or
		// a null currency with a default currency.
		
 		// First we build the in-memory object.
		// This is done here because in this case the object is always cached
		// in memory.
		
		ObjectKeyCached objectKey = new ObjectKeyCached(-1, sessionManager);
		
		F extendableObject = JDBCDatastorePlugin.constructExtendableObject(propertySet, sessionManager, objectKey, parent, true);

		objectKey.setObject(extendableObject);
		
		add(extendableObject);
		
		// Now we insert the new row into the tables.

		int rowId = JDBCDatastorePlugin.insertIntoDatabase(propertySet, extendableObject, listProperty, parent, sessionManager);
		objectKey.setRowId(rowId);

		// If the list of objects are cached, then the objects themselves are cached
		// objects, and vica versa, I think.
		// Add to the cache map now.
		// TODO: This does not work if we have uncached derivable property sets
		// (Which, at time of writing, we don't), because the parameter passed to
		// getMapOfCachedObjects must be a base-most property set (I think).
		Map<Integer, F> map = sessionManager.getMapOfCachedObjects(propertySet);
		map.put(new Integer(rowId), extendableObject);

		return extendableObject;
	}

	public <F extends E> F createNewElement(ExtendableObject parent, ExtendablePropertySet<F> propertySet, Object[] values) {
		// We must create the object before we persist it to the database.
		// The reason why we must do this, and not simply write the
		// default values, is that the constructor only uses the
		// default values array as a guide.  For example, the constructor
		// may replace a null timestamp with the current time, or
		// a null currency with a default currency.
		
 		// First we build the in-memory object.
		// This is done here because in this case the object is always cached
		// in memory.
		
		ObjectKeyCached objectKey = new ObjectKeyCached(-1, sessionManager);
		
		F extendableObject = JDBCDatastorePlugin.constructExtendableObject(propertySet, sessionManager, objectKey, parent, true);

		objectKey.setObject(extendableObject);
		
		add(extendableObject);
		
		// Now we insert the new row into the tables.

		int rowId = JDBCDatastorePlugin.insertIntoDatabase(propertySet, extendableObject, listProperty, parent, sessionManager);
		objectKey.setRowId(rowId);

		// If the list of objects are cached, then the objects themselves are cached
		// objects, and vica versa, I think.
		// Add to the cache map now.
		// TODO: This does not work if we have uncached derivable property sets
		// (Which, at time of writing, we don't), because the parameter passed to
		// getMapOfCachedObjects must be a base-most property set (I think).
		Map<Integer, F> map = sessionManager.getMapOfCachedObjects(propertySet);
		map.put(new Integer(rowId), extendableObject);

		return extendableObject;
	}
	
	public boolean remove(Object o) {
		boolean found = super.remove(o);
		
		// Delete this object from the database.
		if (found) {
			ExtendableObject extendableObject = (ExtendableObject)o;
			IDatabaseRowKey key = (IDatabaseRowKey)extendableObject.getObjectKey();
			boolean foundInDatabase = JDBCDatastorePlugin.deleteFromDatabase(key.getRowId(), extendableObject, sessionManager);
			if (!foundInDatabase) {
				throw new RuntimeException("database inconsistent");
			}
		}
		
		return found;
	}
}
