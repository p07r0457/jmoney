/*
 *
 *  JMoney - A Personal Finance Manager
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

package net.sf.jmoney.fields;

import java.util.Date;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.IPropertyControlFactory;
import net.sf.jmoney.model2.PropertyAccessor;
import net.sf.jmoney.model2.Session;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;

/**
 * A control factory to select an account.
 * 
 * @author Nigel Westbury
 * @author Johann Gyger
 */
public class AccountControlFactory implements IPropertyControlFactory {
    
    private Account[] allAccounts;
    
    public IPropertyControl createPropertyControl(Composite parent, PropertyAccessor propertyAccessor) {
        return new AccountEditor(parent, propertyAccessor);
    }

    /*
     * @author Faucheux
     */
	public CellEditor createCellEditor(Table table) {
	    // Collect the accounts and extract the list of their names
	    Session session = JMoneyPlugin.getDefault().getSession();
	    allAccounts = session.getAccountList();
	    
	    String[] proposedItems = new String [allAccounts.length];
	    for (int i = 0; i<allAccounts.length; i++) proposedItems[i] = allAccounts[i].getFullAccountName();
		return new ComboBoxCellEditor(table, proposedItems);
	}
	
	/*
	 * @author Faucheux
	 */
	public Object getValueTypedForCellEditor(ExtendableObject extendableObject, PropertyAccessor propertyAccessor) {
		// The entry has a reference to the account. We have to find in the list of the accounts
	    // "proposedItems" the name if this account and returns the index of this name.
	    Account account = (Account) extendableObject.getPropertyValue(propertyAccessor);
	    int i = 0;
	    while (i<allAccounts.length && ! (account == allAccounts[i])) i++;
        return new Integer(i);
	}

	/*
	 * @author Faucheux
	 */
	public void setValueTypedForCellEditor(ExtendableObject extendableObject, PropertyAccessor propertyAccessor, Object value) {
	    int index = ((Integer) value).intValue();
	    extendableObject.setPropertyValue(propertyAccessor, allAccounts[index]);
	}

	public String formatValueForMessage(ExtendableObject extendableObject, PropertyAccessor propertyAccessor) {
        Account value = (Account) extendableObject.getPropertyValue(propertyAccessor);
        return value == null ? "<none>" : value.getFullAccountName();
    }

    public String formatValueForTable(ExtendableObject extendableObject, PropertyAccessor propertyAccessor) {
        Account value = (Account) extendableObject.getPropertyValue(propertyAccessor);
        return value == null ? "" : value.getFullAccountName();
    }

	public boolean isEditable() {
		return true;
	}
}