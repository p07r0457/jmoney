/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2002 Johann Gyger <johann.gyger@switzerland.org>
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

package net.sf.jmoney.entrytable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import net.sf.jmoney.fields.AccountInfo;
import net.sf.jmoney.fields.CommodityInfo;
import net.sf.jmoney.isolation.TransactionManager;
import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.SessionChangeAdapter;
import net.sf.jmoney.model2.Transaction;
import net.sf.jmoney.pages.entries.EntryRowSelectionListener;
import net.sf.jmoney.pages.entries.TransactionDialog;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * Class that displays a list of entries in a table. The table contains one row
 * control for each entry in the list.
 * <P>
 * Note that it is possible that one transaction may contain two or more entries
 * in the same account. For example, a user may write a check for a deposit and
 * a check for the balance of a purchase. The user may enter this as a single
 * 'split' transaction containing the two checks and a single entry containing
 * the total purchase amount in an expense category. In this situation, the
 * single transaction will have two top-level rows (one for each check).
 * Expanding one of the rows with show two child entries: the other check and
 * the purchase entry.
 * <P>
 * A simple transaction is a transaction that contains two entries where one
 * entry is in a capital account and the other entry is in an income and expense
 * account. This is a common form of transaction and therefore we make a special
 * effort to display such transactions on a single row.
 * <P>
 * Some of the entry properties apply only to entries in capital accounts, some
 * apply only to entries in income and expense accounts, and some apply to
 * entries in both types of accounts. In a simple transaction, all the
 * properties from both entries have a column. Properties from the transaction
 * are displayed in the top level and also have a column. So, we have a column
 * for all the following:
 * <UL>
 * <LI>Every property in the transaction</LI>
 * <LI>Every property in an entry that may be applicable given the account
 * being listed, with the exception that if all the entries are in the same
 * account (true in most uses of this class) then no column exists for the
 * account property (as such a column would contain the same account in every
 * row and therefore not be of much use)</LI>
 * <LI>Every property in an entry that may be applicable when the entry is in
 * an income and expense account</LI>
 * </UL>
 * 
 * Some properties may be applicable for both entries in the capital account and
 * for entries in income and expense accounts. There will be two columns for
 * such properties. When an entry is being show on its own child row, we have
 * the choice of which of the two columns we use for the property. We chose to
 * show it in the column that would, for a simple transaction, be used to show
 * the property of the entry in the income and expense account. This makes the
 * child rows look more similar and also ensures that a transfer account is
 * shown.
 * <P>
 * Each column is managed by an IEntriesTableProperty object. Each row is
 * managed by an EntryData object. These two classes
 * must work together to determine the contents of a cell.
 * <P>
 * The credit, debit, and balance columns are special cases and special
 * implementations of IEntriesTableProperty handle those three columns. The
 * other columns fall into one of the above three categories. The rest of this
 * explanation applies only to the latter class of columns.
 * <P>
 * A request for cell contents (whether for displaying text or for creating a
 * cell editor) goes first to the IEntriesTableProperty object. That object then
 * gets data from the EntryData object to create and load the control for
 * the cell. As the IEntriesTableProperty object has the property accessor,
 * the property value associated with the cell can be got and set.
 */
public class EntriesTable extends Composite {

	protected Session session;
	
	protected IEntriesContent entriesContent;

	public Block rootBlock;
	
	public VirtualRowTable table;
	
	/**
	 * List of entries to show in the table. Only the top level entries are
	 * included, the other entries in the transactions, which are shown as child
	 * items, are not in this list. The elements are not sorted.
	 */
	Map<Entry, EntryData> entries;

	/**
	 * The comparator that sorts entries according to the current sort order.
	 */
	Comparator<EntryData> rowComparator;
	
	/**
	 * The entries in sorted order.  This list contains the same
	 * items that are in the entries map.
	 */
	List<EntryData> sortedEntries;
	
	/**
	 * Set of listeners for selection changes
	 */
	private Vector<EntryRowSelectionListener> selectionListeners = new Vector<EntryRowSelectionListener>();

	/**
	 * a kludgy field used to indicate that the next selection
	 * event should be ignored.  i.e. the event should not be
	 * passed on to our listeners.
	 */
	protected boolean ignoreSelectionEvent = false;

	public interface IMenuItem {

		String getText();

		void run(Entry selectedEntry);
		
	}
	
	public EntriesTable(Composite parent, FormToolkit toolkit,
			Block rootBlock, final IEntriesContent entriesContent, final Session session, final IMenuItem [] contextMenuItems) {
		super(parent, SWT.NONE);
		
		this.session = session;

		this.rootBlock = rootBlock;

		toolkit.adapt(this, true, false);

		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		setLayout(layout);
		
		this.entriesContent = entriesContent;

		// Fetch and sort the list of top level entries to display.
		buildEntryList();

	    /*
		 * Build the initial sort order. This must be done before we can create
		 * the composite table because the constructor for the composite table
		 * requires a row content provider.
		 */
	    // TODO: This is not the most efficient, nor probably correct
	    // in the general case.
	    sort(getCellList().iterator().next(), true);
	    
		table = new VirtualRowTable(this, rootBlock, this, new ReusableRowProvider(this));
		
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.heightHint = 100;
		gridData.widthHint = 100;
		table.setLayoutData(gridData);

        // Create the button area
		Composite buttonArea = toolkit.createComposite(this);
		
		// Note that the buttons touch each other and also touch
		// the table above.  This makes it clearer that the buttons
		// are tightly associated with the table.
		RowLayout layoutOfButtons = new RowLayout();
		layoutOfButtons.fill = false;
		layoutOfButtons.justify = true;
		layoutOfButtons.marginTop = 0;
		layoutOfButtons.spacing = 5;
		buttonArea.setLayout(layoutOfButtons);
		
        // Create the 'add transaction' button.
        Button addButton = toolkit.createButton(buttonArea, "New Transaction", SWT.PUSH);
        addButton.addSelectionListener(new SelectionAdapter() {
           public void widgetSelected(SelectionEvent event) {
/* not sure if we need this....        	   
				if (!checkAndCommitTransaction(null)) {
					return;
				}
           		
           		insertNewEntry();
*/           		
           }
        });

        // Create the 'duplicate transaction' button.
        Button duplicateButton = toolkit.createButton(buttonArea, "Duplicate Transaction", SWT.PUSH);
        duplicateButton.addSelectionListener(new SelectionAdapter() {
           public void widgetSelected(SelectionEvent event) {
        	   
        	   /*
				 * Try and set the selection to the new empty row. If that works
				 * then we know that the source entry was valid and the
				 * duplicate is going to succeed, so copy the data into the new
				 * empty transaction row.
				 */        	   
/* TODO: get this working        	   
        	   int previousSelection = table.getSelection().y;
				if (table.setSelection(0, getNewEmptyRowIndex())) {
					return;
				}
           		
				Entry selectedEntry = sortedEntries.get(previousSelection).getEntryInAccount();
				
        		Row newSelectedEntry = newEmptyRow;

        		newSelectedEntry.initializeFromTemplate(selectedEntry);
*/        		
           }
        });

        // Create the 'delete transaction' button.
        Button deleteButton = toolkit.createButton(buttonArea, "Delete Transaction", SWT.PUSH);
        deleteButton.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent event) {
        		Entry selectedEntry = getSelectedEntry();
        		if (selectedEntry != null) {
        			// Clear the selection.  This should not be necessary but.....
//        			table.setSelection(new TreeItem [] {});
        			
        			// Does this need be so complex??? It is only in a transaction
        			// so we can undo it.  A more efficient way would be to make the change
        			// in a callback.
        			
        			TransactionManager transactionManager = new TransactionManager(selectedEntry.getObjectKey().getSessionManager());
        			Entry selectedEntry2 = transactionManager.getCopyInTransaction(selectedEntry); 
        			Transaction transaction = selectedEntry2.getTransaction();
        			transaction.getSession().deleteTransaction(transaction);
        			transactionManager.commit("Delete Transaction");
        		}
        	}
        });
        
        // Create the 'add split' button.
        Button addEntryButton = toolkit.createButton(buttonArea, "New Split", SWT.PUSH);
        addEntryButton.addSelectionListener(new SelectionAdapter() {
           public void widgetSelected(SelectionEvent event) {
        		Entry selectedEntry = getSelectedEntry();
        		if (selectedEntry != null) {
        			Transaction transaction = selectedEntry.getTransaction();
        			Entry newEntry = transaction.createEntry();
        			
        			long total = 0;
        			Commodity commodity = null;
        			for (Entry entry: transaction.getEntryCollection()) {
        				if (entry.getCommodity() != null) {
            				if (commodity == null) {
            					commodity = entry.getCommodity();
            				} else if (!commodity.equals(entry.getCommodity())) {
            					// We have entries with mismatching commodities set.
            					// This means there is an exchange of one commodity
            					// for another so we do not expect the total amount
            					// of all the entries to be zero.  Leave the amount
            					// for this new entry blank (a zero amount).
            					total = 0;
            					break;
            				}
        				}

        				total += entry.getAmount();
        			}
        			
        			newEntry.setAmount(-total);
        			
        			// We set the currency by default to be the currency
        			// of the top-level entry.
        			
        			// The currency of an entry is not
        			// applicable if the entry is an entry in a currency account
        			// (because all entries in a currency account must have the
        			// currency of the account).  However, we set it anyway so
        			// the value is there if the entry is set to an income and
        			// expense account (which would cause the currency property
        			// to become applicable).

    				// It may be that the currency of the top-level entry is not known.
    				// This is not possible if entries in a currency account
    				// are being listed, but may be possible if this entries list
    				// control is used for more general purposes.  In this case,
        			// the currency is not set and so the user must enter it.
        			if (selectedEntry.getCommodity() instanceof Currency) {
            			newEntry.setIncomeExpenseCurrency((Currency)selectedEntry.getCommodity());
        			}
        			
               		// Select the new entry in the entries list.
//???                    setSelection(selectedEntry, newEntry);
        		} else {
        			showMessage("You cannot add a new split to an entry until you have selected an entry from the above list.");
        		}
           }
        });
        
/* remove split processing for time being....
        // Create the 'delete split' button.
        Button deleteSplitButton = toolkit.createButton(buttonArea, "Delete Split", SWT.PUSH);
        deleteSplitButton.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent event) {
				// Clean up any cell editor
				closeCellEditor();

				if (fTable.getSelectionCount() != 1) {
					showMessage("No split entry is selected in the entries list");
					return;
				}

				IDisplayableItem data = (IDisplayableItem)fTable.getSelection()[0].getData();
				
				if (!(data instanceof DisplayableEntry)) {
					showMessage("No split entry is selected in the entries list");
					return;
				}

				DisplayableTransaction dTrans = ((DisplayableEntry)data).transactionData;
				
				if (dTrans.otherEntries.size() == 1) {
					showMessage("You cannot delete the transfer account entry in a double entry transaction.  If you do not intend this transaction to be a transfer then change the category as appropriate.");
					return;
				}

        		Entry selectedEntryInAccount = getSelectedEntryInAccount();
        		Entry selectedEntry = getSelectedEntry();
        		if (selectedEntry != null 
        				&& selectedEntry != selectedEntryInAccount) {

        			// Set the previouslySelectedItem to be the item of the top-level entry,
    				// as this item will no longer be valid after we dispose it.
    				previouslySelectedItem = previouslySelectedItem.getParentItem();
    				fTable.setSelection(new TreeItem [] { previouslySelectedItem });
    				
        			Transaction transaction = selectedEntry.getTransaction();
        			transaction.deleteEntry(selectedEntry);
        		}
        	}
        });
*/        
        // Create the 'details' button.
        Button detailsButton = toolkit.createButton(buttonArea, "Details", SWT.PUSH);
        detailsButton.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent event) {
        		Entry selectedEntry = getSelectedEntry();
				if (selectedEntry != null) {
					TransactionDialog dialog = new TransactionDialog(
							getShell(),
							selectedEntry,
							session);
					dialog.open();
				} else {
					// No entry selected.
				}
        	}
        });
        
        
/*		
		fTable.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent event) {
				Point pt = new Point(event.x, event.y);
				final TreeItem item = fTable.getItem(pt);
				
				// User may have clicked on space after the last row (i.e. after the new entry row).
				// The selection is not changed by SWT when the user does this, so ignore
				// this mouse down event, leaving the current selection and any
				// open cell editor.
				if (item == null) {
					return;
				}
				
				// Clean up any previous editor control
				closeCellEditor();

				final int column = getColumn(pt, item);

				// User may have clicked on the expand button, or perhaps somewhere else.
				if (column == -1) {
					return;
				}

				if (event.button == 1) {
					final IDisplayableItem data = (IDisplayableItem)item.getData();

					if (!checkAndCommitTransaction(data)) {
						return;
					}
					
					// Go into edit mode only if the user clicked on a field in the
					// selected row or on the new transaction row.
					if (item != previouslySelectedItem) {
						previouslySelectedItem = item;
						if (data instanceof DisplayableNewEmptyEntry) {
							// Replace with a real transaction.
							// Note that all transactions must have at least
							// two entries.
			           		Transaction transaction = session.createTransaction();
			           		Entry entry1 = transaction.createEntry();
			           		transaction.createEntry();
			           		
			           		// TODO: There is so much duplicated stuff here that it
			           		// would be better if we could get the listener to do this.
			     
			           		// When we set the account, the entry will be added
			           		// to the list through the listener.  However, we want this
			           		// new entry to be replaced into the new entry item.
			           		// We do this by setting a reference to this entry in
			           		// a field.  When the listener is notified of a new entry,
			           		// it checks this field against the new entry and, if
			           		// it matches, places the new entry in the new entry row.
			           		// (The listener then resets the field to null).
			           		newEntryRowEntry = entry1;
			           		entriesContent.setNewEntryProperties(entry1);
			           		JMoneyPlugin.myAssert(newEntryRowEntry == null);
			           		
			           		// Get the balance from the previous item.
			           		long balance;
			           		if (fTable.getItemCount() == 1) {
			           			balance = entriesContent.getStartBalance();
			           		} else {
			           			balance = 
			           				((DisplayableTransaction) 
			           						fTable
			           						.getItem(fTable.getItemCount() - 2)
			           						.getData()
									).balance;
			           		}
			           		
			           		DisplayableTransaction dTrans = new DisplayableTransaction(entry1, balance);
			           		item.setData(dTrans);
			           		
			           		// Although most properties will be blank, some, such
			           		// as the date, default to non-blank values and these
			           		// should be shown now.
			           		updateItem(item);
			           		
			           		// Add to our cached list of all transactions in the table
			           		entries.put(entry1, dTrans);
			           		
			           		// Add another blank new entry row to replace the one we
			           		// have just converted to a new transaction.
			    			TreeItem blankItem = new TreeItem(fTable, SWT.NULL);
			    			blankItem.setData(new DisplayableNewEmptyEntry());
			    			blankItem.setBackground(fTable.getItemCount() % 2 == 0 ? alternateTransactionColor
								: transactionColor);
						} else {
							return;
						}
					}

					// This is not right.  Should data not have been replaced by the
					// new dTrans object created above?
					
					IEntriesTableProperty entryData = (IEntriesTableProperty) fTable.getColumn(column).getData();
					currentCellPropertyControl = entryData.createAndLoadPropertyControl(fTable, data);
					if (currentCellPropertyControl != null) {
						createCellEditor(item, column, currentCellPropertyControl);
						
						/*
						 * If the category property, listen for changes and set
						 * the currency to be the currency of the listed account
						 * whenever the category is set to a multi-currency
						 * category and no currency is set.
						 * /
						if (entryData.getId().equals("common2.net.sf.jmoney.entry.account")) {
							currentCellPropertyControl.getControl().addListener(SWT.Selection, new Listener() {
								public void handleEvent(Event event) {
									Entry changedEntry = data.getEntryForCommon2Fields();
									Account account = changedEntry.getAccount();
									if (account instanceof IncomeExpenseAccount) {
										IncomeExpenseAccount incomeExpenseAccount = (IncomeExpenseAccount)account;
										if (incomeExpenseAccount.isMultiCurrency()
												&& changedEntry.getIncomeExpenseCurrency() == null) {
											// Find the capital account in this transaction and set
											// the currency of this income or expense to match the
											// currency of the capital entry.
											Commodity defaultCommodity = data.getEntryInAccount().getCommodity();
											if (defaultCommodity instanceof Currency) {
												changedEntry.setIncomeExpenseCurrency((Currency)defaultCommodity);
											}
										}
									}
								}
							});
						}
					}
				}
			}
		});
*/	
		
		session.getObjectKey().getSessionManager().addChangeListener(new SessionChangeAdapter() {
			public void objectInserted(ExtendableObject newObject) {
				if (newObject instanceof Entry) {
					Entry newEntry = (Entry) newObject;
					// if the entry is in this table, add it
					if (entriesContent.isEntryInTable(newEntry)) {
						addEntryToTable(newEntry);
					}

					// Even if this entry is not in this table, if one of
					// the other entries in the transaction is in this table
					// then the table view will need updating because the split
					// entry rows will need updating.
					for (Entry entry: newEntry.getTransaction().getEntryCollection()) {
						if (!entry.equals(newEntry)
								&& entries.containsKey(entry)) {
							updateEntryInTable(entry);
						}
					}
				} else if (newObject instanceof Transaction) {
					Transaction newTransaction = (Transaction) newObject;
					// Add all entries in the transaction that are to be listed as
					// an entry in this list.
					for (Entry entry: newTransaction.getEntryCollection()) {
						if (entriesContent.isEntryInTable(entry)) {
							addEntryToTable(entry);
						}
					}
				}
			}

			public void objectRemoved(ExtendableObject deletedObject) {
				if (deletedObject instanceof Entry) {
					Entry deletedEntry = (Entry) deletedObject;
					// if the entry is in this table, remove it.
					if (entries.containsKey(deletedEntry)) {
						removeEntryFromTable(deletedEntry);
					}

					// Even if this entry is not in this table, if one of
					// the other entries in the transaction is in this table
					// then the table view will need updating because the split
					// entry rows will need updating.
					for (Entry entry: deletedEntry.getTransaction().getEntryCollection()) {
						if (!entry.equals(deletedEntry)
								&& entries.containsKey(entry)) {
							updateEntryInTable(entry);
						}
					}
				} else if (deletedObject instanceof Transaction) {
					Transaction deletedTransaction = (Transaction) deletedObject;

					// TODO: This is not complete.  What happens if the entry
					// with focus is in the transaction being deleted?
					
					for (Entry deletedEntry: deletedTransaction.getEntryCollection()) {
						if (entries.containsKey(deletedEntry)) {
							removeEntryFromTable(deletedEntry);
						}
					}
				}
			}

			public void objectChanged(ExtendableObject extendableObject,
					ScalarPropertyAccessor propertyAccessor, Object oldValue,
					Object newValue) {
				if (extendableObject instanceof Entry) {
					Entry entry = (Entry) extendableObject;

					/*
					 * A property change may result in a top-level entry no
					 * longer meeting the requirements to be listed in the
					 * table. If the entry is not either the selected entry or
					 * the parent of the selected entry then the changed entry
					 * is immediately removed from the list.
					 * 
					 * However, if the changed entry is the selected top-level
					 * entry or the parent of the selected child entry then we
					 * do not remove the entry from the list. It would confuse
					 * the user if an entry disappeared while the user was
					 * editing the entry. The entry is instead removed when the
					 * entry is no longer the selected entry. This may still be
					 * a little confusing, because the entry would disappear
					 * when the user selects another entry, but the user is
					 * forced to commit the changes and should not be so
					 * surprised to see that the entry is no longer in the table
					 * once the changes are committed.
					 * 
					 * Note that the entry being changed may be the other entry
					 * in the transaction, whose properties are also diplayed on
					 * the parent row. As long as properties from the 'other'
					 * entry never affect whether an entry is listed then this
					 * code is correct. If, however, properties from the other
					 * entry or properties from the transaction affect whether
					 * an entry is listed then this code will need re-visiting.
					 */
					boolean wasInTable = entries.containsKey(entry);
					boolean isNowInTable = entriesContent.isEntryInTable(entry);
					if (wasInTable && !isNowInTable) {
						removeEntryFromTable(entry);
					} else if (!wasInTable && isNowInTable) {
						addEntryToTable(entry);
					}

					/*
					 * Find all rows on which properties of this entry are
					 * displayed and update those rows. This involves finding
					 * all entries in the same transaction that are listed in
					 * the table contents (including this entry itself if this
					 * entry is a top level entry in the table). TODO: we do not
					 * need to include this entry itself if it were just added
					 * above, but there is no harm in doing so.
					 */
					Transaction transaction = entry.getTransaction();
					for (Entry entry2: transaction.getEntryCollection()) {
						if (entries.containsKey(entry2)) {
							updateEntryInTable(entry2);
						}
					}
				}

				// When a transaction property changes, we notify the entries list
				// control once for each entry in the transaction where the entry is
				// listed as a top level entry in the table.
				// (Only rows for top level entries display transaction properties).
				if (extendableObject instanceof Transaction) {
					Transaction transaction = (Transaction) extendableObject;
					
					for (Entry entry: transaction.getEntryCollection()) {
						if (entries.containsKey(entry)) {
							updateEntryInTable(entry);
						}
					}
				}

				/*
				 * Account names and currency names affect the data displayed in
				 * the entries list. These changes are both infrequent, may
				 * involve a change to a lot of entries, and would involve
				 * finding all transactions that contain both an entry with the
				 * changed account or currency and an entry with in the account
				 * for this page. It is therefore better just to refresh all the
				 * entire entries list, but note that only the text need be
				 * refreshed.
				 * 
				 * TODO: Other properties added by plug-ins could potentially
				 * affect this view too. Should we refresh on ANY property
				 * change on ANY object class, or should we implement something
				 * more complex?
				 */
				if (propertyAccessor == AccountInfo.getNameAccessor()
						|| propertyAccessor == CommodityInfo.getNameAccessor()) {
					table.refreshContentOfAllRows();
				}
			}

			private void updateEntryInTable(Entry entry) {
				/*
				 * It is possible that the amount of this entry changed.
				 * Therefore we must refresh the balances of all later
				 * entries in the sorted list.
				 */
				EntryData data = entries.get(entry);
				updateFollowingValues(sortedEntries.indexOf(data), data.getBalance() - data.getEntry().getAmount());

				// TODO: Test to see if the amount has changed, and update the following
				// rows only if so (as the balances will have changed).
				table.refreshBalancesOfAllRows();
			}

			private void removeEntryFromTable(Entry entry) {
				EntryData data = entries.get(entry);
				
				entries.remove(entry);

				int indexToRemove = sortedEntries.indexOf(data); 
				sortedEntries.remove(indexToRemove);
				
				// Update all the later entries
				updateFollowingValues(indexToRemove, data.getBalance() - data.getEntry().getAmount());

				table.deleteRow(indexToRemove);
			}

			private void addEntryToTable(Entry entry) {
				EntryData newData = new EntryData(entry, session.getObjectKey().getSessionManager());
				
				entries.put(entry, newData);
				
				/*
				 * Find the insert point in the sorted entries.
				 * 
				 * Scan the table to find the correct index to insert this row.
				 * Because rows are likely to be inserted near the bottom of the
				 * table, we scan backwards.
				 * 
				 * Note that we stop the seach if we reach an entry in the table
				 * that has not yet been committed.  We cannot compare against such
				 * entries as we don't have a committed entry available, and also
				 * we want to be sure that all new entry rows remain at the end.
				 */
				int insertIndex = 0;
				long balance = entriesContent.getStartBalance();
				for (int i = sortedEntries.size()-1; i >= 0; i--) {
					EntryData data = sortedEntries.get(i);
					if (data.getEntry() == null
							|| rowComparator.compare(newData, data) >= 0) {
						insertIndex = i + 1;
						balance = data.getBalance();
						break;
					}
				}
				
				// Insert the entry at the appropriate place in the sorted list.
				sortedEntries.add(insertIndex, newData);
				
				updateFollowingValues(insertIndex, balance);
				
				table.insertRow(insertIndex);
			}
		}, this);

	}

	public Collection<IEntriesTableProperty> getCellList() {
		ArrayList<IEntriesTableProperty> cellList = new ArrayList<IEntriesTableProperty>();
		rootBlock.buildCellList(cellList);
		return cellList;
	}

	private void insertNewEntry() {
		Transaction transaction = session.createTransaction();
		Entry entry1 = transaction.createEntry();
		Entry entry2 = transaction.createEntry();
		entriesContent.setNewEntryProperties(entry1);
		
		/*
		 * We set the currency by default to be the currency of the
		 * top-level entry.
		 * 
		 * The currency of an entry is not applicable if the entry is an
		 * entry in a currency account or an income and expense account
		 * that is restricted to a single currency.
		 * However, we set it anyway so the value is there if the entry
		 * is set to an account which allows entries in multiple currencies.
		 * 
		 * It may be that the currency of the top-level entry is not
		 * known. This is not possible if entries in a currency account
		 * are being listed, but may be possible if this entries list
		 * control is used for more general purposes. In this case, the
		 * currency is not set and so the user must enter it.
		 */
		if (entry1.getCommodity() instanceof Currency) {
			entry2.setIncomeExpenseCurrency((Currency)entry1.getCommodity());
		}

		table.setSelection(0, entries.get(entry1).getIndex());
	}

	/**
	 * Adjust the indexes and balances of all entries that follow the given
	 * start position in the sorted list.
	 * 
	 * The EntryData objects hold the committed data, so changing an amount
	 * in an entry will not update the balances until the entry is committed.
	 * If an EntryData object represents a new entry that has never been
	 * committed then the Entry value will be null.  In that case the balance
	 * is not changed by the entry.
	 *  
	 * @param index the index of the first entry that needs updating
	 * @param balance the balance prior to the given start index
	 */
	private void updateFollowingValues(int index, long balance) {
		for (; index < sortedEntries.size(); index++) {
			EntryData data = sortedEntries.get(index);
			
				data.setIndex(index);

				if (data.getEntry() != null) {
					balance += data.getEntry().getAmount();
				}
				data.setBalance(balance);
		}
	}

	public void sort(IEntriesTableProperty sortProperty, boolean ascending) {
		
		// Sort the entries.
		rowComparator = new RowComparator(sortProperty, ascending);
		
		/*
		 * It would be efficient if we could create a sorted TreeSet and copy
		 * the entries into that. However the TreeSet object unfortunately uses
		 * the comparator not just as a comparator but also assumes that if two
		 * objects compare the same then they are the same object. This is a
		 * design flaw, as of course that is wrong. For example, if sorting by
		 * date you cannot assume that two entries are the same simply because
		 * they have the same date. The equals method should be used to
		 * determine equality. Java's own documentation says that the equals
		 * method is used to determine equality but Sun's documentation is
		 * wrong.
		 * 
		 * We therefore copy the entries into a list and then sort that.
		 */
		sortedEntries = new ArrayList<EntryData>();
		sortedEntries.addAll(entries.values());
		Collections.sort(sortedEntries, rowComparator);
		
		// Add an empty row at the end so that users can enter new entries.
		sortedEntries.add(new EntryData(null, session.getObjectKey().getSessionManager()));
		
        /*
         * Having sorted the entries, the indexes and balances must be updated.
         */
		updateFollowingValues(0, entriesContent.getStartBalance());
	}	

	/**
     * Build the list of entries to be shown in the entries list. This method
     * sets the list into <code>entries</code>. This list contains only the
     * top level entries.
     * <P>
     * The entries are unsorted.
     */
    private void buildEntryList() {
        // Note that the balances are not set at this time. This is done
        // when the data is sorted.
        entries = new HashMap<Entry, EntryData>();
        for (Entry accountEntry: entriesContent.getEntries()) {
        	EntryData data = new EntryData(accountEntry, session.getObjectKey().getSessionManager());
            if (matchesFilter(data)) {
                entries.put(accountEntry, data);
            }
        }
    }

	/**
	 * Filters work at the transaction level, not the entry level.
	 * Either the entire transaction is displayed, or none of
	 * the transaction is displayed.  If any entry in a split
	 * transaction matches the filter then the entire transaction
	 * is shown.
	 * 
	 * @param transData
	 * @return
	 */
	private boolean matchesFilter(EntryData transData) {
		if (entriesContent.filterEntry(transData)) {
			return true;
		}

		// TODO: decide if we should be searching split entries.
/*		
		if (!transData.isSimpleEntry()) {
			for (Entry entry2: transData.getSplitEntries()) {
				DisplayableEntry entryData = new DisplayableEntry(entry2,
						transData);
				if (entriesContent.filterEntry(entryData)) {
					return true;
				}
			}
		}
*/
		return false;
	}

	public Entry getSelectedEntry() {
		int row = table.getSelection();
		if (row == -1) {
			return null;
		} else {
			EntryData data = sortedEntries.get(row);
			return data.getEntry();
		}
	}

	/**
	 * Refresh the list of entries.
	 */
	public void refreshEntryList() {
		buildEntryList();

		/*
		 * Build the initial sort order. This must be done before we can create
		 * the composite table because the constructor for the composite table
		 * requires a row content provider.
		 */
	    // TODO: This is not the most efficient, nor probably correct
	    // in the general case.
	    sort(getCellList().iterator().next(), true);
	    
	}

	public void dispose() {
		table.dispose();
	}
/*
	/*
	 * Note that the transaction may have been inserted from within
	 * this page (the user pressed the 'new transaction' button in
	 * this page, or the transaction may have been inserted outside
	 * this page (for example, the user imported some transactions
	 * from a QIF file).  This method is responsible for inserting
	 * the new data into the entries list.  It must not set the
	 * current selection.  If the transaction is being inserted because
	 * the user pressed the 'new transaction' button then the action
	 * code for that button will set the transaction as the selection
	 * after this method has created the table rows for the transaction.
	 * /
	public void addEntryInAccount(Entry entry) {
		DisplayableTransaction dTrans = new DisplayableTransaction(entry, 0);

		// TODO: check that caller ensures this entry is in the entries content provider.
		
		// Do not add this entry to our view if a filter is on and the entry does
		// not match the filter.
		if (!matchesFilter(dTrans)) {
			return;
		}

		entries.put(entry, dTrans);

		// Scan the table to find the correct index to insert this row.
		// Because rows are likely to be inserted near the bottom of
		// the table, we scan backwards.
		Comparator<DisplayableTransaction> rowComparator = new RowComparator(sortColumn, sortAscending);
		int parentIndex = 0;
		long balance = entriesContent.getStartBalance();
		for (int i = fTable.getItemCount()-1; i >= 0; i--) {
			TreeItem item = fTable.getItem(i);
			Object data = item.getData();
			if (data instanceof DisplayableTransaction) {
				DisplayableTransaction dTrans2 = (DisplayableTransaction) data;
				if (rowComparator.compare(dTrans, dTrans2) >= 0) {
					parentIndex = i + 1;
					balance = dTrans2.getBalance();
					break;
				}
			}
		}
		
		// Insert the transaction and its entries now.
		TreeItem parentItem = new TreeItem(fTable, 0, parentIndex);
		parentItem.setData(dTrans);
		
		// Set the column values for this new row (note that the balance
		// column is not set.  The balance column is always set
		// later by the same code that updates all the following balances).
		for (int columnIndex = 0; columnIndex < fTable.getColumnCount(); columnIndex++) {
			IEntriesTableProperty p = (IEntriesTableProperty)fTable.getColumn(columnIndex).getData();
			if (!p.getId().equals("balance")) {
				parentItem.setText(columnIndex, p.getValueFormattedForTable(dTrans));
			}
		}

		if (!dTrans.isSimpleEntry()) {
			/*
			 * Case of an splitted or double entry. We display the transaction
			 * on the first line and the entries of the transaction on the
			 * following ones. However, the transaction line also holds the
			 * properties for the entry in this account, so display just the
			 * other entries underneath.
			 * /
			for (Entry entry2: dTrans.getSplitEntries()) {
				DisplayableEntry entryData = new DisplayableEntry(entry2, dTrans);
				TreeItem childItem = new TreeItem(parentItem, SWT.NULL);
				childItem.setData(entryData);
				updateItem(childItem);
			}
		}

		// Recalculate balances from this point onwards.
		updateBalances(parentIndex, balance);

		// Set colors from this point onwards (colors have switched).
		boolean isAlternated = ((parentIndex % 2) == 0);
		updateColors(parentIndex, isAlternated);
	}

	/**
	 * Remove a parent entry from the table.
	 * <P>
	 * The caller will alread (non-Javadoc)
	 * @see net.sf.jmoney.pages.entries.IEntriesControl#removeTransaction(net.sf.jmoney.model2.Entry)
	 * /
	private void removeEntryInAccount(Entry entry) {
		// TODO: processing is done correctly for entries being added and removed from
		// the set provided by entriesContent.  However, the filter is not kept correctly.
		// This is a problem, because the user may edit properties that make the entry
		// disappear because it no longer passes the filter.  How do we deal with this?
		
		// get the index of the row for the transaction
		int parentIndex = lookupEntryInAccount(entry);

		TreeItem parentItem = fTable.getItem(parentIndex);
		DisplayableTransaction dTrans = (DisplayableTransaction)parentItem.getData();

		// Remove from our cached set of entries
		entries.remove(dTrans.entry);
		
		// Determine the color of this row.  It is used to switch the color of
		// all the following rows.
		boolean isAlternated = (alternateTransactionColor.equals(parentItem
				.getBackground()));

		
		// Dispose it
		parentItem.dispose();

		long balance = dTrans.getBalance() - entry.getAmount();

		// Recalculate balances from this point onwards.
		updateBalances(parentIndex, balance);

		// Set colors from this point onwards (colors have switched).
		updateColors(parentIndex, isAlternated);
	}

	public void updateEntry(Entry entryInAccount, Entry changedEntry,
			PropertyAccessor changedProperty, Object oldValue, Object newValue) {
		int parentIndex = lookupEntryInAccount(entryInAccount);
		JMoneyPlugin.myAssert(parentIndex >= 0);
		TreeItem parentItem = fTable.getItem(parentIndex);
		
		// If there are two entries in the transaction
		// and the changed entry in not the listed entry
		// and the changed property was the account property
		// and the account changed from being a capital account to an income and expense account
		// or vica versa
		// then we must add or remove a child item.
		DisplayableTransaction dTrans = (DisplayableTransaction)parentItem.getData(); 
		if (dTrans.otherEntries.size() == 1
				&& !changedEntry.equals(entryInAccount)
				&& changedProperty == EntryInfo.getAccountAccessor()) {
			
			if ((oldValue instanceof CapitalAccount)
					&& !(newValue instanceof CapitalAccount)) {
				// Remove the single child item.
				parentItem.getItem(0).dispose();
			}
			if (!(oldValue instanceof CapitalAccount)
					&& (newValue instanceof CapitalAccount)) {
				// Add a child item.Remove the single child item.
				TreeItem childItem = new TreeItem(parentItem, SWT.NULL);
				childItem.setData(new DisplayableEntry(changedEntry, dTrans));

				Color colorOfNewEntry = parentItem.getBackground().equals(
						transactionColor) ? entryColor : alternateEntryColor;
				childItem.setBackground(colorOfNewEntry);
			}
		}
		
		// If this is a double entry transaction then changing the other entry
		// may affect the top-level row (the account, for example, is displayed
		// in the top-level row).
		updateItem(parentItem);

		// Update the row containing the changed entry.  This may be
		// the parent row, in which case the parent row is updated
		// twice, but that is okay.
		TreeItem item = lookupSplitEntry(parentItem, changedEntry);
		updateItem(item);

		// If the changed property is the sort property, the entry may
		// need to be moved in the sort order.  This is difficult to do
		// properly, as we do not want to mess up the user while the user
		// is editing the row.  To move a row requires the row item to be
		// disposed and a new row item inserted at the new position.
		// The easiest way may be to see if this is the selected row, 
		// and if it is, move all the rows past this row.
		// Until this is implemented, we leave the row out of order. 

		// Recalculate balances from this point onwards.
		if (changedProperty == EntryInfo.getAmountAccessor()
				&& entryInAccount.equals(changedEntry)) {

			// Determine the balance prior to this entry.  This is most easily done
			// by deducting the old amount of this entry from the old balance.
			long balance = dTrans.getBalance() - ((Long) oldValue).longValue();

			updateBalances(parentIndex, balance);
		}
	}

	/**
	 * This method is called when a transaction property is changed.
	 * This method is called once for each entry in the transaction that
	 * is a top level listed entry.
	 * /
	public void updateTransaction(Entry entryInAccount,
			PropertyAccessor changedProperty, Object oldValue, Object newValue) {
		int parentIndex = lookupEntryInAccount(entryInAccount);
		JMoneyPlugin.myAssert(parentIndex >= 0);
		TreeItem parentItem = fTable.getItem(parentIndex);
		updateItem(parentItem);

		// If the changed property is the sort property, the entry may
		// need to be moved in the sort order.  This is difficult to do
		// properly, as we do not want to mess up the user while the user
		// is editing the row.  To move a row requires the row item to be
		// disposed and a new row item inserted at the new position.
		// The easiest way may be to see if this is the selected row, 
		// and if it is, move all the rows past this row.
		// Until this is implemented, we leave the row out of order. 
	}

	/**
	 * Update the view to show the insertion of a new child
	 * entry to a given top-level entry.
	 * This includes updating the cached list of entries
	 * in the DisplayableTransaction object.
	 * /
	public void addEntry(Entry entryInAccount, Entry newEntry) {
		int parentIndex = lookupEntryInAccount(entryInAccount);
		JMoneyPlugin.myAssert(parentIndex >= 0);
		TreeItem parentItem = fTable.getItem(parentIndex);

		Transaction transaction = entryInAccount.getTransaction();
		DisplayableTransaction dTrans = (DisplayableTransaction)parentItem.getData();
		
		// Update the list of child entries in the
		// DisplayableTransaction object.
		dTrans.otherEntries.add(newEntry);
		
		Color colorOfNewEntry = parentItem.getBackground().equals(
				transactionColor) ? entryColor : alternateEntryColor;

		// If there were no DisplayableEntry rows then this was a
		// simple transaction.  It can no longer be a simple transaction
		// so add a row for entry that had been combined.  We also need
		// to update the combined row.

		if (parentItem.getItemCount() == 0) {
			// The transaction should have three entries,
			// newEntry, entryInAccount, and one other.
			// We need to find the other.
			Entry otherEntry = null;
			for (Entry entry: transaction.getEntryCollection()) {
				if (!entry.equals(entryInAccount) && !entry.equals(newEntry)) {
					if (otherEntry != null) {
						throw new RuntimeException("internal inconsistency");
					}
					otherEntry = entry;
				}
			}

			// Create row for entry that was combined.
			DisplayableEntry dEntry = new DisplayableEntry(otherEntry, dTrans);
			TreeItem newItem = new TreeItem(parentItem, SWT.NONE);
			newItem.setData(dEntry);
			updateItem(newItem);
			newItem.setBackground(colorOfNewEntry);

			// Update the transaction row.
			updateItem(parentItem);
		}

		DisplayableEntry dEntry = new DisplayableEntry(newEntry, dTrans);
		TreeItem newItem = new TreeItem(parentItem, SWT.NONE);
		newItem.setData(dEntry);
		updateItem(newItem);
		newItem.setBackground(colorOfNewEntry);
	}

	/**
	 * Update the view to show the deletion of a child
	 * entry to a given top-level entry.
	 * This includes updating the cached list of entries
	 * in the DisplayableTransaction object.
	 * /
	public void removeEntry(Entry entryInAccount, Entry deletedEntry) {
		// get the index of the row for the transaction
		int transIndex = lookupEntryInAccount(entryInAccount);

		TreeItem parentItem = fTable.getItem(transIndex);
		DisplayableTransaction dTrans = (DisplayableTransaction) parentItem
				.getData();

		// Update the list of child entries in the
		// DisplayableTransaction object.
		dTrans.otherEntries.remove(deletedEntry);
		
		// get the child row item for the entry
		TreeItem childItem = lookupSplitEntry(parentItem, deletedEntry);

		// Dispose it
		childItem.dispose();

		// If the transaction is now a simple transaction,
		// dispose the remaining item, and update the transaction
		// row.
		if (dTrans.isSimpleEntry()) {
			JMoneyPlugin.myAssert(parentItem.getItemCount() == 1);
			parentItem.getItem(0).dispose();
			updateItem(parentItem);
		}
	}
*/
	/**
	 * Add a selection listener.  The listener will be notified whenever
	 * the selected row in the tree changes.
	 * <P>
	 * The event will contain the EntryData object as data.
	 */
	public void addSelectionListener(EntryRowSelectionListener tableSelectionListener) {
		// We do not add the listener directly to the tree control.
		// Instead we keep our own list of listeners and we add our
		// own listener to the tree control.  When our own listener is
		// notified of a change in selection, the listener in turn
		// notifies the listeners added thru this method.
		
		// This approach is necessary because not all selection changes
		// the tree should be passed on to the listeners added thru this
		// method.  The selection change may be ignored if, for example,
		// there is an error in the data on the previous row.  Also,
		// when a selection change is initiated by the code from within
		// a listener, SWT does not seem to generate a SelectionListener
		// event, but we want one generated and by keeping our own list
		// of listeners, we can do that.
		
		selectionListeners.add(tableSelectionListener);
	}

	/**
	 * A private utility method for notifying our listeners of
	 * a selection change event.
	 * 
	 * @param event the event to be passed on to the listeners
	 */
	private void fireSelectionChanges(EntryData data) {
		for (EntryRowSelectionListener listener: selectionListeners) {
			listener.widgetSelected(data);
		}
	}

	private void fireRowDefaultSelection(EntryData data) {
		for (EntryRowSelectionListener listener: selectionListeners) {
			listener.widgetDefaultSelected(data);
		}
	}

	/* (non-Javadoc)
	 * @see net.sf.jmoney.pages.entries.IEntriesControl#getControl()
	 */
	public Control getControl() {
		return table;
	}

	private void showMessage (String message) {
		MessageDialog.openWarning(
				table.getShell(),
				"Disabled Action Selected",
				message);
	}

	private class RowComparator implements Comparator<EntryData> {
		private IEntriesTableProperty sortProperty;
		private boolean ascending;
		
		RowComparator(IEntriesTableProperty sortProperty, boolean ascending) {
			this.sortProperty = sortProperty;
			this.ascending = ascending;
		}
		
		public int compare(EntryData dTrans1, EntryData dTrans2) {
			int result = sortProperty.compare(dTrans1, dTrans2);
			return ascending ? result : -result;
		}
	}

	public Session getSession() {
		return session;
	}
}
