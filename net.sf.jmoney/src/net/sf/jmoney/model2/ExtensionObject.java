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
package net.sf.jmoney.model2;

import java.lang.reflect.Field;


/**
 * @author Nigel
 *
 * This class is the base class from which all classes that implement
 * extension property sets should be derived.
 */
public abstract class ExtensionObject {
	protected ExtendableObject baseObject;
	protected PropertySet propertySet;
	
	void setBaseObject(ExtendableObject baseObject) {
		this.baseObject = baseObject;
	}
	
	void setPropertySet(PropertySet propertySet) {
		this.propertySet = propertySet;
	}

	public IObjectKey getObjectKey() {
    	return baseObject.getObjectKey();
	}
	
	public Session getSession() {
    	return baseObject.getSession();
	}
	
	/**
	 * Two or more instantiated objects may represent the same object
	 * in the datastore.  Such objects should be considered
	 * the same.  Therefore this method overrides the default
	 * implementation that is based on Java identity.
	 * <P>
	 * This method also considers two objects to be the same if either
	 * or both of the objects are extension objects and the underlying
	 * objects are the same.
	 * <P>
	 * @return true if the two objects represent the same object
	 * 		in the datastore, false otherwise.
	 */
	public boolean equals(Object object) {
    	return baseObject.equals(object);
	}
	
	public ExtensionObject getExtension(PropertySet propertySet) {
    	return baseObject.getExtension(propertySet);
    }
    
    public <V> V getPropertyValue(ScalarPropertyAccessor<V> propertyAccessor) {
        return baseObject.getPropertyValue(propertyAccessor);
    }
    
	public <E extends ExtendableObject> ObjectCollection<E> getListPropertyValue(ListPropertyAccessor<E> propertyAccessor) {
    	return baseObject.getListPropertyValue(propertyAccessor);
	}
	
    public <V> void setPropertyValue(ScalarPropertyAccessor<V> propertyAccessor, V value) {
    	baseObject.setPropertyValue(propertyAccessor, value);
    }

	protected void processPropertyChange(ScalarPropertyAccessor propertyAccessor, Object oldValue, Object newValue) {
		((ExtendableObject)baseObject).processPropertyChange(propertyAccessor, oldValue, newValue);
	}

	/**
	 * This method is used to enable other classes in the package to
	 * access protected fields in the extension objects.
	 * 
	 * @param theObjectKeyField
	 * @return
	 */
	Object getProtectedFieldValue(Field theObjectKeyField) {
    	try {
    		return theObjectKeyField.get(this);
    	} catch (IllegalArgumentException e) {
    		e.printStackTrace();
    		throw new RuntimeException("internal error");
    	} catch (IllegalAccessException e) {
    		e.printStackTrace();
    		// TODO: check the protection earlier and raise MalformedPlugin
    		throw new RuntimeException("internal error - field protection problem");
    	}
	}
}
