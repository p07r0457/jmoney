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
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.VerySimpleDateFormat;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.Transaction;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
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

    protected static final String[] PROPERTIES = { "check", "date", "valuta", "description", "debit", "credit", "balance"};
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

        fViewer.setColumnProperties(PROPERTIES);
        fViewer.setCellEditors(createCellEditors());
        fViewer.setCellModifier(new CellModifier());

        fViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                // Display the details of the new selected entry in the fields
                // TODO: update the fields for all AbstractDispayableEntry
                if (event.getSelection() instanceof IStructuredSelection) {
                    IStructuredSelection s = (IStructuredSelection) event.getSelection();
                    if (s.getFirstElement() instanceof DisplayableEntry) {
                        DisplayableEntry de = (DisplayableEntry) s.getFirstElement();
                        fPage.fEntrySection.update(de.entry);
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
        for (int i = 0; i < fViewer.getTable().getItemCount(); i += 2) {
            fViewer.getTable().getItem(i).setBackground(alternateEntryColor);
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
            List entries = new LinkedList();

            Iterator it = account.getEntriesIterator(account.getSession());
            while (it.hasNext()) {
                entries.add(it.next());
            }

            Collections.sort(entries, new Comparator() {
                public int compare(Object a, Object b) {
                    Date d1 = ((Entry) a).getTransaction().getDate();
                    Date d2 = ((Entry) b).getTransaction().getDate();
                    // TODO Taken from EntryComparator as a quick fix
                    if (d1 == null && d2 == null) return 0;
                    if (d1 == null) return 1;
                    if (d2 == null) return -1;
                    return d1.compareTo(d2);
                }
            });

            Vector d_entries = new Vector();
            long saldo = 0;
            for (int i = 0; i < entries.size(); i++) {
                Entry e = (Entry) entries.get(i);
                saldo = saldo + e.getAmount();
                d_entries.add(new DisplayableEntry(e, saldo));
            }
            
            // an empty line to have the possibility to enter a new entry
            d_entries.add(new DisplayableNewEmptyEntry());

            return d_entries.toArray();
        }
    }

    class EntryLabelProvider extends LabelProvider implements ITableLabelProvider {
        // TODO Use formatting capabilities of IPropertyAccessor
        // as soon as they are available.
        protected NumberFormat nf = DecimalFormat.getCurrencyInstance();

        public String getColumnText(Object obj, int index) {
            if (obj instanceof AbstractDisplayableEntry) {
                AbstractDisplayableEntry de = (AbstractDisplayableEntry) obj;
                // TODO: use the good commodity
                Commodity c = fPage.getAccount().getCommodity(null);
	            switch (index) {
	            case 0: // check
	                String s = de.getCheck();
	                return s == null ? "" : s;
	            case 1: // date
	                Date d = de.getDate();
	                return d == null ? "" : fDateFormat.format(d);
	            case 2: // valuta
	                d = de.getValuta();
	                return d == null ? "" : fDateFormat.format(d);
	            case 3: // description
	                s = de.getDescription();
	                return s == null ? "" : s;
	            case 4: // to account
	                s = de.getOtherAccountName();
	                return s == null ? "" : s;
	            case 5: // income
	                return de.getAmount() >= 0 ? c.format(de.getAmount()) : "";
	            case 6: // expense
	                return de.getAmount() < 0 ? c.format(-de.getAmount()) : "";
	            case 7: // balance
	                return c.format(de.getBalance());
	            }
	            return ""; //$NON-NLS-1$
	            
            } else {
                // type of entry not recognize
                return ""; //$NON-NLS-1$
            }
        }
        public Image getColumnImage(Object obj, int index) {
            return null;
        }
    }

    class EntrySorter extends ViewerSorter {
        public int compare(Viewer notused, Object o1, Object o2) {
            if (o1 instanceof DisplayableEntry && o2 instanceof DisplayableEntry) {
                if (o1 instanceof DisplayableNewEmptyEntry) {
                    return -1;
                } else if (o2 instanceof DisplayableNewEmptyEntry) {
                    return 1;
                }
                Date d1 = ((DisplayableEntry) o1).entry.getTransaction().getDate();
                Date d2 = ((DisplayableEntry) o2).entry.getTransaction().getDate();
                // TODO Taken from EntryComparator as a quick fix
                if (d1 == null && d2 == null) return 0;
                if (d1 == null) return 1;
                if (d2 == null) return -1;
                return d1.compareTo(d2);
            } else {
                return super.compare(notused, o1, o2);
            }
        }
    }

    abstract class AbstractDisplayableEntry {
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

        abstract String getOtherAccountName ();
        abstract long   getBalance ();
        

        //TODO: abstract void set/getCommodity();
    }
    
    class DisplayableEntry extends AbstractDisplayableEntry { 
        
        private Entry entry;
        private long balance;

        public DisplayableEntry(Entry entry, long saldo) {
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

        long getBalance () { return balance ; }
    }

    /**
     * This class is to be used for new entries which are currently filled in by the
     * user, before they are comited and transformed in real entries
     * @author Faucheux
     */
    class DisplayableNewEmptyEntry extends AbstractDisplayableEntry {
        
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
        
    }
    

    class CellModifier implements ICellModifier {
        public boolean canModify(Object element, String property) {
            return !property.equals("balance");
        }
        public Object getValue(Object element, String property) {
            int index = getColumnIndex(property);
            return fLabelProvider.getColumnText(element, index);
        }
        
        public void modify(Object element, String property, Object value) {
            
            if (element instanceof Item) {
                element = ((Item) element).getData();
            }
            
            AbstractDisplayableEntry de = (AbstractDisplayableEntry) element;
            // TODO: here, we should set the commodity to the one of the account for a new Entry,
            // and the one of the entry in case the entry already exists
            Commodity c = fPage.getAccount().getCommodity(null);
            int index = getColumnIndex(property);
            switch (index) {
            case 0: // check
                String s = (String) value;
                de.setCheck(s.length() == 0 ? null : s);
                fViewer.update(element, new String[] { property});
                break;
            case 1: // date
                Date d = fDateFormat.parse((String) value);
                de.setDate(d);
                fViewer.update(element, new String[] { property});
                break;
            case 2: // valuta
                d = fDateFormat.parse((String) value);
                de.setValuta(d);
                fViewer.update(element, new String[] { property});
                break;
            case 3: // description
                s = (String) value;
                de.setDescription(s.length() == 0 ? null : s);
                fViewer.update(element, new String[] { property});
                break;
            case 4: // income
                de.setAmount(c.parse((String) value));
                fViewer.refresh();
                break;
            case 5: // expense
                de.setAmount(-c.parse((String) value));
                fViewer.refresh();
                break;
            }
        }
        
        protected int getColumnIndex(String property) {
            int index = 0;
            if (property.equals("check"))
                return index = 0;
            else if (property.equals("date"))
                return index = 1;
            else if (property.equals("valuta"))
                return index = 2;
            else if (property.equals("description"))
                return index = 3;
            else if (property.equals("debit"))
                return index = 4;
            else if (property.equals("credit"))
                return index = 5;
            else if (property.equals("balance")) return index = 6;
            return index;
        }
    }

}