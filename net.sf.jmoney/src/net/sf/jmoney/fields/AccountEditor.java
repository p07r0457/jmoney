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

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Vector;

import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.PropertyAccessor;
import net.sf.jmoney.model2.Session;

import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Note that this class has neither get/set methods for the value being edited
 * and no support for property change listeners.  This is
 * because objects of this class are tied to an CapitalAccount object.  
 * Changes to this
 * object are reflected by this object in the CapitalAccount class objects.  
 * Consumers who are interested in changes to the CapitalAccount class objects should
 * add themselves as listeners to the appropriate PropertyAccessor object.
 *
 * @author Nigel Westbury
 * @author Johann Gyger
 */
public class AccountEditor implements IPropertyControl {

    private ExtendableObject extendableObject;

    private PropertyAccessor accountPropertyAccessor;

    private Combo propertyControl;

    private Vector allAccounts = new Vector();

    /**
     * The session whose accounts are listed in the combo box
     */
    private Session session = null;
    
    /** 
     * @param propertyAccessor the accessor for the property to be edited
     * 			by this control.  The property must be of type Currency.
     */
    public AccountEditor(Composite parent, PropertyAccessor propertyAccessor) {
        propertyControl = new Combo(parent, 0);
        this.accountPropertyAccessor = propertyAccessor;

        // Selection changes are reflected immediately in the
        // mutable account object.  This allows other properties
        // such as money amounts to listen for changes to the
        // currency and change their format to be correct for
        // the newly selected currency.

        propertyControl.addSelectionListener(new SelectionListener() {
            public void widgetSelected(SelectionEvent e) {
                save();
            }
            public void widgetDefaultSelected(SelectionEvent e) {
                // Should this be here?
                save();
            }
        });
    }

    private void addAccounts(Iterator iter, Vector allAccounts) {
        while (iter.hasNext()) {
        	Account account = (Account) iter.next();
        	allAccounts.add(account);
            addAccounts(account.getSubAccountCollection().iterator(), allAccounts);
        }
    }
    
    private String getLabel(Account account) {
    	String text = account.getName();
        if (account instanceof CapitalAccount) {
            text += " (TRANSFER)";
        }
        if (account.getParent() != null) {
        	text += " - " + account.getParent().getFullAccountName();
        }
        return text;
    }
    
    /**
     * Load the control with the value from the given account.
     */
    public void load(ExtendableObject object) {
    	extendableObject = object;
    	
    	if (object == null) {
            propertyControl.setText("");
            propertyControl.removeAll();
    	} else {
    		if (session != object.getSession()) {
    			session = object.getSession();
    			
    			// We keep an array of accounts, the order of the array matches
    			// the order in the combo box.  This allows easy lookup of the 
    			// account given the selected index in the combo.
    			
    			addAccounts(session.getAccountCollection().iterator(), allAccounts);
    			
    			// Sort the accounts by name.
    			Collections.sort(allAccounts, new Comparator() {
    				public int compare(Object arg0, Object arg1) {
    					return ((Account)arg0).getName().compareTo(((Account)arg1).getName());
    				}
    			});
    			
                propertyControl.removeAll();
    			for (Iterator iter = allAccounts.iterator(); iter.hasNext();) {
    				Account account = (Account) iter.next();
    				propertyControl.add(getLabel(account));
    			}
    		}
    		
    		Account account = (Account) object.getPropertyValue(accountPropertyAccessor);
    		if (account != null) {
    			propertyControl.setText(getLabel(account));
    		} else {
    			// This is needed because of control re-use in the entry section
    			propertyControl.setText("");
    		}
    	}
    	propertyControl.setEnabled(object != null);
    }

    /**
     * Save the value from the control back into the object.
     *
     * Editors may update the property on a regular basis, not just when
     * the framework calls the <code>save</code> method.  However, the only time
     * that editors must update the property is when the framework calls this method.
     *
     * In this implementation we save the value back into the entry when the selection
     * is changed.  This causes the change to be seen in other views as soon as the
     * user changes the selection.
     *
     * The framework should never call this method when no account is selected
     * so we can assume that <code>extendableObject</code> is not null.
     */
    public void save() {
        int index = propertyControl.getSelectionIndex();
        if (index != -1) {
        	Account account = (Account)allAccounts.get(index);
        	extendableObject.setPropertyValue(accountPropertyAccessor, account);
        }
    }

    /* (non-Javadoc)
     * @see net.sf.jmoney.model2.IPropertyControl#getControl()
     */
    public Control getControl() {
        return propertyControl;
    }

}