/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2004 Johann Gyger <jgyger@users.sf.net>
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
import java.util.Iterator;
import java.util.Vector;

import net.sf.jmoney.fields.TransactionInfo;
import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.VerySimpleDateFormat;
import net.sf.jmoney.model2.*;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * TODO
 * 
 * @author Johann Gyger
 */
public class EntriesSection extends SectionPart {

//    protected static final String[] PROPERTIES = { "check", "date", "valuta", "description", "debit", "credit", "balance"};
    protected static final Color alternateEntryColor = new Color(Display.getCurrent(), 191, 191, 191);

    protected VerySimpleDateFormat fDateFormat = new VerySimpleDateFormat(JMoneyPlugin.getDefault().getDateFormat());
    protected EntriesPage fPage;
    protected Table fTable;
    protected TableViewer fViewer;
    protected EntryLabelProvider fLabelProvider;
    

    public EntriesSection(EntriesPage page, Composite parent) {
        super(parent, page.getManagedForm().getToolkit(), Section.TITLE_BAR);
        getSection().setText("All Entries");
        fPage = page;
        createClient(page.getManagedForm().getToolkit());
    }

    public void refresh() {
        fViewer.refresh();
        super.refresh();
    }

    protected void createClient(FormToolkit toolkit) {
        Composite container = toolkit.createComposite(getSection());

        GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        container.setLayout(layout);

        createTable(container, toolkit);
        createTableViewer();

        // Create the button area
		Composite buttonArea = toolkit.createComposite(container);
		
		RowLayout layoutOfButtons = new RowLayout();
		layoutOfButtons.fill = false;
		layoutOfButtons.justify = true;
		buttonArea.setLayout(layoutOfButtons);
		
        // Create the 'add transaction' and 'delete transaction' buttons.
        Button addButton = toolkit.createButton(buttonArea, "New Transaction", SWT.PUSH);
        addButton.addSelectionListener(new SelectionAdapter() {
           public void widgetSelected(SelectionEvent event) {
           		Session session = fPage.getAccount().getSession();
           		Transaction transaction = session.createTransaction();
           		Entry entry1 = transaction.createEntry();
           		Entry entry2 = transaction.createEntry();
           		entry1.setAccount(fPage.getAccount());
           		
           		// Select entry1 in the entries list.
                StructuredSelection selection = new StructuredSelection(entry1);
                fViewer.setSelection(selection, true);
           }
        });

        Button deleteButton = toolkit.createButton(buttonArea, "Delete Transaction", SWT.PUSH);
        deleteButton.addSelectionListener(new SelectionAdapter() {
           public void widgetSelected(SelectionEvent event) {
           		
            if (fViewer.getSelection() instanceof IStructuredSelection) {
                IStructuredSelection s = (IStructuredSelection) fViewer.getSelection();
                Object selectedObject = s.getFirstElement();
                // The selected object cannot be null because the 'delete tranaction'
                // button would be disabled if no entry were selected.
               	if (selectedObject instanceof DisplayableTransaction) {
               		DisplayableTransaction de = (DisplayableTransaction) selectedObject;
               		Transaction transaction = de.entry.getTransaction();
               		transaction.getSession().deleteTransaction(transaction);
               		transaction.getSession().registerUndoableChange("Delete Transaction");
                }
            }
           }
        });
        
        fPage.getAccount().getSession().addSessionChangeListener(new SessionChangeAdapter() {

			public void accountChanged(Account account, PropertyAccessor propertyAccessor, Object oldValue, Object newValue) {
				// An account name will change this view if the account
				// is listed.
				
				
			}

			public void entryAdded(Entry newEntry) {
				// if the entry is in this account, refresh the table.
				if (fPage.getAccount().equals(newEntry.getAccount())) {
					refresh();
				}
				
				// Even if this entry is not in this account, if one of
				// the other entries in the transaction is in this account
				// then it is possible that the table view may need updating
				// because the table view is slightly different for split
				// entries.
				// TODO:
			}

			public void entryDeleted(Entry oldEntry) {
				// if the entry was in this account, refresh the table.
				if (fPage.getAccount().equals(oldEntry.getAccount())) {
					refresh();
				}
				
				// Even if this entry was not in this account, if one of
				// the other entries in the transaction is in this account
				// then it is possible that the table view may need updating
				// because the table view is slightly different for split
				// entries.
				// TODO:
			}

			public void objectAdded(ExtendableObject extendableObject) {
				// TODO Auto-generated method stub
				
			}

			public void objectDeleted(ExtendableObject extendableObject) {
				// TODO Auto-generated method stub
				
			}

			public void objectChanged(ExtendableObject extendableObject, PropertyAccessor propertyAccessor, Object oldValue, Object newValue) {
				if (extendableObject instanceof Entry) {
					Entry entry = (Entry)extendableObject;
					if (fPage.getAccount().equals(entry.getAccount())) {
						refresh();
					}
					
					// If the changed entry is in a transaction with
					// two entries, and the other entry is in the account
					// for the table then we also need to refresh.
					if (entry.getTransaction().hasTwoEntries()
							&& entry.getTransaction().getOther(entry).getAccount() == fPage.getAccount()) {
						refresh();
					}
				}
				
				if (extendableObject instanceof Transaction) {
					Transaction transaction = (Transaction)extendableObject;
					for (Iterator iter = transaction.getEntryIterator(); iter.hasNext(); ) {
						Entry entry = (Entry)iter.next();
						if (entry.getAccount().equals(fPage.getAccount())) {
							refresh();
						}
					}
				}
			}
        	
        });
        
        getSection().setClient(container);
        toolkit.paintBordersFor(container);
        fTable.setTopIndex(fTable.getItemCount()-5);
        refresh();
    }

    protected void createTable(Composite container, FormToolkit toolkit) {
        fTable = toolkit.createTable(container, SWT.FULL_SELECTION);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 100;
        fTable.setLayoutData(gd);

        TableColumn col;
        TableLayout tlayout = new TableLayout();
        

        for (Iterator iter = fPage.allEntryDataObjects.iterator(); iter.hasNext(); ) {
        	EntriesSectionProperty entriesSectionProperty = (EntriesSectionProperty)iter.next();
        	
            col = new TableColumn(fTable, SWT.NULL);
            col.setText(entriesSectionProperty.getText());
            tlayout.addColumnData(new ColumnWeightData(50, entriesSectionProperty.getMinimumWidth()));
        }
/*        
        col = new TableColumn(fTable, SWT.NULL);
        col.setText("Check");
        tlayout.addColumnData(new ColumnWeightData(50, 50));

        col = new TableColumn(fTable, SWT.NULL);
        col.setText("Date");
        tlayout.addColumnData(new ColumnWeightData(50, 70));

        col = new TableColumn(fTable, SWT.NULL);
        col.setText("Valuta");
        tlayout.addColumnData(new ColumnWeightData(50, 70));

        col = new TableColumn(fTable, SWT.NULL);
        col.setText("Description");
        tlayout.addColumnData(new ColumnWeightData(50, 300));

        col = new TableColumn(fTable, SWT.NULL);
        col.setText("To account");
        tlayout.addColumnData(new ColumnWeightData(50, 70));
*/
        col = new TableColumn(fTable, SWT.RIGHT);
        col.setText("Debit");
        tlayout.addColumnData(new ColumnWeightData(50, 70));

        col = new TableColumn(fTable, SWT.RIGHT);
        col.setText("Credit");
        tlayout.addColumnData(new ColumnWeightData(50, 70));

        col = new TableColumn(fTable, SWT.RIGHT);
        col.setText("Balance");
        tlayout.addColumnData(new ColumnWeightData(50, 70));

        fTable.setLayout(tlayout);
        fTable.setHeaderVisible(true);
        fTable.setLinesVisible(true);
        
    }

    protected void createTableViewer() {
        fViewer = new TableViewer(fTable);
        fViewer.setContentProvider(new ContentProvider());
        fLabelProvider = new EntryLabelProvider();
        fViewer.setLabelProvider(fLabelProvider);
        fViewer.setSorter(new EntrySorter());

        String columnProperties[] = new String[fPage.allEntryDataObjects.size() + 3];
        {
        int i = 0;
        for ( ; i < fPage.allEntryDataObjects.size(); i++) {
        	columnProperties[i] = ((EntriesSectionProperty)fPage.allEntryDataObjects.get(i)).getId();
        }
        columnProperties[i++] = "debit";
        columnProperties[i++] = "credit";
        columnProperties[i++] = "balance";
        }
        fViewer.setColumnProperties(columnProperties);

//        fViewer.setCellEditors(createCellEditors());
//        fViewer.setCellModifier(new CellModifier());

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
        boolean isOdd = true;
        for (int i = 0; i < fViewer.getTable().getItemCount(); i++) {
            if (fViewer.getTable().getItem(i).getData() instanceof DisplayableTransaction) {
                isOdd = ! isOdd;
            } 
            if (isOdd) {
                fViewer.getTable().getItem(i).setBackground(alternateEntryColor);
            }
        }

    }

    // TODO This is not implemnted properly yet.
    protected CellEditor[] createCellEditors() {
        CellEditor[] result = new CellEditor[8];
        result[0] = new TextCellEditor(fTable); // check
        result[1] = new TextCellEditor(fTable); // date
        result[2] = new TextCellEditor(fTable); // valuta
        result[3] = new TextCellEditor(fTable); // description
        result[4] = new TextCellEditor(fTable); // to account
        result[5] = new TextCellEditor(fTable); // debit
        result[6] = new TextCellEditor(fTable); // credit
        result[7] = new TextCellEditor(fTable); // balance
        return result;
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
            CapitalAccount account = (CapitalAccount) parent;
            Iterator it = account
				.getSortedEntries(TransactionInfo.getDateAccessor(), false)
				.iterator();
            Vector d_entries = new Vector();
            long saldo = 0;
            while (it.hasNext()) {
                Entry e = (Entry) it.next();
                saldo = saldo + e.getAmount();
                d_entries.add(new DisplayableTransaction(e, saldo));
                if (e.getTransaction().hasMoreThanTwoEntries()) {
                    // Case of an splitted entry. We display the transaction on the first line
                    // and the entries of the transaction on the following ones.
                    Iterator itSubEntries = e.getTransaction().getEntryIterator();
                    while (itSubEntries.hasNext()) {
                        // TODO: create a new class for this usage.
                        d_entries.add(new DisplayableEntry((Entry) itSubEntries.next()));
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
            if (obj instanceof DisplayableItem && ! (obj instanceof DisplayableNewEmptyEntry)) {
        		DisplayableItem de = (DisplayableItem) obj;
        		
        		if (index < fPage.allEntryDataObjects.size()) {
        			EntriesSectionProperty entryData = (EntriesSectionProperty)fPage.allEntryDataObjects.get(index);
        			return entryData.getValueFormattedForTable(de.getEntry());
        		} else {
        			Commodity c = fPage.getAccount().getCurrency();
        			switch (index - fPage.allEntryDataObjects.size()) {
        			case 0: // income
        				return de.getEntry().getAmount() >= 0 ? c.format(de.getEntry().getAmount()) : "";
        			case 1: // expense
        				return de.getEntry().getAmount() < 0 ? c.format(-de.getEntry().getAmount()) : "";
        			case 2: // balance
        			    if (de instanceof DisplayableTransaction) {
        			        return c.format(((DisplayableTransaction)de).balance);
        			    } else { 
        			        return "";
        			    }
        			}
            	}
            }

            return ""; //$NON-NLS-1$
        }

        public Image getColumnImage(Object obj, int index) {
            return null;
        }
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
}
    
