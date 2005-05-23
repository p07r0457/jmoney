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
import net.sf.jmoney.model2.Transaction;
import net.sf.jmoney.pages.entries.EntriesTree;
import net.sf.jmoney.pages.entries.IDisplayableItem;
import net.sf.jmoney.pages.entries.IEntriesContent;
import net.sf.jmoney.pages.entries.IEntriesTableProperty;
import net.sf.jmoney.reconciliation.BankStatement;
import net.sf.jmoney.reconciliation.ReconciliationEntryInfo;

import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
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

    private EntriesTree fUnreconciledEntriesControl;
    
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

			public void setNewEntryProperties(Entry newEntry) {
				newEntry.setAccount(fPage.getAccount());
			}
        };

        // Create the table control.
        fUnreconciledEntriesControl = new EntriesTree(container, toolkit, unreconciledTableContents, fPage.getAccount().getSession()); 
		fPage.getEditor().getToolkit().adapt(fUnreconciledEntriesControl.getControl(), true, false);
		
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

        getSection().setClient(container);
        toolkit.paintBordersFor(container);
        refresh();
    }
}
