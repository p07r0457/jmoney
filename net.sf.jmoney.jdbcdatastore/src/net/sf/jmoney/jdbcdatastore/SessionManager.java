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
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import net.sf.jmoney.model2.IExtendableObject;
import net.sf.jmoney.model2.IObjectKey;
import net.sf.jmoney.model2.ISessionManager;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.Session;

import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * Manages a session that is held in a JDBC database.
 *
 * @author Nigel Westbury
 */
public class SessionManager implements ISessionManager {
	
	private Connection connection;
	
	private Statement reusableStatement;
	
	private IObjectKey sessionKey;
	
	/**
	 * For each <code>PropertySet</code> for which objects are required
	 * to be cached, map the <code>PropertySet</code> to a <Map>.
	 * Each map is itself a map of integer ids to extendable objects.
	 * <P>
	 * If a PropertySet is cached then so are all property sets
	 * derived from that property set.  However, derived property
	 * sets do not have their own map.  Instead objects of the
	 * derived property set are put in the map for the base property
	 * set.
	 */
	private Map mapOfCachedObjectMaps = new HashMap();
	
	public SessionManager(Connection connection, IObjectKey sessionKey) {
		this.connection = connection;
		this.sessionKey = sessionKey;

		try {
			this.reusableStatement = connection.createStatement();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("SQL Exception: " + e.getMessage());
		}
	}

	public Session getSession() {
		return (Session)sessionKey.getObject();
	}
	
	/**
	 * @return
	 */
	public IObjectKey getSessionKey() {
		return sessionKey;
	}

	/**
	 * @return
	 */
	public Connection getConnection() {
		return connection;
	}
	
	/**
	 * This method provides a statement object to consumers
	 * which need a statement object and can guarantee that
	 * they will have finished with the statement before
	 * another call to this method is made.  Realistically
	 * that means this method should only be called when the
	 * caller has closed the result set before the calling
	 * method returns and, furthermore, no calls are made to
	 * methods that may make SQL queries.
	 */
	public Statement getReusableStatement() {
		return reusableStatement;
	}
	
	public boolean canClose(IWorkbenchWindow window) {
		// A JDBC database can always be closed without further
		// input from the user.
		return true;
	}
	
	public void close() {
		try {
			reusableStatement.close();
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("SQL Exception: " + e.getMessage());
		}
	}
	
	public String getBriefDescription() {
		// TODO: improve this implementation to give more
		// details of the database.
		return "JDBC database";
	}
	
	public String getFactoryId() {
		return "net.sf.jmoney.jdbcdatastore.factoryid";
	}
	
	private IPersistableElement persistableElement 
	= new IPersistableElement() {
		public String getFactoryId() {
			return "net.sf.jmoney.jdbcdatastore.factoryid";
		}
		public void saveState(IMemento memento) {
			// The session must have been saved by now, because
			// JMoney will not closed until the Session object says
			// it is ok to close, and the Session object will not
			// say it is ok to close unless it has available a file
			// name to which the session can be saved.  (It will ask
			// the user if the session was created using the New menu).
			
			// TODO: get this working.
			//			memento.putString("fileName", sessionFile.getPath());
		}
	};
	
	public Object getAdapter(Class adapter) {
		if (adapter == IPersistableElement.class) {
			return persistableElement;
		}
		return null;
	}
	
	/**
	 * @param accountNumber
	 * @return
	 */
/*	
	public IObjectKey lookupAccountKey(int accountNumber) {
		return (IObjectKey)accountsMap.get(new Integer(accountNumber));
	}
*/
	/**
	 * This method must not be called if the property set
	 * or any base property set is already cached.
	 * The results are unpredictable if the property set
	 * is already cached (this method does not check).
	 * 
	 * @param propertySet
	 */
	public void addCachedPropertySet(PropertySet propertySet) {
		mapOfCachedObjectMaps.put(propertySet, new HashMap());
	}
	
	/**
	 * 
	 * @param propertySet
	 * @return If objects of the given property set are
	 * 			cached, a map of integer id values to cached
	 * 			objects; otherwise null.
	 */
	public Map getMapOfCachedObjects(PropertySet propertySet) {
		for (PropertySet propertySet2 = propertySet; propertySet2 != null; propertySet2 = propertySet2.getBasePropertySet()) {
			Map result = (Map)mapOfCachedObjectMaps.get(propertySet2);
			if (result != null) {
				return result;
			}
		}
		return null;
	}
	
	/**
	 * Some property sets are cached and some are not.
	 * For those that are cached, a map is maintained
	 * that maps the integer id to the cached object.
	 * <P>
	 * There can be multiple such maps, one map for each
	 * property set that is cached.  If a property set
	 * is cached then all property sets derived from that
	 * property set are also cached.  It is possible,
	 * though, that a property set is not cached but one
	 * or more property sets derived from that property
	 * set is cached.  If a property set is cached then
	 * all objects derived from that property set are
	 * put in the same map.
	 * 
	 * @param parentPropertySetId
	 * @param parentId
	 * @return
	 */
	// TODO: We may not need this method.
	public IExtendableObject lookupObject(PropertySet propertySet, int id) {
		Map mapOfObjects = getMapOfCachedObjects(propertySet);
		return (IExtendableObject)mapOfObjects.get(new Integer(id));
	}
	
}
