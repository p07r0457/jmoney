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

package net.sf.jmoney.pages.entries;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Vector;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.fields.AccountInfo;
import net.sf.jmoney.fields.CommodityInfo;
import net.sf.jmoney.fields.EntryInfo;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.IncomeExpenseAccount;
import net.sf.jmoney.model2.PropertyAccessor;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.SessionChangeAdapter;
import net.sf.jmoney.model2.Transaction;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * An implementation of IEntriesControl that displays the entries
 * in a table tree.  The tree contains one top level row for each
 * entry in the account.  The child rows show the other entries in
 * the transaction.
 * <P>
 * It is important to understand how object properties are mapped
 * to cells in the table.  There is one top-level row in the tree
 * control for each entry in the list.  In general, the other entries
 * in the transaction are child rows.  Note that it is possible that
 * one transaction may contain two or more entries in the same account.
 * For example, a user may write a check for a deposit and a check for
 * the balance of a purchase.  The user may enter this as a single 
 * 'split' transaction containing the two checks and a single entry
 * containing the total purchase amount in an expense category.
 * In this situation, the single transaction will have two top-level
 * rows (one for each check).  Expanding one of the rows with show
 * two child entries: the other check and the purchase entry.  
 * <P>
 * A simple transaction is a transaction
 * that contains two entries where one entry is in a capital account
 * and the other entry is in an income and expense account.  This
 * is a common form of transaction and therefore we make a special
 * effort to display such transactions on a single row.
 * <P>
 * Some of the entry properties apply only to entries in capital
 * accounts, some apply only to entries in income and expense
 * accounts, and some apply to entries in both types of accounts.
 * In a simple transaction, all the properties from both entries
 * have a column.  Properties from the transaction are displayed
 * in the top level and also have a column.  So, we have a column
 * for all the following:
 * <UL>
 * <LI>Every property in the transaction</LI>
 * <LI>Every property in an entry that may be applicable given
 * 	   the account being listed, with the exception
 *     that if all the entries are in the same account (true in
 *     most uses of this class) then no column exists for the account 
 *     property (as such a column would contain the same account in
 *     every row and therefore not be of much use)</LI>
 * <LI>Every property in an entry that may be applicable when the
 *     entry is in an income and expense account</LI>
 * </UL>
 * 
 * Some properties may be applicable for both entries in the
 * capital account and for entries in income and expense accounts.
 * There will be two columns for such properties.  When an entry
 * is being show on its own child row, we have the choice of which
 * of the two columns we use for the property.  We chose to show
 * it in the column that would, for a simple transaction, be used
 * to show the property of the entry in the income and expense account.
 * This makes the child rows look more similar and also ensures that
 * a transfer account is shown.
 * <P>
 * Each column is managed by an IEntriesTableProperty object.
 * Each row is managed by an IDisplayableItem object.
 * (Both classes of object are set as the data on the TreeColumn
 * and TreeItem items respectively).  These two classes must
 * work together to determine the contents of a cell.
 * <P>
 * The credit, debit, and balance columns are special cases
 * and special implementations of IEntriesTableProperty handle those
 * three columns.  The other columns fall into one of the above
 * three categories.  The rest of this explanation applies only
 * to the latter class of columns.
 * <P>   
 * A request for cell contents (whether for displaying text
 * or for creating a cell editor) goes first to the IEntriesTableProperty
 * object.  That object then calls one of the following three
 * methods on the IDisplayableItem row, depending on which class
 * of column it is:
 * <UL>
 * <LI>getTransactionForTransactionFields</LI>
 * <LI>getEntryForAccountFields</LI>
 * <LI>getEntryForOtherFields</LI>
 * </UL>
 * The above three methods all return the object that
 * contains the value associated with the cell (or null
 * if no property value is associated with that cell).
 * As the IEntriesTableProperty object has the property
 * accessor, the property value associated with the cell
 * can be got and set.
 */
public class EntriesTree {

	// The darker purple and green lines for the listed entry in each transaction
	protected static final Color transactionColor = new Color(Display
			.getCurrent(), 235, 235, 255);

	protected static final Color alternateTransactionColor = new Color(Display
			.getCurrent(), 235, 255, 237);

	// The lighter colors for the sub-entry lines
	protected static final Color entryColor = new Color(Display.getCurrent(),
			245, 245, 255);

	protected static final Color alternateEntryColor = new Color(Display
			.getCurrent(), 245, 255, 255);

	protected IEntriesContent entriesContent;

	protected Tree fTable;

	private TreeEditor editor;
	
	/**
	 * List of entries to show in the table. Only the top level entries are
	 * included, the other entries in the transactions, which are shown as child
	 * items, are not in this list. The elements are not sorted.
	 * <P>
	 * Element: DisplayableTransaction
	 */
	Vector entries;

	private TreeItem previouslySelectedItem = null;

	/**
	 * The column on which the items are currently sorted
	 */
	protected TreeColumn sortColumn;

	/**
	 * true if items are sorted in an ascending order;
	 * false if items are sorted in a descending order
	 */
	protected boolean sortAscending;

	protected IPropertyControl currentCellPropertyControl = null;

	private Entry newEntryRowEntry;

	public EntriesTree(final Composite container, FormToolkit toolkit,
			final IEntriesContent entriesContent, final Session session) {
		this.entriesContent = entriesContent;

		fTable = new Tree(container, SWT.FULL_SELECTION | SWT.BORDER
				| SWT.H_SCROLL | SWT.HIDE_SELECTION);

		GridData gridData = new GridData(GridData.FILL_BOTH);
		gridData.heightHint = 100;
		gridData.widthHint = 100;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		fTable.setLayoutData(gridData);

        // Create the button area
		Composite buttonArea = toolkit.createComposite(container);
		
		// Note that the buttons touch each other and also touch
		// the table above.  This makes it clearer that the buttons
		// are tightly associated with the table.
		RowLayout layoutOfButtons = new RowLayout();
		layoutOfButtons.fill = false;
		layoutOfButtons.justify = true;
		layoutOfButtons.marginTop = 0;
		layoutOfButtons.spacing = 0;
		buttonArea.setLayout(layoutOfButtons);
		
        // Create the 'add transaction' button.
        Button addButton = toolkit.createButton(buttonArea, "New Transaction", SWT.PUSH);
        addButton.addSelectionListener(new SelectionAdapter() {
           public void widgetSelected(SelectionEvent event) {
           		// Commit any previous transaction
//TODO:           		fPage.transactionManager.commit();
           		
           		Transaction transaction = session.createTransaction();
           		Entry entry1 = transaction.createEntry();
           		Entry entry2 = transaction.createEntry();
           		entriesContent.setNewEntryProperties(entry1);
           		
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
    			if (entry1.getCommodity() instanceof Currency) {
        			entry2.setIncomeExpenseCurrency((Currency)entry1.getCommodity());
    			}
    			
           		// Select entry1 in the entries list.
                setSelection(entry1, entry1);
           }
        });

        // Create the 'delete transaction' button.
        Button deleteButton = toolkit.createButton(buttonArea, "Delete Transaction", SWT.PUSH);
        deleteButton.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent event) {
        		Entry selectedEntry = getSelectedEntry();
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
        		Entry selectedEntry = getSelectedEntryInAccount();
        		if (selectedEntry != null) {
        			closeCellEditor();
        			
        			Transaction transaction = selectedEntry.getTransaction();
        			Entry newEntry = transaction.createEntry();
        			
        			long total = 0;
        			Commodity commodity = null;
        			for (Iterator iter = transaction.getEntryCollection().iterator(); iter.hasNext(); ) {
        				Entry entry = (Entry)iter.next();
        				
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
        			
        			transaction.getSession().registerUndoableChange("New Split");
        			
               		// Select the new entry in the entries list.
                    setSelection(selectedEntry, newEntry);
        		} else {
        			MessageDialog waitDialog = new MessageDialog(
        					container.getShell(),
        					"Disabled Action Selected",
        					null, // accept the default window icon
        					"You cannot add a new split to an entry until you have selected an entry from the above list.",
        					MessageDialog.INFORMATION,
        					new String[] { IDialogConstants.OK_LABEL }, 0);
        			waitDialog.open();
        		}
           }
        });

        // Create the 'delete split' button.
        Button deleteSplitButton = toolkit.createButton(buttonArea, "Delete Split", SWT.PUSH);
        deleteSplitButton.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent event) {
        		Entry selectedEntryInAccount = getSelectedEntryInAccount();
        		Entry selectedEntry = getSelectedEntry();
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
        		Entry selectedEntryInAccount = getSelectedEntryInAccount();
				TransactionDialog dialog = new TransactionDialog(
						container.getShell(),
						selectedEntryInAccount,
						session);
				dialog.open();
        	}
        });
        
		
		
		
		
		TableLayout tlayout = new TableLayout();

		int index = 0;
		for (Iterator iter = entriesContent.getAllEntryDataObjects().iterator(); iter
				.hasNext();) {
			IEntriesTableProperty entriesSectionProperty = (IEntriesTableProperty) iter
					.next();
			addColumn(entriesSectionProperty, index++);
		}

		TreeColumn col;

		col = new TreeColumn(fTable, SWT.RIGHT);
		col.setText("Debit");
		col.setData("layoutData", new ColumnWeightData(2, 70));
		col.setData(entriesContent.getDebitColumnManager());

		col = new TreeColumn(fTable, SWT.RIGHT);
		col.setText("Credit");
		col.setData("layoutData", new ColumnWeightData(2, 70));
		col.setData(entriesContent.getCreditColumnManager());

		col = new TreeColumn(fTable, SWT.RIGHT);
		col.setText("Balance");
		col.setData("layoutData", new ColumnWeightData(2, 70));
		col.setData(entriesContent.getBalanceColumnManager());

		fTable.setLayout(tlayout);
		fTable.setHeaderVisible(true);
		fTable.setLinesVisible(true);

		// Set sort column to first column (this is always the transaction date)
		sortColumn = fTable.getColumn(0);
		sortAscending = true;

		// Fetch and sort the list of top level entries to display.
		buildEntryList();

		// Add the content.  The table is empty (no items)
		// before this method is called, so the item fetcher
		// simply adds news items to the end of the table.
		setTableItems();
		
		// Create the editor.
		editor = new TreeEditor(fTable);
		//The editor must have the same size as the cell and must
		//not be any smaller than 50 pixels.
		editor.horizontalAlignment = SWT.LEFT;
		editor.grabHorizontal = true;
		editor.minimumWidth = 50;

		fTable.addListener(SWT.MenuDetect, new Listener() {
			public void handleEvent(Event event) {
				// Although use of this event would be preferable to use of the MouseDown
				// event, the co-ordinates of the mouse event are not given relative to
				// the table control and therefore it would be non-trivial to determine
				// the column to which the pop-up menu applies.  We therefore process the
				// pop-up menu in the MouseDown event.
				System.out.println("menu detect: " + event.x + ", " + event.y);
			}
		});

		fTable.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent event) {
				System.out.println("mouse: " + event.x + ", " + event.y);
				
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
					// Go into edit mode only if the user clicked on a field in the
					// selected row or on the new transaction row.
					if (item != previouslySelectedItem) {
						previouslySelectedItem = item;
						if (item.getData() instanceof DisplayableNewEmptyEntry) {
							// Replace with a real transaction.
			           		
			           		// Commit any previous transaction
//TODO:			           		fPage.transactionManager.commit();
			           		
			           		Transaction transaction = session.createTransaction();
			           		Entry entry1 = transaction.createEntry();
			           		Entry entry2 = transaction.createEntry();
			           		
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
			           		
			           		item.setData(new DisplayableTransaction(entry1, 0));
			           		
			           		// Although most properties will be blank, some, such
			           		// as the date, default to non-blank values and these
			           		// should be shown now.
			           		updateItem(item);
						} else {
							return;
						}
					}

					IEntriesTableProperty entryData = (IEntriesTableProperty) fTable.getColumn(column).getData();
					IDisplayableItem data = (IDisplayableItem) item.getData();
					currentCellPropertyControl = entryData.createAndLoadPropertyControl(fTable, data);
					if (currentCellPropertyControl != null) {
						createCellEditor(item, column, currentCellPropertyControl);
					}
				} else {
					// Mouse button other than number 1 was pressed.
					// Bring up a pop-up menu.
					// It would be a more consistent interface if this menu were
					// linked to the column header.  However, TableColumn has
					// no setMenu method, nor does the column header respond to
					// SWT.MenuDetect nor any other event when right clicked.
					// This code works but does not follow the popup-menu conventions
					// on even one platform!

					Menu popupMenu = new Menu(fTable.getShell(), SWT.POP_UP);

					MenuItem removeColItem = new MenuItem(popupMenu, SWT.NONE);

					MenuItem shiftColLeftItem = new MenuItem(popupMenu,
							SWT.NONE);

					MenuItem shiftColRightItem = new MenuItem(popupMenu,
							SWT.NONE);

					Object[] messageArgs = new Object[] { fTable.getColumn(
							column).getText() };

					removeColItem.setText(new java.text.MessageFormat(
							"Remove {0} column", java.util.Locale.US)
							.format(messageArgs));
					shiftColLeftItem.setText(new java.text.MessageFormat(
							"Move {0} column left", java.util.Locale.US)
							.format(messageArgs));
					shiftColRightItem.setText(new java.text.MessageFormat(
							"Move {0} column right", java.util.Locale.US)
							.format(messageArgs));

					removeColItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent e) {
							fTable.getColumn(column).dispose();
						}
					});

					shiftColLeftItem
							.addSelectionListener(new SelectionAdapter() {
								public void widgetSelected(SelectionEvent e) {
									if (column > 1) {
										IEntriesTableProperty entryData = (IEntriesTableProperty) fTable
												.getColumn(column).getData();
										removeColumn(column);
										addColumn(entryData, column - 1);
									}
								}
							});

					shiftColRightItem
							.addSelectionListener(new SelectionAdapter() {
								public void widgetSelected(SelectionEvent e) {
									if (column < fTable.getColumnCount() - 1) {
										IEntriesTableProperty entryData = (IEntriesTableProperty) fTable
												.getColumn(column).getData();
										removeColumn(column);
										addColumn(entryData, column + 1);
									}
								}
							});

					MenuItem separatorItem = new MenuItem(popupMenu,
							SWT.SEPARATOR);

					for (Iterator iter = entriesContent
							.getAllEntryDataObjects().iterator(); iter
							.hasNext();) {
						final IEntriesTableProperty entriesSectionProperty = (IEntriesTableProperty) iter
								.next();
						boolean found = false;
						for (int index = 0; index < fTable.getColumnCount(); index++) {
							IEntriesTableProperty entryData2 = (IEntriesTableProperty) (fTable
									.getColumn(index).getData());
							if (entryData2 == entriesSectionProperty) {
								found = true;
								break;
							}
						}

						if (!found) {
							Object[] messageArgs2 = new Object[] { entriesSectionProperty
									.getText() };

							MenuItem addColItem = new MenuItem(popupMenu,
									SWT.NONE);
							addColItem.setText(new java.text.MessageFormat(
									"Add {0} column", java.util.Locale.US)
									.format(messageArgs2));

							addColItem
									.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(
												SelectionEvent e) {
											addColumn(entriesSectionProperty,
													Math.max(1, column));
										}
									});
						}
					}

					popupMenu.setVisible(true);
				}
			}
		});

		session.addSessionChangeListener(new SessionChangeAdapter() {
			public void objectAdded(ExtendableObject newObject) {
				if (newObject instanceof Entry) {
					Entry newEntry = (Entry) newObject;
					// if the entry is in this table, add it
					if (entriesContent.isEntryInTable(newEntry)) {
						addEntryInAccount(newEntry);
					}

					// Even if this entry is not in this table, if one of
					// the other entries in the transaction is in this table
					// then the table view will need updating because the split
					// entry rows will need updating.
					for (Iterator iter = newEntry.getTransaction()
							.getEntryCollection().iterator(); iter.hasNext();) {
						Entry entry = (Entry) iter.next();
						if (!entry.equals(newEntry)
								&& entriesContent.isEntryInTable(entry)) {
							addEntry(entry, newEntry);
						}
					}
				}
			}

			public void objectDeleted(ExtendableObject deletedObject) {
				if (deletedObject instanceof Entry) {
					Entry deletedEntry = (Entry) deletedObject;
					// if the entry is in this table, remove it.
					if (entriesContent.isEntryInTable(deletedEntry)) {
						removeEntryInAccount(deletedEntry);
					}

					// Even if this entry is not in this table, if one of
					// the other entries in the transaction is in this table
					// then the table view will need updating because the split
					// entry rows will need updating.
					for (Iterator iter = deletedEntry.getTransaction()
							.getEntryCollection().iterator(); iter.hasNext();) {
						Entry entry = (Entry) iter.next();
						if (!entry.equals(deletedEntry)
								&& entriesContent.isEntryInTable(entry)) {
							removeEntry(entry, deletedEntry);
						}
					}
				} else if (deletedObject instanceof Transaction) {
					Transaction deletedTransaction = (Transaction) deletedObject;

					for (Iterator iter = deletedTransaction
							.getEntryCollection().iterator(); iter.hasNext();) {
						Entry entry = (Entry) iter.next();
						if (entriesContent.isEntryInTable(entry)) {
							removeEntryInAccount(entry);
						}
					}
				}
			}

			public void objectChanged(ExtendableObject extendableObject,
					PropertyAccessor propertyAccessor, Object oldValue,
					Object newValue) {
				if (extendableObject instanceof Entry) {
					Entry entry = (Entry) extendableObject;

					// If we have been told to ignore this new entry
					// then we had better do so.
					if (entry.equals(newEntryRowEntry)) {
						newEntryRowEntry = null;
						return;
					}
					
					// A property change may result in a top-level entry no longer
					// meeting the requirements to be listed in the table.
					// If the entry is not the selected entry (or, if a split entry
					// is selected, the top-level entry of that split entry) then
					// the changed entry is immediately removed from the list.
					
					// However, if the changed entry is the selected top-level entry
					// (including being the top-level entry for a selected child entry)
					// then we do not remove the entry from the list.  It would confuse
					// the user if an entry disappeared while the user was editing the
					// entry.  The entry is instead removed when the entry is no longer
					// the selected entry.  This may still be a little confusing, because
					// the entry would disappear when the user selects another entry,
					// but the user is forced to commit the changes and should not be
					// so surprised to see that the entry is no longer in the table once
					// the changes are committed.
					
					// Note that the entry being changed may be the other entry
					// in the transaction, whose
					// properties are also diplayed on the parent row. As long
					// as properties
					// from the 'other' entry never affect whether an entry is
					// listed
					// then this
					// code is correct. If, however, properties from the other
					// entry
					// or properties from the transaction affect whether an
					// entry is
					// listed then this code will need re-visiting.
					if (!entry.equals(getSelectedEntryInAccount())) {
						boolean wasInTable = entriesContent.isEntryInTable(
								entry, propertyAccessor, oldValue);
						boolean isNowInTable = entriesContent.isEntryInTable(
								entry, propertyAccessor, newValue);
						if (wasInTable && !isNowInTable) {
							removeEntryInAccount(entry);
						} else if (!wasInTable && isNowInTable) {
							addEntryInAccount(entry);
						}
					}

					// Find all rows on which properties of this entry are
					// displayed
					// and update those rows. This involves finding all entries
					// in
					// the same transaction that are listed in the table
					// contents
					// (including this entry itself if this entry is a top level
					// entry
					// in the table).
					// TODO: we do not need to include this entry itself if it
					// were
					// just added or just removed above. This code is not quite
					// perfect but probably works.
					Transaction transaction = entry.getTransaction();
					for (Iterator iter = transaction.getEntryCollection()
							.iterator(); iter.hasNext();) {
						Entry entry2 = (Entry) iter.next();
						if (entriesContent.isEntryInTable(entry2)) {
							updateEntry(entry2, entry, propertyAccessor,
									oldValue, newValue);
						}
					}
				}

				// When a transaction property changes, we notify the entries list
				// control once for each entry in the transaction where the entry is
				// listed as a top level entry in the table.
				// (Only rows for top level entries display transaction properties).
				if (extendableObject instanceof Transaction) {
					Transaction transaction = (Transaction) extendableObject;
					for (Iterator iter = transaction.getEntryCollection()
							.iterator(); iter.hasNext();) {
						Entry entry = (Entry) iter.next();
						if (entriesContent.isEntryInTable(entry)) {
							updateTransaction(entry, propertyAccessor,
									oldValue, newValue);
						}
					}
				}

				// Account names and currency names affect the data displayed in the
				// entries list.  These changes are both infrequent, may involve a
				// change to a lot of entries, and would involve finding all transactions
				// that contain both an entry with the changed account or currency
				// and an entry with in the account for this page.  It is therefore
				// better just to refresh all the entire entries list, but note that
				// only the text need be refreshed.
				if (propertyAccessor == AccountInfo.getNameAccessor()
						|| propertyAccessor == CommodityInfo.getNameAccessor()) {
					refreshLabels();
				}
			}
		}, container);
	}

	private void createCellEditor(final TreeItem item, final int column, IPropertyControl propertyControl) {
		currentCellPropertyControl = propertyControl;

		Control c = currentCellPropertyControl.getControl();
			editor.setEditor(c, item, column);
			c.setFocus();
			
			if (c instanceof Composite) {
				Composite composite = (Composite)c;

				// Let's see which control has the focus, and listen
				// for the traversal events from that control (listening
				// for traversals from the composite control will not work
				// because none occur).
				
				// This does mean that the user cannot use the tab key to move
				// between controls within the composite.  For example, if a date
				// control has focus then the user cannot tab to the button at the
				// right side of the date control.  Users are more likely to expect
				// the tab key to move to the next property, so that is the trade off.
				Control [] childControls = composite.getChildren();
				for (int i = 0; i < childControls.length; i++) {
					if (childControls[i].isFocusControl()) {
						c = childControls[i];
						break;
					}
				}
			}
			
			c.addTraverseListener(new TraverseListener() {
				public void keyTraversed(TraverseEvent event) {
					if (event.detail == SWT.TRAVERSE_TAB_NEXT) {
						// Clean up this editor control
						closeCellEditor();
						
						// Find the next editable property in the row
						IDisplayableItem data = (IDisplayableItem) item.getData();
						IPropertyControl nextPropertyControl = null;
						int nextColumn = column;
						while (nextPropertyControl == null
								&& nextColumn + 1 < fTable.getColumnCount()) {
							nextColumn++;
							IEntriesTableProperty entryData = (IEntriesTableProperty) fTable.getColumn(nextColumn).getData();
							nextPropertyControl = entryData.createAndLoadPropertyControl(fTable, data);
						}
						
						// Create the next control
						if (nextPropertyControl != null) {
							createCellEditor(item, nextColumn, nextPropertyControl);
						}
					} else if (event.detail == SWT.TRAVERSE_TAB_PREVIOUS) {
						// Clean up this editor control
						closeCellEditor();
						
						// Find the previous editable property in the row
						IDisplayableItem data = (IDisplayableItem) item.getData();
						IPropertyControl nextPropertyControl = null;
						int nextColumn = column;
						while (nextPropertyControl == null
								&& nextColumn - 1 >= 0) {
							nextColumn--;
							IEntriesTableProperty entryData = (IEntriesTableProperty) fTable.getColumn(nextColumn).getData();
							nextPropertyControl = entryData.createAndLoadPropertyControl(fTable, data);
						}
						
						// Create the next control
						if (nextPropertyControl != null) {
							createCellEditor(item, nextColumn, nextPropertyControl);
						}
					}
				}
			});
	}

	/**
	 * Set the content into the table.  If this is the first time this
	 * method is called then the table will be empty and a TableItem
	 * must be created for each row in the table.  If this is a subsequent
	 * call then TableItem objects will already exist and we re-use them,
	 * creating extra at the end or disposing excess if necessary. 
	 */
	private void setTableItems() {
		// Sort the entries.
		Comparator rowComparator = new RowComparator(sortColumn, sortAscending);
		DisplayableTransaction [] sortedEntries = new DisplayableTransaction [entries.size()];
		sortedEntries = (DisplayableTransaction [])entries.toArray(sortedEntries);
		Arrays.sort(sortedEntries, rowComparator); 

		/*
		 * Alternate the color of the entries to improve the readibility 
		 */
		boolean isAlternated = true;
		long saldo = entriesContent.getStartBalance();

		int itemIndex = 0;

		for (int i = 0; i < sortedEntries.length; i++) {
			DisplayableTransaction data = sortedEntries[i];
			
			isAlternated = !isAlternated;

			// Get the next item, using existing items or creating
			// items if there is not an item already at this index.
			TreeItem item;
			if (itemIndex < fTable.getItemCount()) {
				item = fTable.getItem(itemIndex++);
			} else {
				itemIndex++;
				item = new TreeItem(fTable, SWT.NULL);
			}

			saldo = saldo + data.getEntryForAccountFields().getAmount();
			data.setBalance(saldo);
			item.setData(data);
			updateItem(item);

			item.setBackground(isAlternated ? alternateTransactionColor
					: transactionColor);

			// Dispose of all children of this item.
			TreeItem[] childItems = item.getItems();
			for (int j = 0; j < childItems.length; j++) {
				childItems[j].dispose();
			}

			if (data.hasSplitEntries()) {
				// Case of an splitted entry. We display the transaction and
				// account entry on the first line and the other entries of the
				// transaction on the following ones.
				Iterator itSubEntries = data.getSplitEntryIterator();
				while (itSubEntries.hasNext()) {
					Entry entry2 = (Entry) itSubEntries.next();
					DisplayableEntry entryData = new DisplayableEntry(entry2,
							data);
					TreeItem item2 = new TreeItem(item, 0);
					item2.setData(entryData);
					updateItem(item2);

					item2.setBackground(isAlternated ? alternateEntryColor
							: entryColor);
				}
			}
		}

		// an empty line to have the possibility to enter a new entry
		TreeItem item3;
		if (itemIndex < fTable.getItemCount()) {
			item3 = fTable.getItem(itemIndex++);
		} else {
			itemIndex++;
			item3 = new TreeItem(fTable, SWT.NULL);
		}
		item3.setData(new DisplayableNewEmptyEntry());
		updateItem(item3);

		isAlternated = !isAlternated;
		item3.setBackground(isAlternated ? alternateTransactionColor
				: transactionColor);

		// If there were more items in the table than needed,
		// dispose of the excess.
		while (itemIndex < fTable.getItemCount()) {
			fTable.getItem(itemIndex).dispose();
		}
	}

	/**
	 * @param entriesSectionProperty
	 * @param columnIndex
	 */
	private void addColumn(IEntriesTableProperty entriesSectionProperty,
			int columnIndex) {
		final TreeColumn col = new TreeColumn(fTable, SWT.NULL, columnIndex);
		col.setText(entriesSectionProperty.getText());

		col.setData(entriesSectionProperty);

		col.setData("layoutData", new ColumnWeightData(entriesSectionProperty
				.getWeight(), entriesSectionProperty.getMinimumWidth()));

		// Re-calculate the layout.  SWT does not do this automatically when a
		// column is added, resulting in the new column being set to a zero width.
		fTable.layout(true);

		col.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				if (col == sortColumn) {
					sortAscending = !sortAscending;
					col.setImage(JMoneyPlugin.createImageDescriptor(
							sortAscending ? "icons/ArrowUp.gif"
									: "icons/ArrowDown.gif").createImage());
				} else {
					sortColumn.setImage(null);
					sortColumn = col;
					sortAscending = true;
					col.setImage(JMoneyPlugin.createImageDescriptor(
							"icons/ArrowUp.gif").createImage());
				}

				// If the user was editing a cell when the sort order was changed,
				// close the edit control.
				closeCellEditor();
				
				setTableItems();
			}
		});

		// Update the contents of the column.  This is not necessary during initial
		// column setup because columns are setup before columns.  When columns are
		// inserted or moved, however, this is necessary.
		for (int rowIndex = 0; rowIndex < fTable.getItemCount(); rowIndex++) {
			TreeItem item = fTable.getItem(rowIndex);
			IDisplayableItem data = (IDisplayableItem) item.getData();
			item.setText(columnIndex, entriesSectionProperty
					.getValueFormattedForTable(data));
		}
	}

	/**
	 * Removes a column from the table.
	 * 
	 * @param colIndex
	 */
	private void removeColumn(int colIndex) {
		fTable.getColumn(colIndex).dispose();
	}

	/**
	 * 
	 */
	protected void closeCellEditor() {
		Control oldEditor = editor.getEditor();
		if (oldEditor != null) {
			// Save the value from the edit control.
			// TODO: figure out the sequence of events that lead to
			// currentCellPropertyControl being null and clean this up.
			if (currentCellPropertyControl != null) {
				currentCellPropertyControl.save();
				oldEditor.dispose();
				currentCellPropertyControl = null;
			}
		}
	}

	/**
	 * Determine the column in which the mouse was clicked
	 * @param pt
	 * @param item
	 * @return the index of the column
	 */
	protected int getColumn(Point pt, TreeItem item) {
		int i;
		for (i = 0; i < fTable.getColumnCount(); i++) {
			Rectangle rect = item.getBounds(i);
			if (rect.contains(pt)) {
				break;
			}
		}

		if (i == fTable.getColumnCount()) {
			// No column sucessfully hit.
			return -1;
			//throw new RuntimeException("hit test failure");
		}
		return i;
	}

	/**
	 * Build the list of entries to be shown in the entries
	 * list.  This method sets the list into <code>entries</code>.
	 * This list contains only the top level entries.
	 * <P>
	 * The entries are unsorted.
	 */
	private void buildEntryList() {
		// Note that the balances are not set at this time.  This is done
		// when the data is set into the table.  It is just as easy
		// and efficient to do it then and that reduces the effort
		// to keep the balances updated.
		Collection accountEntries = entriesContent.getEntries();

		entries = new Vector();
		int i = 0;
		for (Iterator iter = entriesContent.getEntries().iterator(); iter
				.hasNext();) {
			Entry accountEntry = (Entry) iter.next();
			DisplayableTransaction data = new DisplayableTransaction(
					accountEntry, 0);
			if (matchesFilter(data)) {
				entries.add(data);
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
	private boolean matchesFilter(DisplayableTransaction transData) {
		Transaction trans = transData.getTransactionForTransactionFields();

		if (entriesContent.filterEntry(transData)) {
			return true;
		}

		if (trans.hasMoreThanTwoEntries()) {
			Iterator itSubEntries = trans.getEntryCollection().iterator();
			while (itSubEntries.hasNext()) {
				Entry entry2 = (Entry) itSubEntries.next();
				if (!entry2.equals(transData.getEntryForAccountFields())) {
					DisplayableEntry entryData = new DisplayableEntry(entry2,
							transData);
					if (entriesContent.filterEntry(entryData)) {
						return true;
					}
				}
			}
		}

		return false;
	}

	public Entry getSelectedEntryInAccount() {
		if (fTable.getSelectionCount() != 1) {
			return null;
		}

		Object selectedObject = fTable.getSelection()[0].getData();

		JMoneyPlugin.myAssert(selectedObject != null);
		if (selectedObject != null) {
			IDisplayableItem data = (IDisplayableItem) selectedObject;
			if (selectedObject instanceof DisplayableTransaction) {
				DisplayableTransaction transactionData = (DisplayableTransaction) data;
				return transactionData.getEntryForAccountFields();
			}
			if (selectedObject instanceof DisplayableEntry) {
				DisplayableEntry entryData = (DisplayableEntry) data;
				return entryData.getDisplayableTransaction()
						.getEntryForAccountFields();
			}
		}

		return null;
	}

	public Entry getSelectedEntry() {
		if (fTable.getSelectionCount() != 1) {
			return null;
		}

		Object selectedObject = fTable.getSelection()[0].getData();

		if (selectedObject != null) {
			IDisplayableItem data = (IDisplayableItem) selectedObject;
			if (selectedObject instanceof DisplayableTransaction) {
				DisplayableTransaction transactionData = (DisplayableTransaction) data;
				return transactionData.getEntryForAccountFields();
			}
			if (selectedObject instanceof DisplayableEntry) {
				DisplayableEntry entryData = (DisplayableEntry) data;
				return entryData.getEntryForThisRow();
			}
		}

		return null;
	}

	/**
	 * Class representing a top level entry in the list.
	 * <P>
	 * Note that, despite the name of this class, it is entries and
	 * not transactions that are listed.  For example, if a transaction
	 * has two entries in the account then that transaction will appear
	 * twice in the list.
	 */	public class DisplayableTransaction implements IDisplayableItem {
		private Entry entry;

		private long balance;

		/**
		 * A cache of the entries in this transaction excluding
		 * the entry itself.
		 */
		private Vector otherEntries = new Vector();

		public DisplayableTransaction(Entry entry, long saldo) {
			this.entry = entry;
			this.balance = saldo;
			
			Iterator itSubEntries = entry.getTransaction().getEntryCollection()
			.iterator();
			while (itSubEntries.hasNext()) {
				Entry entry2 = (Entry) itSubEntries.next();
				if (!entry2.equals(entry)) {
					otherEntries.add(entry2);
				}
			}
		}

		/**
		 * @return
		 */
		public boolean hasSplitEntries() {
			return otherEntries.size() > 1
			|| ((Entry)otherEntries.firstElement()).getAccount() instanceof CapitalAccount;
		}

		/**
		 * @return
		 */
		public Iterator getSplitEntryIterator() {
			return otherEntries.iterator();
		}

		public Transaction getTransactionForTransactionFields() {
			return entry.getTransaction();
		}

		public Entry getEntryForAccountFields() {
			return entry;
		}

		public Entry getEntryForOtherFields() {
			if (otherEntries.size() == 1) {
				return (Entry)otherEntries.firstElement();
			} else {
				return null;
			}
		}

		public Entry getEntryForCommon1Fields() {
			return entry;
		}
		
		public Entry getEntryForCommon2Fields() {
			if (otherEntries.size() == 1) {
				return (Entry)otherEntries.firstElement();
			} else {
				return null;
			}
		}
		
		public long getAmount() {
			return entry.getAmount();
		}

		public boolean blankTransactionFields() {
			return false;
		}

		public boolean isBalanceAffected() {
			return true;
		}

		public long getBalance() {
			return balance;
		}

		/**
		 * @param balance
		 */
		public void setBalance(long balance) {
			this.balance = balance;
		}

		public Entry getEntryInAccount() {
			return entry;
		}

		public Entry getEntryForThisRow() {
			return entry;
		}
	}

	class DisplayableEntry implements IDisplayableItem {

		private Entry entry;

		private DisplayableTransaction transactionData;

		public DisplayableEntry(Entry entry,
				DisplayableTransaction transactionData) {
			this.entry = entry;
			this.transactionData = transactionData;
		}

		public DisplayableTransaction getDisplayableTransaction() {
			return this.transactionData;
		}

		public Transaction getTransactionForTransactionFields() {
			return null;
		}

		public Entry getEntryForAccountFields() {
			// If this entry has been set to an income and expense account entry
			// then the capital account fields must be blank and we return null
			// to indicate this, otherwise the capital account properties are
			// associated with this entry.
			if (entry.getAccount() == null
					|| entry.getAccount() instanceof CapitalAccount) {
				return entry;
			} else {
				return null;
			}
		}

		public Entry getEntryForOtherFields() {
			// If this entry has been set to a capital account entry
			// then the income and expense account fields must be blank and we return null
			// to indicate this, otherwise the capital account properties are
			// associated with this entry.
			if (entry.getAccount() == null
					|| entry.getAccount() instanceof IncomeExpenseAccount) {
				return entry;
			} else {
				return null;
			}
		}
		
		public Entry getEntryForCommon1Fields() {
			// If the column contains an entry property that applies to both capital and income and expense
			// accounts, and (in the simple transaction case) displays the property from the capital account,
			// then the column is always blank in the child rows (we use common2 in preference).
			return null;
		}
		
		public Entry getEntryForCommon2Fields() {
			// If the column contains an entry property that applies to both capital and income and expense
			// accounts, then in child rows it will always contain the property from the entry.
			return entry;
		}
		
		/* (non-Javadoc)
		 * @see net.sf.jmoney.ui.internal.pages.account.capital.IDisplayableItem#getAmount()
		 */
		public long getAmount() {
			// TODO Auto-generated method stub
			return 0;
		}

		public boolean blankTransactionFields() {
			// All transaction fields are blank on split-entry rows.
			return true;
		}

		public boolean isBalanceAffected() {
			// The amounts on the split-entry fields do not affect the
			// balance shown in the balance column.
			return false;
		}

		public long getBalance() {
			throw new RuntimeException("");
		}

		public Entry getEntryInAccount() {
			DisplayableTransaction transactionData = getDisplayableTransaction();
			return transactionData.getEntryForAccountFields();
		}

		public Entry getEntryForThisRow() {
			return entry;
		}
	}

	/**
	 * This class is to be used for new entries which are currently filled in by the
	 * user, before they are comited and transformed in real entries
	 * @author Faucheux
	 */
	class DisplayableNewEmptyEntry implements IDisplayableItem {

		public DisplayableNewEmptyEntry() {
			// 	constructor to define a new, empty entry which we be filled in by the user
		}

		/* (non-Javadoc)
		 * @see net.sf.jmoney.ui.internal.pages.account.capital.IDisplayableItem#getTransaction()
		 */
		public Transaction getTransactionForTransactionFields() {
			// TODO Auto-generated method stub
			return null;
		}

		/* (non-Javadoc)
		 * @see net.sf.jmoney.ui.internal.pages.account.capital.IDisplayableItem#getEntryForAccountFields()
		 */
		public Entry getEntryForAccountFields() {
			// TODO Auto-generated method stub
			return null;
		}

		/* (non-Javadoc)
		 * @see net.sf.jmoney.ui.internal.pages.account.capital.IDisplayableItem#getEntryForOtherFields()
		 */
		public Entry getEntryForOtherFields() {
			// TODO Auto-generated method stub
			return null;
		}

		public Entry getEntryForCommon1Fields() {
			// TODO Auto-generated method stub
			return null;
		}
		
		public Entry getEntryForCommon2Fields() {
			// TODO Auto-generated method stub
			return null;
		}
		
		/* (non-Javadoc)
		 * @see net.sf.jmoney.ui.internal.pages.account.capital.IDisplayableItem#getAmount()
		 */
		public long getAmount() {
			// TODO Auto-generated method stub
			return 0;
		}

		/* (non-Javadoc)
		 * @see net.sf.jmoney.ui.internal.pages.account.capital.IDisplayableItem#blankTransactionFields()
		 */
		public boolean blankTransactionFields() {
			// TODO Auto-generated method stub
			return false;
		}

		/* (non-Javadoc)
		 * @see net.sf.jmoney.ui.internal.pages.account.capital.IDisplayableItem#isBalanceAffected()
		 */
		public boolean isBalanceAffected() {
			// TODO Auto-generated method stub
			return false;
		}

		/* (non-Javadoc)
		 * @see net.sf.jmoney.ui.internal.pages.account.capital.IDisplayableItem#getBalance()
		 */
		public long getBalance() {
			// TODO Auto-generated method stub
			return 0;
		}

		/* (non-Javadoc)
		 * @see net.sf.jmoney.pages.entries.IDisplayableItem#getEntryInAccount()
		 */
		public Entry getEntryInAccount() {
			// TODO Auto-generated method stub
			return null;
		}

		/* (non-Javadoc)
		 * @see net.sf.jmoney.pages.entries.IDisplayableItem#getSelectedEntry()
		 */
		public Entry getEntryForThisRow() {
			// TODO Auto-generated method stub
			return null;
		}
	}

	/**
	 * Refresh the text only.
	 * This method is used when text may have changed but the items
	 * in the table have not changed.
	 */
	private void refreshLabels() {
		for (int i = 0; i < fTable.getItemCount(); i++) {
			TreeItem parentItem = fTable.getItem(i);
			updateItem(parentItem);
			
			// Update the children too
			for (int j = 0; j < parentItem.getItemCount(); j++) {
				TreeItem childItem = parentItem.getItem(j);
				updateItem(childItem);
			}
		}
	}

	/**
	 * Refresh the list of entries.
	 */
	public void refreshEntryList() {
		buildEntryList();
		setTableItems();
	}

	public void dispose() {
		fTable.dispose();
	}

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
	 * 
	 * @see net.sf.jmoney.pages.entries.IEntriesControl#addTransaction(net.sf.jmoney.model2.Entry)
	 */
	public void addEntryInAccount(Entry entry) {
		DisplayableTransaction dTrans = new DisplayableTransaction(entry, 0);

		// TODO: check that caller ensures this entry is in the entries content provider.
		
		// Do not add this entry to our view if a filter is on and the entry does
		// not match the filter.
		if (!matchesFilter(dTrans)) {
			return;
		}

		entries.add(dTrans);

		// Scan the table to find the correct index to insert this row.
		// Because rows are likely to be inserted near the bottom of
		// the table, we scan backwards.
		Comparator rowComparator = new RowComparator(sortColumn, sortAscending);
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
		
		// Set the column values for this new row (note that the last
		// column is not set.  The last column is always the balance and is set
		// later by the same code that updates all the following balances).
		for (int columnIndex = 0; columnIndex < fTable.getColumnCount() - 1; columnIndex++) {
			IEntriesTableProperty p = (IEntriesTableProperty)fTable.getColumn(columnIndex).getData();
			parentItem.setText(parentIndex, p.getValueFormattedForTable(dTrans));
		}

		if (entry.getTransaction().hasMoreThanTwoEntries()) {
			// Case of an splitted entry. We display the transaction on the first line
			// and the entries of the transaction on the following ones.
			// However, the transaction line also holds the properties for the entry
			// in this account, so display just the other entries underneath.
			Iterator itSubEntries = entry.getTransaction().getEntryCollection()
					.iterator();
			while (itSubEntries.hasNext()) {
				Entry entry2 = (Entry) itSubEntries.next();
				if (!entry2.equals(entry)) {
					DisplayableEntry entryData = new DisplayableEntry(entry2,
							dTrans);
					TreeItem item2 = new TreeItem(parentItem, SWT.NULL);
					item2.setData(entryData);
				}
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
	 */
	private void removeEntryInAccount(Entry entry) {
		// TODO: processing is done correctly for entries being added and removed from
		// the set provided by entriesContent.  However, the filter is not kept correctly.
		// This is a problem, because the user may edit properties that make the entry
		// disappear because it no longer passes the filter.  How do we deal with this?
		
		// get the index of the row for the transaction
		int parentIndex = lookupEntryInAccount(entry);

		TreeItem parentItem = fTable.getItem(parentIndex);
		DisplayableTransaction dTrans = (DisplayableTransaction)parentItem.getData();
		Transaction transaction = entry.getTransaction();

		// Remove from our cached set of entries
		entries.remove(dTrans);
		
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
		TreeItem item = lookupSplitEntry(fTable.getItem(parentIndex), changedEntry);

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

			// If the entry in the account is the same as the changed
			// entry, then this must be the top row for the transaction.
			DisplayableTransaction dTrans = (DisplayableTransaction) item
					.getData();

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
	 */
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

	public int lookupEntryInAccount(Entry entryInAccount) {
		// Find this entry.  We scan backwards on the assuption
		// that most changes occur at the bottom of the table.
		for (int i = fTable.getItemCount()-1; i >= 0; i--) {
			TreeItem item = fTable.getItem(i);
			Object data = item.getData();
			if (data instanceof DisplayableTransaction) {
				DisplayableTransaction dTrans = (DisplayableTransaction) data;
				if (dTrans.getEntryForAccountFields().equals(entryInAccount)) {
					return i;
				}
			}
		}
		return -1;
	}

	public TreeItem lookupSplitEntry(TreeItem parentItem, Entry entry) {
		// See if this entry is represented by the top level row
		DisplayableTransaction dTrans = (DisplayableTransaction)parentItem.getData();
		if (entry.equals(dTrans.getEntryForAccountFields())
				|| entry.equals(dTrans.getEntryForOtherFields())) {
			return parentItem;
		}
		
		for (int i = 0; i < parentItem.getItemCount(); i++) {
			TreeItem item = parentItem.getItem(i);
			DisplayableEntry dEntry = (DisplayableEntry)item.getData();
			if (dEntry.getEntryForThisRow().equals(entry)) {
				return item;
			}
		}
		
		throw new RuntimeException("");
	}

	private void updateItem(TreeItem item) {
		Object obj = item.getData();
		if (obj instanceof IDisplayableItem) {
			for (int index = 0; index < fTable.getColumnCount(); index++) {
				IEntriesTableProperty entryData = (IEntriesTableProperty) (fTable
						.getColumn(index).getData());
				item.setText(index, entryData
						.getValueFormattedForTable((IDisplayableItem) obj));
			}
		}
	}

	/**
	 * Adjust the balances.
	 * 
	 * @param i the index at which we start looking for DisplayableTransaction
	 * 				rows.  All DisplayableTransaction rows at this index and
	 * 				following are adjusted.
	 * @param balance the balance prior to the first DisplayableTransaction found
	 * 				at or after the given index
	 */
	private void updateBalances(int i, long balance) {
		// The balance column is always the last column
		int balanceColumnIndex = fTable.getColumnCount() - 1;
		
		for (; i < fTable.getItemCount(); i++) {
			TreeItem item = fTable.getItem(i);
			Object data = item.getData();
			if (data instanceof DisplayableTransaction) {
				DisplayableTransaction dTrans = (DisplayableTransaction) item
						.getData();

				balance += dTrans.getEntryForAccountFields().getAmount();
				dTrans.setBalance(balance);

				item.setText(balanceColumnIndex, entriesContent
						.getBalanceColumnManager().getValueFormattedForTable(
								dTrans));
			}
		}
	}

	private void updateColors(int i, boolean isAlternated) {
		for (; i < fTable.getItemCount(); i++) {
			TreeItem item = fTable.getItem(i);

			isAlternated = !isAlternated;

			if (isAlternated) {
				item.setBackground(alternateTransactionColor);
			} else {
				item.setBackground(transactionColor);
			}

			// Process the child items
			for (int j = 0; j < item.getItemCount(); j++) {
				TreeItem childItem = item.getItem(j);

				if (isAlternated) {
					childItem.setBackground(alternateEntryColor);
				} else {
					childItem.setBackground(entryColor);
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see net.sf.jmoney.pages.entries.IEntriesControl#setOtherEntry(net.sf.jmoney.model2.Entry, net.sf.jmoney.model2.Entry)
	 */
	public void setSelection(Entry entryInAccount, Entry entryToSelect) {
		int index = lookupEntryInAccount(entryInAccount);
		JMoneyPlugin.myAssert(index >= 0);
		TreeItem item = lookupSplitEntry(fTable.getItem(index), entryToSelect);
		JMoneyPlugin.myAssert(item != null);
		fTable.setSelection(new TreeItem [] {item});
	}

	/**
	 * Update the view to show the insertion of a new child
	 * entry to a given top-level entry.
	 * This includes updating the cached list of entries
	 * in the DisplayableTransaction object.
	 */
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
			for (Iterator iter = transaction.getEntryCollection().iterator(); iter
					.hasNext();) {
				Entry entry = (Entry) iter.next();
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
	 */
	public void removeEntry(Entry entryInAccount, Entry deletedEntry) {
		// get the index of the row for the transaction
		int transIndex = lookupEntryInAccount(entryInAccount);

		TreeItem parentItem = fTable.getItem(transIndex);
		DisplayableTransaction dTrans = (DisplayableTransaction) parentItem
				.getData();
		Transaction transaction = deletedEntry.getTransaction();

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
		if (!dTrans.hasSplitEntries()) {
			JMoneyPlugin.myAssert(parentItem.getItemCount() == 1);
			parentItem.getItem(0).dispose();
			updateItem(parentItem);
		}
	}

	/* (non-Javadoc)
	 * @see net.sf.jmoney.pages.entries.IEntriesControl#addSelectionListener(org.eclipse.swt.events.SelectionListener)
	 */
	public void addSelectionListener(SelectionListener tableSelectionListener) {
		fTable.addSelectionListener(tableSelectionListener);
	}

	/* (non-Javadoc)
	 * @see net.sf.jmoney.pages.entries.IEntriesControl#getControl()
	 */
	public Control getControl() {
		return fTable;
	}
	
	private class RowComparator implements Comparator {
		private IEntriesTableProperty sortProperty;
		private boolean ascending;
		
		RowComparator(TreeColumn sortColumn, boolean ascending) {
			this.sortProperty = (IEntriesTableProperty)sortColumn.getData();
			this.ascending = ascending;
		}
		
		public int compare(Object obj1, Object obj2) {
			DisplayableTransaction dTrans1 = (DisplayableTransaction) obj1;
			DisplayableTransaction dTrans2 = (DisplayableTransaction) obj2;
			int result = sortProperty.compare(dTrans1, dTrans2);
			return sortAscending ? result : -result;
		}
	}
}
