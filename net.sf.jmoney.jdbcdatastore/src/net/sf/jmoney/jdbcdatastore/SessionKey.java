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

import net.sf.jmoney.model2.ExtendableObjectHelperImpl;
import net.sf.jmoney.model2.ExtensionProperties;
import net.sf.jmoney.model2.IExtendableObject;
import net.sf.jmoney.model2.ISessionManagement;
import net.sf.jmoney.model2.PropertyAccessor;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.Session;

/**
 * This class provides the IObjectKey implementation that
 * is used only for the session object.  In this implementation,
 * the session object must be passed to this object after this
 * object is constructed by calling the setObject method.
 *
 * @author Nigel Westbury
 */
public class SessionKey implements IDatabaseRowKey {
	private IExtendableObject extendableObject;
	private SessionManagementImpl sessionManager;

	/**
	 * The object itself is not passed to the constructor.
	 * The caller must call the <code>setObject</code> method
	 * after constructing the object.  The reason for this
	 * two part construction is that the constructor for the
	 * object requires this object key.
	 */
	SessionKey() {
	}
	
	public IExtendableObject getObject() {
		return extendableObject;
	}

	void setObject(IExtendableObject extendableObject, SessionManagementImpl sessionManager) {
		this.extendableObject = extendableObject;
		this.sessionManager = sessionManager;
	}

	public Collection createIndexValuesList(PropertyAccessor propertyAccessor) {
		// TODO: complete this.  All objects of the type in 
		// the session are fetched.
		throw new RuntimeException("code not completed");
//		return new IndexValuesList(sessionManager, rowId);
	}

	public int getRowId() {
		// All session objects have a row id of zero.
		// Only one session can exist.
		return 0;
	}

	public void updateProperties(PropertySet actualPropertySet, Object[] oldValues, Object[] newValues, ExtensionProperties [] extensionProperties) {
		// actualPropertySet is always the session property set.
		JDBCDatastorePlugin.updateProperties(actualPropertySet, 0, oldValues, newValues, sessionManager);
	}

	public Session getSession() {
		return sessionManager.getSession();
	}

	public ISessionManagement getSessionManager() {
		return sessionManager;
	}
}
