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

import java.util.Map;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.model2.*;

/**
 * An implementation of the IncomeExpenseAccount interface
 */
public class IncomeExpenseAccountImpl extends AbstractAccountImpl implements IncomeExpenseAccount {

	private String fullAccountName = null;

	public IncomeExpenseAccountImpl(
			IObjectKey objectKey, 
			Map extensions, 
			IObjectKey parent,
			String accountName,
			IListManager subAccounts) {
		super(objectKey, extensions, parent, subAccounts);
		setName(accountName);
	}

	protected boolean isMutable() {
		return false;
	}

	protected IExtendableObject getOriginalObject() {
		// This method should be called only if isMutable returns true,
		// which it never does.  However, we must provide an implementation.
		throw new RuntimeException("should never be called");
	}
	
	protected String getExtendablePropertySetId() {
		return "net.sf.jmoney.category";
	}
	
	public String getFullAccountName() {
		if (fullAccountName == null) {
			fullAccountName = name;
			Account ancestorCategory = getParent();
			while (ancestorCategory != null) {
				fullAccountName = ancestorCategory.getName() + ":" + fullAccountName;
				ancestorCategory = ancestorCategory.getParent();
			}
		}
		return fullAccountName;
	}

	// TODO: use debugger to see if this version is called
	// when the Method object references the version in the
	// abstract base class.  If not then this is broke.
	public void setName(String name) {
		super.setName(name);
		fullAccountName = null;
	}

	/**
	 * This method is required by the JMoney framework.
	 * 
	 * @param name
	 * @param extensionProperties
	 * @return
	 */
    public IncomeExpenseAccount createSubAccount() {
    	IncomeExpenseAccount newAccount = (IncomeExpenseAccount)subAccounts.createNewElement(
				this, 
				JMoneyPlugin.getIncomeExpenseAccountPropertySet());

		// Fire the event.
/* how do we get the session???    	
        final AccountAddedEvent event = new AccountAddedEvent(session, newAccount);
        session.fireEvent(
        	new ISessionChangeFirer() {
        		public void fire(SessionChangeListener listener) {
        			listener.accountAdded(event);
        		}
       		});
*/
        return newAccount;
    }
    
    static public  Object [] getDefaultProperties() {
		return new Object [] { "new category" };
	}
}
