/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2002 Johann Gyger <johann.gyger@switzerland.org>
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

import java.io.Serializable;
import java.util.Iterator;

/**
 *
 * @author  Nigel
 *
 * To add fields and methods to an Entry object, one should
 * derive a class on AbstractEntryExtension.  This mechanism
 * allows multiple extensions to an Entry object to be added
 * and maintained at runtime.
 *
 */
public abstract class AccountExtension extends ExtensionObject implements Account, Serializable {
    
	/**
	 * following two are set by setBaseAccount
	 */
    /* protected */ public Account account;
    protected PropertySet propertySet;
    
    /** Creates a new instance of AbstractEntryExtension */
    public AccountExtension() {
    }

    /*
     * All extensions to Account objects implement the Account interface.  This is for convenience
     * so the comsumer can get a single object that supports both the base Entry
     * methods and the extension methods.  All Account interface methods are passed
     * on to the base Account object.
     */
    
	/**
	 * @return the name of the category.
	 */
	public String getName() {
            return account.getName();
        }

	/**
	 * @return the full qualified name of the category.
	 */
	public String getFullAccountName() {
            return account.getFullAccountName();
        }

        public Account getParent() {
            return account.getParent();
        }
        
        public Iterator getSubAccountIterator() {
            return account.getSubAccountIterator();
        }

        public ExtensionObject getExtension(PropertySet propertySet) {
        	return account.getExtension(propertySet);
        }
        
        public Object getPropertyValue(PropertyAccessor propertyAccessor) {
            return account.getPropertyValue(propertyAccessor);
        }
        
        public int getIntegerPropertyValue(PropertyAccessor propertyAccessor) {
            return account.getIntegerPropertyValue(propertyAccessor);
        }
        
        // TODO: check whether we need this method.
        public String getPropertyValueAsString(PropertyAccessor propertyAccessor) {
            return account.getPropertyValueAsString(propertyAccessor);
        }
        
        public void setPropertyValue(PropertyAccessor propertyAccessor, Object value) {
        	if (account instanceof MutableAccount) {
        		((MutableAccount)account).setPropertyValue(propertyAccessor, value);
        	} else {
        		throw new RuntimeException("Setting value in a non-mutable extension.");
        	}
        }

        public void setIntegerPropertyValue(PropertyAccessor propertyAccessor, int value) {
        	if (account instanceof MutableAccount) {
        		((MutableAccount)account).setIntegerPropertyValue(propertyAccessor, value);
        	} else {
        		throw new RuntimeException("Setting value in a non-mutable extension.");
        	}
        }
        
        public void setCharacterPropertyValue(PropertyAccessor propertyAccessor, char value) {
        	if (account instanceof MutableAccount) {
        		((MutableAccount)account).setCharacterPropertyValue(propertyAccessor, value);
        	} else {
        		throw new RuntimeException("Setting value in a non-mutable extension.");
        	}
        }

        // TODO: check whether we need this method.
        public void setPropertyValueFromString(PropertyAccessor propertyAccessor, String value) {
        	if (account instanceof MutableAccount) {
        		((MutableAccount)account).setPropertyValueFromString(propertyAccessor, value);
        	} else {
        		throw new RuntimeException("Setting value in a non-mutable extension.");
        	}
        }

        // Required to be implemented in all classes derived from ExtensionObject
    	void setBaseObject(IExtendableObject baseObject) {
    		this.account = (Account)baseObject;
    	}

  
        protected void firePropertyChange(String propertyLocalName, Object oldValue, Object newValue) {
            
            if (!newValue.equals(oldValue)) {
            	try {
            		propertySet.getProperty(propertyLocalName).firePropertyChange(
            				account, oldValue, newValue);
            	} catch (PropertyNotFoundException e) {
            		throw new RuntimeException("no such property registered " + propertyLocalName);
            	}
            }
        }
        
        protected void firePropertyChange(String propertyLocalName, int oldValue, int newValue) {
            firePropertyChange(propertyLocalName, new Integer(oldValue), new Integer(newValue));
        }
        
        protected void firePropertyChange(String propertyLocalName, char oldValue, char newValue) {
            firePropertyChange(propertyLocalName, new Character(oldValue), new Character(newValue));
        }
}
