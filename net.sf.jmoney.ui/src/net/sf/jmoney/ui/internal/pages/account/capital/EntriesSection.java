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
                if (event.getSelection() instanceof IStructuredSelection) {
                    IStructuredSelection s = (IStructuredSelection) event.getSelection();
                    DisplayableEntry de = (DisplayableEntry) s.getFirstElement();
                    fPage.fEntrySection.update(de.entry);
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
        CellEditor[] result = new CellEditor[7];
        result[0] = new TextCellEditor(fTable); // check
        result[1] = new TextCellEditor(fTable); // date
        result[2] = new TextCellEditor(fTable); // valuta
        result[3] = new TextCellEditor(fTable); // description
        result[4] = new TextCellEditor(fTable); // debit
        result[5] = new TextCellEditor(fTable); // credit
        result[6] = new TextCellEditor(fTable); // balance
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

            return d_entries.toArray();
        }
    }

    class EntryLabelProvider extends LabelProvider implements ITableLabelProvider {
        // TODO Use formatting capabilities of IPropertyAccessor
        // as soon as they are available.
        protected NumberFormat nf = DecimalFormat.getCurrencyInstance();
        public String getColumnText(Object obj, int index) {
            DisplayableEntry de = (DisplayableEntry) obj;
            Commodity c = de.entry.getAccount().getCommodity(de.entry);
            switch (index) {
            case 0: // check
                String s = de.entry.getCheck();
                return s == null ? "" : s;
            case 1: // date
                Date d = de.entry.getTransaction().getDate();
                return d == null ? "" : fDateFormat.format(d);
            case 2: // valuta
                d = de.entry.getValuta();
                return d == null ? "" : fDateFormat.format(d);
            case 3: // description
                s = de.entry.getDescription();
                return s == null ? "" : s;
            case 4: // income
                return de.entry.getAmount() >= 0 ? c.format(de.entry.getAmount()) : "";
            case 5: // expense
                return de.entry.getAmount() < 0 ? c.format(-de.entry.getAmount()) : "";
            case 6: // balance
                return c.format(de.balance);
            }
            return ""; //$NON-NLS-1$
        }
        public Image getColumnImage(Object obj, int index) {
            return null;
        }
    }

    class EntrySorter extends ViewerSorter {
        public int compare(Viewer notused, Object o1, Object o2) {
            if (o1 instanceof DisplayableEntry && o2 instanceof DisplayableEntry) {
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

    class DisplayableEntry {
        private Entry entry;
        private long balance;

        public DisplayableEntry(Entry entry, long saldo) {
            this.entry = entry;
            this.balance = saldo;
        }
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
            DisplayableEntry de = (DisplayableEntry) element;
            Commodity c = de.entry.getAccount().getCommodity(de.entry);
            int index = getColumnIndex(property);
            switch (index) {
            case 0: // check
                String s = (String) value;
                de.entry.setCheck(s.length() == 0 ? null : s);
                fViewer.update(element, new String[] { property});
                break;
            case 1: // date
                Date d = fDateFormat.parse((String) value);
                de.entry.getTransaction().setDate(d);
                fViewer.update(element, new String[] { property});
                break;
            case 2: // valuta
                d = fDateFormat.parse((String) value);
                de.entry.setValuta(d);
                fViewer.update(element, new String[] { property});
                break;
            case 3: // description
                s = (String) value;
                de.entry.setDescription(s.length() == 0 ? null : s);
                fViewer.update(element, new String[] { property});
                break;
            case 4: // income
                de.entry.setAmount(c.parse((String) value));
                fViewer.refresh();
                break;
            case 5: // expense
                de.entry.setAmount(-c.parse((String) value));
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