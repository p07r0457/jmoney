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
import net.sf.jmoney.fields.AccountInfo;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.PropertyAccessor;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.SessionChangeAdapter;
import net.sf.jmoney.model2.SessionChangeListener;

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
		CapitalAccount account = null;
		Session session = null;
		
		/**
		 * List of the IPropertyControl objects for the
		 * properties that can be edited in this panel.
		 */
		Vector propertyControlList = new Vector();
	
		// Listen for changes to the account properties.
		// If anyone else changes any of the account properties
		// then we update the values in the edit controls.
		// Note that we do not need to deal with the case where
		// the account is deleted or the session is closed.  
		// If the account is deleted then
		// the navigation view will destroy the node and destroy
		// this view.
		
		private SessionChangeListener listener =
			new SessionChangeAdapter() {
		    public void accountChanged(final Account account, PropertyAccessor propertyAccessor, Object oldValue, Object newValue) {
				if (account == AccountPropertiesControl.this.account) {
					System.out.println("Property changed, reloading control: " + propertyAccessor.getLocalName());
					// Find the control for this property.
					IPropertyControl propertyControl = (IPropertyControl)propertyControlList.get(propertyAccessor.getIndexIntoScalarProperties());
					propertyControl.load(account);
				}
			}
		};
		
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
			
			JMoneyPlugin.getDefault().addSessionChangeListener(listener);
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

								// When a control gets the focus, save the old value here.
								// This value is used in the change message.
								String oldValueText;
								
								public void focusLost(FocusEvent e) {
									System.out.println("Focus lost: " + propertyAccessor.getLocalName());
									
									if (AccountPropertiesControl.this.session.isSessionFiring()) {
										return;
									}
									
									propertyControl.save();
									String newValueText = propertyAccessor.formatValueForMessage(
											AccountPropertiesControl.this.account);
									
									String description;
									if (propertyAccessor == AccountInfo.getNameAccessor()) {
										description = 
											"rename account from " + oldValueText
											+ " to " + newValueText;
									} else {
										description = 
											"change " + propertyAccessor.getShortDescription() + " property"
											+ " in '" + AccountPropertiesControl.this.account.getName() + "' account"
											+ " from " + oldValueText
											+ " to " + newValueText;
									}
									AccountPropertiesControl.this.session.registerUndoableChange(description);
								}
								public void focusGained(FocusEvent e) {
									System.out.println("Focus gained: " + propertyAccessor.getLocalName());
									// Save the old value of this property for use in our 'undo' message.
									oldValueText = propertyAccessor.formatValueForMessage(
											AccountPropertiesControl.this.account);
								}
							});
					
					// Add to our list of controls.
					propertyControlList.add(propertyControl);
				}
			}
			
			// Set the values from the account object into the control fields.
			for (Iterator iter = propertyControlList.iterator(); iter.hasNext(); ) {
				IPropertyControl propertyControl = (IPropertyControl)iter.next();
				propertyControl.load(account);
			}
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
