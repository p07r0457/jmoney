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

package net.sf.jmoney.serializeddatastore;

import java.util.Iterator;
import java.util.Vector;

import net.sf.jmoney.model2.*;

/**
 * An implementation of the Account interface
 */
public abstract class AbstractAccountImpl extends ExtendableObjectHelperImpl implements Account {

        protected Account parent;
  
        protected Vector subAccounts = null;
        
	public String getFullAccountName() {
		return getName();
	}

        public Account getParent() {
            return parent;
        }
        
        // Package access only so this is not serialized.
        void setParent(Account parent) {
            this.parent = parent;
        }
        
        /**
         * Because this method is called only by the MutableAccount class,
         * and only one MutableAccount class object can exist for this object,
         * and the MutableAccount class is not thread safe, this method
         * does not need to be thread safe.
         */
        public void addSubAccount(Account subAccount) {
            if (subAccounts == null) {
                subAccounts = new Vector();
            }
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

        // Methods required so these objects can be serialized as beans.
        
        public void setSubAccounts(Vector subAccounts) {
            this.subAccounts = subAccounts;
        }
        
        public Vector getSubAccounts() {
            return subAccounts;
        }
}
