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

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Vector;

import net.sf.jmoney.entrytable.Block;
import net.sf.jmoney.entrytable.ButtonCellControl;
import net.sf.jmoney.entrytable.CellBlock;
import net.sf.jmoney.entrytable.EntriesSectionProperty;
import net.sf.jmoney.entrytable.EntriesTable;
import net.sf.jmoney.entrytable.EntryData;
import net.sf.jmoney.entrytable.HorizontalBlock;
import net.sf.jmoney.entrytable.ICellControl;
import net.sf.jmoney.entrytable.IEntriesContent;
import net.sf.jmoney.entrytable.IEntriesTableProperty;
import net.sf.jmoney.entrytable.EntriesTable.IMenuItem;
import net.sf.jmoney.fields.EntryInfo;
import net.sf.jmoney.fields.TransactionInfo;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.reconciliation.BankStatement;
import net.sf.jmoney.reconciliation.ReconciliationEntry;
import net.sf.jmoney.reconciliation.ReconciliationEntryInfo;
import net.sf.jmoney.reconciliation.ReconciliationPlugin;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
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

    private EntriesTable fReconciledEntriesControl;
    
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
    
    private IEntriesContent reconciledTableContents = null;

    private ArrayList<IEntriesTableProperty> cellList;
    
    private long openingBalance = 0;
    
    public StatementSection(ReconcilePage page, Composite parent) {
        super(parent, page.getManagedForm().getToolkit(), Section.TITLE_BAR);
        getSection().setText("Entries Shown on Statement");
        fPage = page;
    	this.toolkit = page.getManagedForm().getToolkit();
    	
        container = toolkit.createComposite(getSection());

        reconciledTableContents = new IEntriesContent() {
			public Collection<Entry> getEntries() {
		        Vector<Entry> requiredEntries = new Vector<Entry>();

		        // If no statement is set, return an empty collection.
		        // The table will not be visible in this situation, but
		        // this method will be called and so we must allow for
		        // this situation.
		        if (fPage.getStatement() == null) {
		        	return requiredEntries;
		        }
		        
				/* The caller always sorts, so there is no point in us returning
				 * sorted results.  It may be at some point we decide it is more
				 * efficient to get the database to sort for us, but that would
				 * only help the first time the results are fetched, it would not
				 * help on a re-sort.  It also only helps if the database indexes
				 * on the date.		
				CurrencyAccount account = fPage.getAccount();
		        Collection<Entry> accountEntries = 
		        	account
						.getSortedEntries(TransactionInfo.getDateAccessor(), false);
				*/
				Collection<Entry> accountEntries = fPage.getAccount().getEntries();

		        for (Entry entry: accountEntries) {
		        	BankStatement statement = entry.getPropertyValue(ReconciliationEntryInfo.getStatementAccessor());
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

			public boolean filterEntry(EntryData data) {
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

		IMenuItem unreconcileAction = new IMenuItem() {

			public String getText() {
				return "Unreconcile";
			}

			public void run(Entry selectedEntry) {
				// If the blank new entry row, entry will be null.
				// We must guard against that.

				// The EntriesTree control will always validate and commit
				// any outstanding changes before firing a default selection
				// event.  We set the property to take the entry out of the
				// statement and immediately commit the change.
				if (selectedEntry != null) {
					selectedEntry.setPropertyValue(ReconciliationEntryInfo.getStatementAccessor(), null);
					fPage.transactionManager.commit("Unreconcile Entry");
				}
			}
		};

		// Load the 'unreconcile' indicator
		URL installURL = ReconciliationPlugin.getDefault().getBundle().getEntry("/icons/unreconcile.gif");
		final Image unreconcileImage = ImageDescriptor.createFromURL(installURL).createImage();
		parent.addDisposeListener(new DisposeListener(){
			public void widgetDisposed(DisposeEvent e) {
				unreconcileImage.dispose();
			}
		});
		
		IEntriesTableProperty unreconcileButton = new IEntriesTableProperty() {

			public int compare(EntryData trans1, EntryData trans2) {
				// TODO Sort this out.  We cannot sort on this.
				return 0;
			}

			public ICellControl createCellControl(Composite parent,
					Session session) {
				return new ButtonCellControl(parent, unreconcileImage, "Remove Entry from this Statement") {

					@Override
					protected void run(EntryData data) {
						unreconcileEntry(data);
					}
				};
			}

			public String getId() {
				return "unreconcile";
			}

			public int getMinimumWidth() {
				return 20;
			}

			public String getText() {
				return "";
			}

			public int getWeight() {
				return 0;
			}
		};
		
		/*
		 * Setup the layout structure of the header and rows.
		 */
		Block rootBlock = new HorizontalBlock(new Block [] {
				new CellBlock(unreconcileButton),
				new CellBlock(EntriesSectionProperty.createTransactionColumn(TransactionInfo.getDateAccessor())),
				new CellBlock(EntriesSectionProperty.createEntryColumn(EntryInfo.getValutaAccessor())),
				new CellBlock(EntriesSectionProperty.createEntryColumn(EntryInfo.getCheckAccessor())),
				new CellBlock(EntriesSectionProperty.createEntryColumn(EntryInfo.getMemoAccessor())),
				new CellBlock(fPage.debitColumnManager),
				new CellBlock(fPage.creditColumnManager),
				new CellBlock(fPage.balanceColumnManager),
		});
		
		cellList = new ArrayList<IEntriesTableProperty>();
		rootBlock.buildCellList(cellList);

		// Create the table control.
        fReconciledEntriesControl = new EntriesTable(container, toolkit, rootBlock, reconciledTableContents, fPage.getAccount().getSession(), new IMenuItem [] { unreconcileAction }); 
        
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

/* This code needs to be made to work, but dragging the button would suffice (and drop anyway on the row?}
		fReconciledEntriesControl.addSelectionListener(new EntryRowSelectionAdapter() {
        });
        
        // Allow entries to be dropped in the statment tablein the account to be moved from the unreconciled list
        final DropTarget dropTarget = new DropTarget(fReconciledEntriesControl.getControl(), DND.DROP_MOVE);
 	         
        // Provide data in Text format???
        Transfer[] types = new Transfer[] {TextTransfer.getInstance()};
        dropTarget.setTransfer(types);
         	 
        dropTarget.addDropListener(new DropTargetAdapter() {

			public void dragEnter(DropTargetEvent event) {
				if (/* we don't want to accept drop* /false) {
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
					//Entry unrecEntryInAccount = (Entry)event.data;
					Entry unrecEntryInAccount = fPage.entryBeingDragged;
	    			Point pt = fReconciledEntriesControl.getControl().toControl(event.x, event.y);
					final TreeItem item = ((Tree)fReconciledEntriesControl.getControl()).getItem(pt);
					Entry recEntryInAccount = ((IDisplayableItem)item.getData()).getEntryInAccount();

					/*
					 * Merge data from dragged transaction into the target transaction
					 * and delete the dragged transaction.
					 * /
					mergeTransaction(unrecEntryInAccount, recEntryInAccount);
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
*/        
        getSection().setClient(container);
        toolkit.paintBordersFor(container);
        refresh();
    }

	public void unreconcileEntry(EntryData selectedObject) {
		Entry entry = selectedObject.getEntry();
		// If the blank new entry row, entry will be null.
		// We must guard against that.
		
// TODO: How do we handle the blank row?
		
		// The EntriesTree control will always validate and commit
		// any outstanding changes before firing a default selection
		// event.  We set the property to take the entry out of the
		// statement and immediately commit the change.
		if (entry != null) {
			entry.setPropertyValue(ReconciliationEntryInfo.getStatementAccessor(), null);
			fPage.transactionManager.commit("Unreconcile Entry");
		}
	}
	
	/**
	 * Merge data from other transaction.
	 * 
	 * The normal case is that the reconciled transaction was imported from the bank
	 * and the statement being merged into this transaction has been manually edited.
	 * We therefore take the following properties from the reconciled transaction:
	 * - the valuta date
	 * - the amount
	 * - the check number
	 * 
	 * And we take all other properties and all the other entries from the source transaction.
	 * 
	 * However, if any property is null in one transaction and non-null in the
	 * other then we use the non-null property.
	 * 
	 * If any of the following conditions apply in the target transaction then
	 * we give a warning to the user.  These conditions indicate that there is data
	 * in the target transaction that will be lost and that information was probably
	 * manually entered.
	 * 
	 * - the transaction has split entries
	 * - there are properties set in the other entry other that the required account,
	 *     or the account in the other entry is not the default account
	 *
	 * We actually merge the target into the source transaction and then set the source
	 * entry as reconciled.  This is a little easier.
	 * 
	 * The merge constitutes a single undoable action.  
	 */
	private void mergeTransaction(Entry unrecEntryInAccount, Entry recEntryInAccount) {
		Entry recOther = recEntryInAccount.getTransaction().getOther(recEntryInAccount);
		Entry unrecOther = unrecEntryInAccount.getTransaction().getOther(unrecEntryInAccount);
		
		if (recOther == null) {
	        MessageBox diag = new MessageBox(this.getSection().getShell(), SWT.YES | SWT.NO);
	        diag.setText("Warning");
	        diag.setMessage("The target entry has split entries.  These entries will be replaced by the data from the transaction for the dragged entry.  The split entries will be lost.  Are you sure you want to do this?");
	        if (diag.open() != SWT.YES) {
	        	return;
	        }
		}
		
		// There should not be any uncommitted changes.
		if (fPage.transactionManager.hasChanges()) {
			System.out.println("something is wrong");
		}
		
		if (recEntryInAccount.getCheck() != null) {
			unrecEntryInAccount.setCheck(recEntryInAccount.getCheck());
		}
		
		if (recEntryInAccount.getValuta() != null) {
			unrecEntryInAccount.setValuta(recEntryInAccount.getValuta());
		} else {
			/*
			 * If no value date in the entry from the bank then use the transaction
			 * date as the value date.  The transaction date will be taken from
			 * the manually entered transaction.
			 */
			unrecEntryInAccount.setValuta(recEntryInAccount.getTransaction().getDate());
		}
		
		if (recEntryInAccount.getAmount() != unrecEntryInAccount.getAmount()) {
	        MessageBox diag = new MessageBox(this.getSection().getShell(), SWT.YES | SWT.NO);
	        diag.setText("Warning");
	        diag.setMessage(
	        		"The target entry has an amount of " 
	        		+ EntryInfo.getAmountAccessor().formatValueForMessage(recEntryInAccount) 
	        		+ " and the dragged entry has an amount of " 
	        		+ EntryInfo.getAmountAccessor().formatValueForMessage(unrecEntryInAccount) 
	        		+ "."
	        		+ "These amounts should normally be equal.  It may be that the incorrect amount was originally entered for the dragged entry and you want the amount corrected to the amount given by the import.  If so, continue and the amount will be corrected.  Do you want to continue?");
	        if (diag.open() != SWT.YES) {
	        	return;
	        }

	        unrecEntryInAccount.setAmount(recEntryInAccount.getAmount());
		}

		/*
		 * All other properties are taken from the target transaction only if
		 * the property is null in the source transaction.
		 */
		for (ScalarPropertyAccessor<?> propertyAccessor: EntryInfo.getPropertySet().getScalarProperties3()) {
			copyProperty(propertyAccessor, unrecEntryInAccount, recEntryInAccount);
		}

		/*
		 * Now we delete the reconciled entry and set the previously unreconciled
		 * entry to be reconciled.
		 */
		Session session = fPage.transactionManager.getSession();
		session.getTransactionCollection().remove(recEntryInAccount.getTransaction());

		ReconciliationEntry unrecEntry2 = unrecEntryInAccount.getExtension(ReconciliationEntryInfo.getPropertySet());
		unrecEntry2.setStatement(fPage.getStatement());
		
		fPage.transactionManager.commit("Match Entry to Bank's Entry");
	}

	/**
	 * Helper method to copy a property from the target entry to the source entry if the
	 * property is null in the source entry but not null in the target entry.
	 */
	private <V> void copyProperty(ScalarPropertyAccessor<V> propertyAccessor, Entry unrecEntryInAccount, Entry recEntryInAccount) {
		V sourceValue = unrecEntryInAccount.getPropertyValue(propertyAccessor);
		V targetValue = recEntryInAccount.getPropertyValue(propertyAccessor);
		if (sourceValue == null && targetValue != null) {
			unrecEntryInAccount.setPropertyValue(propertyAccessor, targetValue);
		}
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

    	// TODO: this should be automatic from the above, i.e.
    	// content provider should tell the table?
    	fReconciledEntriesControl.table.refreshContent();
    	
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
