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
 * To add fields and methods to an Account object, one should
 * derive a class on AccountExtension.  This mechanism
 * allows multiple extensions to an Account object to be added
 * and maintained at runtime.
 *
 */
public abstract class AccountExtension extends ExtensionObject implements Account, Serializable {
    
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
            return getBaseObject().getName();
        }

	/**
	 * @return the full qualified name of the category.
	 */
	public String getFullAccountName() {
            return getBaseObject().getFullAccountName();
        }

        public Account getParent() {
            return getBaseObject().getParent();
        }
        
        public Iterator getSubAccountIterator() {
            return getBaseObject().getSubAccountIterator();
        }

    	public Account getBaseObject() {
    		return (Account)baseObject;
    	}

  
        protected void firePropertyChange(String propertyLocalName, Object oldValue, Object newValue) {
            
            if (!newValue.equals(oldValue)) {
            	try {
            		propertySet.getProperty(propertyLocalName).firePropertyChange(
            				baseObject, oldValue, newValue);
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
