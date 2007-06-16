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

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.entrytable.BalanceColumn;
import net.sf.jmoney.entrytable.Block;
import net.sf.jmoney.entrytable.ButtonCellControl;
import net.sf.jmoney.entrytable.CellBlock;
import net.sf.jmoney.entrytable.DebitAndCreditColumns;
import net.sf.jmoney.entrytable.EntriesTable;
import net.sf.jmoney.entrytable.EntryData;
import net.sf.jmoney.entrytable.EntryRowControl;
import net.sf.jmoney.entrytable.HorizontalBlock;
import net.sf.jmoney.entrytable.ICellControl;
import net.sf.jmoney.entrytable.IEntriesContent;
import net.sf.jmoney.entrytable.IndividualBlock;
import net.sf.jmoney.entrytable.OtherEntriesButton;
import net.sf.jmoney.entrytable.PropertyBlock;
import net.sf.jmoney.entrytable.RowSelectionTracker;
import net.sf.jmoney.entrytable.SingleOtherEntryPropertyBlock;
import net.sf.jmoney.fields.EntryInfo;
import net.sf.jmoney.fields.TransactionInfo;
import net.sf.jmoney.isolation.TransactionManager;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.reconciliation.BankStatement;
import net.sf.jmoney.reconciliation.ReconciliationEntryInfo;
import net.sf.jmoney.reconciliation.ReconciliationPlugin;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
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

    private ArrayList<CellBlock<EntryData>> cellList;
    
    private long openingBalance = 0;
    
    public StatementSection(ReconcilePage page, Composite parent, RowSelectionTracker rowTracker) {
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
	        	BankStatement statement = entry.getPropertyValue(ReconciliationEntryInfo.getStatementAccessor());
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
				// It is assumed that the entry is in a data manager that is a direct
				// child of the data manager that contains the account.
				TransactionManager tm = (TransactionManager)newEntry.getDataManager();
				newEntry.setAccount(tm.getCopyInTransaction(fPage.getAccount()));

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

		// Load the 'unreconcile' indicator
		URL installURL = ReconciliationPlugin.getDefault().getBundle().getEntry("/icons/unreconcile.gif");
		final Image unreconcileImage = ImageDescriptor.createFromURL(installURL).createImage();
		parent.addDisposeListener(new DisposeListener(){
			public void widgetDisposed(DisposeEvent e) {
				unreconcileImage.dispose();
			}
		});
		
		CellBlock<EntryData> unreconcileButton = new CellBlock<EntryData>(20, 0) {

			@Override
			public ICellControl<EntryData> createCellControl(final Composite parent) {
				// TODO: remove cast in following line.  There is a risk of a cast exception.
				ButtonCellControl cellControl = new ButtonCellControl((EntryRowControl)parent, unreconcileImage, "Remove Entry from this Statement") {
					@Override
					protected void run(EntryRowControl rowControl) {
						unreconcileEntry(rowControl);
					}
				};


				// Allow entries to be dropped in the statement table in the account to be moved from the unreconciled list
				final DropTarget dropTarget = new DropTarget(cellControl.getControl(), DND.DROP_MOVE);

				// Data is provided using a local reference only (can only drag and drop
				// within the Java VM)
				Transfer[] types = new Transfer[] { LocalSelectionTransfer.getTransfer() };
				dropTarget.setTransfer(types);

				dropTarget.addDropListener(new DropTargetAdapter() {

					public void dragEnter(DropTargetEvent event) {
						/*
						 * We want to check what is being dragged, in case it is not an
						 * EntryData object.  Unfortunately this is not available on all platforms,
						 * only on Windows.  The following call to the nativeToJava method will
						 * return the ISelection object on Windows but null on other platforms.
						 * If we get null back, we assume the drop is valid.
						 */
						ISelection selection = (ISelection)LocalSelectionTransfer.getTransfer().nativeToJava(event.currentDataType);
						if (selection == null) {
							// The selection cannot be determined on this platform - accept the drop
							return;
						}
						
						if (selection instanceof StructuredSelection) {
							StructuredSelection structured = (StructuredSelection)selection;
							if (structured.size() == 1
									&& structured.getFirstElement() instanceof EntryData) {
								// We do want to accept the drop
								return;
							}
						}

						// we don't want to accept drop
						event.detail = DND.DROP_NONE;
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
						if (LocalSelectionTransfer.getTransfer().isSupportedType(event.currentDataType)) {
							ISelection selection = LocalSelectionTransfer.getTransfer().getSelection();
							EntryData unrecEntryInAccount = (EntryData)((StructuredSelection)selection).getFirstElement();
							EntryRowControl dropRow = (EntryRowControl)parent;

							/*
							 * Merge data from dragged transaction into the target transaction
							 * and delete the dragged transaction.
							 */
							boolean success = mergeTransaction(unrecEntryInAccount, dropRow);
							if (!success) {
								event.detail = DND.DROP_NONE;
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


				return cellControl;
			}

			@Override
			public void createHeaderControls(Composite parent) {
				/*
				 * All CellBlock implementations must create a control because
				 * the header and rows must match. Maybe these objects could
				 * just point to the header controls, in which case this would
				 * not be necessary.
				 * 
				 * Note also we use Label, not an empty Composite, because we
				 * don't want a preferred height that is higher than the labels.
				 */
				new Label(parent, SWT.NONE);
			}
		};
		
		IndividualBlock<EntryData> transactionDateColumn = PropertyBlock.createTransactionColumn(TransactionInfo.getDateAccessor());
		CellBlock<EntryData> debitColumnManager = new DebitAndCreditColumns("Debit", "debit", fPage.getAccount().getCurrency(), true);     //$NON-NLS-2$
		CellBlock<EntryData> creditColumnManager = new DebitAndCreditColumns("Credit", "credit", fPage.getAccount().getCurrency(), false); //$NON-NLS-2$
		CellBlock<EntryData> balanceColumnManager = new BalanceColumn(fPage.getAccount().getCurrency());

		/*
		 * Setup the layout structure of the header and rows.
		 */
		Block<EntryData> rootBlock = new HorizontalBlock<EntryData>(
				unreconcileButton,
				transactionDateColumn,
				PropertyBlock.createEntryColumn(EntryInfo.getValutaAccessor()),
				PropertyBlock.createEntryColumn(EntryInfo.getCheckAccessor()),
				PropertyBlock.createEntryColumn(EntryInfo.getMemoAccessor()),
				new OtherEntriesButton(
						new HorizontalBlock<Entry>(
								new SingleOtherEntryPropertyBlock(EntryInfo.getAccountAccessor()),
								new SingleOtherEntryPropertyBlock(EntryInfo.getMemoAccessor(), JMoneyPlugin.getResourceString("Entry.description")),
								new SingleOtherEntryPropertyBlock(EntryInfo.getAmountAccessor())
						)
				),
				debitColumnManager,
				creditColumnManager,
				balanceColumnManager
		);
		
		cellList = new ArrayList<CellBlock<EntryData>>();
		rootBlock.buildCellList(cellList);

		// Create the table control.
        fReconciledEntriesControl = new EntriesTable(container, toolkit, rootBlock, reconciledTableContents, fPage.getAccount().getSession(), transactionDateColumn, rowTracker); 
        
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

        getSection().setClient(container);
        toolkit.paintBordersFor(container);
        refresh();
    }

	public void unreconcileEntry(EntryRowControl rowControl) {
		Entry entry = rowControl.getUncommittedTopEntry();
		
		// If the blank new entry row, entry will be null.
		// We must guard against that.
		
// TODO: How do we handle the blank row?
		
		// The EntriesTree control will always validate and commit
		// any outstanding changes before firing a default selection
		// event.  We set the property to take the entry out of the
		// statement and immediately commit the change.
		if (entry != null) {
			entry.setPropertyValue(ReconciliationEntryInfo.getStatementAccessor(), null);
			
			/*
			 * We tell the row control to commit its changes. These changes
			 * include the above change. They may also include prior changes
			 * made by the user.
			 * 
			 * The tables and controls in this editor should all be capable of
			 * updating themselves correctly when the change is committed. There
			 * is a listener that is listening for changes to the committed data
			 * and this listener should ensure all is updated appropriately,
			 * just as though the change came from outside this view. However,
			 * we must go through the row control to commit the changes. This
			 * ensures that the row control knows that its changes are being
			 * committed and it does not get confused when the listener
			 * processes the changes.
			 */
			rowControl.commitChanges("Unreconcile Entry");
		}
	}
	
	/**
	 * Merge data from other transaction.
	 * 
	 * The normal case is that the reconciled transaction was imported from the
	 * bank and the statement being merged into this transaction has been
	 * manually edited. We therefore take the following properties from the
	 * reconciled transaction: - the valuta date - the amount - the check number
	 * 
	 * And we take all other properties and all the other entries from the
	 * source transaction.
	 * 
	 * However, if any property is null in one transaction and non-null in the
	 * other then we use the non-null property.
	 * 
	 * If any of the following conditions apply in the target transaction then
	 * we give a warning to the user. These conditions indicate that there is
	 * data in the target transaction that will be lost and that information was
	 * probably manually entered.
	 *  - the transaction has split entries - there are properties set in the
	 * other entry other that the required account, or the account in the other
	 * entry is not the default account
	 * 
	 * The source and targets will be in different transaction managers. We want
	 * to take the properties from the source transaction and move them into the
	 * target transaction without committing the target transaction. The taret
	 * row control should update itself automatically because the controls
	 * always listen for property changes in the model.
	 * 
	 * The source is deleted, with any uncommitted changes being dropped, but
	 * this is done by the drop source.
	 * 
	 * The merge constitutes a single undoable action.
	 * 
	 * @return true if the merge succeeded, false if it failed (in which case
	 *         the user is notified by this method of the failure)
	 */
	private boolean mergeTransaction(EntryData unrecEntryData, EntryRowControl recRowControl) {
		
		Entry unrecEntryInAccount = unrecEntryData.getEntry();
		Entry recEntryInAccount = recRowControl.getUncommittedTopEntry();
		
		Entry recOther = recEntryInAccount.getTransaction().getOther(recEntryInAccount);
		Entry unrecOther = unrecEntryInAccount.getTransaction().getOther(unrecEntryInAccount);
		
		if (recOther == null) {
	        MessageBox diag = new MessageBox(this.getSection().getShell(), SWT.YES | SWT.NO);
	        diag.setText("Warning");
	        diag.setMessage("The target entry has split entries.  These entries will be replaced by the data from the transaction for the dragged entry.  The split entries will be lost.  Are you sure you want to do this?");
	        if (diag.open() != SWT.YES) {
	        	return false;
	        }
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
	        	return false;
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
		
		return true;
	}

	/**
	 * Helper method to copy a property from the target entry to the source entry if the
	 * property is null in the source entry but not null in the target entry.
	 */
	private <V> void copyProperty(ScalarPropertyAccessor<V> propertyAccessor, Entry sourceAccount, Entry targetAccount) {
		V sourceValue = sourceAccount.getPropertyValue(propertyAccessor);
		V targetValue = targetAccount.getPropertyValue(propertyAccessor);
		if (sourceValue != null && targetValue == null) {
			if (sourceValue instanceof ExtendableObject) {
				/*
				 * We need to get a copy in the correct data manager.
				 */
// TODO: sort this out.				
//				UncommittedObjectKey uncommittedObjectKey = (UncommittedObjectKey)((ExtendableObject)sourceValue).getObjectKey();
//				V committedObject = propertyAccessor.getClassOfValueObject().cast(uncommittedObjectKey.getCommittedObjectKey().getObject());
//				targetValue = ((TransactionManager)targetAccount.getDataManager()).getCopyInTransaction(committedObject);
			} else {
				targetAccount.setPropertyValue(propertyAccessor, sourceValue);
			}
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
