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

package net.sf.jmoney.bookkeepingPages;

import java.util.Iterator;
import java.util.Vector;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.ui.IMemento;

import net.sf.jmoney.IBookkeepingPageListener;
import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.ObjectLockedForEditException;
import net.sf.jmoney.model2.PropertyAccessor;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.Session;

/**
 * @author Nigel
 *
 * As each folder view will load its own instances of the extension classes,
 * and each folder view will only display the tab items for a single
 * object at any point of time, this class can cache the tab items
 * and re-use them for each selected object.
 */
public class AccountPropertiesPages implements IBookkeepingPageListener {
	
	AccountPropertiesControl propertiesControl;
	
	/**
	 * The implementation for the composite control that contains
	 * the account property controls.
	 */
	private class AccountPropertiesControl extends Composite {
		CapitalAccount account;
		Session session;
		
		/**
		 * List of the IPropertyControl objects for the
		 * properties that can be edited in this panel.
		 */
		Vector propertyControlList = new Vector();
		
		/**
		 * @param parent
		 */
		public AccountPropertiesControl(Composite parent) {
			super(parent, SWT.NULL);

			GridLayout layout = new GridLayout();
			layout.numColumns = 2;
			setLayout(layout);
			GridData data = new GridData();
			data.verticalAlignment = GridData.FILL;
			data.horizontalAlignment = GridData.FILL;
			setLayoutData(data);
			
			// TODO: what is this?
			pack();
		}

		void setAccount(CapitalAccount account, Session session) {
			this.account = account;
			this.session = session;
			
			// Create the controls to edit the properties.
			// This is done here and not when this composite
			// is constructed because the property set depends
			// on the type of the account object.
			
			propertyControlList.clear();
			
			// Add the properties for the Account objects.
			PropertySet extendablePropertySet = PropertySet.getPropertySet(account.getClass());
			for (Iterator iter = extendablePropertySet.getPropertyIterator3(); iter.hasNext(); ) {
				final PropertyAccessor propertyAccessor = (PropertyAccessor)iter.next();
				if (propertyAccessor.isScalar()) {
					Label propertyLabel = new Label(this, 0);
					propertyLabel.setText(propertyAccessor.getShortDescription());
					final IPropertyControl propertyControl = propertyAccessor.createPropertyControl(this);
					propertyControl.getControl().addFocusListener(
							new FocusAdapter() {
								public void focusLost(FocusEvent e) {
									propertyControl.save();
									updateAndReleaseAccount();
								}
								public void focusGained(FocusEvent e) {
									lockAccountForEdit();
								}
							});
					
					// Add to our list of controls.
					propertyControlList.add(propertyControl);
				}
			}
			
			// Get a lock on the account and set the property
			// values into the controls.
			lockAccountForEdit();
		}

		void lockAccountForEdit() {
			// Try to get a lock on the account.		
//			try {
				// TODO: Decide if we need the ability to get a lock on the account.

				// Set the values from the account object into the Text fields.
				for (Iterator iter = propertyControlList.iterator(); iter.hasNext(); ) {
					IPropertyControl propertyControl = (IPropertyControl)iter.next();
					propertyControl.load(account);
				}
/*				
			} catch (ObjectLockedForEditException e) {
				// Someone else is editing the properties of this
				// account so we gray out the controls.
				
				// TODO: do we need a flag to indicate we are in disabled mode?
				
				// Set the values from the account object into the Text fields.
				for (Iterator iter = propertyControlList.iterator(); iter.hasNext(); ) {
					IPropertyControl propertyControl = (IPropertyControl)iter.next();
					propertyControl.loadDisabled(account);
				}
			}
*/			
		}
		
		void updateAndReleaseAccount() {
			// Property values are copied from the control into
			// the account object whenever a control
			// loses focus.  Therefore we know that all values
			// have been copied from the controls into the
			// account object by the time we get here
			// and we need only commit the changes to any
			// underlying database.
			JMoneyPlugin.getChangeManager().applyChanges("update account properties");
		}
	}
	
	public void init(IMemento memento) {
		// The user cannot change the way this view is displayed,
		// so nothing to do here.
	}
	
	public void saveState(IMemento memento) {
		// The user cannot change the way this view is displayed,
		// so nothing to do here.
	}
	
	/* (non-Javadoc)
	 * @see net.sf.jmoney.IBookkeepingPageListener#getPageCount(java.lang.Object)
	 */
	public int getPageCount(Object selectedObject) {
		if (selectedObject instanceof CapitalAccount) {
			return 1;
		}
		return 0;
	}
	
	/* (non-Javadoc)
	 * @see net.sf.jmoney.IBookkeepingPageListener#createPages(java.lang.Object, org.eclipse.swt.widgets.Composite)
	 */
	public BookkeepingPage[] createPages(Object selectedObject, Session session, Composite parent) {
		if (selectedObject instanceof CapitalAccount) {
			CapitalAccount account = (CapitalAccount)selectedObject;
			AccountPropertiesControl propertiesControl = new AccountPropertiesControl(parent);
			propertiesControl.setAccount(account, session);
			return new BookkeepingPage[] 
									   { new BookkeepingPage(propertiesControl, JMoneyPlugin.getResourceString("AccountPropertiesPanel.title")) };
		}
		return null;
	}
}
