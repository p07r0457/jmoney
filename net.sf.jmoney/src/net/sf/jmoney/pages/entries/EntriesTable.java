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

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.fields.TransactionInfo;
import net.sf.jmoney.model2.CurrencyAccount;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.Transaction;

import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
//import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
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
    protected TableViewer fViewer;

    /** Element: EntriesSectionProperty */
    Vector visibleEntryDataObjects = new Vector();
    
	protected IPropertyControl currentCellPropertyControl = null;
	
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
        
        for (Iterator iter = fPage.allEntryDataObjects.iterator(); iter.hasNext(); ) {
        	IEntriesTableProperty entriesSectionProperty = (IEntriesTableProperty)iter.next();
        	
        	final TableColumn col = new TableColumn(fTable, SWT.NULL);
            col.setText(entriesSectionProperty.getText());
            
            tlayout.addColumnData(
            		new ColumnWeightData(
            				entriesSectionProperty.getWeight(), 
							entriesSectionProperty.getMinimumWidth()));
					
            visibleEntryDataObjects.add(entriesSectionProperty);

            col.addSelectionListener(new SelectionAdapter() {
            	public void widgetSelected(SelectionEvent event) {
            	col.setImage(JMoneyPlugin.createImageDescriptor("icons/ArrowUp.gif").createImage());
            	}
            	});        
        }

        TableColumn col;

        col = new TableColumn(fTable, SWT.RIGHT);
        col.setText("Debit");
        tlayout.addColumnData(new ColumnWeightData(2, 70));
        visibleEntryDataObjects.add(fPage.debitColumnManager);
        
        col = new TableColumn(fTable, SWT.RIGHT);
        col.setText("Credit");
        tlayout.addColumnData(new ColumnWeightData(2, 70));
        visibleEntryDataObjects.add(fPage.creditColumnManager);

        col = new TableColumn(fTable, SWT.RIGHT);
        col.setText("Balance");
        tlayout.addColumnData(new ColumnWeightData(2, 70));
        visibleEntryDataObjects.add(fPage.balanceColumnManager);

        fTable.setLayout(tlayout);
        fTable.setHeaderVisible(true);
        fTable.setLinesVisible(true);
        
        fViewer = new TableViewer(fTable);
        fViewer.setContentProvider(new ContentProvider());
        fViewer.setLabelProvider(new EntryLabelProvider());
        fViewer.setSorter(new EntrySorter());
        fViewer.setUseHashlookup(true);
        
        String[] columnProperties = new String[visibleEntryDataObjects.size()];
        for (int i = 0; i < visibleEntryDataObjects.size(); i++) {
        	columnProperties[i] = ((IEntriesTableProperty)visibleEntryDataObjects.get(i)).getId();
        }
        fViewer.setColumnProperties(columnProperties);

        fViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                // Display the details of the new selected entry in the fields
                // TODO: update the fields for all AbstractDispayableEntry
                if (event.getSelection() instanceof IStructuredSelection) {
                    IStructuredSelection s = (IStructuredSelection) event.getSelection();
                    Object selectedObject = s.getFirstElement();
                    // TODO: This code is duplicated below.
                    // The selected object might be null.  This occurs when the table is refreshed.
                    // I don't understand this so I am simply bypassing the update
                    // in this case.  Nigel
                    if (selectedObject != null) {
                    	IDisplayableItem data = (IDisplayableItem)selectedObject;
                    	DisplayableTransaction transactionData = null;
                    	if (selectedObject instanceof DisplayableTransaction) {
                    		transactionData = (DisplayableTransaction) data;
                    	} else if (selectedObject instanceof DisplayableEntry) {
                    		transactionData = ((DisplayableEntry)data).getDisplayableTransaction();
                    	}
                    	if (transactionData != null) {
                    		fPage.fEntrySection.update(transactionData.getEntryForAccountFields());
                    	}
                    }
                }
            }
        });

        fViewer.setInput(fPage.getAccount());

        /*
         * Alternate the color of the entries to improve the readibility 
         * Remark: I'm not sure it's the better implementation. Not sure too
         * that it's works when the entries are changed (inserted or deleted)...
         */
        boolean isAlternated = true;
        for (int i = 0; i < fViewer.getTable().getItemCount(); i++) {
            TableItem item = fViewer.getTable().getItem(i);
            Object data = item.getData();
            // alternate the color
            if (data instanceof DisplayableTransaction) isAlternated = ! isAlternated;
            
            if (isAlternated) {
                if (data instanceof DisplayableTransaction) item.setBackground(alternateTransactionColor);
                if (data instanceof DisplayableEntry)       item.setBackground(alternateEntryColor);
            } else {
                if (data instanceof DisplayableTransaction) item.setBackground(transactionColor);
                if (data instanceof DisplayableEntry)       item.setBackground(entryColor);                
            }
        }

        
		fTable.setTopIndex(fTable.getItemCount()-5);

        // Create the editor.
        
    	final TableEditor editor = new TableEditor(fTable);
    	
    	//The editor must have the same size as the cell and must
    	//not be any smaller than 50 pixels.
    	editor.horizontalAlignment = SWT.LEFT;
    	editor.grabHorizontal = true;
    	editor.minimumWidth = 50;

    	fTable.addMouseListener(new MouseAdapter() {
    		public void mouseDown(MouseEvent e) {
    			// Clean up any previous editor control
    			Control oldEditor = editor.getEditor();
    			if (oldEditor != null) {
    				// Save the value from the edit control.
    				// TODO: figure out the sequence of events that lead to
    				// these values being null and clean this up.
    				try {
    				currentCellPropertyControl.save();
    				oldEditor.dispose();
    				currentCellPropertyControl = null;
    				} catch (NullPointerException e2) {
    					// Should not happen.
    				}
    			}
    			
    			Rectangle clientArea = fTable.getClientArea ();
    			Point pt = new Point (e.x, e.y);
    			boolean visible = false;
    			
    			final TableItem item = fTable.getItem (pt);
    			for (int i=0; i<fTable.getColumnCount (); i++) {
    				Rectangle rect = item.getBounds (i);
    				if (rect.contains (pt)) {
    					final int column = i;
    					
    					IEntriesTableProperty entryData = (IEntriesTableProperty)visibleEntryDataObjects.get(column);
    					IDisplayableItem data = (IDisplayableItem)item.getData();
    					currentCellPropertyControl = entryData.createAndLoadPropertyControl(fTable, data);
    					// If a control was created (i.e. this was not a
    					// non-editable column such as the balance column)
    					if (currentCellPropertyControl != null) {
    						Control c = currentCellPropertyControl.getControl();
    						editor.setEditor (c, item, i);
    						c.setFocus ();
    					}
    					
    					return;
    				}
    				if (!visible && rect.intersects (clientArea)) {
    					visible = true;
    				}
    			}
    			if (!visible) return;
    		}
    	});
	}

	/* (non-Javadoc)
	 * @see net.sf.jmoney.ui.internal.pages.account.capital.IEntriesControl#setSelection(org.eclipse.jface.viewers.StructuredSelection)
	 */
	public void setSelection(StructuredSelection selection) {
        fViewer.setSelection(selection, true);
	}

	/* (non-Javadoc)
	 * @see net.sf.jmoney.ui.internal.pages.account.capital.IEntriesControl#getSelection()
	 */
	public Entry getSelectedEntry() {
        if (fViewer.getSelection() instanceof IStructuredSelection) {
            IStructuredSelection s = (IStructuredSelection) fViewer.getSelection();
            Object selectedObject = s.getFirstElement();
            // The selected object cannot be null because the 'delete tranaction'
            // button would be disabled if no entry were selected.
            // TODO This code is duplicated above.
            
            if (selectedObject != null) {
            	IDisplayableItem data = (IDisplayableItem)selectedObject;
            	DisplayableTransaction transactionData = null;
            	if (selectedObject instanceof DisplayableTransaction) {
            		transactionData = (DisplayableTransaction) data;
            	} else if (selectedObject instanceof DisplayableEntry) {
            		transactionData = ((DisplayableEntry)data).getDisplayableTransaction();
            	}
            	if (transactionData != null) {
            		return transactionData.getEntryForAccountFields();
            	} else {
            		return null;
            	}
            }
        }
        
        return null;
	}

	
    /**
     * Class to sort two displayable items.
     * For the time: two transactions are sorted by the date,
     * two Entries are sort, first by their transaction then (if they belong the same transaction)
     * by the amount.
     * 
     * In all case, a new Entry is sorted as least.
     * 
     * @author Faucheux
     */
    class EntrySorter extends ViewerSorter {
        public int compare(Viewer notused, Object o1, Object o2) {
            
            // The case of a new Entry
            if (o1 instanceof DisplayableNewEmptyEntry) return 1;
            if (o2 instanceof DisplayableNewEmptyEntry) return -1;

            // Compare the transactions
            DisplayableTransaction t1 = getDisplayableTransaction(o1);
            DisplayableTransaction t2 = getDisplayableTransaction(o2);
            
            if (t1 == t2) {
            	// Case of two items of the same transaction
                if (o1 instanceof DisplayableTransaction) return -1;
                if (o2 instanceof DisplayableTransaction) return 1;
                long amount1 = ((DisplayableEntry)o1).getEntry().getAmount();
                long amount2 = ((DisplayableEntry)o2).getEntry().getAmount();
                if (amount1 != amount2) {
                if (amount1 > 0 || amount2 > 0) {
                	return (amount1 > amount2) ? -1 : 1;
                } else {
                	return (amount1 < amount2) ? -1 : 1;
                }
                } else {
                	// The order does not matter
                	return 0;
                }
            }
            
            // Case of two different transactions
            Date d1 = t1.getTransactionForTransactionFields().getDate();
            Date d2 = t2.getTransactionForTransactionFields().getDate();
            if (d1 != null || d2 != null) {
            	if (d1 == null) return 1;
            	if (d2 == null) return -1;
            	int difference = d1.compareTo(d2); 
            	if (difference != 0) return difference;
            }
            
            // We must NOT return zero if the two transactions are in fact
            // different, even if the dates are the same and we do not care about
            // the order.  The reason is that we must sort the transactions into
            // the same order as was used when the balances were calculated.  If
            // the transactions with the same date are displayed in a different order
            // than was used when the balances were calculated then the balances will
            // look incorrect.
            
            // There is another reason why we must not return zero
            // for transactions that are different.  The entries must be sorted after
            // the containing transaction.  To ensure that entries are sorted correctly
            // after the transaction, all transactions must be different.
            // The hashCode method may return the same value for two different
            // transactions.
            // Therefore use of the hashCode method could cause entries to be muddled up (tho it is extremely
            // unlikely).
            
            // The sure way is to look up the DisplayableTransaction.
            // Each DisplayableTransaction has a sequence number that
            // was assigned when the balances were set.  Use of this
            // sequence number solves both the above issues.

            return getDisplayableTransaction(o1).getSequenceNumber()
				- getDisplayableTransaction(o2).getSequenceNumber();
        }
        
        private DisplayableTransaction getDisplayableTransaction(Object o) {
        	if (o instanceof DisplayableEntry) {
        		return ((DisplayableEntry)o).getDisplayableTransaction();
        	} else {
        		return (DisplayableTransaction)o;
        	}
        }
    }

    class DisplayableTransaction implements IDisplayableItem {
        private Entry entry;
        private long balance;
        private int sequenceNumber;

        public DisplayableTransaction(Entry entry, long saldo, int sequenceNumber) {
            this.entry = entry;
            this.balance = saldo;
            this.sequenceNumber = sequenceNumber;
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
		 * @return a number that is assigned sequentially to
		 * 		DisplayableTransaction objects in the order
		 * 		in which the balance was calculated
		 */
		public int getSequenceNumber() {
			return sequenceNumber;
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

    class ContentProvider implements IStructuredContentProvider {
        public void dispose() {
            // TODO Auto-generated method stub
        }
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            // TODO Auto-generated method stub
        }
        /** 
         * Returns the elements the table has to display. 
         * We should give the entry back with other informations the display
         * needs, for example the saldo.
         */
        public Object[] getElements(Object parent) {
            CurrencyAccount account = (CurrencyAccount) parent;
            Iterator it = account
				.getSortedEntries(TransactionInfo.getDateAccessor(), false)
				.iterator();
            fPage.entryToContentMap = new HashMap();
            Vector d_entries = new Vector();
            long saldo = account.getStartBalance();
            int sequenceNumber = 0;
            while (it.hasNext()) {
                Entry e = (Entry) it.next();
                saldo = saldo + e.getAmount();
                DisplayableTransaction data = new DisplayableTransaction(e, saldo, sequenceNumber++);
                fPage.entryToContentMap.put(e, data);
                d_entries.add(data);
                if (e.getTransaction().hasMoreThanTwoEntries()) {
                    // Case of an splitted entry. We display the transaction on the first line
                    // and the entries of the transaction on the following ones.
                	// However, the transaction line also holds the properties for the entry
                	// in this account, so display just the other entries underneath.
                    Iterator itSubEntries = e.getTransaction().getEntryIterator();
                    while (itSubEntries.hasNext()) {
                        Entry entry2 = (Entry) itSubEntries.next();
                        if (!entry2.equals(e)) {
                            DisplayableEntry entryData = new DisplayableEntry(entry2, data);
                        	d_entries.add(entryData);
                        }
                    }
                }
            }
            
            // an empty line to have the possibility to enter a new entry
            d_entries.add(new DisplayableNewEmptyEntry());

            return d_entries.toArray();
        }
    }

    class EntryLabelProvider extends LabelProvider implements ITableLabelProvider {
        protected NumberFormat nf = DecimalFormat.getCurrencyInstance();

        public String getColumnText(Object obj, int index) {
            if (obj instanceof IDisplayableItem) {
       			IEntriesTableProperty entryData = (IEntriesTableProperty)visibleEntryDataObjects.get(index);
   				return entryData.getValueFormattedForTable((IDisplayableItem)obj);
            }

            return ""; //$NON-NLS-1$
        }

        public Image getColumnImage(Object obj, int index) {
            return null;
        }
    }

    /**
	 * Refresh the viewer.
	 */
	public void refresh() {
		fViewer.refresh();
	}	

	/**
	 * Update the viewer.
	 */
	public void update(Object element) {
		fViewer.update(element, null);
	}	

	public void dispose() {
		fTable.dispose();
	}
}
