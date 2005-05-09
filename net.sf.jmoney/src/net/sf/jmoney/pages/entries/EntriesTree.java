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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import net.sf.jmoney.fields.TransactionInfo;
import net.sf.jmoney.model2.CurrencyAccount;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.PropertyAccessor;
import net.sf.jmoney.model2.Transaction;

import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableTreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableTree;
import org.eclipse.swt.custom.TableTreeEditor;
import org.eclipse.swt.custom.TableTreeItem;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionListener;
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
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * An implementation of IEntriesControl that displays the entries
 * in a table tree.  The tree contains one top level row for each
 * entry in the account.  The child rows show the other entries in
 * the transaction.
 */
public class EntriesTree implements IEntriesControl {

//    protected static final Color transactionColor          = new Color(Display.getCurrent(), 125, 215, 060);
//    protected static final Color alternateTransactionColor = new Color(Display.getCurrent(), 100, 160, 200);
    protected static final Color transactionColor          = new Color(Display.getCurrent(), 237, 237, 255);
    protected static final Color alternateTransactionColor = new Color(Display.getCurrent(), 237, 255, 237);
    protected static final Color entryColor                = new Color(Display.getCurrent(), 180, 225, 140);
    protected static final Color alternateEntryColor       = new Color(Display.getCurrent(), 135, 185, 205);

	protected EntriesPage fPage;
	protected TableTree fTableTree;
    protected TableTreeViewer fViewer;
    protected EntryLabelProvider fLabelProvider;
    
    /** Element: EntriesSectionProperty */
    Vector visibleEntryDataObjects = new Vector();

    /**
	 * Map Entry objects to TableItem objects. This map is needed because an
	 * efficient refresh of the table requires the TableItem object, whereas the
	 * listener that listens for changes to the model receives Entry and
	 * Transaction objects.
	 */
	Map entryToContentMap;
    
	protected IPropertyControl currentCellPropertyControl = null;
	
    public EntriesTree(Composite container, EntriesPage page) {
		this.fPage = page;
		FormToolkit toolkit = page.getEditor().getToolkit();
    	
    	fTableTree = new TableTree(container, SWT.FULL_SELECTION | SWT.HIDE_SELECTION);
    	Table table = fTableTree.getTable();
    	
        GridData gridData = new GridData(GridData.FILL_BOTH);
        gridData.heightHint = 500;
        gridData.widthHint = 100;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		fTableTree.setLayoutData(gridData);

        TableColumn col;
        TableLayout tlayout = new TableLayout();
        
        for (Iterator iter = fPage.allEntryDataObjects.iterator(); iter.hasNext(); ) {
        	IEntriesTableProperty entriesSectionProperty = (IEntriesTableProperty)iter.next();
        	
            col = new TableColumn(table, SWT.NULL);
            col.setText(entriesSectionProperty.getText());
            
            col.setData("layoutData",
            		new ColumnWeightData(
            				entriesSectionProperty.getWeight(), 
							entriesSectionProperty.getMinimumWidth()));
            
            visibleEntryDataObjects.add(entriesSectionProperty);
        }

        col = new TableColumn(table, SWT.RIGHT);
        col.setText("Debit");
        col.setData("layoutData", new ColumnWeightData(2, 70));
        visibleEntryDataObjects.add(fPage.debitColumnManager);

        col = new TableColumn(table, SWT.RIGHT);
        col.setText("Credit");
        col.setData("layoutData", new ColumnWeightData(2, 70));
        visibleEntryDataObjects.add(fPage.creditColumnManager);

        col = new TableColumn(table, SWT.RIGHT);
        col.setText("Balance");
        col.setData("layoutData", new ColumnWeightData(2, 70));
        visibleEntryDataObjects.add(fPage.balanceColumnManager);

        
        table.setLayout(tlayout);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);

        fViewer = new TableTreeViewer(fTableTree);
        fViewer.setContentProvider(new ContentProvider());
        fLabelProvider = new EntryLabelProvider();
        fViewer.setLabelProvider(fLabelProvider);
        fViewer.setSorter(new EntrySorter());

        String[] columnProperties = new String[visibleEntryDataObjects.size()];
        for (int i = 0; i < fPage.allEntryDataObjects.size(); i++) {
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
                    		fPage.fEntrySection.update(transactionData.getEntryForAccountFields(), transactionData.getEntryForAccountFields());
                    	} else if (selectedObject instanceof DisplayableEntry) {
                    		transactionData = ((DisplayableEntry)data).getDisplayableTransaction();
                    		fPage.fEntrySection.update(transactionData.getEntryForAccountFields(), ((DisplayableEntry)data).entry);
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
        TableTreeItem [] items = fViewer.getTableTree().getItems();
        for (int i = 0; i < fViewer.getTableTree().getItemCount(); i++) {
            TableTreeItem item = items[i];
            Object data = item.getData();
            
            // alternate the color
            if (data instanceof DisplayableTransaction) isAlternated = ! isAlternated;
            
            if (isAlternated) {
                if (data instanceof DisplayableTransaction) item.setBackground(alternateTransactionColor);
                if (data instanceof Entry)                  item.setBackground(alternateEntryColor);
            } else {
                if (data instanceof DisplayableTransaction) item.setBackground(transactionColor);
                if (data instanceof Entry)                  item.setBackground(entryColor);                
            }
        }

        // Create the editor.
        
    	final TableTreeEditor editor = new TableTreeEditor(fTableTree);
    	//The editor must have the same size as the cell and must
    	//not be any smaller than 50 pixels.
    	editor.horizontalAlignment = SWT.LEFT;
    	editor.grabHorizontal = true;
    	editor.minimumWidth = 50;
    	
    	fTableTree.getTable().addMouseListener(new MouseAdapter() {
/*    	    		
    	    		public void handleEvent(Event e) {
    			// Clean up any previous editor control
    			Control oldEditor = editor.getEditor();
    			if (oldEditor != null) oldEditor.dispose();
    			
    			Rectangle clientArea = fTableTree.getTable().getClientArea ();
    			Point pt = new Point (e.x, e.y);
    			//int index = fTableTree.getTable().getTopIndex ();
    			//while (index < fTableTree.getTable().getItemCount ()) {
    				boolean visible = false;

    				// Identify the selected row
        			TableTreeItem item2 = (TableTreeItem)e.item;
        			if (item2 == null) return;
        			
    				final TableTreeItem item = fTableTree.getItem (pt);
    				for (int i=0; i<fTableTree.getTable().getColumnCount (); i++) {
    					Rectangle rect = item.getBounds (i);
    					if (rect.contains (pt)) {
    						final int column = i;
    						final Text text = new Text (fTableTree.getTable(), SWT.NONE);
    			
    						editor.setEditor (text, item, i);
    						text.setText (item.getText (i));
    						text.selectAll ();
    						text.setFocus ();
    						return;
    					}
    					if (!visible && rect.intersects (clientArea)) {
    						visible = true;
    					}
    				}
    				if (!visible) return;
    				//index++;
    			//}
    			
    			
    			// The control that will be the editor must be a child of the Table
    			Text newEditor = new Text(fTableTree.getTable(), SWT.NONE);
    			newEditor.setText(item.getText(EDITABLECOLUMN));
    			newEditor.addModifyListener(new ModifyListener() {
    				public void modifyText(ModifyEvent e) {
    					Text text = (Text)editor.getEditor();
    					editor.getItem().setText(EDITABLECOLUMN, text.getText());
    				}
    			});
    			newEditor.selectAll();
    			newEditor.setFocus();
    			editor.setEditor(newEditor, item, EDITABLECOLUMN);
    		}
*/
					public void mouseDoubleClick(MouseEvent e) {
						// TODO Auto-generated method stub
						
					}

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
		    			
		    			Rectangle clientArea = fTableTree.getClientArea ();
		    			Point pt = new Point (e.x, e.y);

		    			boolean visible = false;
		    			
		    			final TableTreeItem item = fTableTree.getItem (pt);
		    			for (int i=0; i<fTableTree.getTable().getColumnCount (); i++) {
		    				Rectangle rect = item.getBounds (i);
		    				if (rect.contains (pt)) {
		    					final int column = i;
		    					
		    					IEntriesTableProperty entryData = (IEntriesTableProperty)fPage.allEntryDataObjects.get(column);
		    					IDisplayableItem data = (IDisplayableItem)item.getData();
		    					currentCellPropertyControl = entryData.createAndLoadPropertyControl(fTableTree.getTable(), data);
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
		    			
		    			// The control that will be the editor must be a child of the Table
/*
		    			Text newEditor = new Text(fTableTree.getTable(), SWT.NONE);
		    			newEditor.setText(item.getText(EDITABLECOLUMN));
		    			newEditor.addModifyListener(new ModifyListener() {
		    				public void modifyText(ModifyEvent e) {
		    					Text text = (Text)editor.getEditor();
		    					editor.getItem().setText(EDITABLECOLUMN, text.getText());
		    				}
		    			});
*/		    			
					}
    	});
	}
	
	/* (non-Javadoc)
	 * @see net.sf.jmoney.ui.internal.pages.account.capital.IEntriesControl#setSelection(org.eclipse.jface.viewers.StructuredSelection)
	 */
	public void setSelection(Entry entryInAccount, Entry entryToSelect) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see net.sf.jmoney.ui.internal.pages.account.capital.IEntriesControl#getSelectedEntry()
	 */
	public Entry getSelectedEntryInAccount() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see net.sf.jmoney.ui.internal.pages.account.capital.IEntriesControl#getSelectedEntry()
	 */
	public Entry getSelectedEntry() {
		// TODO Auto-generated method stub
		return null;
	}

    /**
     * Class to sort two displayable items.
     * For the time: two transactions are sorted by the date,
     * two Entries are sort, first by their transaction then (if they belong the same transaction)
     * by the amount.
     * <p><p>
     * In all case, a new Entry is sorted as least.
     * 
     * @author Faucheux
     */
    class EntrySorter extends ViewerSorter {
        public int compare(Viewer notused, Object o1, Object o2) {
            // The case of a new Entry
            if (o1 instanceof DisplayableNewEmptyEntry) return 1;
            if (o2 instanceof DisplayableNewEmptyEntry) return -1;

            // Elements must be either both DisplayableTransaction
            // objects or both Entry objects.
            
            // Compare transactions
            if (o1 instanceof DisplayableTransaction) {
            	DisplayableTransaction t1 = (DisplayableTransaction)o1;
            	DisplayableTransaction t2 = (DisplayableTransaction)o2;
            	return t1.getSequenceNumber() - t2.getSequenceNumber();
            }
            
            // Compare entries
            if (o1 instanceof Entry) {
            	Entry e1 = (Entry)o1;
            	Entry e2 = (Entry)o2;
                return (e1.getAmount() - e2.getAmount() > 0) ? -1 : 1;
            }
            
            // We should not get here
            return 0;
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

		/**
		 * @return
		 */
		public Transaction getTransaction() {
			return entry.getTransaction();
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
		public Entry getSelectedEntry() {
			// TODO Auto-generated method stub
			return null;
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
		public Entry getSelectedEntry() {
			// TODO Auto-generated method stub
			return null;
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
		public Entry getSelectedEntry() {
			// TODO Auto-generated method stub
			return null;
		}
    }

    private class ContentProvider implements ITreeContentProvider {
        public void dispose() {
            // TODO Auto-generated method stub
        }
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            // TODO Auto-generated method stub
        }
        /** 
         * Returns the elements the table has to display. 
         * Only the transaction are displayed here. If a transaction has several entries, these
         * one are give to the table through getChildrens.
         * <p><p>
         * We should give the entry back with other informations the display
         * needs, for example the saldo.
         */
        public Object[] getElements(Object parent) {
            CurrencyAccount account = (CurrencyAccount) parent;
            Iterator it = account
				.getSortedEntries(TransactionInfo.getDateAccessor(), false)
				.iterator();
            entryToContentMap = new HashMap();
            Vector d_entries = new Vector();
            long saldo = account.getStartBalance();
            int sequenceNumber = 0;
            while (it.hasNext()) {
                Entry e = (Entry) it.next();
                saldo = saldo + e.getAmount();
                DisplayableTransaction de = new DisplayableTransaction(e, saldo, sequenceNumber++);
                entryToContentMap.put(e, de);
                d_entries.add(de);
             }
            
            // an empty line to have the possibility to enter a new entry
            d_entries.add(new DisplayableNewEmptyEntry());

            return d_entries.toArray();
        }
        
        /** 
         * Returns the children of an element.
         * Only transactions have children.
         */
        public Object[] getChildren(Object parent) {
        	Vector d_entries = new Vector();
        	
        	if (parent instanceof DisplayableTransaction) {
                // Case of an splitted entry. We display the transaction on the first line
                // and the entries of the transaction on the following ones.
            	// However, the transaction line also holds the properties for the entry
            	// in this account, so display just the other entries underneath.
        		DisplayableTransaction dTrans = (DisplayableTransaction)parent; 
        		Iterator itSubEntries = dTrans.getTransaction().getEntryCollection().iterator();
        		while (itSubEntries.hasNext()) {
                    Entry entry2 = (Entry) itSubEntries.next();
                    if (!entry2.equals(dTrans.getEntryForAccountFields())) {
                    	d_entries.add(new DisplayableEntry(entry2, dTrans));
                    }
        		}
        	}

            return d_entries.toArray();
        }
		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
		 */
		public Object getParent(Object element) {
			if (element instanceof CurrencyAccount) {
				return null;
			} else {
				return fPage.getAccount();
			}
		}

		/**
		 * Returns true only if the displayed transaction is a splitted one.
		 * @author Faucheux
		 */
		public boolean hasChildren(Object element) {
		    return (element instanceof DisplayableTransaction
		            && ((DisplayableTransaction) element).getTransaction().hasMoreThanTwoEntries());
		}
    }

    private class EntryLabelProvider extends LabelProvider implements ITableLabelProvider {
        protected NumberFormat nf = DecimalFormat.getCurrencyInstance();

        public String getColumnText(Object obj, int index) {
            if (obj instanceof IDisplayableItem) {
       			IEntriesTableProperty entryData = (IEntriesTableProperty)visibleEntryDataObjects.get(index);
            	IDisplayableItem data = (IDisplayableItem)obj;
   				return entryData.getValueFormattedForTable(data);
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
/*	
	public void update(Object element) {
		fViewer.update(element, null);
	}	
*/
	/* (non-Javadoc)
	 * @see net.sf.jmoney.ui.internal.pages.account.capital.IEntriesControl#dispose()
	 */
	public void dispose() {
		fTableTree.dispose();
	}

	/* (non-Javadoc)
	 * @see net.sf.jmoney.pages.entries.IEntriesControl#addTransaction(net.sf.jmoney.model2.Entry)
	 */
	public void addEntryInAccount(Entry entry) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see net.sf.jmoney.pages.entries.IEntriesControl#updateEntry(net.sf.jmoney.model2.Entry)
	 */
	public void updateEntry(Entry entryInAccount, Entry entryChanged, PropertyAccessor propertyAccessor, Object oldValue, Object newValue) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see net.sf.jmoney.pages.entries.IEntriesControl#removeTransaction(net.sf.jmoney.model2.Entry)
	 */
	public void removeEntryInAccount(Entry entry) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see net.sf.jmoney.pages.entries.IEntriesControl#updateTransaction(net.sf.jmoney.model2.Entry)
	 */
	public void updateTransaction(Entry entry) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see net.sf.jmoney.pages.entries.IEntriesControl#addEntry(net.sf.jmoney.model2.Entry, net.sf.jmoney.model2.Entry)
	 */
	public void addEntry(Entry entryInAccount, Entry newEntry) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see net.sf.jmoney.pages.entries.IEntriesControl#removeEntry(net.sf.jmoney.model2.Entry, net.sf.jmoney.model2.Entry)
	 */
	public void removeEntry(Entry entryInAccount, Entry oldEntry) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see net.sf.jmoney.pages.entries.IEntriesControl#addSelectionListener(org.eclipse.swt.events.SelectionListener)
	 */
	public void addSelectionListener(SelectionListener tableSelectionListener) {
		fTableTree.addSelectionListener(tableSelectionListener);
	}

	/* (non-Javadoc)
	 * @see net.sf.jmoney.pages.entries.IEntriesControl#getControl()
	 */
	public Control getControl() {
		return fTableTree;
	}
}
