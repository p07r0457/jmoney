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

import java.util.Date;
import java.util.Map;
import java.util.Vector;

import net.sf.jmoney.model2.*;

/**
 * An implementation of the IncomeExpenseAccount interface
 */
public class IncomeExpenseAccountImpl extends AbstractAccountImpl implements IncomeExpenseAccount {

	private String accountName;

	private String fullAccountName = null;

	protected Vector children;
	
	public IncomeExpenseAccountImpl(
			IObjectKey objectKey, 
			Map extensions, 
			IObjectKey parent,
			String accountName,
			IListManager subAccounts) {
		super(objectKey, extensions, parent, subAccounts);
		setName(accountName);
		children = new Vector();
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
		AbstractAccountImpl newAccount = (AbstractAccountImpl)subAccounts.createNewElement(this, propertySet, account);

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
        
        public int getLevel () {
            if (parentKey == null) {
                return 0;
            } else {
                System.err.println("Warning: ParentKey is not null! > Level is false"); // TODO
                return 1;
            }
        }
        
        /**
         * @author Faucheux
         */
     	public long getBalance(Session session, Date fromDate, Date toDate) {
     	   return 0;
     	}

        /**
         * @author Faucheux
         */
     	public long getBalanceWithSubAccounts(Session session, Date fromDate, Date toDate) {
     	    return 0;
     	}

        public void addChild(Account a) {
            children.add(a);
        }
}
