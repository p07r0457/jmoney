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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
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

    private IEntriesControl fReconciledEntriesControl;
    
    private Composite containerOfEntriesControl1;
    
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
    
    /**
	 * Contains two controls:
	 * - the table container
	 * - the button area container
	 * Layout: GridLayout
     */
	private Composite containerOfTableAndButtons;
	
    private FormToolkit toolkit;
    
    private SelectionListener tableSelectionListener = null;
    
    private IEntriesContent reconciledTableContents = null;
    
    public StatementSection(ReconcilePage page, Composite parent) {
        super(parent, page.getManagedForm().getToolkit(), Section.TITLE_BAR);
        getSection().setText("Entries Shown on Statement");
        fPage = page;
    	this.toolkit = page.getManagedForm().getToolkit();
    	
        container = toolkit.createComposite(getSection());

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

			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub
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
				// This entry is to be shown if both the account and
				// the statement match.
	        	BankStatement statement = (BankStatement)entry.getPropertyValue(ReconciliationEntryInfo.getStatementAccessor());
				return fPage.getAccount().equals(entry.getAccount())
	        	 && fPage.getStatement().equals(statement);
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
	        	 && fPage.getStatement().equals(statement);
			}

			public boolean filterEntry(IDisplayableItem data) {
				// No filter here, so entries always match
				return true;
			}

			public long getStartBalance() {
				return fPage.getAccount().getStartBalance();
			}
        	
        };

        
        // Create the control that contains the table control and the buttons
        // underneath it.  These controls are grouped into a composite because
        // they are displayed alternatively with the 'no statement' label.
        containerOfTableAndButtons = toolkit.createComposite(container);
        GridLayout layout23 = new GridLayout();
        layout23.numColumns = 1; // not needed????
        layout23.marginHeight = 0;
        layout23.marginWidth = 0;
        containerOfTableAndButtons.setLayout(layout23);
        
        // Create the control that contains the Table or TableTree control.
        // Although this control only contains a single child control,
        // we need to create it so we can destroy and re-create the
        // child control without altering the sequence of controls
        // in the grid container.
        
        containerOfEntriesControl1 = toolkit.createComposite(containerOfTableAndButtons);
        containerOfEntriesControl1.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_RED));
        GridLayout layout21 = new GridLayout();
        layout21.marginHeight = 0;
        layout21.marginWidth = 0;
        containerOfEntriesControl1.setLayout(layout21);

        GridData gridData = new GridData(GridData.FILL_BOTH);
        gridData.heightHint = 50;
        gridData.widthHint = 200;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
        containerOfEntriesControl1.setLayoutData(gridData);

        
		// We manage the layout of 'container' ourselves because we want either
		// the 'containerOfTableAndButtons' to be visible or the 'no statement'
		// message to be visible.  There is no suitable layout for
		// this.  Therefore clear out the layout manager.
        container.setLayout(null);

		// Create the control that will be visible if no session is open
		noStatementMessage = new Label(container, SWT.WRAP);
		noStatementMessage.setText(ReconciliationPlugin.getResourceString("EntriesSection.noStatementMessage"));
		noStatementMessage.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_RED));

        // Initially set to the table control.
        // TODO: save this information in the memento so
        // the user always gets the last selected view.
        fReconciledEntriesControl = new EntriesTable(containerOfEntriesControl1, reconciledTableContents, fPage.getAccount().getSession()); 
		fPage.getEditor().getToolkit().adapt(fReconciledEntriesControl.getControl(), true, false);
		fReconciledEntriesControl.addSelectionListener(tableSelectionListener);
        
		// TODO: do not duplicate this.
		if (fPage.getStatement() == null) {
			noStatementMessage.setSize(container.getSize());
			//fReconciledEntriesControl.getControl().setSize(0, 0);
			containerOfTableAndButtons.setSize(0, 0);
		} else {
			noStatementMessage.setSize(0, 0);
			//fReconciledEntriesControl.getControl().setSize(containerOfEntriesControl1.getSize());
			containerOfTableAndButtons.setSize(container.getSize());
			containerOfTableAndButtons.layout(true);  // ??????
		}
        
        
		// There is no layout set on the navigation view.
		// Therefore we must listen for changes to the size of
		// the navigation view and adjust the size of the visible
		// control to match.
		container.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				if (fPage.getStatement() == null) {
					noStatementMessage.setSize(container.getSize());
					containerOfTableAndButtons.setSize(0, 0);
				} else {
					noStatementMessage.setSize(0, 0);
					containerOfTableAndButtons.setSize(container.getSize());
					containerOfTableAndButtons.layout(true);  // ??????
				}
			}
			
		});
		
		// Listen for double clicks.
		// Double clicking on an entry in the statement table remove that entry from
		// the statement (the entry will then appear in the table of unreconciled entries).
		fReconciledEntriesControl.addSelectionListener(new SelectionAdapter() {
			public void widgetDefaultSelected(SelectionEvent e) {
                Object selectedObject = e.item.getData();
                // The selected object might be null.  This occurs when the table is refreshed.
                // I don't understand this so I am simply bypassing the update
                // in this case.  Nigel
                if (selectedObject != null) {
                	IDisplayableItem data = (IDisplayableItem)selectedObject;
                	Entry entry = data.getEntryInAccount();
                	entry.setPropertyValue(ReconciliationEntryInfo.getStatementAccessor(), null);
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
        
        // Create the button area
		Composite buttonArea = toolkit.createComposite(containerOfTableAndButtons);
		
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
                fReconciledEntriesControl.setSelection(entry1, entry1);
           }
        });

        // Create the 'delete transaction' button.
        Button deleteButton = toolkit.createButton(buttonArea, "Delete Transaction", SWT.PUSH);
        deleteButton.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent event) {
        		// TODO: Should we allow deletion from top table?
        		// TODO: Should we allow a selection in one table only?
        		Entry selectedEntry = fReconciledEntriesControl.getSelectedEntry();
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
        		Entry selectedEntry = fReconciledEntriesControl.getSelectedEntryInAccount();
        		if (selectedEntry != null) {
        			Transaction transaction = selectedEntry.getTransaction();
        			Entry newEntry = transaction.createEntry();
        			transaction.getSession().registerUndoableChange("New Split");
        			
               		// Select the new entry in the entries list.
                    fReconciledEntriesControl.setSelection(selectedEntry, newEntry);
        		}
           }
        });

        // Create the 'delete split' button.
        Button deleteSplitButton = toolkit.createButton(buttonArea, "Delete Split", SWT.PUSH);
        deleteSplitButton.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent event) {
           		// TODO: We must allow the user to add a split in the
           		// top table too.
        		Entry selectedEntryInAccount = fReconciledEntriesControl.getSelectedEntryInAccount();
        		Entry selectedEntry = fReconciledEntriesControl.getSelectedEntry();
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
        		// TODO: We must support this in lower table too
        		Entry selectedEntryInAccount = fReconciledEntriesControl.getSelectedEntryInAccount();
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
    	if (fReconciledEntriesControl instanceof EntriesTable) {
    		// Already set to table view, so nothing to do.
    		return;
    	}
    	
    	fReconciledEntriesControl.dispose();
    	fReconciledEntriesControl = new EntriesTable(containerOfEntriesControl1, reconciledTableContents, fPage.getAccount().getSession());
		fPage.getEditor().getToolkit().adapt(fReconciledEntriesControl.getControl(), true, false);
		fReconciledEntriesControl.addSelectionListener(tableSelectionListener);
        containerOfEntriesControl1.layout(false);
	}

	/**
	 * Set the entries list to be a table tree.  If any other
	 * entries control is set, destroy it first.
	 */
    public void setTreeView() {
    	if (fReconciledEntriesControl instanceof EntriesTree) {
    		// Already set to tree view, so nothing to do.
    		return;
    	}
    	
    	fReconciledEntriesControl.dispose();
//TODO:    	fReconciledEntriesControl = new EntriesTree(containerOfEntriesControl, reconciledTableContents);
		fPage.getEditor().getToolkit().adapt(fReconciledEntriesControl.getControl(), true, false);
		fReconciledEntriesControl.addSelectionListener(tableSelectionListener);
        containerOfEntriesControl1.layout(false);
    }

	/**
	 * @param statement
	 */
	public void setStatement(BankStatement statement) {
		// Statement will already have been set in fPage.
    	fReconciledEntriesControl.refresh();


		if (statement == null) {
	        getSection().setText("Entries Shown on Statement");
	        refresh();  // Must refresh to see new section title

	        noStatementMessage.setSize(container.getSize());
			containerOfTableAndButtons.setSize(0, 0);
		} else {
	        getSection().setText("Entries Shown on Statement " + statement.toLocalizedString());
	        getSection().layout(false);  // Required to get the new section title to show

	        noStatementMessage.setSize(0, 0);
			containerOfTableAndButtons.setSize(container.getSize());
			containerOfTableAndButtons.layout(true);  // ??????
		}
	}
}
