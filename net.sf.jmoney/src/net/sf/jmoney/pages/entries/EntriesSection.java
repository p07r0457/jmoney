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

import java.util.Iterator;
import java.util.Vector;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.VerySimpleDateFormat;
import net.sf.jmoney.fields.AccountInfo;
import net.sf.jmoney.fields.CommodityInfo;
import net.sf.jmoney.fields.EntryInfo;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.PropertyAccessor;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.SessionChangeAdapter;
import net.sf.jmoney.model2.Transaction;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
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
public class EntriesSection extends SectionPart {

    protected VerySimpleDateFormat fDateFormat = new VerySimpleDateFormat(JMoneyPlugin.getDefault().getDateFormat());
    protected EntriesPage fPage;
    protected IEntriesControl fEntriesControl;
    
    Composite containerOfEntriesControl;
    FormToolkit toolkit;
    
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
        layout.numColumns = 1;
        containerOfEntriesControl.setLayout(layout2);
        
        GridData gridData = new GridData(GridData.FILL_BOTH);
        gridData.heightHint = 200;
        layout2.marginHeight = 0;
        layout2.marginWidth = 0;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
        containerOfEntriesControl.setLayoutData(gridData);

        // Initially set to the table control.
        // TODO: save this information in the memento so
        // the user always gets the last selected view.
        fEntriesControl = new EntriesTable(containerOfEntriesControl, fPage); 

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
        
        fPage.getAccount().getSession().addSessionChangeListener(new SessionChangeAdapter() {
			public void entryAdded(Entry newEntry) {
				// if the entry is in this account, tell the entries list control
				// that the transaction for this entry should be added to the list.
				if (fPage.getAccount().equals(newEntry.getAccount())) {
					fEntriesControl.addEntryInAccount(newEntry);
				}
				
				// Even if this entry is not in this account, if one of
				// the other entries in the transaction is in this account
				// then the table view will need updating because the split
				// entry rows will need updating.
				for (Iterator iter = newEntry.getTransaction().getEntryCollection().iterator(); iter.hasNext(); ) {
					Entry entry = (Entry)iter.next();
					if (!entry.equals(newEntry) 
							&& fPage.getAccount().equals(entry.getAccount())) {
						fEntriesControl.addEntry(entry, newEntry);
					}
				}
			}

			public void entryDeleted(Entry oldEntry) {
				// if the entry was in this account, refresh the table.
				if (fPage.getAccount().equals(oldEntry.getAccount())) {
					fEntriesControl.removeEntryInAccount(oldEntry);
				}
				
				// Even if this entry is not in this account, if one of
				// the other entries in the transaction is in this account
				// then the table view will need updating because the split
				// entry rows will need updating.
				for (Iterator iter = oldEntry.getTransaction().getEntryCollection().iterator(); iter.hasNext(); ) {
					Entry entry = (Entry)iter.next();
					if (!entry.equals(oldEntry) 
							&& fPage.getAccount().equals(entry.getAccount())) {
						fEntriesControl.removeEntry(entry, oldEntry);
					}
				}
			}

			public void transactionDeleted(Transaction oldTransaction, Vector entriesInTransaction) {
				for (Iterator iter = entriesInTransaction.iterator(); iter.hasNext(); ) {
					Entry entry = (Entry)iter.next();
					if (fPage.getAccount().equals(entry.getAccount())) {
						fEntriesControl.removeEntryInAccount(entry);
					}
				}
			}
			
			public void objectAdded(ExtendableObject extendableObject) {
			}

			public void objectDeleted(ExtendableObject extendableObject) {
				// Nothing to do here.  When a transaction is deleted, an event is
				// fired for each entry in the transaction and we process those so there
				// is no additional processing for the case where the transaction is deleted.
			}

			public void objectChanged(ExtendableObject extendableObject, PropertyAccessor propertyAccessor, Object oldValue, Object newValue) {
				if (extendableObject instanceof Entry) {
					Entry entry = (Entry)extendableObject;
					
					if (propertyAccessor == EntryInfo.getAccountAccessor()) {
						if (fPage.getAccount().equals(oldValue)) {
							fEntriesControl.removeEntryInAccount(entry);
						} else if (fPage.getAccount().equals(newValue)) {
							fEntriesControl.addEntryInAccount(entry);
						}
					}
					
					Transaction transaction = entry.getTransaction();
					for (Iterator iter = transaction.getEntryCollection().iterator(); iter.hasNext(); ) {
						Entry entry2 = (Entry)iter.next();
						if (fPage.getAccount().equals(entry2.getAccount())) {
					    	fEntriesControl.updateEntry(entry2, entry, propertyAccessor, oldValue, newValue);
						}
					}
				}
				
				// When a transaction property changes, we notify the entries list
				// control once for each entry in the transaction where the account
				// of the entry is the account for the entries list.
				if (extendableObject instanceof Transaction) {
					Transaction transaction = (Transaction)extendableObject;
					for (Iterator iter = transaction.getEntryCollection().iterator(); iter.hasNext(); ) {
						Entry entry = (Entry)iter.next();
						if (fPage.getAccount().equals(entry.getAccount())) {
					    	fEntriesControl.updateTransaction(entry);
						}
					}
				}
				
				// Account names and currency names affect the data displayed in the
				// entries list.  These changes are both infrequent, may involve the
				// change to a lot of entries, and would involve finding all transactions
				// that contain both an entry with the changed account or currency
				// and an entry with in the account for this page.  It is therefore
				// better just to refresh the entire entries list.
				if (propertyAccessor == AccountInfo.getNameAccessor()
						|| propertyAccessor == CommodityInfo.getNameAccessor()) {
					fEntriesControl.refresh();
				}
			}
        }, container);
        
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
    	fEntriesControl = new EntriesTable(containerOfEntriesControl, fPage);
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
        containerOfEntriesControl.layout(false);
    }
}
