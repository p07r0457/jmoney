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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;

import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.ExtendablePropertySet;
import net.sf.jmoney.model2.IObjectKey;

/**
 * Class that iterates over a set of objects when the objects
 * are not cached and thus must be read from the database.
 * <P>
 * Objects of this class are constructed from a result set
 * that contains the properties for a set of objects to be iterated.
 * This class implements the Iterator interface and will return
 * the set of ExtendableObject objects.
 *
 * @author Nigel Westbury
 */
class UncachedObjectIterator<E extends ExtendableObject> implements Iterator<E> {
	private ResultSet resultSet;
	private ExtendablePropertySet<E> propertySet;
	private IObjectKey parentKey;
	private SessionManager sessionManager;
	private boolean isAnother;

	/**
	 * 
	 * @param resultSet
	 * @param propertySet
	 * @param parentKey The caller may pass a null parent key.
	 * 			In that case, a new parent key will be generated
	 * 			for each object in the list.  If all the objects
	 * 			in the list have the same parent then pass this
	 * 			parent.  If the objects in the list have different
	 * 			parents then pass null.
	 * @param sessionManager
	 */
	UncachedObjectIterator(ResultSet resultSet, ExtendablePropertySet<E> propertySet, IObjectKey parentKey, SessionManager sessionManager) {
		this.resultSet = resultSet;
		this.propertySet = propertySet;
		this.parentKey = parentKey;
		this.sessionManager = sessionManager;
		
		// Position on first row.
		try {
			isAnother = resultSet.next();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("internal error");
		}
	}
	
	public boolean hasNext() {
		return isAnother;
	}
	
	// TODO: This method is not complete.
	// It will not work if the list contains a list of objects
	// of a particular property set, but that property set is
	// a derivable property set.  To materialize the actual
	// objects, we need to query the rows from the tables for
	// the derived property sets appropriate for each object.
	public E next() {
		try {
			int id = resultSet.getInt("_ID");
			ObjectKeyCached key = new ObjectKeyCached(id, sessionManager);
			
			E extendableObject;
			if (parentKey == null) {
				extendableObject = JDBCDatastorePlugin.materializeObject(resultSet, propertySet, key, sessionManager);
			} else {
				extendableObject = JDBCDatastorePlugin.materializeObject(resultSet, propertySet, key, parentKey, sessionManager);
			}

			key.setObject(extendableObject);
			
			// Rowset must be left positioned on the following row.
			isAnother = resultSet.next();
			
			return extendableObject;
		} catch (SQLException e3) {
			e3.printStackTrace();
			throw new RuntimeException("internal error");
		}
	}
	
	public void remove() {
		throw new RuntimeException("unimplemented method");
	}
}