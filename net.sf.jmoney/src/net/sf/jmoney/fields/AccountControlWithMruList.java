/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2006 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.fields;

import java.util.Iterator;
import java.util.LinkedList;

import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.Session;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.List;
import org.eclipse.ui.IMemento;

public class AccountControlWithMruList extends AccountComposite {
	protected Session session;
    protected List accountList;
    protected AccountControl accountControl;
    
    protected LinkedList recentlyUsedList = new LinkedList();
    
	public AccountControlWithMruList(Composite parent, Session session, Class<? extends Account> accountClass) {
		super(parent, SWT.NONE);
		this.session = session;
		
		setLayout(new GridLayout(1, false));
		
        accountList = new List(this, SWT.NONE);
        accountControl = new AccountControl(this, session, accountClass);

        accountList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        accountControl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        accountList.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				int selection = accountList.getSelectionIndex();
				if (selection >= 0) {
					accountControl.setAccount((Account)recentlyUsedList.get(selection));
				}
			}
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});
	}

	public void rememberChoice() {
    	Account account = accountControl.getAccount();
    	if (account != null) {
    		if (recentlyUsedList.size() != 0) {
    			int index = recentlyUsedList.indexOf(account);
    			if (index == -1) {
    				// Drop off head if list is already full
    	    		if (recentlyUsedList.size() >= 5) {
    	    			recentlyUsedList.removeFirst();
    	    			accountList.remove(0);
    	    		}
    			} else {
    				recentlyUsedList.remove(account);
    				accountList.remove(index);
    			}
    		}
    		recentlyUsedList.addLast(account);
    		accountList.add(account.getName());
    	}
	}

	public Account getAccount() {
	    return accountControl.getAccount();
	}
	
	public void setAccount(Account account) {
		accountControl.setAccount(account);
	}

	public void init(IMemento memento) {
		if (memento != null) {
			IMemento [] mruAccountMementos = memento.getChildren("mruAccount");
			for (int i = 0; i < mruAccountMementos.length; i++) {
				String fullAccountName = mruAccountMementos[i].getString("name");
				Account account = session.getAccountByFullName(fullAccountName);
	    		recentlyUsedList.addLast(account);
	    		accountList.add(account.getName());
			}
		}
	}

	public void saveState(IMemento memento) {
		for (Iterator iter = recentlyUsedList.iterator(); iter.hasNext(); ) {
			Account account = (Account)iter.next();
			memento.createChild("mruAccount").putString("name", account.getFullAccountName());
		}
	}
}
