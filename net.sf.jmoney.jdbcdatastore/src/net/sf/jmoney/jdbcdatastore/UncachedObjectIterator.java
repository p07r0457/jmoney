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
import java.sql.Statement;
import java.util.Iterator;

import net.sf.jmoney.jdbcdatastore.SessionManager.DatabaseListKey;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.ExtendablePropertySet;

/**
 * Class that iterates over a set of objects when the objects
 * are not cached and thus must be read from the database.
 * <P>
 * Objects of this class are constructed from a result set
 * that contains the properties for a set of objects to be iterated.
 * This class implements the Iterator interface and will return
 * the set of ExtendableObject objects.
 * <P>
 * Derivable property sets are not supported by this class; the
 * property set must be a final property set.  At the time of
 * writing, this class is used for the Transaction property
 * set only.
 * 
 * @author Nigel Westbury
 */
class UncachedObjectIterator<E extends ExtendableObject> implements Iterator<E> {
	private ResultSet resultSet;
	private ExtendablePropertySet<E> propertySet;
	private DatabaseListKey<? super E> listKey;
	private SessionManager sessionManager;
	private boolean isAnother;

	/**
	 * It is the responsibility of this iterator to close both the result set
	 * and the statement when the iterator is done.
	 * 
	 * There is a problem in that if the user of this iterator does not do a
	 * complete iteration of all elements then the result set and statement are
	 * never closed. We could guard against this by closing these when this
	 * iterator is garbage collected. However, for time being we just make it a
	 * requirement that the iteration is always completed.
	 * 
	 * @param resultSet
	 * @param propertySet
	 *            The property set for the objects in this list, which must be
	 *            final (cannot be a derivable property set)
	 * @param listKey
	 *            The caller may pass a null list key. In that case, a new
	 *            list key will be generated for each object in the list. If
	 *            all the objects in the list have the same parent then pass
	 *            this parent. If the objects in the list have different parents
	 *            then pass null.
	 * @param sessionManager
	 */
	UncachedObjectIterator(ResultSet resultSet, ExtendablePropertySet<E> propertySet, DatabaseListKey<? super E> listKey, SessionManager sessionManager) {
		this.resultSet = resultSet;
		this.propertySet = propertySet;
		this.listKey = listKey;
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
	
	public E next() {
		try {
			/*
			 * If parentKey is null then objects in this collection have
			 * different parents. In that case we must build a parent key for
			 * each object. No additional database access is necessary to do
			 * this because the foreign keys to the parent rows will be in the
			 * result set.
			 */
			DatabaseListKey<? super E> listKey2;
			if (listKey == null) {
				listKey2 = sessionManager.buildParentKey(resultSet, propertySet);
			} else {
				listKey2 = listKey;
			}
			
			ObjectKey key = new ObjectKey(resultSet, propertySet, listKey2, sessionManager);
			E extendableObject = propertySet.getImplementationClass().cast(key.getObject());
			
			// Rowset must be left positioned on the following row.
			isAnother = resultSet.next();
			
			/*
			 * We have reached the end, so close the statement and result
			 * set now.  (It is the responsibility of this iterator to do
			 * so).
			 */
			if (!isAnother) {
				Statement statement = resultSet.getStatement();
				resultSet.close();
				statement.close();
			}
			
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