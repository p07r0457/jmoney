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

package net.sf.jmoney.ui.internal.pages.account.capital;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import net.sf.jmoney.fields.TransactionInfo;
import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.CurrencyAccount;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.Transaction;
import net.sf.jmoney.ui.internal.pages.account.capital.EntriesTable.DisplayableItem;
import net.sf.jmoney.ui.internal.pages.account.capital.EntriesTable.DisplayableNewEmptyEntry;
import net.sf.jmoney.ui.internal.pages.account.capital.EntriesTable.DisplayableTransaction;

import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableTreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableTree;
import org.eclipse.swt.custom.TableTreeEditor;
import org.eclipse.swt.custom.TableTreeItem;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
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
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
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

	protected IPropertyControl currentCellPropertyControl = null;
	
    public EntriesTree(Composite container, EntriesPage page) {
		this.fPage = page;
		FormToolkit toolkit = page.getEditor().getToolkit();
    	
    	fTableTree = new TableTree(container, SWT.FULL_SELECTION | SWT.HIDE_SELECTION);
    	Table table = fTableTree.getTable();
    	
        GridData gridData = new GridData(GridData.FILL_BOTH);
        gridData.heightHint = 800;
        gridData.heightHint = 300;
		gridData.horizontalAlignment = GridData.FILL;
		gridData.verticalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		fTableTree.setLayoutData(gridData);

        TableColumn col;
        TableLayout tlayout = new TableLayout();
        
        for (Iterator iter = fPage.allEntryDataObjects.iterator(); iter.hasNext(); ) {
        	IEntriesTableProperty entriesSectionProperty = (IEntriesTableProperty)iter.next();
        	
            col = new TableColumn(table, SWT.NULL);
            col.setText(entriesSectionProperty.getText());
            
            // TODO: figure out how to get the columns to use up all
            // the width of the screen and no more (i.e. no scrolling).
            tlayout.addColumnData(
            		new ColumnWeightData(
            				entriesSectionProperty.getWeight(), 
							entriesSectionProperty.getMinimumWidth()));
            
            visibleEntryDataObjects.add(entriesSectionProperty);
        }

        col = new TableColumn(table, SWT.RIGHT);
        col.setText("Debit");
        tlayout.addColumnData(new ColumnWeightData(2, 70));
        visibleEntryDataObjects.add(fPage.debitColumnManager);

        col = new TableColumn(table, SWT.RIGHT);
        col.setText("Credit");
        tlayout.addColumnData(new ColumnWeightData(2, 70));
        visibleEntryDataObjects.add(fPage.creditColumnManager);

        col = new TableColumn(table, SWT.RIGHT);
        col.setText("Balance");
        tlayout.addColumnData(new ColumnWeightData(2, 70));
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
                    // The selected object might be null.  This occurs when the table is refreshed.
                    // I don't understand this so I am simply bypassing the update
                    // in this case.  Nigel
                    if (selectedObject != null) {
                    	if (selectedObject instanceof DisplayableTransaction) {
                    		DisplayableTransaction de = (DisplayableTransaction) selectedObject;
                    		fPage.fEntrySection.update(de.entry);
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
                if (data instanceof DisplayableEntry)       item.setBackground(alternateEntryColor);
            } else {
                if (data instanceof DisplayableTransaction) item.setBackground(transactionColor);
                if (data instanceof DisplayableEntry)       item.setBackground(entryColor);                
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
		    			if (oldEditor != null) oldEditor.dispose();
		    			
		    			Rectangle clientArea = fTableTree.getClientArea ();
		    			Point pt = new Point (e.x, e.y);

		    			boolean visible = false;
		    			
		    			final TableTreeItem item = fTableTree.getItem (pt);
		    			for (int i=0; i<fTableTree.getTable().getColumnCount (); i++) {
		    				Rectangle rect = item.getBounds (i);
		    				if (rect.contains (pt)) {
		    					final int column = i;
		    					
		    					IEntriesTableProperty entryData = (IEntriesTableProperty)fPage.allEntryDataObjects.get(column);
		    					currentCellPropertyControl = entryData.createAndLoadPropertyControl(fTableTree.getTable(), item.getData());
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
    	
        fTableTree.pack(true);
	}
	
	/* (non-Javadoc)
	 * @see net.sf.jmoney.ui.internal.pages.account.capital.IEntriesControl#setSelection(org.eclipse.jface.viewers.StructuredSelection)
	 */
	public void setSelection(StructuredSelection selection) {
		// TODO Auto-generated method stub

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
            
            assert (o1 instanceof DisplayableItem);
            assert (o2 instanceof DisplayableItem);
            
            // The case of a new Entry
            if (o1 instanceof DisplayableNewEmptyEntry) return 1;
            if (o2 instanceof DisplayableNewEmptyEntry) return -1;

            // Compare the transactions
            DisplayableItem di1 = (DisplayableItem)o1;
            DisplayableItem di2 = (DisplayableItem)o2;
            
            // Case of two items of the same transaction
            if (di1.getTransaction() == (di2.getTransaction())) {
                if (di1 instanceof DisplayableTransaction) return -1;
                if (di2 instanceof DisplayableTransaction) return 1;
                return (di1.getAmount() - di2.getAmount() > 0) ? -1 : 1;
            }
            
            // Case of two different transactions
            Date d1 = di1.getTransaction().getDate();
            Date d2 = di2.getTransaction().getDate();
            // TODO Taken from EntryComparator as a quick fix
            if (d1 == null && d2 == null) return 0;
            if (d1 == null) return 1;
            if (d2 == null) return -1;
            int difference = d1.compareTo(d2); 
            if (difference != 0) return difference;
            // TODO perhaps an other test for transactions with the same date?
            return di1.getTransaction().hashCode() - di2.getTransaction().hashCode(); 
        }
    }

    abstract class DisplayableItem {
        abstract String getCheck ();
        abstract Date   getDate ();
        abstract Date   getValuta ();
        abstract String getDescription ();
        abstract long   getAmount ();
        
        abstract void setCheck (String check);
        abstract void setDate (Date date);
        abstract void setValuta (Date valuata);
        abstract void setDescription (String description);
        abstract void setAmount (long amount);

        // abstract String getOtherAccountName ();
        // abstract long   getBalance ();
        abstract Transaction getTransaction ();
        abstract Entry       getEntry ();

        //TODO: abstract void set/getCommodity();
    }
    
    class DisplayableEntry extends DisplayableItem { 
        
        private Entry entry;

        public DisplayableEntry(Entry entry) {
            this.entry = entry;
        }
        
        /* Trivial functions */
        String getCheck ()        { return entry.getCheck(); }
        Date   getDate ()         { return entry.getTransaction().getDate(); }
        Date   getValuta ()       { return entry.getValuta(); }
        String getDescription ()  { return entry.getDescription(); }
        long   getAmount ()       { return entry.getAmount(); }

        void setCheck (String check)              { entry.setCheck(check); }
        void setDate (Date date)                  { entry.getTransaction().setDate(date); }
        void setValuta (Date valuta)              { entry.setValuta(valuta); }
        void setDescription (String description)  { entry.setDescription(description); }
        void setAmount (long amount)              { entry.setAmount(amount); }

        String      getAccountName () {return entry.getAccount().getName(); }
        Transaction getTransaction () {return entry.getTransaction(); }
        Entry getEntry() { return entry; }
    }
    
    
    /**
     * This class is to be used for new entries which are currently filled in by the
     * user, before they are comited and transformed in real entries
     * @author Faucheux
     */
    class DisplayableNewEmptyEntry extends DisplayableItem {
        
        String check = null;
        Date   date = null;
        Date   valuta = null;
        String description = null;
        long   amount = 0;
        
        public DisplayableNewEmptyEntry(){
            // 	constructor to define a new, empty entry which we be filled in by the user
        }

        String getCheck ()        { return check; }
        Date   getDate ()         { return date; }
        Date   getValuta ()       { return valuta; }
        String getDescription ()  { return description; }
        long   getAmount ()       { return amount;}
        
        void setCheck (String check)              { this.check = check; }
        void setDate (Date date)                  { this.date = date; }
        void setValuta (Date valuta)              { this.valuta = valuta; }
        void setDescription (String description)  { this.description = description; }
        void setAmount (long amount)              { this.amount = amount; }

        String getOtherAccountName () { return null; }
        long   getBalance () { return 0; }
        Transaction getTransaction () {return null; }
        Entry getEntry () { return null ; }
    }

    class DisplayableTransaction extends DisplayableItem { 
        
        private Entry entry;
        private long balance;

        public DisplayableTransaction(Entry entry, long saldo) {
            this.entry = entry;
            this.balance = saldo;
        }
        
        /* Trivial functions */
        String getCheck ()        { return entry.getCheck(); }
        Date   getDate ()         { return entry.getTransaction().getDate(); }
        Date   getValuta ()       { return entry.getValuta(); }
        String getDescription ()  { return entry.getDescription(); }
        long   getAmount ()       { return entry.getAmount(); }

        void setCheck (String check)              { entry.setCheck(check); }
        void setDate (Date date)                  { entry.getTransaction().setDate(date); }
        void setValuta (Date valuta)              { entry.setValuta(valuta); }
        void setDescription (String description)  { entry.setDescription(description); }
        void setAmount (long amount)              { entry.setAmount(amount); }

        String getOtherAccountName () {
            Transaction t = entry.getTransaction();
            if (t.hasMoreThanTwoEntries())
                return "-- splitted --";
            else
                return t.getOther(entry).getAccount().getName();
        }

        long  getBalance () { return balance ; }
        Entry getEntry() { return entry; }
        Transaction getTransaction () {return entry.getTransaction(); }
        
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
            fPage.entryToContentMap = new HashMap();
            Vector d_entries = new Vector();
            long saldo = account.getStartBalance();
            while (it.hasNext()) {
                Entry e = (Entry) it.next();
                saldo = saldo + e.getAmount();
                DisplayableTransaction de = new DisplayableTransaction(e, saldo);
                fPage.entryToContentMap.put(e, de);
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
        		DisplayableTransaction dTrans = (DisplayableTransaction)parent; 
        		Iterator itSubEntries = dTrans.getTransaction().getEntryIterator();
        		while (itSubEntries.hasNext()) {
        			// TODO: create a new class for this usage.
        			Entry entry = (Entry) itSubEntries.next();
        			if (entry != dTrans.getEntry()) {
        				d_entries.add(new DisplayableEntry(entry));
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
            if (obj instanceof DisplayableItem && ! (obj instanceof DisplayableNewEmptyEntry)) {
        		DisplayableItem de = (DisplayableItem) obj;
        	
       			IEntriesTableProperty entryData = (IEntriesTableProperty)visibleEntryDataObjects.get(index);
   				return entryData.getValueFormattedForTable(de);
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

	/* (non-Javadoc)
	 * @see net.sf.jmoney.ui.internal.pages.account.capital.IEntriesControl#dispose()
	 */
	public void dispose() {
		fTableTree.dispose();
	}	
}
