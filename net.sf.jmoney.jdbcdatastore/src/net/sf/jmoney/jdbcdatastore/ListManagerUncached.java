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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Vector;

import net.sf.jmoney.model2.ExtendableObjectHelperImpl;
import net.sf.jmoney.model2.ExtensionProperties;
import net.sf.jmoney.model2.IExtendableObject;
import net.sf.jmoney.model2.MalformedPluginException;
import net.sf.jmoney.model2.IListManager;
import net.sf.jmoney.model2.PropertyAccessor;
import net.sf.jmoney.model2.PropertySet;

/**
 * Every datastore implementation must provide an implementation
 * of the IListManager interface.  This implementation supplies
 * an implementation that executes each method by submitting
 * an SQL statement to the database.
 *
 * @author Nigel Westbury
 */
public class ListManagerUncached implements IListManager {
	private IDatabaseRowKey parentKey;
	private SessionManagementImpl sessionManager;
	private PropertyAccessor listProperty;

	private PropertySet typedPropertySet;
	
	public ListManagerUncached(IDatabaseRowKey parentKey, SessionManagementImpl sessionManager, PropertyAccessor listProperty) {
		this.parentKey = parentKey;
		this.sessionManager = sessionManager;
		this.listProperty = listProperty;

		// TODO: Cache this to improve performance.

		// Find the property set returned by the list.
		Class valueClass = listProperty.getValueClass();
		typedPropertySet = null;
		for (Iterator iter = PropertySet.getPropertySetIterator(); iter.hasNext(); ) {
			PropertySet propertySet = (PropertySet)iter.next();
			if (propertySet.getInterfaceClass() == valueClass) {
				typedPropertySet = propertySet;
				break;
			}
		}
		if (typedPropertySet == null)
			throw new RuntimeException(valueClass.getName() + " not found.");
	}
	
	public IExtendableObject createNewElement(ExtendableObjectHelperImpl parent, PropertySet propertySet, Object [] values, ExtensionProperties [] extensionProperties) {
		// TODO: implement this.
		// It is almost identical to the version below.
		return null;
	}
	
	public IExtendableObject createNewElement(ExtendableObjectHelperImpl parent, PropertySet propertySet) {

		// Use the default constructor for the object.
		// This creates an object with the following properties:
		// - it is not a part of the object store
		// - its properties are set to the default values
		// - it has no location (i.e. parent) set
		// - it can be added to the object store by calling 
		//		the appropriate add<PropertyName> method.
		//		This passes an ObjectKey and a parent to the object.
		// - none of the list values can contain properties,
		//		though this may change at a later time.
		// - if it contains references to objects in the object store
		//		then it may not be 'kept' without a listener, just as references to
		//		objects in the object store may not be kept without a listener.
		//		(the objects may be deleted).
		
		// No, I have changed my mind.  For time being, get an
		// array of default values from a static method.
		// This is passed to this method.
		Object values[] = propertySet.getDefaultPropertyValues2();
		
		
		// First we insert the new row into the tables.
		
		int rowId = JDBCDatastorePlugin.insertIntoDatabase(propertySet, propertySet.getDefaultPropertyValues2(), listProperty, parent, sessionManager);
		
 		// Now we build the in-memory object.  This object is
		// returned to the caller.
		
		Vector constructorProperties = propertySet.getConstructorProperties();
		int numberOfParameters = constructorProperties.size();
		if (!propertySet.isExtension()) {
			numberOfParameters += 3;
		}
		Object[] constructorParameters = new Object[numberOfParameters];
		
		ObjectKeyCached objectKey = new ObjectKeyCached(rowId, sessionManager);
		
		constructorParameters[0] = objectKey;
		constructorParameters[1] = null;
		constructorParameters[2] = parent.getObjectKey();
	
		// For all lists, set the Collection object to be a Vector.
		// For all primative properties, get the value from the passed object.
		// For all extendable objects, we get the property value from
		// the passed object and then get the object key from that.
		// This works because all objects must be in a list and that
		// list owns the object, not us.
		int indexIntoScalarProperties = 0;
		for (Iterator iter = constructorProperties.iterator(); iter.hasNext(); ) {
			PropertyAccessor propertyAccessor = (PropertyAccessor)iter.next();
			int index = propertyAccessor.getIndexIntoConstructorParameters();
			if (propertyAccessor.isScalar()) {
				// Get the value from the passed object.
				Object value = values[indexIntoScalarProperties++];
				
/*				
				Object objectWithProperties = values;				
				
				try {
					value = propertyAccessor.getTheGetMethod().invoke(objectWithProperties, null);
				} catch (IllegalAccessException e) {
					throw new MalformedPluginException("Method '" + propertyAccessor.getTheGetMethod().getName() + "' in '" + propertyAccessor.getPropertySet().getInterfaceClass().getName() + "' must be public.");
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					throw new RuntimeException("internal error");
				} catch (InvocationTargetException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					throw new RuntimeException("internal error");
				}
*/				
				if (value != null) {
					if (propertyAccessor.getValueClass().isPrimitive()
							|| propertyAccessor.getValueClass() == String.class
							|| propertyAccessor.getValueClass() == Long.class
							|| propertyAccessor.getValueClass() == Date.class) {
						constructorParameters[index] = value;
					} else {
						constructorParameters[index] = ((IExtendableObject)value).getObjectKey();
					}
				} else { 
					constructorParameters[index] = null;
				}
			} else {
				// Must be an element in an array.
				// If an object is not cached, then neither are
				// any lists in the object.  i.e. do not materialized
				// the objects in a list just because the owning object
				// is materialized.
				constructorParameters[index] = new ListManagerUncached(objectKey, sessionManager, propertyAccessor);
			}
		}
		
		// We can now create the object.
		// The parameters to the constructor have been placed
		// in the constructorParameters array so we need only
		// to call the constructor.
		
		Constructor constructor = propertySet.getConstructor();
		ExtendableObjectHelperImpl extendableObject;
		try {
			extendableObject = (ExtendableObjectHelperImpl)constructor.newInstance(constructorParameters);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			throw new RuntimeException("internal error");
		} catch (InstantiationException e) {
			e.printStackTrace();
			throw new RuntimeException("internal error");
		} catch (IllegalAccessException e) {
			throw new MalformedPluginException("Constructor must be public.");
		} catch (InvocationTargetException e) {
			e.printStackTrace();
			throw new MalformedPluginException("An exception occured within a constructor in a plug-in.");
		}
		
		objectKey.setObject(extendableObject);

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

	public Iterator iterator() {
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
			return new UncachedObjectIterator(resultSet, typedPropertySet, parentKey, sessionManager);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("internal error");
		}
	}

	public Object[] toArray() {
		throw new RuntimeException("method not implemented");
	}

	public Object[] toArray(Object[] a) {
		throw new RuntimeException("method not implemented");
	}

	public boolean add(Object o) {
		throw new RuntimeException("method not implemented");
	}

	public boolean remove(Object o) {
		// Delete this object from the database.
		IExtendableObject extendableObject = (IExtendableObject)o;
		IDatabaseRowKey key = (IDatabaseRowKey)extendableObject.getObjectKey();
		return JDBCDatastorePlugin.deleteFromDatabase(key.getRowId(), extendableObject, sessionManager);
	}

	public boolean containsAll(Collection c) {
		throw new RuntimeException("method not implemented");
	}

	public boolean addAll(Collection c) {
		throw new RuntimeException("method not implemented");
	}

	public boolean removeAll(Collection c) {
		throw new RuntimeException("method not implemented");
	}

	public boolean retainAll(Collection c) {
		throw new RuntimeException("method not implemented");
	}

	public void clear() {
		throw new RuntimeException("method not implemented");
	}
}
