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

import java.util.Iterator;

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

	public Iterator getPropertyIterator(PropertyAccessor propertyAccessor) {
    	return baseObject.getPropertyIterator(propertyAccessor);
	}
	
	public IObjectKey getObjectKey() {
    	return baseObject.getObjectKey();
	}
	
	public ExtensionObject getExtension(PropertySet propertySet) {
    	return baseObject.getExtension(propertySet);
    }
    
    public Object getPropertyValue(PropertyAccessor propertyAccessor) {
        return baseObject.getPropertyValue(propertyAccessor);
    }
    
    public int getIntegerPropertyValue(PropertyAccessor propertyAccessor) {
        return baseObject.getIntegerPropertyValue(propertyAccessor);
    }
    
    public long getLongPropertyValue(PropertyAccessor propertyAccessor) {
        return getLongPropertyValue(propertyAccessor);
    }
    
    public String getStringPropertyValue(PropertyAccessor propertyAccessor) {
        return getStringPropertyValue(propertyAccessor);
    }
    
    public char getCharacterPropertyValue(PropertyAccessor propertyAccessor) {
        return baseObject.getCharacterPropertyValue(propertyAccessor);
    }
    
    public void setPropertyValue(PropertyAccessor propertyAccessor, Object value) {
    	baseObject.setPropertyValue(propertyAccessor, value);
    }

    public void setIntegerPropertyValue(PropertyAccessor propertyAccessor, int value) {
    	baseObject.setIntegerPropertyValue(propertyAccessor, value);
    }
    
    public void setLongPropertyValue(PropertyAccessor propertyAccessor, long value) {
    	baseObject.setLongPropertyValue(propertyAccessor, value);
    }
    
    public void setStringPropertyValue(PropertyAccessor propertyAccessor, String value) {
    	baseObject.setStringPropertyValue(propertyAccessor, value);
    }
    
    public void setCharacterPropertyValue(PropertyAccessor propertyAccessor, char value) {
    	baseObject.setCharacterPropertyValue(propertyAccessor, value);
    }

	protected void processPropertyChange(PropertyAccessor propertyAccessor, Object oldValue, Object newValue) {
		((ExtendableObject)baseObject).processPropertyChange(propertyAccessor, oldValue, newValue);
	}
}
