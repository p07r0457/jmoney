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

package net.sf.jmoney.serializeddatastore;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IListManager;
import net.sf.jmoney.model2.PropertyAccessor;
import net.sf.jmoney.model2.PropertySet;

/**
 * Every datastore implementation must provide an implementation
 * of the IListManager interface.  This implementation simply
 * uses the Vector class to keep a list of objects.
 *
 * @author Nigel Westbury
 */
public class SimpleListManager extends Vector implements IListManager {

	private SessionManager sessionManager;

	public SimpleListManager(SessionManager sessionManager) {
	 	this.sessionManager = sessionManager;
	 }

	public ExtendableObject createNewElement(ExtendableObject parent, PropertySet propertySet) {
		Collection constructorProperties = propertySet.getDefaultConstructorProperties();
		
		int numberOfParameters = constructorProperties.size();
		if (!propertySet.isExtension()) {
			numberOfParameters += 3;
		}
		Object[] constructorParameters = new Object[numberOfParameters];
		
		SimpleObjectKey objectKey = new SimpleObjectKey(sessionManager);
		
		int index = 0;
		// TODO: We can assert that the property set is not an extension
		if (!propertySet.isExtension()) {
			constructorParameters[0] = objectKey;
			constructorParameters[1] = null;
			constructorParameters[2] = parent.getObjectKey();
			index = 3;
		}
		
		// Construct the extendable object using the 'default' constructor.
		// This constructor takes the minimum number of parameters necessary
		// to properly construct the object, setting default values for all
		// the scalar properties.  We must, however, pass objects that manage
		// any lists within the object.
		
		// Add a list manager for each list property in the object.
		for (Iterator iter = constructorProperties.iterator(); iter.hasNext(); ) {
			PropertyAccessor propertyAccessor = (PropertyAccessor)iter.next();
			constructorParameters[index++] = new SimpleListManager(sessionManager);
		}
		
		// We can now create the object.
		ExtendableObject extendableObject = (ExtendableObject)propertySet.constructDefaultImplementationObject(constructorParameters);
		
		objectKey.setObject(extendableObject);

		add(extendableObject);
		
		// If an account is added then we
		// must add a list that will contain the entries in the account.
		if (extendableObject instanceof Account) {
			Account account = (Account)extendableObject;
			sessionManager.addAccountList(account);
		}
		
		// If an entry is added then we
		// must update the lists of entries in each account.
		if (extendableObject instanceof Entry) {
			Entry entry = (Entry)extendableObject;
			if (entry.getAccount() != null) {
				sessionManager.addEntryToList(entry.getAccount(), entry);
			}
		}
		
        // This plug-in needs to know if a session has been
		// modified so it knows whether the session needs to
		// be saved.  Mark the session as modified now.
		sessionManager.setModified();
		
		return extendableObject;
	}

	public ExtendableObject createNewElement(ExtendableObject parent, PropertySet propertySet, Object[] values) {
		Collection constructorProperties = propertySet.getConstructorProperties();
		
		int numberOfParameters = constructorProperties.size();
		if (!propertySet.isExtension()) {
			numberOfParameters += 3;
		}
		Object[] constructorParameters = new Object[numberOfParameters];
		
		SimpleObjectKey objectKey = new SimpleObjectKey(sessionManager);
		
		Map extensionMap = new HashMap();
		
		int index = 0;
		if (!propertySet.isExtension()) {
			constructorParameters[0] = objectKey;
			constructorParameters[1] = extensionMap;
			constructorParameters[2] = parent.getObjectKey();
			index = 3;
		}
		
		int indexIntoValues = 0;
		for (Iterator iter = propertySet.getPropertyIterator3(); iter.hasNext(); ) {
			PropertyAccessor propertyAccessor = (PropertyAccessor)iter.next();
			
			// For this property, determine the parameter value to be passed to the
			// constructor.
			Object value;
			if (propertyAccessor.isScalar()) {
				value = values[indexIntoValues++];
			} else {
				value = new SimpleListManager(sessionManager);
			}

			// Determine how this value is passed to the constructor.
			// If the property comes from an extension then we must set
			// the property into an extension, otherwise we simply set
			// the property into the constructor parameters.
			if (!propertyAccessor.getPropertySet().isExtension()) {
				constructorParameters[propertyAccessor.getIndexIntoConstructorParameters()] = value;
			} else {
				Object [] extensionConstructorParameters = (Object[])extensionMap.get(propertyAccessor.getPropertySet());
				if (extensionConstructorParameters == null) {
					extensionConstructorParameters = new Object [propertyAccessor.getPropertySet().getConstructorProperties().size()];
					extensionMap.put(propertyAccessor.getPropertySet(), extensionConstructorParameters);
				}
				extensionConstructorParameters[propertyAccessor.getIndexIntoConstructorParameters()] = value;
			}
		}
			
		// We can now create the object.
		ExtendableObject extendableObject = (ExtendableObject)propertySet.constructImplementationObject(constructorParameters);
		
		objectKey.setObject(extendableObject);

		add(extendableObject);
		
		// If an account is added then we
		// must add a list that will contain the entries in the account.
		if (extendableObject instanceof Account) {
			Account account = (Account)extendableObject;
			sessionManager.addAccountList(account);
		}
		
		// If an entry is added then we
		// must update the lists of entries in each account.
		if (extendableObject instanceof Entry) {
			Entry entry = (Entry)extendableObject;
			if (entry.getAccount() != null) {
				sessionManager.addEntryToList(entry.getAccount(), entry);
			}
		}
		
        // This plug-in needs to know if a session has been
		// modified so it knows whether the session needs to
		// be saved.  Mark the session as modified now.
		sessionManager.setModified();
		
		return extendableObject;
	}
	
	public boolean remove(Object object) {
		// If an account is removed then we
		// clear out the list.
		if (object instanceof Account) {
			Account account = (Account)object;
			sessionManager.removeAccountList(account);
		}
		
		// If an entry is removed then we
		// must update the lists of entries in each account.
		if (object instanceof Entry) {
			Entry entry = (Entry)object;
			if (entry.getAccount() != null) {
				sessionManager.removeEntryFromList(entry.getAccount(), entry);
			}
		}
		
        // This plug-in needs to know if a session has been
		// modified so it knows whether the session needs to
		// be saved.  Mark the session as modified now.
		sessionManager.setModified();
		
		return super.remove(object);
	}
}
