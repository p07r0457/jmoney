/*
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2004 Johann Gyger <jgyger@users.sf.net>
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
 */
package net.sf.jmoney.pages.entries;

import java.util.Collection;
import java.util.Vector;

import net.sf.jmoney.fields.EntryInfo;
import net.sf.jmoney.fields.TransactionInfo;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.CurrencyAccount;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.PropertyAccessor;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.Transaction;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * TODO
 * 
 * @author Johann Gyger
 */
public class EntriesSection extends SectionPart implements IEntriesContent {

    private EntriesPage fPage;
    private IEntriesControl fEntriesControl;
    
    private Composite containerOfEntriesControl;
    private FormToolkit toolkit;
    
    private SelectionListener tableSelectionListener = null;
    
    public EntriesSection(EntriesPage page, Composite parent) {
        super(parent, page.getManagedForm().getToolkit(), Section.TITLE_BAR);
        getSection().setText("All Entries");
        fPage = page;
        createClient(page.getManagedForm().getToolkit());
    }

    public void refreshEntryList() {
    	fEntriesControl.refresh();
//        super.refresh();
    }

    protected void createClient(FormToolkit toolkit) {
    	this.toolkit = toolkit;
    	
        final Composite container = toolkit.createComposite(getSection());

        GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        container.setLayout(layout);

        // Create the control that contains the Table or TableTree control.
        // Although this control only contains a single child control,
        // we need to create it so we can destroy and re-create the
        // child control without altering the sequence of controls
        // in the grid container.
        containerOfEntriesControl = toolkit.createComposite(container);
        GridLayout layout2 = new GridLayout();
        containerOfEntriesControl.setLayout(layout2);
        
        GridData gridData = new GridData(GridData.FILL_BOTH);
        gridData.heightHint = 200;
        //layout2.marginHeight = 0;
        //layout2.marginWidth = 0;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
        containerOfEntriesControl.setLayoutData(gridData);

        tableSelectionListener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
                Object selectedObject = e.item.getData();

                // TODO: This code is duplicated below.
                // The selected object might be null.  This occurs when the table is refreshed.
                // I don't understand this so I am simply bypassing the update
                // in this case.  Nigel
                if (selectedObject != null) {
                	IDisplayableItem data = (IDisplayableItem)selectedObject;

                	Entry entryInAccount = data.getEntryInAccount();
                	Entry selectedEntry = data.getSelectedEntry();
                	
                	if (fPage.currentTransaction != null
                			&& !fPage.currentTransaction.equals(entryInAccount.getTransaction())) {
                		fPage.commitTransaction();
                	}
                	// TODO: Support the blank transaction.
                	// The following fails on the blank transaction. 
            		fPage.currentTransaction = entryInAccount.getTransaction();
            		
                	if (selectedEntry != null) {
                		fPage.fEntrySection.update(entryInAccount, selectedEntry);
                	}
                }
			}
        };

        // Initially set to the table control.
        // TODO: save this information in the memento so
        // the user always gets the last selected view.
        fEntriesControl = new EntriesTable(containerOfEntriesControl, this, fPage.getAccount().getSession()); 
		fPage.getEditor().getToolkit().adapt(fEntriesControl.getControl(), true, false);
		fEntriesControl.addSelectionListener(tableSelectionListener);

        // Create the button area
		Composite buttonArea = toolkit.createComposite(container);
		
		RowLayout layoutOfButtons = new RowLayout();
		layoutOfButtons.fill = false;
		layoutOfButtons.justify = true;
		buttonArea.setLayout(layoutOfButtons);
		
        // Create the 'add transaction' button.
        Button addButton = toolkit.createButton(buttonArea, "New Transaction", SWT.PUSH);
        addButton.addSelectionListener(new SelectionAdapter() {
           public void widgetSelected(SelectionEvent event) {
           		Session session = fPage.getAccount().getSession();
           		
           		// Commit any previous transaction
           		fPage.transactionManager.commit();
           		
           		Transaction transaction = session.createTransaction();
           		Entry entry1 = transaction.createEntry();
           		Entry entry2 = transaction.createEntry();
           		entry1.setAccount(fPage.getAccount());
           		
           		// Select entry1 in the entries list.
                fEntriesControl.setSelection(entry1, entry1);
           }
        });

        // Create the 'delete transaction' button.
        Button deleteButton = toolkit.createButton(buttonArea, "Delete Transaction", SWT.PUSH);
        deleteButton.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent event) {
        		Entry selectedEntry = fEntriesControl.getSelectedEntry();
        		if (selectedEntry != null) {
        			Transaction transaction = selectedEntry.getTransaction();
        			transaction.getSession().deleteTransaction(transaction);
        			transaction.getSession().registerUndoableChange("Delete Transaction");
        		}
        	}
        });
        
        // Create the 'add split' button.
        Button addEntryButton = toolkit.createButton(buttonArea, "New Split", SWT.PUSH);
        addEntryButton.addSelectionListener(new SelectionAdapter() {
           public void widgetSelected(SelectionEvent event) {
           		Session session = fPage.getAccount().getSession();

        		Entry selectedEntry = fEntriesControl.getSelectedEntryInAccount();
        		if (selectedEntry != null) {
        			Transaction transaction = selectedEntry.getTransaction();
        			Entry newEntry = transaction.createEntry();
        			transaction.getSession().registerUndoableChange("New Split");
        			
               		// Select the new entry in the entries list.
                    fEntriesControl.setSelection(selectedEntry, newEntry);
        		}
           }
        });

        // Create the 'delete split' button.
        Button deleteSplitButton = toolkit.createButton(buttonArea, "Delete Split", SWT.PUSH);
        deleteSplitButton.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent event) {
        		Entry selectedEntryInAccount = fEntriesControl.getSelectedEntryInAccount();
        		Entry selectedEntry = fEntriesControl.getSelectedEntry();
        		if (selectedEntry != null && selectedEntry != selectedEntryInAccount) {
        			Transaction transaction = selectedEntry.getTransaction();
        			transaction.deleteEntry(selectedEntry);
        			transaction.getSession().registerUndoableChange("Delete Split");
        		}
        	}
        });
        
        // Create the 'details' button.
        Button detailsButton = toolkit.createButton(buttonArea, "Details", SWT.PUSH);
        detailsButton.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent event) {
        		Entry selectedEntryInAccount = fEntriesControl.getSelectedEntryInAccount();
				TransactionDialog dialog = new TransactionDialog(
						container.getShell(),
						selectedEntryInAccount,
						fPage.getAccount().getSession(), 
						fPage.getAccount().getCurrency());
				dialog.open();
        	}
        });
        
        getSection().setClient(container);
        toolkit.paintBordersFor(container);
        refresh();
    }

	/**
	 * This method is called whenever the filter has changed
	 * and must be re-applied. 
	 */
	void refreshFilter() {
		// Refresh the entries table control
		fEntriesControl.refresh();
	}
	
	/**
	 * Set the entries list to be a flat table.  If any other
	 * entries control is set, destroy it first.
	 */
	public void setTableView() {
    	if (fEntriesControl instanceof EntriesTable) {
    		// Already set to table view, so nothing to do.
    		return;
    	}
    	
    	fEntriesControl.dispose();
    	fEntriesControl = new EntriesTable(containerOfEntriesControl, this, fPage.getAccount().getSession());
		fPage.getEditor().getToolkit().adapt(fEntriesControl.getControl(), true, false);
		fEntriesControl.addSelectionListener(tableSelectionListener);
        containerOfEntriesControl.layout(false);
	}

	/**
	 * Set the entries list to be a table tree.  If any other
	 * entries control is set, destroy it first.
	 */
    public void setTreeView() {
    	if (fEntriesControl instanceof EntriesTree) {
    		// Already set to tree view, so nothing to do.
    		return;
    	}
    	
    	fEntriesControl.dispose();
    	fEntriesControl = new EntriesTree(containerOfEntriesControl, fPage);
		fPage.getEditor().getToolkit().adapt(fEntriesControl.getControl(), true, false);
		fEntriesControl.addSelectionListener(tableSelectionListener);
        containerOfEntriesControl.layout(false);
    }

	/* (non-Javadoc)
	 * @see net.sf.jmoney.pages.entries.IEntriesContent#getAllEntryDataObjects()
	 */
	public Vector getAllEntryDataObjects() {
		return fPage.allEntryDataObjects;
	}

	/* (non-Javadoc)
	 * @see net.sf.jmoney.pages.entries.IEntriesContent#getDebitColumnManager()
	 */
	public IEntriesTableProperty getDebitColumnManager() {
		return fPage.debitColumnManager;
	}

	/* (non-Javadoc)
	 * @see net.sf.jmoney.pages.entries.IEntriesContent#getCreditColumnManager()
	 */
	public IEntriesTableProperty getCreditColumnManager() {
		return fPage.creditColumnManager;
	}

	/* (non-Javadoc)
	 * @see net.sf.jmoney.pages.entries.IEntriesContent#getBalanceColumnManager()
	 */
	public IEntriesTableProperty getBalanceColumnManager() {
		return fPage.balanceColumnManager;
	}

	public Collection getEntries() {
        CurrencyAccount account = fPage.getAccount();
        Collection accountEntries = 
        	account
				.getSortedEntries(TransactionInfo.getDateAccessor(), false);
        return accountEntries;
	}

	public boolean isEntryInTable(Entry entry) {
		return fPage.getAccount().equals(entry.getAccount());
	}

	public boolean isEntryInTable(Entry entry, PropertyAccessor propertyAccessor, Object value) {
		Account account;
		if (propertyAccessor == EntryInfo.getAccountAccessor()) {
			account = (Account)value;
		} else {
			account = entry.getAccount();
		}
		return fPage.getAccount().equals(account);
	}

	// TODO: Do we need this????
	public int isEntryInTable(Entry entry, PropertyAccessor propertyAccessor, Object oldValue, Object newValue) {
		if (propertyAccessor == EntryInfo.getAccountAccessor()) {
			boolean wasInTable   = fPage.getAccount().equals(oldValue); 
			boolean isNowInTable = fPage.getAccount().equals(newValue); 
			if (wasInTable && !isNowInTable) {
				return -1; 
			} else if (!wasInTable && isNowInTable) {
				return 1;
			} else {
				return 0;
			}
 		} else {
			return 0;
		}
	}
	
	/* (non-Javadoc)
	 * @see net.sf.jmoney.pages.entries.IEntriesContent#filterEntry(net.sf.jmoney.pages.entries.EntriesTable.DisplayableTransaction)
	 */
	public boolean filterEntry(IDisplayableItem data) {
		return fPage.filter.filterEntry(data);
	}

	/* (non-Javadoc)
	 * @see net.sf.jmoney.pages.entries.IEntriesContent#getStartBalance()
	 */
	public long getStartBalance() {
        return fPage.getAccount().getStartBalance();
	}
}
