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
 * of the IListManager interface.  This implementation simply
 * uses the Vector class to keep a list of objects.
 *
 * @author Nigel Westbury
 */
public class ListManagerCached extends Vector implements IListManager {
	private SessionManagementImpl sessionManager;
	private PropertyAccessor listProperty;
	
	public ListManagerCached(SessionManagementImpl sessionManager, PropertyAccessor listProperty) {
		this.sessionManager = sessionManager;
		this.listProperty = listProperty;
	}

	public IExtendableObject createNewElement(ExtendableObjectHelperImpl parent, PropertySet propertySet) {

		// First we insert the new row into the tables.

		Object [] values = propertySet.getDefaultPropertyValues2();
		int rowId = JDBCDatastorePlugin.insertIntoDatabase(propertySet, values, listProperty, parent, sessionManager);
		
 		// Now we build the in-memory object.
		// This is done here because in this case the object is always cached
		// in memory.
		
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
		for (Iterator iter = constructorProperties.iterator(); iter.hasNext(); ) {
			PropertyAccessor propertyAccessor = (PropertyAccessor)iter.next();
			int index = propertyAccessor.getIndexIntoConstructorParameters();
			if (propertyAccessor.isScalar()) {
				// Get the value from the passed object.
				Object value;
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
				value = values[propertyAccessor.getIndexIntoScalarProperties()];
				
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
				constructorParameters[index] = new ListManagerCached(sessionManager, propertyAccessor);
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
			throw new MalformedPluginException("An exception occured within a constructor in a plug-in.", e);
		}
		
		objectKey.setObject(extendableObject);

		add(extendableObject);
		
		return extendableObject;
	}
	
	public boolean remove(Object o) {
		boolean found = super.remove(o);
		
		// Delete this object from the database.
		if (found) {
			IExtendableObject extendableObject = (IExtendableObject)o;
			IDatabaseRowKey key = (IDatabaseRowKey)extendableObject.getObjectKey();
			boolean foundInDatabase = JDBCDatastorePlugin.deleteFromDatabase(key.getRowId(), extendableObject, sessionManager);
			if (!foundInDatabase) {
				throw new RuntimeException("database inconsistent");
			}
		}
		
		return found;
	}
}