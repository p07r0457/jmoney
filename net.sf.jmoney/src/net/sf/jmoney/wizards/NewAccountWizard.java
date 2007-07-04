/*
*
*  JMoney - A Personal Finance Manager
*  Copyright (c) 2007 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.wizards;

import java.util.Vector;

import net.sf.jmoney.fields.AccountInfo;
import net.sf.jmoney.fields.IncomeExpenseAccountInfo;
import net.sf.jmoney.isolation.TransactionManager;
import net.sf.jmoney.isolation.UncommittedObjectKey;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.ExtendablePropertySet;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.IncomeExpenseAccount;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.model2.Session;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

public class NewAccountWizard extends Wizard {
	
	private ExtendablePropertySet<? extends Account> accountPropertySet;

	private TransactionManager transactionManager;
	
	private Account newUncommittedAccount;
	
	/**
	 * This is set when 'finish' is pressed and the new account is committed.
	 */
	private Account newCommittedAccount;
	
	/**
	 * 
	 * @param finalPropertySet the property set object of the class
	 * 		of account to create 
	 * @param parentAccount the parent account or null if this is to be
	 * 		a top level account 
	 */
	public NewAccountWizard(Session session, IncomeExpenseAccount parentAccount) {
		this.accountPropertySet = IncomeExpenseAccountInfo.getPropertySet();
		
		this.setWindowTitle("Create a New Category");
		this.setHelpAvailable(true);
		
		transactionManager = new TransactionManager(session.getDataManager());
		
		IncomeExpenseAccount parentAccount2 = transactionManager.getCopyInTransaction(parentAccount);
		if (parentAccount2 == null) {
			Session session2 = transactionManager.getSession();
			newUncommittedAccount = session2.createAccount(accountPropertySet);
		} else {
			newUncommittedAccount = parentAccount2.createSubAccount();
		}
//		PlatformUI.getWorkbench().getHelpSystem().setHelp(this.getContainer(), "com.toutvirtual.help.locationDialogId");
	}
	
	/**
	 * 
	 * @param finalPropertySet the property set object of the class
	 * 		of account to create 
	 * @param parentAccount the parent account or null if this is to be
	 * 		a top level account 
	 */
	public NewAccountWizard(Session session, CapitalAccount parentAccount, ExtendablePropertySet<? extends CapitalAccount> accountPropertySet) {
		this.accountPropertySet = accountPropertySet;
		
		this.setWindowTitle("Create a New Account");
		this.setHelpAvailable(true);
		
		transactionManager = new TransactionManager(session.getDataManager());
		
		CapitalAccount parentAccount2 = transactionManager.getCopyInTransaction(parentAccount);
		if (parentAccount2 == null) {
			Session session2 = transactionManager.getSession();
			newUncommittedAccount = session2.createAccount(accountPropertySet);
		} else {
			newUncommittedAccount = parentAccount2.createSubAccount(accountPropertySet);
		}
//		PlatformUI.getWorkbench().getHelpSystem().setHelp(this.getContainer(), "com.toutvirtual.help.locationDialogId");
	}
	
	@Override
	public void addPages()
	{
		// Show the page that prompts for all the property values.
		WizardPage propertyPage = new PropertyPage("propertyPage");
		addPage(propertyPage);

		WizardPage summaryPage = new SummaryPage("summaryPage");
		addPage(summaryPage);
	}
	
	@Override
	public boolean performFinish() {
		// TODO: verify properties are valid.
		
		transactionManager.commit("Add New Account");
		
		newCommittedAccount = (Account)((UncommittedObjectKey)newUncommittedAccount.getObjectKey()).getCommittedObjectKey().getObject();
		
		return true;
	}
	
	class PropertyPage extends WizardPage {
		/**
		 * List of the IPropertyControl objects for the
		 * properties that can be edited in this panel.
		 */
		private Vector<IPropertyControl> propertyControlList = new Vector<IPropertyControl>();
	
		Text accountNameTextbox;
		
		PropertyPage(String pageName) {
			super(pageName);
			setTitle("Account Properties");
			setMessage("Enter values for the account properties");
		}
		
		public void createControl(Composite parent) {
			Composite container = new Composite(parent, SWT.NONE);
			
			GridLayout layout = new GridLayout();
			layout.numColumns = 2;
			container.setLayout(layout);
			
			// Create the controls to edit the properties.
			
			// Add the properties for the Account objects.
			for (final ScalarPropertyAccessor<?> propertyAccessor: accountPropertySet.getScalarProperties3()) {

				Label propertyLabel = new Label(container, SWT.NONE);
				propertyLabel.setText(propertyAccessor.getDisplayName() + ':');
				final IPropertyControl propertyControl = propertyAccessor.createPropertyControl(container);

				// Bit of a kludge.  We have special processing for the account
				// name, so save this one.
				if (propertyAccessor == AccountInfo.getNameAccessor()) {
					accountNameTextbox = (Text)propertyControl.getControl();
				}
				
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
						    @Override	
							public void focusLost(FocusEvent e) {
								// TODO: Verify this is needed.  Clean it up?
								if (NewAccountWizard.this.newUncommittedAccount.getDataManager().isSessionFiring()) {
									return;
								}

								propertyControl.save();
							}
						});

				// Add to our list of controls.
				propertyControlList.add(propertyControl);
			}
			
			// Set the values from the account object into the control fields.
			for (IPropertyControl propertyControl: propertyControlList) {
				propertyControl.load(newUncommittedAccount);
			}
			
			setPageComplete(false);
			accountNameTextbox.addModifyListener(new ModifyListener(){
				public void modifyText(ModifyEvent e) {
					setPageComplete(accountNameTextbox.getText().length() != 0);
				}
			});
			
			accountNameTextbox.setFocus();
			
			setControl(container);			
		}
		
		@Override
		public boolean canFlipToNextPage() {
			/*
			 * This method controls whether the 'Next' button is enabled.
			 */
			return accountNameTextbox.getText().length() != 0;
		}
		
		@Override
		public void performHelp() {
			PlatformUI.getWorkbench().getHelpSystem().displayHelp("com.toutvirtual.help.serverConnectionDialogId");
		}
	}
	
	
	class SummaryPage extends WizardPage {
		
		SummaryPage(String pageName) {
			super(pageName);
			setTitle("Summary");
			setMessage("");
		}
		
		public void createControl(Composite parent) {
			Composite container = new Composite(parent, SWT.NONE);
			
			GridLayout layout = new GridLayout();
			layout.marginWidth = 10;
			layout.marginHeight =10;
			container.setLayout(layout);
			
			GridData gd1 = new GridData();
			gd1.grabExcessHorizontalSpace = true;
			gd1.horizontalAlignment = SWT.FILL;
			gd1.widthHint = 300;
			
			Label introText = new Label(container, SWT.WRAP);
			introText.setText("The account has been setup.  To view the account, double click on the account in the navigation view.");
			introText.setLayoutData(gd1);
			
			setControl(container);			
		}
		
		@Override
		public void performHelp() {
			MessageBox messageBox = new MessageBox(getShell(), SWT.ICON_WARNING | SWT.OK);
			messageBox.setMessage("No help is available for this page.");
			messageBox.open();
		}
	}

	public Account getNewAccount() {
		return newCommittedAccount;
	}
}

