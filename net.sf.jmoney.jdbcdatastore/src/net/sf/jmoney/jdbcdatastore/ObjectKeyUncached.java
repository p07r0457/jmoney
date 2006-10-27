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

import net.sf.jmoney.model2.DataManager;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.ExtendablePropertySet;
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
	private ExtendablePropertySet<? extends ExtendableObject> typedPropertySet;
	private SessionManager sessionManager;
	
	/**
	 * @param PropertySet 
	 * 		The property set for the type of this reference.
	 * 		WARNING: This is not the actual property set for the object.
	 * 		The object may have a property set that is derived from
	 * 		the property set for the type of the reference.
	 * 		The actual property set of the object cannot be determined
	 * 		until the object is read from the database and therefore
	 * 		is not passed to the constructor.
	 */
	ObjectKeyUncached(int rowId, ExtendablePropertySet<? extends ExtendableObject> typedPropertySet, SessionManager sessionManager) {
		this.rowId = rowId;
		this.typedPropertySet = typedPropertySet;
		this.sessionManager = sessionManager;
	}
	
	public ExtendableObject getObject() {
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
					PropertySet.getExtendablePropertySet(id);
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
				for (ExtendablePropertySet propertySet2 = typedPropertySet; propertySet2 != typedPropertySet; propertySet2 = propertySet2.getBasePropertySet()) {
					String tableName = typedPropertySet.getId().replace('.', '_'); 
					tableList = tableName
					+ ", " + tableList;
					whereClause = " AND " + tableName + "_ID=" + rowId;
				}
				
				sql += tableList + " WHERE _ID=" + rowId + whereClause;
				
				rs = sessionManager.getReusableStatement().executeQuery(sql);
			}

			rs.next();

			ExtendableObject extendableObject = JDBCDatastorePlugin.materializeObject(rs, typedPropertySet, this, sessionManager);
			
			return extendableObject;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("SQL error");
		}
	}

	public int getRowId() {
		return rowId;
	}

	public boolean equals(Object object) {
		if (object instanceof ObjectKeyUncached) {
			ObjectKeyUncached otherKey = (ObjectKeyUncached)object; 
			return this.rowId == otherKey.getRowId()
			    && this.typedPropertySet == otherKey.typedPropertySet
			    && this.sessionManager == otherKey.sessionManager;
		} else {
			return false;
		}
	}
	
	public int hashCode() {
		return rowId;
	}
	
	public void updateProperties(ExtendablePropertySet actualPropertySet, Object[] oldValues, Object[] newValues) {
		JDBCDatastorePlugin.updateProperties(actualPropertySet, rowId, oldValues, newValues, sessionManager);
	}

	public Session getSession() {
		return sessionManager.getSession();
	}

	public DataManager getSessionManager() {
		return sessionManager;
	}

	/**
	 * Until an object has been persisted to the database, no
	 * row id is available.  The row id is set to -1 initially.
	 * When the object is persisted, this method must be called
	 * to set the row id to the actual row id.
	 * <P>
	 * Although uncached objects must be written to the database
	 * immediately, because the getObject method reads the object
	 * from the database, we cannot write the object to the database
	 * until the object has been created, hence we need this method.
	 *  
	 * @param rowId The row id obtained when the object is
	 * 			persisted in the database.
	 */
	public void setRowId(int rowId) {
		this.rowId = rowId;
	}
}
