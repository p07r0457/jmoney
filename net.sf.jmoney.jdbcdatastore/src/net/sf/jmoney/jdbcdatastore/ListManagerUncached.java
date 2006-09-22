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
import java.util.Collection;
import java.util.Iterator;

import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IListManager;
import net.sf.jmoney.model2.ListPropertyAccessor;
import net.sf.jmoney.model2.PropertySet;

/**
 * Every datastore implementation must provide an implementation
 * of the IListManager interface.  This implementation supplies
 * an implementation that executes each method by submitting
 * an SQL statement to the database.
 *
 * @author Nigel Westbury
 */
public class ListManagerUncached<E extends ExtendableObject> implements IListManager<E> {
	private IDatabaseRowKey parentKey;
	private SessionManager sessionManager;
	private ListPropertyAccessor<E> listProperty;

	private PropertySet<E> typedPropertySet;
	
	public ListManagerUncached(IDatabaseRowKey parentKey, SessionManager sessionManager, ListPropertyAccessor<E> listProperty) {
		this.parentKey = parentKey;
		this.sessionManager = sessionManager;
		this.listProperty = listProperty;

		// TODO: Cache this to improve performance.

		// Find the property set returned by the list.
		Class<E> valueClass = listProperty.getValueClass();
		typedPropertySet = PropertySet.getPropertySet(valueClass);
		if (typedPropertySet == null)
			throw new RuntimeException(valueClass.getName() + " not found.");
	}
	
	public <F extends E> F createNewElement(ExtendableObject parent, PropertySet<F> propertySet) {
 		// First build the in-memory object.  Even though the object is not
		// cached in the object key, the object must be constructed to get
		// the default values to be written to the database and the
		// object must be constructed so it can be returned to the caller.
		
		ObjectKeyUncached objectKey = new ObjectKeyUncached(-1, typedPropertySet, sessionManager);
		
		// If an object is not cached, then neither are
		// any lists in the object.  i.e. do not materialized
		// the objects in a list just because the owning object
		// is materialized.

		F extendableObject = JDBCDatastorePlugin.constructExtendableObject(propertySet, sessionManager, objectKey, parent, false);
		
		// Insert the new object into the tables.
		
		int rowId = JDBCDatastorePlugin.insertIntoDatabase(propertySet, extendableObject, listProperty, parent, sessionManager);
		objectKey.setRowId(rowId);
		
		return extendableObject;
	}
	
	public <F extends E> F createNewElement(ExtendableObject parent, PropertySet<F> propertySet, Object[] values) {
 		// First build the in-memory object.  Even though the object is not
		// cached in the object key, the object must be constructed to get
		// the default values to be written to the database and the
		// object must be constructed so it can be returned to the caller.
		
		ObjectKeyUncached objectKey = new ObjectKeyUncached(-1, typedPropertySet, sessionManager);
		
		// If an object is not cached, then neither are
		// any lists in the object.  i.e. do not materialized
		// the objects in a list just because the owning object
		// is materialized.

		F extendableObject = JDBCDatastorePlugin.constructExtendableObject(propertySet, sessionManager, objectKey, parent, false, values);
		
		// Insert the new object into the tables.
		
		int rowId = JDBCDatastorePlugin.insertIntoDatabase(propertySet, extendableObject, listProperty, parent, sessionManager);
		objectKey.setRowId(rowId);
		
		return extendableObject;
	}

	public int size() {
		try {
			String tableName = typedPropertySet.getId().replace('.', '_');
			String columnName = listProperty.getName().replace('.', '_');
			ResultSet resultSet = sessionManager.getReusableStatement().executeQuery(
					"SELECT COUNT(*) FROM " + tableName
					+ " WHERE \"" + columnName + "\" = " + parentKey.getRowId());
			resultSet.next();
			int size = resultSet.getInt(1);
			resultSet.close();
			return size;
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
		// We execute a SQL statement and pass to result set
		// to an UncachedObjectIterator object which will return
		// the entries in the result set.  However, we must
		// create a new statement as the iterator is being
		// returned from this method call.

		// If the type of object held by the list is a type
		// from which property sets must be derived then it takes
		// two steps to fetch the results.  We first submit a query
		// fetching the result set that includes columns from this
		// and any base tables.  The iterator must then join the results
		// with data from the appropriate derived tables as it
		// reads each row.
		
		// Some databases (e.g. HDBSQL) execute queries faster
		// if JOIN ON is used rather than a WHERE clause, and
		// will also execute faster if the smallest table is
		// put first.  The smallest table in this case is the
		// table represented by typedPropertySet and the larger
		// tables are the base tables.
		
		String tableName = typedPropertySet.getId().replace('.', '_');
		String sql = "SELECT * FROM " + tableName;
		for (PropertySet propertySet2 = typedPropertySet.getBasePropertySet(); propertySet2 != null; propertySet2 = propertySet2.getBasePropertySet()) {
			String tableName2 = propertySet2.getId().replace('.', '_');
			sql += " JOIN " + tableName2
					+ " ON " + tableName + "._ID = " + tableName2 + "._ID";
		}
		
		// Add the WHERE clause.
		// There is a parent column with the same name as the name of
		// the list property.  Only if the number in this column is the
		// same as the id of the owner of this list is the object in this
		// list.
		
		// We do not want rows where the value of the parent column is
		// null to be returned, so use IFNULL.
		
		sql += " WHERE IFNULL(\"" + listProperty.getName().replace('.', '_')
				+ "\", -1) = " +  + parentKey.getRowId();
		
		try {
			Statement stmt = sessionManager.getConnection().createStatement();
			ResultSet resultSet = stmt.executeQuery(sql);
			return new UncachedObjectIterator<E>(resultSet, typedPropertySet, parentKey, sessionManager);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("internal error");
		}
	}

	public Object[] toArray() {
		throw new RuntimeException("method not implemented");
	}

	public <T> T[] toArray(T[] arg0) {
		throw new RuntimeException("method not implemented");
	}

	public boolean add(E extendableObject) {
		throw new RuntimeException("method not implemented");
	}

	public boolean remove(Object o) {
		// Delete this object from the database.
		ExtendableObject extendableObject = (ExtendableObject)o;
		IDatabaseRowKey key = (IDatabaseRowKey)extendableObject.getObjectKey();
		return JDBCDatastorePlugin.deleteFromDatabase(key.getRowId(), extendableObject, sessionManager);
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
