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
import java.util.Collection;

import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.ExtensionProperties;
import net.sf.jmoney.model2.IExtendableObject;
import net.sf.jmoney.model2.ISessionManagement;
import net.sf.jmoney.model2.PropertyAccessor;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.PropertySetNotFoundException;
import net.sf.jmoney.model2.Session;

/**
 * This class provides an IObjectKey implementation for objects
 * that are not cached.  Every time getObject is called,
 * the properties for the object are read from the database
 * and a new instance of the object is constructed.
 *
 * @author Nigel Westbury
 */
public class ObjectKeyUncached implements IDatabaseRowKey {
	private int rowId;
	
	/**
	 * PropertySet for the type of this reference.
	 * WARNING: This is not the actual property set for the object.
	 * The object may have a property set that is derived from
	 * the property set for the type of the reference.
	 * The actual property set of the object cannot be determined
	 * until the object is read from the database and therefore
	 * is not passed to the constructor.
	 */
	private PropertySet typedPropertySet;
	private SessionManagementImpl sessionManager;
	
	ObjectKeyUncached(int rowId, PropertySet propertySet, SessionManagementImpl sessionManager) {
		this.rowId = rowId;
		this.typedPropertySet = propertySet;
		this.sessionManager = sessionManager;
	}
	
	public IExtendableObject getObject() {
		// The object is constructed on demand.
		
		// If the class of extendable objects that may be referenced by
		// this key is an abstract class then the process is a two step
		// process.  The first step is to read the base table.  That table
		// will have a column that indicates the actual derived class of
		// the object.  A second select statement must then be submitted
		// that selects the columns from the appropriate tables of
		// additional properties for the derived class.
		
		try {
			String sql = "SELECT * FROM "
				+ typedPropertySet.getId().replace('.', '_')
				+ " WHERE _ID = " + rowId;
			
			ResultSet rs = sessionManager.getReusableStatement().executeQuery(sql);
			
			if (typedPropertySet.isDerivable()) {
				// Get the final property set.
				rs.next();
				String id = rs.getString("_PROPERTY_SET_ID");
				
				try {
					PropertySet finalPropertySet = PropertySet.getPropertySet(id);
				} catch (PropertySetNotFoundException e1) {
					// TODO: The most probable cause is that an object
					// is stored in the database, but the plug-in that supports
					// the object has now gone.
					// We need to think about the proper way of
					// handling this scenario.
					e1.printStackTrace();
					throw new RuntimeException("Property set stored in database is no longer supported by the installed plug-ins.");
				}
				
				// Build the SQL statement that will return all
				// the rows from the base and derived tables.
				sql = "SELECT * FROM "
					+ typedPropertySet.getId().replace('.', '_');
				
				String tableList = "";
				String whereClause = "";
				for (PropertySet propertySet2 = typedPropertySet; propertySet2 != typedPropertySet; propertySet2 = propertySet2.getBasePropertySet()) {
					String tableName = typedPropertySet.getId().replace('.', '_'); 
					tableList = tableName
					+ ", " + tableList;
					whereClause = " AND " + tableName + "_ID=" + rowId;
				}
				
				sql += tableList + " WHERE _ID=" + rowId + whereClause;
				
				rs = sessionManager.getReusableStatement().executeQuery(sql);
			}
			
			ExtendableObject extendableObject = JDBCDatastorePlugin.materializeObject(rs, typedPropertySet, this, sessionManager);
			
			return extendableObject;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("SQL error");
		}
	}

	public Collection createIndexValuesList(PropertyAccessor propertyAccessor) {
		return new IndexValuesList(sessionManager, rowId, propertyAccessor);
	}

	public int getRowId() {
		return rowId;
	}

	public void updateProperties(PropertySet actualPropertySet, Object[] oldValues, Object[] newValues, ExtensionProperties [] extensionProperties) {
		JDBCDatastorePlugin.updateProperties(actualPropertySet, rowId, oldValues, newValues, sessionManager);
	}

	public Session getSession() {
		return sessionManager.getSession();
	}

	public ISessionManagement getSessionManager() {
		throw new RuntimeException("should only be called for session keys");
	}
}
