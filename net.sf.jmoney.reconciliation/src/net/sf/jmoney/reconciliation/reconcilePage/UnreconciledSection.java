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

package net.sf.jmoney.reconciliation.reconcilePage;

import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;

import net.sf.jmoney.fields.EntryInfo;
import net.sf.jmoney.fields.TransactionInfo;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.CurrencyAccount;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.PropertyAccessor;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.Transaction;
import net.sf.jmoney.pages.entries.EntriesTable;
import net.sf.jmoney.pages.entries.EntriesTree;
import net.sf.jmoney.pages.entries.IDisplayableItem;
import net.sf.jmoney.pages.entries.IEntriesContent;
import net.sf.jmoney.pages.entries.IEntriesControl;
import net.sf.jmoney.pages.entries.IEntriesTableProperty;
import net.sf.jmoney.pages.entries.TransactionDialog;
import net.sf.jmoney.reconciliation.BankStatement;
import net.sf.jmoney.reconciliation.ReconciliationEntryInfo;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
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
 * Class implementing the section containing the unreconciled
 * entries on the account reconciliation page.
 * 
 * @author Nigel Westbury
 */
public class UnreconciledSection extends SectionPart {

    private ReconcilePage fPage;

    private IEntriesControl fUnreconciledEntriesControl;
    
    private Composite containerOfEntriesControl2;
    
    private FormToolkit toolkit;
    
    private SelectionListener tableSelectionListener = null;
    
    private IEntriesContent unreconciledTableContents = null;
    
    public UnreconciledSection(ReconcilePage page, Composite parent) {
        super(parent, page.getManagedForm().getToolkit(), Section.TITLE_BAR);
        getSection().setText("Unreconciled Entries");
        fPage = page;
    	this.toolkit = page.getManagedForm().getToolkit();
    	
        final Composite container = toolkit.createComposite(getSection());

        GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        container.setLayout(layout);

        unreconciledTableContents = new IEntriesContent() {

			public Vector getAllEntryDataObjects() {
				return fPage.allEntryDataObjects;
			}

			public IEntriesTableProperty getDebitColumnManager() {
				return fPage.debitColumnManager;
			}

			public IEntriesTableProperty getCreditColumnManager() {
				return fPage.creditColumnManager;
			}

			public IEntriesTableProperty getBalanceColumnManager() {
				return fPage.balanceColumnManager;
			}

			public Collection getEntries() {
		        CurrencyAccount account = fPage.getAccount();
		        Collection accountEntries = 
		        	account
						.getSortedEntries(TransactionInfo.getDateAccessor(), false);
		        
		        Vector requiredEntries = new Vector();
		        for (Iterator iter = accountEntries.iterator(); iter.hasNext(); ) {
		        	Entry entry = (Entry)iter.next();
		        	if (entry.getPropertyValue(ReconciliationEntryInfo.getStatementAccessor()) == null) {
		        		requiredEntries.add(entry);
		        	}
		        }
		        
		        return requiredEntries;
			}

			public boolean isEntryInTable(Entry entry) {
				// This entry is to be shown if the account
				// matches and no statement is set.
	        	BankStatement statement = (BankStatement)entry.getPropertyValue(ReconciliationEntryInfo.getStatementAccessor());
				return fPage.getAccount().equals(entry.getAccount())
	        	 && statement == null;
			}

			public boolean isEntryInTable(Entry entry, PropertyAccessor propertyAccessor, Object value) {
				Account account;
				if (propertyAccessor == EntryInfo.getAccountAccessor()) {
					account = (Account)value;
				} else {
					account = entry.getAccount();
				}

				BankStatement statement;
				if (propertyAccessor == ReconciliationEntryInfo.getStatementAccessor()) {
					statement = (BankStatement)value;
				} else {
					statement = (BankStatement)entry.getPropertyValue(ReconciliationEntryInfo.getStatementAccessor());
				}
				
				return fPage.getAccount().equals(account)
	        	 && statement == null;
			}

			public boolean filterEntry(IDisplayableItem data) {
				// No filter here, so entries always match
				return true;
			}

			public long getStartBalance() {
				return fPage.getAccount().getStartBalance();
			}
        	
        };

        
        // Create the control that contains the Table or TableTree control.
        // Although this control only contains a single child control,
        // we need to create it so we can destroy and re-create the
        // child control without altering the sequence of controls
        // in the grid container.
        
        containerOfEntriesControl2 = toolkit.createComposite(container);
        GridLayout layout22 = new GridLayout();
        layout22.marginHeight = 0;
        layout22.marginWidth = 0;
        containerOfEntriesControl2.setLayout(layout22);

        GridData gridData11 = new GridData(GridData.FILL_BOTH);
        gridData11.heightHint = 50;
        gridData11.widthHint = 200;
		gridData11.grabExcessHorizontalSpace = true;
		gridData11.grabExcessVerticalSpace = true;
		containerOfEntriesControl2.setLayoutData(gridData11);
        
        
        // Initially set to the table control.
        // TODO: save this information in the memento so
        // the user always gets the last selected view.
        fUnreconciledEntriesControl = new EntriesTable(containerOfEntriesControl2, unreconciledTableContents, fPage.getAccount().getSession()); 
		fPage.getEditor().getToolkit().adapt(fUnreconciledEntriesControl.getControl(), true, false);
		
        GridData gridData1 = new GridData(GridData.FILL_BOTH);
        gridData1.heightHint = 50;
        gridData1.widthHint = 200;
		gridData1.grabExcessHorizontalSpace = true;
		gridData1.grabExcessVerticalSpace = true;
		fUnreconciledEntriesControl.getControl().setLayoutData(gridData1);
/*
        GridData gridData2 = new GridData(GridData.FILL_BOTH);
        gridData2.heightHint = 200;
        //layout2.marginHeight = 0;
        //layout2.marginWidth = 0;
		gridData2.grabExcessHorizontalSpace = true;
		gridData2.grabExcessVerticalSpace = true;
        containerOfEntriesControl2.setLayoutData(gridData2);
*/
        
        // Allow entries in the account to be moved from the unreconciled list
        final DragSource dragSource = new DragSource(fUnreconciledEntriesControl.getControl(), DND.DROP_MOVE);
 	         
//      Provide data in Text format
        Transfer[] types = new Transfer[] {TextTransfer.getInstance()};
        dragSource.setTransfer(types);
         	 
        dragSource.addDragListener(new DragSourceListener() {
        	public void dragStart(DragSourceEvent event) {
        		// TODO: Is it correct to use the current selection?
        		// See if a drag also sets the selection.

        		Entry entry = fUnreconciledEntriesControl.getSelectedEntry();
        		
        		// Do not start the drag if the empty 'new entry' row is being dragged.
        		if (entry == null) {
        			event.doit = false;
        		}
        	}
        	public void dragSetData(DragSourceEvent event) {
        		// Provide the data of the requested type.
        		if (TextTransfer.getInstance().isSupportedType(event.dataType)) {
            		Entry entry = fUnreconciledEntriesControl.getSelectedEntry();
        			//fPage.entryBeingDragged = entry;
        			//event.data = "get entry from fPage";
            		event.data = entry;
        		}
        	}
        	public void dragFinished(DragSourceEvent event) {
        		// If a move operation has been performed, remove the data
        		// from the source.  In a drag and drop, the transation entries
        		// and properties are merged with the target transaction, so we
        		// should now delete this transaction.
        		// TODO: might it be better to merge the target into the source????
        		// This would reduce the database updates.
        		if (event.detail == DND.DROP_MOVE) {
        			Entry entry = (Entry)event.data;
        			Transaction transaction = entry.getTransaction();
        			transaction.getSession().deleteTransaction(transaction);
        		}
        	}
        });
        
        fUnreconciledEntriesControl.getControl().addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				dragSource.dispose();
			}
        });
        
        tableSelectionListener = new SelectionListener() {

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

			// Default selection sets the statement, causing the entry to move from
			// the unreconciled table to the statement table.
			public void widgetDefaultSelected(SelectionEvent e) {
				if (fPage.getStatement() != null) {
					Object selectedObject = e.item.getData();
					// The selected object might be null.  This occurs when the table is refreshed.
					// I don't understand this so I am simply bypassing the update
					// in this case.  Nigel
					if (selectedObject != null) {
						IDisplayableItem data = (IDisplayableItem)selectedObject;
						Entry entry = data.getEntryInAccount();
						entry.setPropertyValue(ReconciliationEntryInfo.getStatementAccessor(), fPage.getStatement());
					}
				}
			}
        };

		fUnreconciledEntriesControl.addSelectionListener(tableSelectionListener);

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
           		
           		// Select entry1 in the lower entries list.
                fUnreconciledEntriesControl.setSelection(entry1, entry1);
           }
        });

        // Create the 'delete transaction' button.
        Button deleteButton = toolkit.createButton(buttonArea, "Delete Transaction", SWT.PUSH);
        deleteButton.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent event) {
        		// TODO: Should we allow deletion from top table?
        		// TODO: Should we allow a selection in one table only?
        		Entry selectedEntry = fUnreconciledEntriesControl.getSelectedEntry();
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

           		// TODO: We must allow the user to add a split in the
           		// top table too.
        		Entry selectedEntry = fUnreconciledEntriesControl.getSelectedEntryInAccount();
        		if (selectedEntry != null) {
        			Transaction transaction = selectedEntry.getTransaction();
        			Entry newEntry = transaction.createEntry();
        			transaction.getSession().registerUndoableChange("New Split");
        			
               		// Select the new entry in the entries list.
                    fUnreconciledEntriesControl.setSelection(selectedEntry, newEntry);
        		}
           }
        });

        // Create the 'delete split' button.
        Button deleteSplitButton = toolkit.createButton(buttonArea, "Delete Split", SWT.PUSH);
        deleteSplitButton.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent event) {
           		// TODO: We must allow the user to add a split in the
           		// top table too.
        		Entry selectedEntryInAccount = fUnreconciledEntriesControl.getSelectedEntryInAccount();
        		Entry selectedEntry = fUnreconciledEntriesControl.getSelectedEntry();
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
        		Entry selectedEntryInAccount = fUnreconciledEntriesControl.getSelectedEntryInAccount();
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
	 * Set the entries list to be a flat table.  If any other
	 * entries control is set, destroy it first.
	 */
	public void setTableView() {
    	if (fUnreconciledEntriesControl instanceof EntriesTable) {
    		// Already set to table view, so nothing to do.
    		return;
    	}
    	
    	fUnreconciledEntriesControl.dispose();
    	fUnreconciledEntriesControl = new EntriesTable(containerOfEntriesControl2, unreconciledTableContents, fPage.getAccount().getSession());
		fPage.getEditor().getToolkit().adapt(fUnreconciledEntriesControl.getControl(), true, false);
		fUnreconciledEntriesControl.addSelectionListener(tableSelectionListener);
        containerOfEntriesControl2.layout(false);
	}

	/**
	 * Set the entries list to be a table tree.  If any other
	 * entries control is set, destroy it first.
	 */
    public void setTreeView() {
    	if (fUnreconciledEntriesControl instanceof EntriesTree) {
    		// Already set to tree view, so nothing to do.
    		return;
    	}
    	
    	fUnreconciledEntriesControl.dispose();
//TODO:    	fUnreconciledEntriesControl = new EntriesTree(containerOfEntriesControl2, unreconciledTableContents);
		fPage.getEditor().getToolkit().adapt(fUnreconciledEntriesControl.getControl(), true, false);
		fUnreconciledEntriesControl.addSelectionListener(tableSelectionListener);
        containerOfEntriesControl2.layout(false);
    }
}
