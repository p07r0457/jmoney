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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.Iterator;
import java.util.Vector;

import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IExtendableObject;
import net.sf.jmoney.model2.MalformedPluginException;
import net.sf.jmoney.model2.IListManager;
import net.sf.jmoney.model2.PropertyAccessor;
import net.sf.jmoney.model2.PropertySet;

/**
 * @author Nigel
 *
 * Every datastore implementation must provide an implementation
 * of the IListManager interface.  This implementation simply
 * uses the Vector class to keep a list of objects.
 */
public class SimpleListManager extends Vector implements IListManager {

	private SessionManager sessionManager;

	public SimpleListManager(SessionManager sessionManager) {
	 	this.sessionManager = sessionManager;
	 }

	/* (non-Javadoc)
	 * @see net.sf.jmoney.model2.IListManager#createNewElement(net.sf.jmoney.model2.ExtendableObject, net.sf.jmoney.model2.PropertySet, java.lang.Object[], net.sf.jmoney.model2.ExtensionProperties[])
	 */
	public IExtendableObject createNewElement(ExtendableObject parent, PropertySet propertySet/*, Object[] values, ExtensionProperties[] extensionProperties */) {
		Vector constructorProperties = propertySet.getConstructorProperties();
		int numberOfParameters = constructorProperties.size();
		if (!propertySet.isExtension()) {
			numberOfParameters += 3;
		}
		Object[] constructorParameters = new Object[numberOfParameters];
		
		Object [] values = propertySet.getDefaultPropertyValues2();
		
		SimpleObjectKey objectKey = new SimpleObjectKey(sessionManager);
		
		constructorParameters[0] = objectKey;
		constructorParameters[1] = null;
		constructorParameters[2] = parent.getObjectKey();
		
		// For all lists, set the Collection object to be a Vector.
		// For all primative properties, get the value from the passed object.
		// For all extendable objects, we get the property value from
		// the passed object and then get the object key from that.
		// This works because all objects must be in a list and that
		// list owns the object, not us.
		int valueIndex = 0;
		for (Iterator iter = constructorProperties.iterator(); iter.hasNext(); ) {
			PropertyAccessor propertyAccessor = (PropertyAccessor)iter.next();
			int index = propertyAccessor.getIndexIntoConstructorParameters();
			if (propertyAccessor.isScalar()) {
				// Get the value from the passed object array.
				Object value = values[valueIndex++];
				
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
				constructorParameters[index] = new SimpleListManager(sessionManager);
			}
		}
		
		// We can now create the object.
		// The parameters to the constructor have been placed
		// in the constructorParameters array so we need only
		// to call the constructor.
		
		Constructor constructor = propertySet.getConstructor();
		ExtendableObject extendableObject;
		try {
			extendableObject = (ExtendableObject)constructor.newInstance(constructorParameters);
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
		
        // This plug-in needs to know if a session has been
		// modified so it knows whether the session needs to
		// be saved.  Mark the session as modified now.
		sessionManager.setModified(true);
		
		return extendableObject;
	}
	
	public boolean remove(Object object) {
		boolean found = super.remove(object);
		
		// Fire the event.
		if (found) {
			sessionManager.getSession().objectDeleted((IExtendableObject)object);
		}
		
		return found;
	}
}
