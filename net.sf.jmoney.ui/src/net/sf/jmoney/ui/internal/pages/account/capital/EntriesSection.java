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

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.Vector;

import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.Entry;

import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * TODO
 * 
 * @author Johann Gyger
 */
public class EntriesSection extends SectionPart {

	protected EntriesPage fPage;
    protected Table fTable; 
	protected TableViewer fViewer;

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

        TableColumn col1 = new TableColumn(fTable, SWT.NULL);
        col1.setText("Check");
        TableColumn col2 = new TableColumn(fTable, SWT.NULL);
        col2.setText("Date");
        TableColumn col3 = new TableColumn(fTable, SWT.NULL);
        col3.setText("Description");
        TableColumn col4 = new TableColumn(fTable, SWT.RIGHT);
        col4.setText("Debit");
        TableColumn col5 = new TableColumn(fTable, SWT.RIGHT);
        col5.setText("Credit");
        TableColumn col6 = new TableColumn(fTable, SWT.RIGHT);
        col6.setText("Balance");

        TableLayout tlayout = new TableLayout();
        tlayout.addColumnData(new ColumnWeightData(50, 50));
        tlayout.addColumnData(new ColumnWeightData(50, 70));
        tlayout.addColumnData(new ColumnWeightData(50, 300));
        tlayout.addColumnData(new ColumnWeightData(50, 70));
        tlayout.addColumnData(new ColumnWeightData(50, 70));
        tlayout.addColumnData(new ColumnWeightData(50, 70));
        fTable.setLayout(tlayout);

        fTable.setHeaderVisible(true);
        fTable.setLinesVisible(true);
    }

	protected void createTableViewer() {
        fViewer = new TableViewer(fTable);
        fViewer.setContentProvider(new ContentProvider());
        fViewer.setInput(fPage.getAccount());
        fViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                handleSelectionChanged();
            }
        });
        
        fViewer.setLabelProvider(new MyLabelProvider());
        fViewer.setSorter(new mySorter());
    }
	
	protected void handleSelectionChanged() {
	    // TODO
	}

	class ContentProvider implements IStructuredContentProvider {

	    public void dispose() {
            // TODO Auto-generated method stub
        }

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            // TODO Auto-generated method stub
        }

        public Object[] getElements(Object parent) {
            CapitalAccount account = (CapitalAccount) parent;
            Iterator it = account.getEntriesIterator(null);

            // TODO Iterator workaround
            Vector entries = new Vector();
            while (it.hasNext()) {
                entries.add(it.next());
            }
            
            return entries.toArray();
        }

	}
	
    // Definition of the LabelProvider, or "how we display the information
    // in the table".
	private final class MyLabelProvider extends LabelProvider implements ITableLabelProvider {
	    
	    private final DateFormat df;
	    private final NumberFormat nf;
	    
	    public MyLabelProvider() {
	        df = new SimpleDateFormat("dd.MM.yyyy");
	        nf = DecimalFormat.getCurrencyInstance();
	    }
	    
	    public String getColumnText(Object o, int colnr) {
	        if (o instanceof Entry) {
	            Entry e = (Entry) o;
	            if (colnr == 1 ) {  
	                return df.format(e.getTransaction().getDate());
	            } else if (colnr == 2) { 
	                return e.getDescription();
	            } else if (colnr == 3) {  
	                if (e.getAmount() > 0) 
	                   return nf.format(e.getAmount() / 100); 
	                   else return new String ();
	            } else if (colnr == 4) {
	                if (e.getAmount() < 0) 
	                   return nf.format( - e.getAmount() / 100); 
	                   else return new String ();
	            } else if (colnr == 5) {
	                return new String ("TODO");  //TODO: How display the saldo?
	            }

	        }
	        return new String();
	    }
	    
	    public Image getColumnImage(Object o, int colnr) {return null;}

	}
	
	private final class mySorter extends ViewerSorter {
	    public int compare (Viewer notused, Object o1, Object o2) {
	        if (o1 instanceof Entry && o2 instanceof Entry) {
	            return ((Entry) o1).getTransaction().getDate()
	               .compareTo(((Entry) o2).getTransaction().getDate());
	        } else {
	            return super.compare(notused, o1, o2);
	        }
	    }
	}
}

