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
import java.util.Date;
import java.util.Iterator;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.fields.TransactionInfo;
import net.sf.jmoney.model2.CurrencyAccount;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.Transaction;

import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * An implementation of IEntriesControl that displays the entries
 * in a flat table.
 */
public class EntriesTable implements IEntriesControl {

	// The darker blue and green lines
	//protected static final Color transactionColor          = new Color(Display.getCurrent(), 125, 215, 060);
	//protected static final Color alternateTransactionColor = new Color(Display.getCurrent(), 100, 160, 200);
	//protected static final Color entryColor                = new Color(Display.getCurrent(), 180, 225, 140);
	//protected static final Color alternateEntryColor       = new Color(Display.getCurrent(), 135, 185, 205);
	
	// The  lighter purple and green lines
	// As suggested by Tom Drummond
    protected static final Color transactionColor          = new Color(Display.getCurrent(), 237, 237, 255);
    protected static final Color alternateTransactionColor = new Color(Display.getCurrent(), 237, 255, 237);
    // But made a little darker to distinguish from the entry lines
    //protected static final Color transactionColor          = new Color(Display.getCurrent(), 230, 230, 255);
    //protected static final Color alternateTransactionColor = new Color(Display.getCurrent(), 230, 255, 237);

    protected static final Color entryColor                = new Color(Display.getCurrent(), 240, 240, 255);
    protected static final Color alternateEntryColor       = new Color(Display.getCurrent(), 240, 255, 255);

	protected EntriesPage fPage;
	protected Table fTable;
    
    /**
     * List of entries to show in the entries list.
     * Only the entries in the listed account are included,
     * the other entries in the transactions are not included
     * even tho information from these entries may be shown
     * by the entries list control.
     * <P>
     * This is an array (not a Vector) so that we can sort it
     * using Arrays.sort.
     */
	DisplayableTransaction [] entries;
    
	protected IPropertyControl currentCellPropertyControl = null;

	/**
	 * The index of the balance column
	 */
	private int balanceColumnIndex;

	private TableItem previouslySelectedItem = null;
	
	/**
	 * The column on which the items are currently sorted
	 */
	protected TableColumn sortColumn;
	
	/**
	 * true if items are sorted in an ascending order;
	 * false if items are sorted in a descending order
	 */
	protected boolean sortAscending;
	
	/**
	 * @param container
	 * @param page
	 */
	public EntriesTable(final Composite container, EntriesPage page) {
		this.fPage = page;
		FormToolkit toolkit = page.getEditor().getToolkit();
		
        fTable = toolkit.createTable(container, SWT.FULL_SELECTION);

        GridData gridData = new GridData(GridData.FILL_BOTH);
        gridData.heightHint = 100;
        gridData.widthHint = 100;
        gridData.grabExcessHorizontalSpace = true;
        gridData.grabExcessVerticalSpace = true;
        fTable.setLayoutData(gridData);

        TableLayout tlayout = new TableLayout();
        
        int index = 0;
        for (Iterator iter = fPage.allEntryDataObjects.iterator(); iter.hasNext(); ) {
        	IEntriesTableProperty entriesSectionProperty = (IEntriesTableProperty)iter.next();
        	addColumn(entriesSectionProperty, index++);
        }

        
        TableColumn col;

        col = new TableColumn(fTable, SWT.RIGHT);
        col.setText("Debit");
        col.setData("layoutData", new ColumnWeightData(2, 70));
        col.setData(fPage.debitColumnManager);
        
        col = new TableColumn(fTable, SWT.RIGHT);
        col.setText("Credit");
        col.setData("layoutData", new ColumnWeightData(2, 70));
        col.setData(fPage.creditColumnManager);

        col = new TableColumn(fTable, SWT.RIGHT);
        col.setText("Balance");
        col.setData("layoutData", new ColumnWeightData(2, 70));
        col.setData(fPage.balanceColumnManager);

        // FIXME: This is wrong because the balance column may move.
        balanceColumnIndex = fPage.allEntryDataObjects.size() + 2;
        
        fTable.setLayout(tlayout);
        fTable.setHeaderVisible(true);
        fTable.setLinesVisible(true);

        // Set sort column to first column (this is the date)
        // TODO: Deal with case where the date is not the first column
		sortColumn = fTable.getColumn(0);
		sortAscending = true;

        // Set the list of entries to display in the entries list.
        buildEntryList();

		// Add the content.  The table is empty (no items)
        // before this method is called, so the item fetcher
        // simply adds news items to the end of the table.
        setTableItems(new IItemFetcher() {
			public TableItem getNextItem() {
		    	return new TableItem(fTable, SWT.NULL);
			}
        });
        
        fTable.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
                Object selectedObject = e.item.getData();
                
                // TODO: This code is duplicated below.
                // The selected object might be null.  This occurs when the table is refreshed.
                // I don't understand this so I am simply bypassing the update
                // in this case.  Nigel
                if (EntriesPage.IS_ENTRY_SECTION_TO_DISPLAY && selectedObject != null) {
                	IDisplayableItem data = (IDisplayableItem)selectedObject;
                	DisplayableTransaction transactionData = null;
                	if (selectedObject instanceof DisplayableTransaction) {
                		transactionData = (DisplayableTransaction) data;
                		fPage.fEntrySection.update(transactionData.getEntryForAccountFields(), transactionData.getEntryForAccountFields());
                	} else if (selectedObject instanceof DisplayableEntry) {
                		transactionData = ((DisplayableEntry)data).getDisplayableTransaction();
                		fPage.fEntrySection.update(transactionData.getEntryForAccountFields(), ((DisplayableEntry)data).entry);
                	}
                }
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub
			}
        });

		fTable.setTopIndex(fTable.getItemCount()-5);

        // Create the editor.
    	final TableEditor editor = new TableEditor(fTable);
    	
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
			}
    	});
    	
    	fTable.addMouseListener(new MouseAdapter() {
    		public void mouseDown(MouseEvent event) {
    			// Clean up any previous editor control
    			closeCellEditor(editor);
    			
    			Point pt = new Point (event.x, event.y);
				final TableItem item = fTable.getItem(pt);
    			final int column = getColumn(pt, item);
    			
    			if (event.button == 1) {
    				// Go into edit mode only if the user clicked on a field in the
    				// selected row.
    				if (item != previouslySelectedItem) {
    					previouslySelectedItem = item;
    					return;
    				}
    				
    				IEntriesTableProperty entryData = (IEntriesTableProperty)fTable.getColumn(column).getData();
    				IDisplayableItem data = (IDisplayableItem)item.getData();
    				currentCellPropertyControl = entryData.createAndLoadPropertyControl(fTable, data);
    				// If a control was created (i.e. this was not a
    				// non-editable column such as the balance column)
    				if (currentCellPropertyControl != null) {
    					Control c = currentCellPropertyControl.getControl();
    					editor.setEditor (c, item, column);
    					c.setFocus ();
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

    	            MenuItem shiftColLeftItem = new MenuItem(popupMenu, SWT.NONE);
    	            
    	            MenuItem shiftColRightItem = new MenuItem(popupMenu, SWT.NONE);
    				
    				Object [] messageArgs = new Object[] {
    						fTable.getColumn(column).getText()
    				};
    				
    	            removeColItem.setText(
    						new java.text.MessageFormat(
    								"Remove {0} column",
    								java.util.Locale.US)
    								.format(messageArgs)
    	            		);
    	            shiftColLeftItem.setText(
    						new java.text.MessageFormat(
    								"Move {0} column left",
    								java.util.Locale.US)
    								.format(messageArgs)
    	            		);
    	            shiftColRightItem.setText(
    						new java.text.MessageFormat(
    								"Move {0} column right",
    								java.util.Locale.US)
    								.format(messageArgs)
    	            		);

    	            removeColItem.addSelectionListener(new SelectionAdapter() {
    					public void widgetSelected(SelectionEvent e) {
    						fTable.getColumn(column).dispose();
    					}
    	            });
    				
    	            shiftColLeftItem.addSelectionListener(new SelectionAdapter() {
    					public void widgetSelected(SelectionEvent e) {
    						if (column > 1) {
    							IEntriesTableProperty entryData = (IEntriesTableProperty)fTable.getColumn(column).getData();
    							removeColumn(column);
    							addColumn(entryData, column-1);
    						}
    					}
    	            });
    				
    	            shiftColRightItem.addSelectionListener(new SelectionAdapter() {
    					public void widgetSelected(SelectionEvent e) {
    						if (column < fTable.getColumnCount()-1) {
    							IEntriesTableProperty entryData = (IEntriesTableProperty)fTable.getColumn(column).getData();
    							removeColumn(column);
    							addColumn(entryData, column+1);
    						}
    					}
    	            });
    				
    	            MenuItem separatorItem = new MenuItem(popupMenu, SWT.SEPARATOR);

    	            for (Iterator iter = fPage.allEntryDataObjects.iterator(); iter.hasNext(); ) {
    		        	final IEntriesTableProperty entriesSectionProperty = (IEntriesTableProperty)iter.next();
    		        	boolean found = false;
    		        	for (int index = 0; index < fTable.getColumnCount(); index++) {
    		        		IEntriesTableProperty entryData2 = (IEntriesTableProperty)(fTable.getColumn(index).getData());
    						if (entryData2 == entriesSectionProperty) {
    							found = true;
    							break;
    						}
    		        	}
    		        	
    		        	if (!found) {
    	    				Object [] messageArgs2 = new Object[] {
    	    						entriesSectionProperty.getText()
    	    				};
    	    				
    	    	            MenuItem addColItem = new MenuItem(popupMenu, SWT.NONE);
    	    	            addColItem.setText(
    	    						new java.text.MessageFormat(
    	    								"Add {0} column",
    	    								java.util.Locale.US)
    	    								.format(messageArgs2)
    	    	            );
    	    	            
    	    	            addColItem.addSelectionListener(new SelectionAdapter() {
    	    	            	public void widgetSelected(SelectionEvent e) {
        	            			addColumn(entriesSectionProperty, Math.max(1, column));
    	    	            	}
    	    	            });
    		        	}
    		        }

    	            popupMenu.setVisible(true);
    			}
    		}
    	});
	}

    /**
	 * 
	 */
	protected void closeCellEditor(TableEditor editor) {
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
	protected int getColumn(Point pt, TableItem item) {
		int i;
		for (i=0; i<fTable.getColumnCount(); i++) {
			Rectangle rect = item.getBounds (i);
			if (rect.contains (pt)) {
				break;
			}
		}
		
		if (i == fTable.getColumnCount()) {
			// No column sucessfully hit.
			throw new RuntimeException("hit test failure");
		}
		return i;
	}

	/**
     * Build the list of entries to be shown in the entries
     * list.  This method sets the list into <code>entries</code>.
     * This list contains only the entries that are in
     * this account.  Although the entry list control may chose
     * to show information from other entries in the transaction,
     * these are not included in this array.
     */
    private void buildEntryList() {
    	// Note that the balances are not set at this time.  This is done
    	// when the data is set into the table.  It is just as easy
    	// and efficient to do it then and that reduces the effort
    	// to keep the balances updated.
        CurrencyAccount account = fPage.getAccount();
        Collection accountEntries = 
        	account
				.getSortedEntries(TransactionInfo.getDateAccessor(), false);
        entries = new DisplayableTransaction[accountEntries.size()];
        int i = 0;
        for (Iterator iter = accountEntries.iterator(); iter.hasNext(); ) {
            Entry accountEntry = (Entry) iter.next();
        	DisplayableTransaction data = new DisplayableTransaction(accountEntry, 0);
            entries[i++] = data;
        }    	
    }
    
	interface IItemFetcher {
		TableItem getNextItem();
	}
	
	/**
	 * Set the content into the table.  If this is the first time this
	 * method is called then the table will be empty and a TableItem
	 * must be created for each row in the table.  If this is a subsequent
	 * call then TableItem objects will already exist and we re-use them,
	 * creating extra at the end or disposing excess if necessary. 
	 */
	private void setTableItems(IItemFetcher itemFetcher) {
        /*
         * Alternate the color of the entries to improve the readibility 
         */
        boolean isAlternated = true;

        long saldo = fPage.getAccount().getStartBalance();
        for (int i = 0; i < entries.length; i++) {
        	DisplayableTransaction data = entries[i];

        	isAlternated = ! isAlternated;
        	
        	TableItem item = itemFetcher.getNextItem();
        	saldo = saldo + data.getEntryForAccountFields().getAmount();
        	data.setBalance(saldo);
        	item.setData(data);
        	updateItem(item);
        	
        	item.setBackground(isAlternated 
        			? alternateTransactionColor
        					: transactionColor);

            Transaction trans = data.getTransactionForTransactionFields(); 
            if (trans.hasMoreThanTwoEntries()) {
        		// Case of an splitted entry. We display the transaction on the first line
        		// and the entries of the transaction on the following ones.
        		// However, the transaction line also holds the properties for the entry
        		// in this account, so display just the other entries underneath.
        		Iterator itSubEntries = trans.getEntryIterator();
        		while (itSubEntries.hasNext()) {
        			Entry entry2 = (Entry) itSubEntries.next();
        			if (!entry2.equals(data.getEntryForAccountFields())) {
        	        	TableItem item2 = itemFetcher.getNextItem();
        				DisplayableEntry entryData = new DisplayableEntry(entry2, data);
        				item2.setData(entryData);
        				updateItem(item2);

        	        	item2.setBackground(isAlternated 
        	        			? alternateEntryColor
        	        					: entryColor);
        			}
        		}
        	}
        }
        
        // an empty line to have the possibility to enter a new entry
        TableItem item3 = itemFetcher.getNextItem();
        item3.setData(new DisplayableNewEmptyEntry());
        updateItem(item3);
	}

	/**
	 * @param entriesSectionProperty
	 * @param columnIndex
	 */
	private void addColumn(IEntriesTableProperty entriesSectionProperty, int columnIndex) {
    	final TableColumn col = new TableColumn(fTable, SWT.NULL, columnIndex);
        col.setText(entriesSectionProperty.getText());

        col.setData(entriesSectionProperty);
        
        col.setData("layoutData", 
        		new ColumnWeightData(
        				entriesSectionProperty.getWeight(), 
						entriesSectionProperty.getMinimumWidth()));

        // Re-calculate the layout.  SWT does not do this automatically when a
        // column is added, resulting in the new column being set to a zero width.
        fTable.layout(true);

        col.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent event) {
        		if (col == sortColumn) {
        			sortAscending = !sortAscending;
              		col.setImage(JMoneyPlugin.createImageDescriptor(sortAscending?"icons/ArrowUp.gif":"icons/ArrowDown.gif").createImage());
        		} else {
            		sortColumn.setImage(null);
    				sortColumn = col;
    				sortAscending = true;
            		col.setImage(JMoneyPlugin.createImageDescriptor("icons/ArrowUp.gif").createImage());
        		}
        		sortItems((IEntriesTableProperty)sortColumn.getData(), sortAscending);
        	}
        });

        // Update the contents of the column.  This is not necessary during initial
        // column setup because columns are setup before columns.  When columns are
        // inserted or moved, however, this is necessary.
		for (int rowIndex = 0 ; rowIndex < fTable.getItemCount(); rowIndex++) {
			TableItem item = fTable.getItem(rowIndex);
			IDisplayableItem data = (IDisplayableItem)item.getData();
			item.setText(columnIndex, entriesSectionProperty.getValueFormattedForTable(data));
		}
	}

	/**
	 * Called when the sort order changes.
	 * The account entries are re-sorted and the items are
	 * re-set into the table.
	 * 
	 * @param property
	 * @param sortAscending
	 */
	protected void sortItems(final IEntriesTableProperty property, final boolean sortAscending) {
		Arrays.sort(entries, new Comparator() {
			public int compare(Object obj1, Object obj2) {
				DisplayableTransaction dTrans1 = (DisplayableTransaction)obj1;
				DisplayableTransaction dTrans2 = (DisplayableTransaction)obj2;
				
				// TODO: This is not right.  Sorting on the text value
				// is wrong for dates, amounts etc.
				// Must think about how to do this properly.
				String text1 = property.getValueFormattedForTable(dTrans1);
				String text2 = property.getValueFormattedForTable(dTrans2);
				
				int result = text1.compareToIgnoreCase(text2);
				
				return sortAscending ? result : -result; 
			}
		});
		
		// Replace the data in each TableItem object.
        // The correct number of items should be in the table.
		// However, just in case something has gone wrong, or perhaps
		// the datastore is a database and someone outside of JMoney
		// has changed the database, we correctly adjust the number
		// of items in the table.
		
		class ItemFetcher implements IItemFetcher {
        	int itemIndex = 0;
			public TableItem getNextItem() {
				if (itemIndex < fTable.getItemCount()) {
					return fTable.getItem(itemIndex++);
				} else {
					itemIndex++;
					return new TableItem(fTable, SWT.NULL);
				}
			}
		}

		ItemFetcher itemFetcher = new ItemFetcher();
        setTableItems(itemFetcher);
        
        // If there were more items in the table than needed,
        // dispose of the excess.
		while (itemFetcher.itemIndex < fTable.getItemCount()) {
			fTable.getItem(itemFetcher.itemIndex).dispose();
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

	/* (non-Javadoc)
	 * @see net.sf.jmoney.ui.internal.pages.account.capital.IEntriesControl#getSelection()
	 */
	public Entry getSelectedEntryInAccount() {
		if (fTable.getSelectionCount() != 1) {
			return null;
		}
		
		Object selectedObject = fTable.getSelection()[0].getData();
		
		if (selectedObject != null) {
			IDisplayableItem data = (IDisplayableItem)selectedObject;
			if (selectedObject instanceof DisplayableTransaction) {
				DisplayableTransaction transactionData = (DisplayableTransaction) data;
				return transactionData.getEntryForAccountFields();
			}
			if (selectedObject instanceof DisplayableEntry) {
				DisplayableEntry entryData = (DisplayableEntry)data;
				return entryData.getDisplayableTransaction().getEntryForAccountFields();
			}
		}
		
		return null;
	}

	
	/* (non-Javadoc)
	 * @see net.sf.jmoney.ui.internal.pages.account.capital.IEntriesControl#getSelection()
	 */
	public Entry getSelectedEntry() {
		if (fTable.getSelectionCount() != 1) {
			return null;
		}
		
		Object selectedObject = fTable.getSelection()[0].getData();
		
		if (selectedObject != null) {
			IDisplayableItem data = (IDisplayableItem)selectedObject;
			if (selectedObject instanceof DisplayableTransaction) {
				DisplayableTransaction transactionData = (DisplayableTransaction) data;
				return transactionData.getEntryForAccountFields();
			}
			if (selectedObject instanceof DisplayableEntry) {
				DisplayableEntry entryData = (DisplayableEntry)data;
				return entryData.getEntry();
			}
		}
		
		return null;
	}

	
    class DisplayableTransaction implements IDisplayableItem {
        private Entry entry;
        private long balance;

        public DisplayableTransaction(Entry entry, long saldo) {
            this.entry = entry;
            this.balance = saldo;
        }
    	
		public Transaction getTransactionForTransactionFields() {
			return entry.getTransaction();
		}

		public Entry getEntryForAccountFields() {
			return entry;
		}

		public Entry getEntryForOtherFields() {
			if (entry.getTransaction().hasMoreThanTwoEntries()) {
				return null;
			} else {
				return entry.getTransaction().getOther(entry);
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
    }
    
    class DisplayableEntry implements IDisplayableItem { 
        
        private Entry entry;
        private DisplayableTransaction transactionData;

        public DisplayableEntry(Entry entry, DisplayableTransaction transactionData) {
            this.entry = entry;
            this.transactionData = transactionData;
        }
        
		public DisplayableTransaction getDisplayableTransaction() {
			return this.transactionData;
		}

		Entry getEntry() { return entry; }

        public Transaction getTransactionForTransactionFields() {
        	return null;
        }

        public Entry getEntryForAccountFields() {
			return entry;
		}

		public Entry getEntryForOtherFields() {
			return null;
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
    }
    
    
    /**
     * This class is to be used for new entries which are currently filled in by the
     * user, before they are comited and transformed in real entries
     * @author Faucheux
     */
    class DisplayableNewEmptyEntry implements IDisplayableItem {
        
        public DisplayableNewEmptyEntry(){
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
    }

    /**
	 * Refresh the viewer.
	 */
    //TODO: rename this to refreshLabels
	public void refresh() {
		//fViewer.refresh();
		// TODO: complete
	}	

	/**
	 * Update the viewer.
	 */
	// TODO: remove this
	public void update(Object element) {
//		fViewer.update(element, null);
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
		// Insert in date order
		// Most entries are added near the end, so scan
		// backwards.
		Date date = entry.getTransaction().getDate();
		
		long balance;
        boolean isAlternated;
		int i;

		// Subtract 1 to get row count excluding the 'empty item' for new entries
		int realItemCount = fTable.getItemCount() - 1;

		for (i = realItemCount-1; ; i-- ) { 
			if (i < 0) {
				balance = fPage.getAccount().getStartBalance();
				isAlternated = true;
				break;
			}
			
			TableItem item = fTable.getItem(i);
			Object itemData = item.getData();
			if (itemData instanceof DisplayableTransaction) {
				DisplayableTransaction de = (DisplayableTransaction)itemData;
				if (date == null ||
				  	 date.compareTo(de.getTransactionForTransactionFields().getDate()) >= 0) {
					balance = de.getBalance();
					isAlternated = (alternateTransactionColor.equals(item.getBackground()));
					break;
				}
			}
		}
		
		// We have found the last transaction that goes before
		// this transaction.  Now goes forwards thru the entries
		// for this transaction to find the index at which this
		// transaction should be inserted.
		int insertIndex;
		if (i == -1) {
			insertIndex = 0;
		} else {
			 while (++i < realItemCount) {
				Object itemData = fTable.getItem(i).getData();
				if (itemData instanceof DisplayableTransaction) {
					break;
				}
			}
			insertIndex = i;
		}

		int originalInsertIndex = insertIndex;
		
		// Insert the transaction and its entries now.
		TableItem item = new TableItem(fTable, 0, insertIndex++);

// TODO: clean this up.  These are now set later as they must
//		be set in all remaining entries.
//		balance += entry.getAmount();
		
		DisplayableTransaction de = new DisplayableTransaction(entry, balance);
		item.setData(de);

		// Set the column values for this new row.
		for (int index = 0; index < fPage.allEntryDataObjects.size(); index++ ) {
			IEntriesTableProperty p = (IEntriesTableProperty)fPage.allEntryDataObjects.get(index);
			item.setText(index, p.getValueFormattedForTable(de));
		}
		
        if (entry.getTransaction().hasMoreThanTwoEntries()) {
            // Case of an splitted entry. We display the transaction on the first line
            // and the entries of the transaction on the following ones.
        	// However, the transaction line also holds the properties for the entry
        	// in this account, so display just the other entries underneath.
            Iterator itSubEntries = entry.getTransaction().getEntryIterator();
            while (itSubEntries.hasNext()) {
                Entry entry2 = (Entry) itSubEntries.next();
                if (!entry2.equals(entry)) {
                    DisplayableEntry entryData = new DisplayableEntry(entry2, de);
            		TableItem item2 = new TableItem(fTable, 0, insertIndex++);
                	item2.setData(entryData);
                }
            }
        }
        
		// Recalculate balances from this point onwards.
		updateBalances(i, balance);
		
		// Set colors from this point onwards (colors have switched).
		updateColors(i, isAlternated);
	}

	/* (non-Javadoc)
	 * @see net.sf.jmoney.pages.entries.IEntriesControl#updateEntry(net.sf.jmoney.model2.Entry)
	 */
	public void updateEntry(Entry entryInAccount, Entry entryChanged) {
		int index = lookupItem(entryInAccount, entryChanged);
		if (index >= 0) {
			updateItem(fTable.getItem(index));
		}
	}

	public int lookupItem(Entry entryInAccount, Entry entryChanged) {
		// Find this item.
		for (int i = 0; i < fTable.getItemCount(); i++) {
			TableItem item = fTable.getItem(i); 
			Object data = item.getData();
			if (data instanceof DisplayableTransaction) {
				DisplayableTransaction dTrans = (DisplayableTransaction)data;
				if (dTrans.getEntryForAccountFields().equals(entryInAccount)) {
					// 
					if (entryChanged.equals(dTrans.getEntryForAccountFields())
							|| entryChanged.equals(dTrans.getEntryForOtherFields())) {
						return i;
					} else {
						do {
							i++;
							TableItem item2 = fTable.getItem(i); 
							Object data2 = item2.getData();
							if (data2 instanceof DisplayableEntry) {
								DisplayableEntry dEntry = (DisplayableEntry)data2;
								if (dEntry.getEntry().equals(entryChanged)) {
									return i;
								}
							} else {
								break;
							}
						} while (true);
					}
				}
			}
		}
		return -1;
	}

	private void updateItem(TableItem item) {
		Object obj = item.getData();
        if (obj instanceof IDisplayableItem) {
        	for (int index = 0; index < fTable.getColumnCount(); index++) {
        		IEntriesTableProperty entryData = (IEntriesTableProperty)(fTable.getColumn(index).getData());
				item.setText(index, entryData.getValueFormattedForTable((IDisplayableItem)obj));
        	}
        }
	}
	
	/* (non-Javadoc)
	 * @see net.sf.jmoney.pages.entries.IEntriesControl#removeTransaction(net.sf.jmoney.model2.Entry)
	 */
	public void removeEntryInAccount(Entry entry) {
		// get the index of the row for the transaction
		int i = lookupItem(entry, entry);

		TableItem transItem = fTable.getItem(i);
		DisplayableTransaction dTrans = (DisplayableTransaction)transItem.getData(); 
		Transaction transaction = entry.getTransaction();

		// Determine the color of this row.  It is used to switch the color of
		// all the following rows.
		boolean isAlternated = (alternateTransactionColor.equals(transItem.getBackground()));
		
		// Dispose it
		fTable.getItem(i).dispose();

		// Dispose any following DisplayableEntry rows
		TableItem item = fTable.getItem(i);
		while (item.getData() instanceof DisplayableEntry) {
			item.dispose();
			item = fTable.getItem(i);
		}

		long balance = dTrans.getBalance() - entry.getAmount(); 

		// Recalculate balances from this point onwards.
		updateBalances(i, balance);
		
		// Set colors from this point onwards (colors have switched).
		updateColors(i, isAlternated);
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
		for ( ; i < fTable.getItemCount(); i++) {
			TableItem item = fTable.getItem(i);
			Object data = item.getData(); 
			if (data instanceof DisplayableTransaction) {
				DisplayableTransaction dTrans = (DisplayableTransaction)item.getData();

				balance += dTrans.getEntryForAccountFields().getAmount();
        		dTrans.setBalance(balance);

//				int balanceColumnNumber = visibleEntryDataObjects.size() - 1;
				item.setText(balanceColumnIndex, fPage.balanceColumnManager.getValueFormattedForTable(dTrans));
			}
		}
	}
	
	
	private void updateColors(int i, boolean isAlternated) {
		for ( ; i < fTable.getItemCount(); i++) {
			TableItem item = fTable.getItem(i);
			Object data = item.getData(); 

			if (!(data instanceof DisplayableEntry)) isAlternated = ! isAlternated;
			
			if (isAlternated) {
				if (data instanceof DisplayableTransaction) item.setBackground(alternateTransactionColor);
				if (data instanceof DisplayableEntry)       item.setBackground(alternateEntryColor);
			} else {
				if (data instanceof DisplayableTransaction) item.setBackground(transactionColor);
				if (data instanceof DisplayableEntry)       item.setBackground(entryColor);                
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see net.sf.jmoney.pages.entries.IEntriesControl#updateTransaction(net.sf.jmoney.model2.Entry)
	 */
	public void updateTransaction(Entry entry) {
		int index = lookupItem(entry, entry);
		if (index >= 0) {
			updateItem(fTable.getItem(index));
		}
	}

	/* (non-Javadoc)
	 * @see net.sf.jmoney.pages.entries.IEntriesControl#setOtherEntry(net.sf.jmoney.model2.Entry, net.sf.jmoney.model2.Entry)
	 */
	public void setSelection(Entry entryInAccount, Entry entryToSelect) {
		int index = lookupItem(entryInAccount, entryToSelect);
		if (index >= 0) {
			fTable.setSelection(index);
		}
	}

	/* (non-Javadoc)
	 * @see net.sf.jmoney.pages.entries.IEntriesControl#addEntry(net.sf.jmoney.model2.Entry, net.sf.jmoney.model2.Entry)
	 */
	public void addEntry(Entry entryInAccount, Entry newEntry) {
		// get the index of the row for the transaction
		int i = lookupItem(entryInAccount, entryInAccount);

		int transIndex = i;
		TableItem transItem = fTable.getItem(i);
		DisplayableTransaction dTrans = (DisplayableTransaction)transItem.getData(); 
		Transaction transaction = newEntry.getTransaction();
		
		// scan forwards past the entries in the transaction
		do {
			i++;
		} while (fTable.getItem(i).getData() instanceof DisplayableEntry);

		Color colorOfNewEntry = 
			transItem.getBackground().equals(transactionColor)
			? entryColor : alternateEntryColor;
		
		// If there were no DisplayableEntry rows then this was a
		// simple transaction.  It can no longer be a simple transaction
		// so add a row for entry that had been combined.  We also need
		// to update the combined row.
		
		if (i == transIndex + 1) {
			// The transaction should have three entries,
			// newEntry, entryInAccount, and one other.
			// We need to find the other.
			Entry otherEntry = null;
			for (Iterator iter = transaction.getEntryIterator(); iter.hasNext(); ) {
				Entry entry = (Entry)iter.next();
				if (!entry.equals(entryInAccount) && !entry.equals(newEntry)) {
					if (otherEntry != null) {
						throw new RuntimeException("internal inconsistency");
					}
					otherEntry = entry;
				}
			}
			
			// Create row for entry that was combined.
			DisplayableEntry dEntry = new DisplayableEntry(otherEntry, dTrans);
			TableItem newItem = new TableItem(fTable, SWT.NONE, i);
			newItem.setData(dEntry);
			updateItem(newItem);
			newItem.setBackground(colorOfNewEntry);

			// Update the transaction row.
			updateItem(transItem);
		}
		
		DisplayableEntry dEntry = new DisplayableEntry(newEntry, dTrans);
		TableItem newItem = new TableItem(fTable, SWT.NONE, i);
		newItem.setData(dEntry);
		updateItem(newItem);
		newItem.setBackground(colorOfNewEntry);
	}

	/* (non-Javadoc)
	 * @see net.sf.jmoney.pages.entries.IEntriesControl#removeEntry(net.sf.jmoney.model2.Entry, net.sf.jmoney.model2.Entry)
	 */
	public void removeEntry(Entry entryInAccount, Entry oldEntry) {
		// get the index of the row for the transaction
		int transIndex = lookupItem(entryInAccount, entryInAccount);

		TableItem transItem = fTable.getItem(transIndex);
		DisplayableTransaction dTrans = (DisplayableTransaction)transItem.getData(); 
		Transaction transaction = oldEntry.getTransaction();

		// get the index of the row for the entry
		int i2 = lookupItem(entryInAccount, oldEntry);
		
		// Dispose it
		fTable.getItem(i2).dispose();
		
		// If the transaction is now a simple transaction,
		// dispose the remaining item, and update the transaction
		// row.
		if (dTrans.getEntryForOtherFields() != null) {
			fTable.getItem(transIndex+1).dispose();
			updateItem(transItem);
		}
	}
}
