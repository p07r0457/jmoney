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

import net.sf.jmoney.fields.AccountInfo;

/**
 * An implementation of the Account interface
 */
public abstract class Account extends ExtendableObject {
	
	protected IObjectKey parentKey;
	
	protected String name;

	protected IListManager subAccounts;
	
	protected Account(
			IObjectKey objectKey, 
			Map extensions, 
			IObjectKey parentKey,
			String name,
			IListManager subAccounts) {
		super(objectKey, extensions);
		this.parentKey = parentKey;
		this.name = name;
		this.subAccounts = subAccounts;
	}
	
	/**
	 * @return the name of this account.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param aName the name of this account.
	 */
	
	public void setName(String newName) {
		String oldName = name;
		name = newName;
		
		// Notify the change manager.
		processPropertyChange(AccountInfo.getNameAccessor(), oldName, newName);
	}

	public String getFullAccountName() {
		return getName();
	}
	
	public Account getParent() {
		ExtendableObject parent = parentKey.getObject();
		if (parent instanceof Account) {
			return (Account)parent;
		} else {
			return null;
		}
	}
	
	public Iterator getSubAccountIterator() {
		return subAccounts.iterator();
	}
	
	boolean deleteSubAccount(Account subAccount) {
		return subAccounts.remove(subAccount);
	}
	
    /**
	 * This method is used for debugging purposes only.
	 */
	public String toString() {
		return getName();
	}
	
	public int compareTo(Object o) {
		Account c = (Account) o;
		return getName().compareTo(c.getName());
	}

    public int getLevel () {
        int level;
        if (getParent() == null)
            level = 0;
        else 
            level = getParent().getLevel() + 1;
        System.out.println("Level from " + this.name + ", child of " + getParent() +" is " + level);
        return level;
    }
}
