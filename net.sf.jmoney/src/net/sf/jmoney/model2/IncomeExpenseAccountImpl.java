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

import net.sf.jmoney.model2.*;

/**
 * An implementation of the IncomeExpenseAccount interface
 */
//Kludge:  This implements MutableIncomeExpenseAccount.
//You might think it should be implementing only IncomeExpenseAccount,
//and you would be right.  However, the setters are needed
//when reading the data from the XML.  Until this whole data
//storage mess is sorted out, we will leave this as is.
public class IncomeExpenseAccountImpl extends AbstractAccountImpl implements MutableIncomeExpenseAccount {

	private String accountName;

	private String fullAccountName = null;

	public IncomeExpenseAccountImpl(
			IObjectKey objectKey, 
			Map extensions, 
			String accountName,
			IListManager subAccounts) {
		super(objectKey, extensions, subAccounts);
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
	
	public String getName() {
		return accountName;
	}

	public String getFullAccountName() {
		if (fullAccountName == null) {
                    fullAccountName = accountName;
                    Account ancestorCategory = getParent();
                    while (ancestorCategory != null) {
			fullAccountName = ancestorCategory.getName() + ":" + fullAccountName;
                        ancestorCategory = ancestorCategory.getParent();
                    }
		}
		return fullAccountName;
	}

	public void setName(String accountName) {
		this.accountName = accountName;
		fullAccountName = null;
	}

	/**
	 * 
	 * @param accountPropertySet
	 * @return
	 */
	public Account createNewSubAccount(Session session, PropertySet propertySet, IncomeExpenseAccount account) {
		AbstractAccountImpl newAccount = (AbstractAccountImpl)subAccounts.createNewElement(propertySet, account);

		newAccount.setParent(this);

		// Fire the event.
        final AccountAddedEvent event = new AccountAddedEvent(session, newAccount);
        session.fireEvent(
        	new ISessionChangeFirer() {
        		public void fire(SessionChangeListener listener) {
        			listener.accountAdded(event);
        		}
       		});
        
        return newAccount;
	}

        // TODO: Ensure no mutable interface on this object already.
        public MutableIncomeExpenseAccount createNewSubAccount(Session session) {
            return new MutableIncomeExpenseAccountImpl(session, this, 0);
        }
        
        public MutableIncomeExpenseAccount createMutableAccount(Session session) throws ObjectLockedForEditException {
            return new MutableIncomeExpenseAccountImpl(session, this);
        }

		/* (non-Javadoc)
		 * @see net.sf.jmoney.model2.MutableIncomeExpenseAccount#commit()
		 */
		public IncomeExpenseAccount commit() {
			throw new RuntimeException("should never be called");
		}
	    
	    // This method is used by the datastore implementations.
	    // TODO: Should this be moved to a separate initialization interface?
		public void addSubAccount(IncomeExpenseAccount subAccount) {
			super.addSubAccount(subAccount);
		}
}
