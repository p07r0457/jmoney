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
import java.util.Iterator;
import java.util.Vector;

import net.sf.jmoney.model2.ExtendableObject;
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
	public ExtendableObject createNewElement(ExtendableObject parent, PropertySet propertySet/*, Object[] values, ExtensionProperties[] extensionProperties */) {
		Collection constructorProperties = propertySet.getDefaultConstructorProperties();
		
		int numberOfParameters = constructorProperties.size();
		if (!propertySet.isExtension()) {
			numberOfParameters += 3;
		}
		Object[] constructorParameters = new Object[numberOfParameters];
		
		SimpleObjectKey objectKey = new SimpleObjectKey(sessionManager);
		
		int index = 0;
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
		
        // This plug-in needs to know if a session has been
		// modified so it knows whether the session needs to
		// be saved.  Mark the session as modified now.
		sessionManager.setModified();
		
		return extendableObject;
	}
	
	public boolean remove(Object object) {
		return super.remove(object);
	}
}
