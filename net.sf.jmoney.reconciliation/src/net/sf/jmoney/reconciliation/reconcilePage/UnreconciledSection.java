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

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.fields.EntryInfo;
import net.sf.jmoney.fields.TransactionInfo;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.CurrencyAccount;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.PropertyAccessor;
import net.sf.jmoney.model2.Transaction;
import net.sf.jmoney.pages.entries.EntriesTree;
import net.sf.jmoney.pages.entries.EntryRowSelectionAdapter;
import net.sf.jmoney.pages.entries.EntryRowSelectionListener;
import net.sf.jmoney.pages.entries.IDisplayableItem;
import net.sf.jmoney.pages.entries.IEntriesContent;
import net.sf.jmoney.pages.entries.IEntriesTableProperty;
import net.sf.jmoney.pages.entries.EntriesTree.DisplayableEntry;
import net.sf.jmoney.pages.entries.EntriesTree.DisplayableTransaction;
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
    
    private EntryRowSelectionListener tableSelectionListener = null;
    
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
				// TODO: figure out how we keep this up to date.
				// The EntriesTree class has no mechanism for refreshing
				// the opening balance.  It should have.
				return 0;
			}

			public void setNewEntryProperties(Entry newEntry) {
				newEntry.setAccount(fPage.getAccount());
			}
        };

        // Create the table control.
        fUnreconciledEntriesControl = new EntriesTree(container, toolkit, fPage.transactionManager, unreconciledTableContents, fPage.getAccount().getSession()); 
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
        
        tableSelectionListener = new EntryRowSelectionAdapter() {
			public void widgetSelected(IDisplayableItem selectedObject) {
			    JMoneyPlugin.myAssert(selectedObject != null);
    			
    			// We should never get here with the item data set to the
    			// DisplayableNewEmptyEntry object as a result of the user
    			// selecting the row.  The reason being that the EntryTree
    			// object intercepts mouse down events first and replaces the
    			// data with a new entry.  However, SWT seems to set the selection
    			// to the last row in certain circumstances such as when
    			// applying a filter.  In such a situation, both the top-level
    			// entry and the selected entry will be given as null.
    			// Two null values passed to the entry section will cause
    			// the section to be blanked.
    			
    			    IDisplayableItem data = (IDisplayableItem)selectedObject;
    			
                if (selectedObject instanceof DisplayableTransaction) {
                    DisplayableTransaction transData = (DisplayableTransaction) selectedObject;
                    if (transData.isSimpleEntry()) {
                        fPage.fEntrySection.update(data.getEntryInAccount(), data.getEntryForOtherFields(), true);
                    } else {
                        fPage.fEntrySection.update(data.getEntryInAccount(), null, true);
                    }
                } else if (selectedObject instanceof DisplayableEntry) {
                    fPage.fEntrySection.update(data.getEntryForThisRow(), null, false);
                } else {
                    // We were not on a transaction (we were probably on the
                    // blank 'new transaction' line.
                    fPage.fEntrySection.update(null, null, false);
                }
            }

			// Default selection sets the statement, causing the entry to move from
			// the unreconciled table to the statement table.
			public void widgetDefaultSelected(IDisplayableItem data) {
				if (fPage.getStatement() != null) {
					Entry entry = data.getEntryInAccount();
					
					// If the user double clicked on the blank new entry row, then
					// entry will be null.  We must guard against that.
					
					// The EntriesTree control will always validate and commit
					// any outstanding changes before firing a default selection
					// event.  We set the property to put the entry into the
					// statement and immediately commit the change.
					if (entry != null) {
						entry.setPropertyValue(ReconciliationEntryInfo.getStatementAccessor(), fPage.getStatement());
						fPage.transactionManager.commit();
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
