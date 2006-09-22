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

package net.sf.jmoney.pages.accountProperties;

import java.util.Vector;

import net.sf.jmoney.IBookkeepingPage;
import net.sf.jmoney.IBookkeepingPageFactory;
import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.fields.AccountInfo;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.SessionChangeAdapter;
import net.sf.jmoney.model2.SessionChangeListener;
import net.sf.jmoney.views.NodeEditor;
import net.sf.jmoney.views.SectionlessPage;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * @author Nigel Westbury
 */
public class AccountPropertiesPage implements IBookkeepingPageFactory {
	
    private static final String PAGE_ID = "net.sf.jmoney.accountProperties";
    
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
		Vector<IPropertyControl> propertyControlList = new Vector<IPropertyControl>();
	
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
        	public void objectChanged(ExtendableObject extendableObject, ScalarPropertyAccessor changedProperty, Object oldValue, Object newValue) {
				if (extendableObject.equals(AccountPropertiesControl.this.account)) {
					// Find the control for this property.
					IPropertyControl propertyControl = propertyControlList.get(changedProperty.getIndexIntoScalarProperties());
					propertyControl.load(account);
				}
			}
		};
		
		/**
		 * @param parent
		 */
		public AccountPropertiesControl(Composite parent, CapitalAccount account, FormToolkit toolkit) {
			super(parent, SWT.NULL);

			GridLayout layout = new GridLayout();
			layout.numColumns = 2;
			setLayout(layout);
			
			// TODO: what is this?
			pack();
			
			this.account = account;
			
			session = account.getSession();
			
			// Create the controls to edit the properties.
			
			// Add the properties for the Account objects.
			PropertySet<?> extendablePropertySet = PropertySet.getPropertySet(account.getClass());
			for (final ScalarPropertyAccessor<?> propertyAccessor: extendablePropertySet.getScalarProperties3()) {

				Label propertyLabel = new Label(this, 0);
				propertyLabel.setText(propertyAccessor.getDisplayName() + ':');
				final IPropertyControl propertyControl = propertyAccessor.createPropertyControl(this, session);

				/*
				 * If the control factory set up grid data then leave it
				 * alone. Otherwise set up the grid data based on the
				 * properties minimum sizes and expansion weights. <P> The
				 * control widths are set to the minimum width plus 10 times
				 * the expansion weight. (As we are not short of space, we
				 * make them a little bigger than their minimum sizes). A
				 * minimum of 100 pixels is then applied because this makes
				 * the right sides of the smaller controls line up, which
				 * looks a little more tidy.
				 */  
				if (propertyControl.getControl().getLayoutData() == null) {
					GridData gridData = new GridData();
					gridData.minimumWidth = propertyAccessor.getMinimumWidth();
					gridData.widthHint = Math.max(propertyAccessor.getMinimumWidth() + 10 * propertyAccessor.getWeight(), 100);
					propertyControl.getControl().setLayoutData(gridData);
				}

				propertyControl.getControl().addFocusListener(
						new FocusAdapter() {

							// When a control gets the focus, save the old value here.
							// This value is used in the change message.
							String oldValueText;

							public void focusLost(FocusEvent e) {
								if (AccountPropertiesControl.this.session.getObjectKey().getSessionManager().isSessionFiring()) {
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
										"change " + propertyAccessor.getDisplayName() + " property"
										+ " in '" + AccountPropertiesControl.this.account.getName() + "' account"
										+ " from " + oldValueText
										+ " to " + newValueText;
								}
								AccountPropertiesControl.this.session.registerUndoableChange(description);
							}
							public void focusGained(FocusEvent e) {
								// Save the old value of this property for use in our 'undo' message.
								oldValueText = propertyAccessor.formatValueForMessage(
										AccountPropertiesControl.this.account);
							}
						});

				// Add to our list of controls.
				propertyControlList.add(propertyControl);

				toolkit.adapt(propertyLabel, false, false);
				toolkit.adapt(propertyControl.getControl(), true, true);
			}
			
			// Set the values from the account object into the control fields.
			for (IPropertyControl propertyControl: propertyControlList) {
				propertyControl.load(account);
			}

			session.getObjectKey().getSessionManager().addChangeListener(listener, this);
		}
	}


	/* (non-Javadoc)
	 * @see net.sf.jmoney.IBookkeepingPage#createPages(java.lang.Object, org.eclipse.swt.widgets.Composite)
	 */
	public IBookkeepingPage createFormPage(NodeEditor editor, IMemento memento)
	{
		SectionlessPage formPage = new SectionlessPage(
				editor,
				PAGE_ID, 
				JMoneyPlugin.getResourceString("AccountPropertiesPanel.title"),  //$NON-NLS-1$
				JMoneyPlugin.getResourceString("AccountPropertiesPanel.header")) { //$NON-NLS-1$
			
			public Composite createControl(Object nodeObject, Composite parent, FormToolkit toolkit, IMemento memento) {
				CapitalAccount account = (CapitalAccount)nodeObject;
				return new AccountPropertiesControl(parent, account, toolkit);
			}

			public void saveState(IMemento memento) {
				// The user cannot change the view of the account properties
				// so there is no 'view state' to save.
			}
		};

		try {
			editor.addPage(formPage);
		} catch (PartInitException e) {
			JMoneyPlugin.log(e);
			// TODO: cleanly leave out this page.
		}
		
		return formPage;
	}
}