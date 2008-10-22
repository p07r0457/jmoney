package net.sf.jmoney.transactionDialog;

/*
*
*  JMoney - A Personal Finance Manager
*  Copyright (c) 2006, 2008 Nigel Westbury <westbury@users.sourceforge.net>
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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.jmoney.entrytable.BaseEntryRowControl;
import net.sf.jmoney.entrytable.Block;
import net.sf.jmoney.entrytable.CellBlock;
import net.sf.jmoney.entrytable.FocusCellTracker;
import net.sf.jmoney.entrytable.Header;
import net.sf.jmoney.entrytable.HorizontalBlock;
import net.sf.jmoney.entrytable.InvalidUserEntryException;
import net.sf.jmoney.entrytable.RowSelectionTracker;
import net.sf.jmoney.entrytable.SingleOtherEntryPropertyBlock;
import net.sf.jmoney.entrytable.SplitEntryRowControl;
import net.sf.jmoney.isolation.TransactionManager;
import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.EntryInfo;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.model2.SessionChangeAdapter;
import net.sf.jmoney.model2.Transaction;
import net.sf.jmoney.model2.TransactionInfo;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.DialogMessageArea;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * This class manages the transaction dialog that appears when
 * the 'details' button is pressed in the account entries list page.
 * <P>
 * The transaction dialog displays a single transaction.  This is a lower-level
 * view of the transaction, showing all the applicable underlying properties
 * of each entry.
 * 
 * @author Nigel Westbury
 */
public class TransactionDialog extends Dialog {
	
	private static final int NEW_SPLIT_ID     = IDialogConstants.CLIENT_ID + 0;
	private static final int DELETE_SPLIT_ID  = IDialogConstants.CLIENT_ID + 1;
	private static final int ADJUST_AMOUNT_ID = IDialogConstants.CLIENT_ID + 2;

	private TransactionManager transactionManager;
	
	private Entry topEntry;
	
	private DialogMessageArea messageArea;

	private Composite tableComposite;

	private RowSelectionTracker<SplitEntryRowControl> rowTracker;

	private FocusCellTracker cellTracker;

	private Block<Entry, SplitEntryRowControl> rootBlock;

	private Transaction transaction;

	/**
	 * Note that an Entry object is passed, not a Transaction object as one might
	 * expect.  This allows this dialog to put the 'main' entry first with the
	 * 'split' entries below.  Of course, which entry is the 'main' entry depends
	 * on the context from which this dialog was opened.
	 */
	public TransactionDialog(Shell parentShell, Entry originalEntry) {
		super(parentShell);

		/*
		 * All changes within this dialog are made within a transaction, so canceling
		 * is trivial (the transaction is simply not committed).
		 */
		transactionManager = new TransactionManager(originalEntry.getDataManager());
    	
    	topEntry = transactionManager.getCopyInTransaction(originalEntry);
    	
   		transaction = topEntry.getTransaction();
	}

	@Override
	protected void buttonPressed(int buttonId) {
		switch (buttonId) {
		case IDialogConstants.OK_ID:
			// All edits are transferred to the model as they are made,
			// so we just need to commit them.
    		transactionManager.commit("Edit Transaction");
			break;
		case NEW_SPLIT_ID:
			newSplit();
			break;
		case DELETE_SPLIT_ID:
			deleteSplit();
			break;
		case ADJUST_AMOUNT_ID:
			adjustAmount();
			break;
		} 
		super.buttonPressed(buttonId);
	}

	private void newSplit() {
		Entry newEntry = topEntry.getTransaction().createEntry();

		// If all entries so far are in the same currency then set the
		// amount of the new entry to be the amount that takes the balance
		// to zero.  If we cannot determine the currency because the user
		// has not yet entered the necessary data, assume that the currencies
		// are all the same.
		Commodity commodity = null;
   		boolean mismatchedCommodities = false;
   		long totalAmount = 0;
        for (Entry entry: topEntry.getTransaction().getEntryCollection()) {
        	if (commodity == null) {
        		// No commodity yet determined, so set to the commodity for
        		// this entry, if any.
        		commodity = entry.getCommodity(); 
        	} else {
        		if (!commodity.equals(entry.getCommodity())) {
        			mismatchedCommodities = true;
        			break;
        		}
        	}
        	totalAmount += entry.getAmount();
        }
        
        if (!mismatchedCommodities) {
        	newEntry.setAmount(-totalAmount);
        }
        
		createEntryRow(newEntry);

        getShell().pack(true);
	}

	private void deleteSplit() {
		SplitEntryRowControl rowControl = rowTracker.getSelectedRow();
		rowControl.dispose();
		transaction.deleteEntry(rowControl.getInput());

		getShell().pack(true);
	}
	
	private void adjustAmount() {
		SplitEntryRowControl rowControl = rowTracker.getSelectedRow();
		// TODO: Is a row ever not selected?
		if (rowControl != null) {
			Entry entry = rowControl.getInput();
			
			long totalAmount = 0;
			for (Entry eachEntry: transaction.getEntryCollection()) {
				totalAmount += eachEntry.getAmount();
			}
			
			entry.setAmount(entry.getAmount() - totalAmount);
		}
	}
	
	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText("Transaction Details");
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, NEW_SPLIT_ID,
				"New Split", false);
		createButton(parent, DELETE_SPLIT_ID,
				"Delete Split", false);
		createButton(parent, ADJUST_AMOUNT_ID,
				"Adjust Amount", false);
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.OK_ID,
				IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		composite.setLayout(new GridLayout(1, false));

		// Message label
		messageArea = new DialogMessageArea();
		messageArea.createContents(composite);
		
		// Ensure the message area is shown and fills the space
		messageArea.setTitleLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		messageArea.setMessageLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		/*
		 * Any errors that would prevent the transaction from being committed
		 * are shown. This dialog may be opened on an uncommitted transaction
		 * (causing a third level transaction to be created). In that case the
		 * transaction may not be valid even before this dialog is opened. We
		 * must be sure that error messages are shown straight away.
		 */
		updateErrorMessage();
		
		transactionManager.addChangeListener(new SessionChangeAdapter() {
			@Override
			public void performRefresh() {
				updateErrorMessage();
			}
		}, composite);
		
		Label label = new Label(composite, SWT.WRAP);
		label.setText("Transactions are normally shown in a concise manner, showing details "
						+ "in a way appropriate for the context.  However, sometimes you just need more flexibility in editing the transaction or you need to see the details. "
						+ "This dialog shows the underlying structure and properties of the transaction and allows you to edit them. "
						+ "You need to be a little more careful when using this dialog.");

		GridData messageData = new GridData();
		Rectangle rect = getShell().getMonitor().getClientArea();
		messageData.widthHint = rect.width/2;
		label.setLayoutData(messageData);

        // Create the transaction property area
		createTransactionPropertiesArea(composite).setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		// Create the table area
		GridData tableData = new GridData(SWT.FILL, SWT.FILL, true, true);
		createEntriesTable(composite).setLayoutData(tableData);

		applyDialogFont(composite);
		return composite;
	}

	private Control createTransactionPropertiesArea(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(10, false));

        // Add properties from the transaction.
   		for (ScalarPropertyAccessor<?> propertyAccessor: TransactionInfo.getPropertySet().getScalarProperties3()) {
        	Label propertyLabel = new Label(composite, 0);
        	propertyLabel.setText(propertyAccessor.getDisplayName() + ':');
        	IPropertyControl<ExtendableObject> propertyControl = propertyAccessor.createPropertyControl(composite);
        	propertyControl.load(topEntry.getTransaction());
        }
		return composite;
	}

	private Control createEntriesTable(Composite parent) {
		
		/*
		 * Setup the layout structure of the header and rows.
		 */

		CellBlock<Entry, SplitEntryRowControl> debitColumnManager = SplitEntryDebitAndCreditColumns.createDebitColumn(transaction.getSession().getDefaultCurrency());
		CellBlock<Entry, SplitEntryRowControl> creditColumnManager = SplitEntryDebitAndCreditColumns.createCreditColumn(transaction.getSession().getDefaultCurrency());
		
		rootBlock = new HorizontalBlock<Entry, SplitEntryRowControl>(
								new SingleOtherEntryPropertyBlock(EntryInfo.getAccountAccessor()),
								new SingleOtherEntryPropertyBlock(EntryInfo.getMemoAccessor()),
								new PropertiesBlock(), 
								debitColumnManager,
								creditColumnManager
		);

		/*
		 * Ensure indexes are set.
		 */
		rootBlock.initIndexes(0);

		rowTracker = new RowSelectionTracker<SplitEntryRowControl>();
		cellTracker = new FocusCellTracker();
		tableComposite = new Composite(parent, SWT.NONE);
		tableComposite.setLayout(new GridLayout(1, false));

		new Header<Entry>(tableComposite, SWT.NONE, rootBlock).setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		
		for (Entry entry: topEntry.getTransaction().getEntryCollection()) {
			createEntryRow(entry);
//			rowControls.put(entry, row);
		}
			
	    
		return tableComposite;
	}

	private void createEntryRow(final Entry entry) {
		SplitEntryRowControl row = new SplitEntryRowControl(tableComposite, SWT.NONE, rootBlock, false, rowTracker, cellTracker);
		row.setInput(entry);
		row.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
	}

	/**
	 * Sets or clears the error message.
	 * If not <code>null</code>, the OK button is disabled.
	 * 
	 * @param errorMessage
	 *            the error message, or <code>null</code> to clear
	 */
	public void updateErrorMessage() {
		String errorMessage = null;

		try {
			BaseEntryRowControl.baseValidation(topEntry.getTransaction());
			
			// No exception was thrown, so transaction is valid.
			
			// If there are two currencies/commodities involved in
			// the transaction then the exchange rate or conversion cost
			// or net price or whatever is displayed.
			
			Map<Commodity, Long> amounts = new HashMap<Commodity, Long>();

			for (Entry entry: transaction.getEntryCollection()) {
				Commodity commodity = entry.getCommodity();
				Long previousAmount = amounts.get(commodity);
				if (previousAmount == null) {
					amounts.put(commodity, entry.getAmount());
				} else {
					amounts.put(commodity, entry.getAmount() + previousAmount);
				}
			}

			if (amounts.size() == 2) {
				List<Map.Entry<Commodity, Long>> a = new ArrayList<Map.Entry<Commodity, Long>>(amounts.entrySet());
				String message = MessageFormat.format("This transaction results in 1 {0} = {2} {1}, or 1 {2} = {3} {0}.",
						a.get(0).getKey().getName(),
						a.get(1).getKey().getName(),
						a.get(0).getValue(),
						a.get(1).getValue()
				);
				messageArea.updateText(message, IMessageProvider.INFORMATION);
			} else {
//				messageArea.clearErrorMessage();    ?????
				messageArea.restoreTitle();
			}
		} catch (InvalidUserEntryException e) {
			errorMessage = e.getLocalizedMessage();
			messageArea.updateText(errorMessage, IMessageProvider.ERROR);
		}
		
		// If called during createDialogArea, the okButton
		// will not have been created yet.
		Button okButton = this.getButton(IDialogConstants.OK_ID);
		if (okButton != null) {
			okButton.setEnabled(errorMessage == null);
		}
	}

}
