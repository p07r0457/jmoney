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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;

import net.sf.jmoney.jdbcdatastore.SessionManager.DatabaseListKey;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.ExtendablePropertySet;
import net.sf.jmoney.model2.IListManager;
import net.sf.jmoney.model2.IValues;
import net.sf.jmoney.model2.ReferenceViolationException;

/**
 * Every datastore implementation must provide an implementation
 * of the IListManager interface.  This implementation supplies
 * an implementation that executes each method by submitting
 * an SQL statement to the database.
 *
 * @author Nigel Westbury
 */
public class ListManagerUncached<E extends ExtendableObject> implements IListManager<E> {
	SessionManager sessionManager;
	DatabaseListKey<E> listKey;
	
	public ListManagerUncached(SessionManager sessionManager, DatabaseListKey<E> listKey) {
		this.sessionManager = sessionManager;
		this.listKey = listKey;
	}
	
	public <F extends E> F createNewElement(ExtendablePropertySet<F> propertySet) {
 		/*
		 * First build the in-memory object. Even though the object is not
		 * cached in the parent list property, the object must be constructed to
		 * get the default values to be written to the database and the object
		 * must be constructed so it can be returned to the caller.
		 */
		ObjectKey objectKey = new ObjectKey(sessionManager);
		
		/*
		 * Constructing the object means constructing the object key. Both
		 * contain a reference to the other, so they have the same lifetime.
		 */
		// TODO: remove constructWithCachedList parameter
		F extendableObject = sessionManager.constructExtendableObject(propertySet, objectKey, sessionManager.constructListKey(listKey));
		objectKey.setObject(extendableObject);
		
		// Insert the new object into the tables.
		
		int rowId = sessionManager.insertIntoDatabase(propertySet, extendableObject, listKey);
		objectKey.setRowId(rowId);
		
		return extendableObject;
	}
	
	public <F extends E> F createNewElement(ExtendablePropertySet<F> propertySet, IValues values) {
 		/*
		 * First build the in-memory object. Even though the object is not
		 * cached in the parent list property, the object must be constructed to
		 * get the default values to be written to the database and the object
		 * must be constructed so it can be returned to the caller.
		 */
		ObjectKey objectKey = new ObjectKey(sessionManager);
		
		/*
		 * Constructing the object means constructing the object key. Both
		 * contain a reference to the other, so they have the same lifetime.
		 */
		F extendableObject = sessionManager.constructExtendableObject(propertySet, objectKey, listKey, values);
		objectKey.setObject(extendableObject);
		
		// Insert the new object into the tables.
		
		int rowId = sessionManager.insertIntoDatabase(propertySet, extendableObject, listKey);
		objectKey.setRowId(rowId);
		
		return extendableObject;
	}

	public void deleteElement(E extendableObject) throws ReferenceViolationException {
		// Delete this object from the database.
		IDatabaseRowKey key = (IDatabaseRowKey)extendableObject.getObjectKey();
		sessionManager.deleteFromDatabase(key);
	}

	public void moveElement(E extendableObject, IListManager originalListManager) {
		sessionManager.reparentInDatabase(extendableObject, listKey);
	}

	public int size() {
		try {
			String tableName = listKey.listPropertyAccessor.getElementPropertySet().getId().replace('.', '_');
			String columnName = listKey.listPropertyAccessor.getName().replace('.', '_');
			String sql = "SELECT COUNT(*) FROM " + tableName
			+ " WHERE \"" + columnName + "\" = ?";
			System.out.println(sql);
			PreparedStatement stmt = sessionManager.getConnection().prepareStatement(sql);
			try {
				stmt.setInt(1, listKey.parentKey.getRowId());
				ResultSet resultSet = stmt.executeQuery();
				resultSet.next();
				int size = resultSet.getInt(1);
				resultSet.close();
				return size;
			} finally {
				stmt.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("internal error");
		}
	}
	
	public boolean isEmpty() {
		throw new RuntimeException("method not implemented");
	}

	public boolean contains(Object o) {
		throw new RuntimeException("method not implemented");
	}

	public Iterator<E> iterator() {
		/*
		 * We execute a SQL statement and pass the result set to an
		 * UncachedObjectIterator object which will return the entries in the
		 * result set. However, we must create a new statement because the
		 * iterator is being returned from this method call.
		 * 
		 * This class only supports lists where the element type is a final
		 * property set. Therefore we know the exact type of every element in
		 * the list before we execute any query. This saves us from having to
		 * iterate over the final property sets (like the ListManagerCached
		 * object has to).
		 * 
		 * The UnchachedObjectIterator is responsible for closing the result set
		 * and the associated statement.
		 */		
		ResultSet rs = sessionManager.runWithReconnect(new IRunnableSql<ResultSet>() {
			public ResultSet execute(Connection connection) throws SQLException {
				// Although the connection is passed, it is not really necessary because it
				// is taken from the session manager, and that would be the same connection.
				PreparedStatement stmt = sessionManager.executeListQuery(listKey, listKey.listPropertyAccessor.getElementPropertySet());
				return stmt.executeQuery();
			}
		});

		return new UncachedObjectIterator<E>(rs, listKey.listPropertyAccessor.getElementPropertySet(), listKey, sessionManager);
	}

	public Object[] toArray() {
		throw new RuntimeException("method not implemented");
	}

	public <T> T[] toArray(T[] arg0) {
		throw new RuntimeException("method not implemented");
	}

	public boolean add(E extendableObject) {
		/*
		 * This list is not cached so there is nothing to do. The object has
		 * already been added to the database so the list will be
		 * correct if it is fetched.
		 */
		return true;
	}

	public boolean remove(Object o) {
		/*
		 * This list is not cached so there is nothing to do. The object has
		 * already been removed from the database so the list will be
		 * correct if it is fetched.
		 */
		return true;
	}

	public boolean containsAll(Collection<?> arg0) {
		throw new RuntimeException("method not implemented");
	}

	public boolean addAll(Collection<? extends E> arg0) {
		throw new RuntimeException("method not implemented");
	}

	public boolean removeAll(Collection<?> arg0) {
		throw new RuntimeException("method not implemented");
	}

	public boolean retainAll(Collection<?> arg0) {
		throw new RuntimeException("method not implemented");
	}

	public void clear() {
		throw new RuntimeException("method not implemented");
	}
}
