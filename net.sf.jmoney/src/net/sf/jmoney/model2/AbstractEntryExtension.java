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
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

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
public abstract class AbstractEntryExtension extends ExtensionObject implements Entry {
    
	/**
	 * This is set by setBaseEntry.
	 */
    /* protected */ public Entry entry;
    
    /** Creates a new instance of AbstractEntryExtension */
    public AbstractEntryExtension() {
    }
    
    /*
     * All extensions implement the Entry interface.  This is for convenience
     * so the comsumer can get a single object that supports both the base Entry
     * methods and the extension methods.  All Entry interface methods are passed
     * on to the base Entry object.
     */
    
	/**
	 * Returns the key to the object.
	 */
	public IObjectKey getObjectKey() {
		return entry.getObjectKey();
	}

	/**
	 * Returns the description.
	 */
	public String getDescription() {
		return entry.getDescription();
	}

	/**
	 * Returns the category.
	 */
	public Account getAccount() {
		return entry.getAccount();
	}

	/**
	 * Returns the amount.
	 */
	public long getAmount() {
		return entry.getAmount();
	}

	/**
	 * Sets the description.
	 */
	public void setDescription(String aDescription) {
		entry.setDescription(aDescription);
	}

	/**
	 * Sets the category.
	 */
	public void setAccount(Account account) {
		entry.setAccount(account);
	}

	/**
	 * Sets the amount.
	 */
	public void setAmount(long anAmount) {
		entry.setAmount(anAmount);
	}

	public Transaction getTransaxion() {
            return entry.getTransaxion();
	}

	/**
	 * Returns the creation.
	 */
	public long getCreation() {
		return entry.getCreation();
	}

	/**
	 * Returns the check.
	 */
	public String getCheck() {
		return entry.getCheck();
	}

	/**
	 * Returns the valuta.
	 */
	public Date getValuta() {
		return entry.getValuta();
	}

        // TODO: should really be in a utility class.
	public String getFullAccountName() {
		return entry.getFullAccountName();
	}

	/**
	 * Returns the memo.
	 */
	public String getMemo() {
		return entry.getMemo();
	}

	/**
	 * Sets the creation.
	 */
	public void setCreation(long creation) {
		entry.setCreation(creation);
	}

	/**
	 * Sets the check.
	 */
	public void setCheck(String check) {
		entry.setCheck(check);
	}

	/**
	 * Sets the valuta.
	 */
	public void setValuta(Date valuta) {
		entry.setValuta(valuta);
	}

	/**
	 * Sets the memo.
	 */
	public void setMemo(String memo) {
		entry.setMemo(memo);
	}
            

        
        public ExtensionObject getExtension(PropertySet propertySetKey) {
        	return entry.getExtension(propertySetKey);
        }
        
        public Map getExtensionsAsIs() {
        	return entry.getExtensionsAsIs();
        }
        
        public Object getPropertyValue(PropertyAccessor propertyAccessor) {
            return entry.getPropertyValue(propertyAccessor);
        }
        
        public int getIntegerPropertyValue(PropertyAccessor propertyAccessor) {
            return getIntegerPropertyValue(propertyAccessor);
        }
        
        public long getLongPropertyValue(PropertyAccessor propertyAccessor) {
            return getLongPropertyValue(propertyAccessor);
        }
        
        public String getStringPropertyValue(PropertyAccessor propertyAccessor) {
            return getStringPropertyValue(propertyAccessor);
        }
        
        public Iterator getPropertyIterator(PropertyAccessor propertyAccessor) {
            return entry.getPropertyIterator(propertyAccessor);
        }
        
        // TODO: check whether we need this method.
        public String getPropertyValueAsString(PropertyAccessor propertyAccessor) {
            return getPropertyValueAsString(propertyAccessor);
        }
        
        public void setPropertyValue(PropertyAccessor propertyAccessor, Object value) {
            setPropertyValue(propertyAccessor, value);
        }

        public void setIntegerPropertyValue(PropertyAccessor propertyAccessor, int value) {
            setIntegerPropertyValue(propertyAccessor, value);
        }
        
        public void setLongPropertyValue(PropertyAccessor propertyAccessor, long value) {
            setLongPropertyValue(propertyAccessor, value);
        }
        
        public void setStringPropertyValue(PropertyAccessor propertyAccessor, String value) {
            setStringPropertyValue(propertyAccessor, value);
        }
        
        public void setCharacterPropertyValue(PropertyAccessor propertyAccessor, char value) {
            setCharacterPropertyValue(propertyAccessor, value);
        }

        // TODO: check whether we need this method.
        public void setPropertyValueFromString(PropertyAccessor propertyAccessor, String value) {
            setPropertyValueFromString(propertyAccessor, value);
        }

        // Required to be implemented in all classes derived from ExtensionObject
    	void setBaseObject(IExtendableObject baseObject) {
    		this.entry = (Entry)baseObject;
    	}

    	protected void firePropertyChange(String propertyLocalName, Object oldValue, Object newValue) {
            if (!newValue.equals(oldValue)) {
            	try {
            		propertySet.getProperty(propertyLocalName).firePropertyChange(
            				entry, oldValue, newValue);
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
        
		public boolean isMutable() {
			// Should not be called on an extension.
			throw new RuntimeException("isMutable called on an extension");
		}
}
