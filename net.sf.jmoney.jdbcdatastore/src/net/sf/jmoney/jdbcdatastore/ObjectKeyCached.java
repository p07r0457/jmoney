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

import java.util.Collection;

import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.ISessionManager;
import net.sf.jmoney.model2.PropertyAccessor;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.Session;

/**
 * This class provides an IObjectKey implementation for objects
 * that are cached.  An instance of the object must be constructed
 * by the code that constructed this ObjectKeyCached object and
 * must be set into this object by calling the setObject method.
 * The cached object will then be returned each time getObject 
 * is called.
 *
 * @author Nigel Westbury
 */
public class ObjectKeyCached implements IDatabaseRowKey {
	private ExtendableObject extendableObject;
	private int rowId;
	private SessionManager sessionManager;

	/**
	 * The object itself is not passed to the constructor.
	 * The caller must call the <code>setObject</code> method
	 * after constructing the object.  The reason for this
	 * two part construction is that the constructor for the
	 * object requires this object key.
	 * 
	 * @param rowId The id of the row in the tables which
	 * 			contains the information for this object.
	 * 			Although this object key does not need to
	 * 			be able to construct the object (it contains
	 * 			a reference	to a cached object), this object
	 * 			must be able to provide this id to other code
	 * 			in this plug-in. 
	 */
	ObjectKeyCached(int rowId, SessionManager sessionManager) {
		this.rowId = rowId;
		this.sessionManager = sessionManager;
	}
	
	void setObject(ExtendableObject extendableObject) {
		this.extendableObject = extendableObject;
	}

	public ExtendableObject getObject() {
		return extendableObject;
	}

	public Collection createIndexValuesList(PropertyAccessor propertyAccessor) {
		// For time being, the Vector class supports all we need.
		// It may be that this will have to be changed to a class
		// that extends Vector and provides more methods.
		// TO DO: update above comment when design complete.
		return new IndexValuesList(sessionManager, this, propertyAccessor);
	}

	public int getRowId() {
		return rowId;
	}

	public void updateProperties(PropertySet actualPropertySet, Object[] oldValues, Object[] newValues) {
		JDBCDatastorePlugin.updateProperties(actualPropertySet, rowId, oldValues, newValues, sessionManager);
	}

	public void updateProperties(PropertySet actualPropertySet, PropertyAccessor propertyAccessor, Object oldValue, Object newValue) {
		JDBCDatastorePlugin.updateProperties(actualPropertySet, rowId, propertyAccessor, oldValue, newValue, sessionManager);
	}

	public Session getSession() {
		return sessionManager.getSession();
	}

	public ISessionManager getSessionManager() {
		throw new RuntimeException("should only be called for session keys");
	}

	/**
	 * Until an object has been persisted to the database, no
	 * row id is available.  The row id is set to -1 initially.
	 * When the object is persisted, this method must be called
	 * to set the row id to the actual row id.
	 * 
	 * @param rowId The row id obtained when the object is
	 * 			persisted in the database.
	 */
	public void setRowId(int rowId) {
		this.rowId = rowId;
	}
}
