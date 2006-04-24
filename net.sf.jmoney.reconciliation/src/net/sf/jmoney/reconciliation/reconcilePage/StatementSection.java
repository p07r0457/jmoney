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
import net.sf.jmoney.reconciliation.ReconciliationPlugin;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * Class implementing the section containing the reconciled
 * entries for a statement on the account reconciliation page.
 * 
 * @author Nigel Westbury
 */
public class StatementSection extends SectionPart {

    private ReconcilePage fPage;

    private EntriesTree fReconciledEntriesControl;
    
	/**
	 * Contains two controls:
	 * - noStatementMessage
	 * - containerOfTableAndButtons
	 * Layout: AlternativeLayout
	 */
	private Composite container;
    
    /**
     * Control for the text that is displayed when no session
     * is open.
     */
    private Label noStatementMessage;
    
    private FormToolkit toolkit;
    
    private EntryRowSelectionListener tableSelectionListener = null;
    
    private IEntriesContent reconciledTableContents = null;

	private long openingBalance = 0;
    
    public StatementSection(ReconcilePage page, Composite parent) {
        super(parent, page.getManagedForm().getToolkit(), Section.TITLE_BAR);
        getSection().setText("Entries Shown on Statement");
        fPage = page;
    	this.toolkit = page.getManagedForm().getToolkit();
    	
        container = toolkit.createComposite(getSection());

        tableSelectionListener = new EntryRowSelectionAdapter() {
            public void widgetSelected(IDisplayableItem selectedObject) {
                JMoneyPlugin.myAssert(selectedObject != null);

                // We should never get here with the item data set to the
                // DisplayableNewEmptyEntry object as a result of the user
                // selecting the row. The reason being that the EntryTree
                // object intercepts mouse down events first and replaces the
                // data with a new entry. However, SWT seems to set the
                // selection
                // to the last row in certain circumstances such as when
                // applying a filter. In such a situation, both the top-level
                // entry and the selected entry will be given as null.
                // Two null values passed to the entry section will cause
                // the section to be blanked.

                IDisplayableItem data = (IDisplayableItem) selectedObject;

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
        };

        reconciledTableContents = new IEntriesContent() {

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
		        Vector requiredEntries = new Vector();

		        // If no statement is set, return an empty collection.
		        // The table will not be visible in this situation, but
		        // this method will be called and so we must allow for
		        // this situation.
		        if (fPage.getStatement() == null) {
		        	return requiredEntries;
		        }
		        
		        CurrencyAccount account = fPage.getAccount();
		        Collection accountEntries = 
		        	account
						.getSortedEntries(TransactionInfo.getDateAccessor(), false);

		        for (Iterator iter = accountEntries.iterator(); iter.hasNext(); ) {
		        	Entry entry = (Entry)iter.next();
		        	BankStatement statement = (BankStatement)entry.getPropertyValue(ReconciliationEntryInfo.getStatementAccessor());
		        	if (fPage.getStatement().equals(statement)) {
		        		requiredEntries.add(entry);
		        	}
		        }
		        
		        return requiredEntries;
			}

			public boolean isEntryInTable(Entry entry) {
		        // If no statement is set, nothing is in the table.
		        // The table will not be visible in this situation, but
		        // this method will be called and so we must allow for
		        // this situation.
		        if (fPage.getStatement() == null) {
		        	return false;
		        }
		        
				// This entry is to be shown if both the account and
				// the statement match.
	        	BankStatement statement = (BankStatement)entry.getPropertyValue(ReconciliationEntryInfo.getStatementAccessor());
				return fPage.getAccount().equals(entry.getAccount())
	        	 && fPage.getStatement().equals(statement);
			}

			public boolean isEntryInTable(Entry entry, PropertyAccessor propertyAccessor, Object value) {
		        // If no statement is set, nothing is in the table.
		        // The table will not be visible in this situation, but
		        // this method will be called and so we must allow for
		        // this situation.
		        if (fPage.getStatement() == null) {
		        	return false;
		        }
		        
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
	        	 && fPage.getStatement().equals(statement);
			}

			public boolean filterEntry(IDisplayableItem data) {
				// No filter here, so entries always match
				return true;
			}

			public long getStartBalance() {
				return openingBalance;
			}

			public void setNewEntryProperties(Entry newEntry) {
				newEntry.setAccount(fPage.getAccount());
				newEntry.setPropertyValue(ReconciliationEntryInfo.getStatementAccessor(), fPage.getStatement());
			}
        };
        
		// We manage the layout of 'container' ourselves because we want either
		// the 'containerOfTableAndButtons' to be visible or the 'no statement'
		// message to be visible.  There is no suitable layout for
		// this.  Therefore clear out the layout manager.
        container.setLayout(null);

		// Create the control that will be visible if no session is open
		noStatementMessage = new Label(container, SWT.WRAP);
		noStatementMessage.setText(ReconciliationPlugin.getResourceString("EntriesSection.noStatementMessage"));
		noStatementMessage.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_RED));

        // Create the table control.
        fReconciledEntriesControl = new EntriesTree(container, toolkit, fPage.transactionManager, reconciledTableContents, fPage.getAccount().getSession()); 
		fReconciledEntriesControl.addSelectionListener(tableSelectionListener);
        
		// TODO: do not duplicate this.
		if (fPage.getStatement() == null) {
			noStatementMessage.setSize(container.getSize());
			//fReconciledEntriesControl.getControl().setSize(0, 0);
			fReconciledEntriesControl.setSize(0, 0);
		} else {
			noStatementMessage.setSize(0, 0);
			//fReconciledEntriesControl.getControl().setSize(containerOfEntriesControl1.getSize());
			fReconciledEntriesControl.setSize(container.getSize());
			fReconciledEntriesControl.layout(true);  // ??????
		}
        
        
		// There is no layout set on the navigation view.
		// Therefore we must listen for changes to the size of
		// the navigation view and adjust the size of the visible
		// control to match.
		container.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				if (fPage.getStatement() == null) {
					noStatementMessage.setSize(container.getSize());
					fReconciledEntriesControl.setSize(0, 0);
				} else {
					noStatementMessage.setSize(0, 0);
					fReconciledEntriesControl.setSize(container.getSize());
					fReconciledEntriesControl.layout(true);  // ??????
				}
			}
		});
		
		// Listen for double clicks.
		// Double clicking on an entry in the statement table remove that entry from
		// the statement (the entry will then appear in the table of unreconciled entries).
		fReconciledEntriesControl.addSelectionListener(new EntryRowSelectionAdapter() {
			public void widgetDefaultSelected(IDisplayableItem selectedObject) {
				Entry entry = selectedObject.getEntryInAccount();
				// If the blank new entry row, entry will be null.
				// We must guard against that.

				// The EntriesTree control will always validate and commit
				// any outstanding changes before firing a default selection
				// event.  We set the property to take the entry out of the
				// statement and immediately commit the change.
				if (entry != null) {
					entry.setPropertyValue(ReconciliationEntryInfo.getStatementAccessor(), null);
					fPage.transactionManager.commit();
				}
			}
        });
        
        // Allow entries to be dropped in the statment tablein the account to be moved from the unreconciled list
        final DropTarget dropTarget = new DropTarget(fReconciledEntriesControl.getControl(), DND.DROP_MOVE);
 	         
        // Provide data in Text format???
        Transfer[] types = new Transfer[] {TextTransfer.getInstance()};
        dropTarget.setTransfer(types);
         	 
        dropTarget.addDropListener(new DropTargetListener() {

			public void dragEnter(DropTargetEvent event) {
				if (/* we don't want to accept drop*/false) {
					event.detail = DND.DROP_NONE;
				}
			}

			public void dragLeave(DropTargetEvent event) {
				// TODO Auto-generated method stub
				
			}

			public void dragOperationChanged(DropTargetEvent event) {
				// TODO Auto-generated method stub
				
			}

			public void dragOver(DropTargetEvent event) {
				// TODO Auto-generated method stub
				
			}

			public void drop(DropTargetEvent event) {
				if (TextTransfer.getInstance().isSupportedType(event.currentDataType)) {
					//String text = (String) event.data;
					Entry unrecEntryInAccount = (Entry)event.data;
					// TODO Do this properly.
	    			Point pt = new Point (event.x, event.y);
	    			// TODO: following line fails if a tree control
					final TableItem item = ((Table)fReconciledEntriesControl.getControl()).getItem(pt);
					Entry recEntryInAccount = ((IDisplayableItem)item).getEntryInAccount();

					// TODO Merge data from other transaction.
					Entry recOther = recEntryInAccount.getTransaction().getOther(recEntryInAccount);
					Entry unrecOther = unrecEntryInAccount.getTransaction().getOther(unrecEntryInAccount);
					if (unrecOther.getAccount() != null) {
						recOther.setAccount(unrecOther.getAccount());
					}
					if (unrecOther.getMemo() != null) {
						recOther.setMemo(unrecOther.getMemo());
					}
					if (unrecOther.getDescription() != null) {
						recOther.setDescription(unrecOther.getDescription());
					}
				}
				
			}

			public void dropAccept(DropTargetEvent event) {
				// TODO Auto-generated method stub
			}
        });
        
        fReconciledEntriesControl.getControl().addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				dropTarget.dispose();
			}
        });
        
        getSection().setClient(container);
        toolkit.paintBordersFor(container);
        refresh();
    }

	/**
	 * @param statement
	 * @param openingBalance
	 */
	public void setStatement(BankStatement statement, long openingBalance) {
		// Statement will already have been set in fPage,
		// but we must save the opening balance for ourselves.
		this.openingBalance = openingBalance;
		
    	fReconciledEntriesControl.refreshEntryList();

		if (statement == null) {
	        getSection().setText("Entries Shown on Statement");
	        refresh();  // Must refresh to see new section title

	        noStatementMessage.setSize(container.getSize());
	        fReconciledEntriesControl.setSize(0, 0);
		} else {
	        getSection().setText("Entries Shown on Statement " + statement.toLocalizedString());
	        getSection().layout(false);  // Required to get the new section title to show

	        noStatementMessage.setSize(0, 0);
	        fReconciledEntriesControl.setSize(container.getSize());
	        fReconciledEntriesControl.layout(true);  // ??????
		}
	}
}
