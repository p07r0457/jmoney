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

import java.util.Iterator;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.VerySimpleDateFormat;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.PropertyAccessor;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.SessionChangeAdapter;
import net.sf.jmoney.model2.Transaction;

import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * TODO
 * 
 * @author Johann Gyger
 */
public class EntriesSection extends SectionPart {

    protected VerySimpleDateFormat fDateFormat = new VerySimpleDateFormat(JMoneyPlugin.getDefault().getDateFormat());
    protected EntriesPage fPage;
    protected IEntriesControl fEntriesControl;
    
    Composite containerOfEntriesControl;
    FormToolkit toolkit;
    
    public EntriesSection(EntriesPage page, Composite parent) {
        super(parent, page.getManagedForm().getToolkit(), Section.TITLE_BAR);
        getSection().setText("All Entries");
        fPage = page;
        createClient(page.getManagedForm().getToolkit());
    }

    public void refresh() {
    	fEntriesControl.refresh();
        super.refresh();
    }

    protected void createClient(FormToolkit toolkit) {
    	this.toolkit = toolkit;
    	
        Composite container = toolkit.createComposite(getSection());

        GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        container.setLayout(layout);

        // Create the control that contains the Table or TableTree control.
        // Although this control only contains a single child control,
        // we need to create it so we can destroy and re-create the
        // child control without altering the sequence of controls
        // in the grid container.
        containerOfEntriesControl = toolkit.createComposite(container);
        GridLayout layout2 = new GridLayout();
        layout.numColumns = 1;
        containerOfEntriesControl.setLayout(layout2);
        
        GridData gridData = new GridData(GridData.FILL_BOTH);
        gridData.heightHint = 300;
        gridData.widthHint = 800;
		gridData.horizontalAlignment = GridData.FILL;
		gridData.verticalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
        containerOfEntriesControl.setLayoutData(gridData);

        // Initially set to the table control.
        // TODO: save this information in the memento so
        // the user always gets the last selected view.
        fEntriesControl = new EntriesTable(containerOfEntriesControl, fPage); 

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

                // fViewer.setSelection(selection, true);
                fEntriesControl.setSelection(selection);
           }
        });

        Button deleteButton = toolkit.createButton(buttonArea, "Delete Transaction", SWT.PUSH);
        deleteButton.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent event) {
        		
        		Entry selectedEntry = fEntriesControl.getSelectedEntry();
        		if (selectedEntry != null) {
        			Transaction transaction = selectedEntry.getTransaction();
        			transaction.getSession().deleteTransaction(transaction);
        			transaction.getSession().registerUndoableChange("Delete Transaction");
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
        refresh();
    }

	/**
	 * Set the entries list to be a flat table.  If any other
	 * entries control is set, destroy it first.
	 */
	public void setTableView() {
    	if (fEntriesControl instanceof EntriesTable) {
    		// Already set to table view, so nothing to do.
    		return;
    	}
    	
    	fEntriesControl.dispose();
    	
    	fEntriesControl = new EntriesTable(containerOfEntriesControl, fPage);

        toolkit.paintBordersFor(containerOfEntriesControl);
        refresh();
        containerOfEntriesControl.pack(true);
	}

	/**
	 * Set the entries list to be a table tree.  If any other
	 * entries control is set, destroy it first.
	 */
    public void setTreeView() {
    	if (fEntriesControl instanceof EntriesTree) {
    		// Already set to tree view, so nothing to do.
    		return;
    	}
    	
    	fEntriesControl.dispose();
    	
    	fEntriesControl = new EntriesTree(containerOfEntriesControl, fPage);

    	containerOfEntriesControl.redraw();
    	containerOfEntriesControl.update();
    	
        toolkit.paintBordersFor(containerOfEntriesControl);
//        refresh();
        containerOfEntriesControl.pack(true);
    }
}
