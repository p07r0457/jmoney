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

import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import net.sf.jmoney.model2.*;

/**
 * An implementation of the Account interface
 */
public abstract class AbstractAccountImpl extends ExtendableObjectHelperImpl implements Account {

        protected Account parent;
  
        protected IListManager subAccounts;
        
        protected AbstractAccountImpl(
        		IObjectKey objectKey, 
				Map extensions, 
				IListManager subAccounts) {
        	super(objectKey, extensions);
			this.subAccounts = subAccounts;
        }
        
	public String getFullAccountName() {
		return getName();
	}

        public Account getParent() {
            return parent;
        }

        // This method is used when setting the back references
    	// TODO: we should be able to do this in the initializers.
    	// If so then the datastore no longer needs to do this
    	// and we can remove this public method.
        public void setParent(Account parent) {
            this.parent = parent;
        }
        
        /**
         * This method is called by the MutableAccount class,
         * and also by the datastore during object initialization.
         */
        public void addSubAccount(Account subAccount) {
            subAccounts.add(subAccount);
        }
        
        void removeSubAccount(Account subAccount) {
            subAccounts.remove(subAccount);
        }

        public Iterator getSubAccountIterator() {
            return subAccounts == null
                ? new EmptyIterator()
                : subAccounts.iterator();
        }
     
    	public String toString() {
    		return getName();
    	}

    	public int compareTo(Object o) {
    		Account c = (Account) o;
    		return getName().compareTo(c.getName());
    	}
}
