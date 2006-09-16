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

package net.sf.jmoney.reconciliation.reconcilePage;

import net.sf.jmoney.fields.AccountControl;
import net.sf.jmoney.isolation.TransactionManager;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IncomeExpenseAccount;
import net.sf.jmoney.reconciliation.ReconciliationAccount;
import net.sf.jmoney.reconciliation.ReconciliationAccountInfo;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * An input dialog that allows the user to configure the methods for importing statement data
 * for a particular account.
 * 
 * @author Nigel Westbury
 */
class ImportOptionsDialog extends Dialog {
	/**
	 * The account for which we are configuring.
	 */
	private ReconciliationAccount account;

	private Button reconcilableButton;
	
	private AccountControl<IncomeExpenseAccount> defaultAccountControl;

	/**
	 * Ok button widget.
	 */
	private Button okButton;
	
	/**
	 * Error message label widget.
	 */
	private Text errorMessageText;
	
	/**
	 * Creates an input dialog with OK and Cancel buttons. Note that the dialog
	 * will have no visual representation (no widgets) until it is told to open.
	 * <p>
	 * Note that the <code>open</code> method blocks for input dialogs.
	 * </p>
	 * 
	 * @param parentShell
	 *            the parent shell
	 */
	public ImportOptionsDialog(Shell parentShell, ReconciliationAccount account) {
		super(parentShell);
		this.account = account;
	}

	protected void buttonPressed(int buttonId) {
		if (buttonId == IDialogConstants.OK_ID) {
			/*
			 * These changes must be done within a transaction.
			 */
	    	TransactionManager transactionManager = new TransactionManager(account.getObjectKey().getSessionManager());
	    	ExtendableObject ourAccount1 = transactionManager.getCopyInTransaction(account.getBaseObject()); 
	    	ReconciliationAccount ourAccount2 = (ReconciliationAccount) ourAccount1.getExtension(ReconciliationAccountInfo.getPropertySet(), true);
	    	
			boolean isReconcilable = reconcilableButton.getSelection();
			
			ourAccount2.setReconcilable(isReconcilable);
			
			if (isReconcilable) {
				// TODO: implement decorators etc. and stop OK being pressed if
				// no account is selected.
				IncomeExpenseAccount defaultCategory = defaultAccountControl.getAccount();
				
				if (defaultCategory == null) {
					// TODO: Set the error message.
					return;
				}
				
				IncomeExpenseAccount defaultCategoryInTransaction = transactionManager.getCopyInTransaction(defaultCategory);
				ourAccount2.setDefaultCategory(defaultCategoryInTransaction);
			} else {
				ourAccount2.setDefaultCategory(null);
			}

			transactionManager.commit();
		}
		super.buttonPressed(buttonId);
	}

	protected void configureShell(Shell shell) {
		super.configureShell(shell);
			shell.setText("Import Options for " + account.getName());
	}

	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		okButton = createButton(parent, IDialogConstants.OK_ID,
				IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
	}

	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);

		composite.setLayout(new GridLayout(1, false));
		
		Label label = new Label(composite, SWT.WRAP);
		label.setText("JMoney allows you to import bank account statements from the bank's servers. " +
				"Before these records can be imported into JMoney, categories must be assigned to each entry " +
				"because a requirement of JMoney is that all entries have an account or category assigned. " +
				"Select here the category that is to be initially assigned to each imported entry.");
		
		GridData messageData = new GridData();
		Rectangle rect = getShell().getMonitor().getClientArea();
		messageData.widthHint = rect.width/2;
		label.setLayoutData(messageData);
		
		reconcilableButton = new Button(composite, SWT.CHECK);
		reconcilableButton.setText("Statements can be imported?");
		
		final Composite stackContainer = new Composite(composite, 0);
		
		final StackLayout stackLayout = new StackLayout();
		stackContainer.setLayout(stackLayout);

		// Create the control containing the controls to be shown when 'is reconcilable'
		final Control whenIsReconcilableControl = createCategoryControls(stackContainer);

		reconcilableButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (reconcilableButton.getSelection()) {
					stackLayout.topControl = whenIsReconcilableControl;
				} else {
					stackLayout.topControl = null;
				}
				stackContainer.layout(false);
			}
		});

		reconcilableButton.setSelection(account.isReconcilable());
		if (account.isReconcilable()) {
			stackLayout.topControl = whenIsReconcilableControl;
			defaultAccountControl.setAccount(account.getDefaultCategory());
		}
		
		applyDialogFont(composite);
		return composite;
	}

	private Control createCategoryControls(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		
		new Label(composite, SWT.NONE).setText("Default category:");
		defaultAccountControl = new AccountControl<IncomeExpenseAccount>(composite, account.getSession(), IncomeExpenseAccount.class);
		GridData accountData = new GridData();
		accountData.widthHint = 200;
		defaultAccountControl.setLayoutData(accountData);
		
		return composite;
	}

	/**
	 * Sets or clears the error message.
	 * If not <code>null</code>, the OK button is disabled.
	 * 
	 * @param errorMessage
	 *            the error message, or <code>null</code> to clear
	 */
	public void setErrorMessage(String errorMessage) {
		errorMessageText.setText(errorMessage == null ? "" : errorMessage); //$NON-NLS-1$
		
		// If called during createDialogArea, the okButton
		// will not have been created yet.
		if (okButton != null) {
			okButton.setEnabled(errorMessage == null);
		}
		errorMessageText.getParent().update();
	}
}
