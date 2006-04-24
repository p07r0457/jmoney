/*
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2004 Johann Gyger <jgyger@users.sf.net>
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
 */
package net.sf.jmoney.pages.entries;

import java.util.Collection;
import java.util.Vector;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.fields.EntryInfo;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.PropertyAccessor;
import net.sf.jmoney.pages.entries.EntriesTree.DisplayableEntry;
import net.sf.jmoney.pages.entries.EntriesTree.DisplayableTransaction;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * TODO
 * 
 * @author Johann Gyger
 */
public class EntriesSection extends SectionPart implements IEntriesContent {

    private EntriesPage fPage;
    private EntriesTree fEntriesControl;
    
    private EntryRowSelectionListener tableSelectionListener = null;
    
    public EntriesSection(EntriesPage page, Composite parent) {
        super(parent, page.getManagedForm().getToolkit(), Section.TITLE_BAR);
        getSection().setText("All Entries");
        fPage = page;
        createClient(page.getManagedForm().getToolkit());
    }

    public void refreshEntryList() {
    	fEntriesControl.refreshEntryList();
    }

    protected void createClient(FormToolkit toolkit) {
    	
    	tableSelectionListener = new EntryRowSelectionAdapter() {
    		public void widgetSelected(IDisplayableItem selectedObject) {
    			JMoneyPlugin.myAssert(selectedObject != null);
    			
    			// We should never get here with the item data set to the
    			// DisplayableNewEmptyEntry object as a result of the user
    			// selecting the row.  The reason being that the EntryTree
    			// object intercepts mouse down events first and replaces the
    			// data with a new entry.  However, SWT seems to set the selection
    			// to the last row in certain circumstances such as when
    			// applying a filter.  In such a situation, both the top-level
    			// entry and the selected entry will be given as null.
    			// Two null values passed to the entry section will cause
    			// the section to be blanked.
    			
    			IDisplayableItem data = (IDisplayableItem)selectedObject;

    			if (selectedObject instanceof DisplayableTransaction) {
    				DisplayableTransaction transData = (DisplayableTransaction)selectedObject;
    				if (transData.isSimpleEntry()) {
    					fPage.fEntrySection.update(data.getEntryInAccount(), data.getEntryForOtherFields(), true);
    				} else {
    					fPage.fEntrySection.update(data.getEntryInAccount(), null, true);
    				}
    			} else if (selectedObject instanceof DisplayableEntry) {
					fPage.fEntrySection.update(data.getEntryForThisRow(), null, false);
    			} else {
    				// We were not on a transaction (we were probably on the
    				// blank 'new transaction' line.
					fPage.fEntrySection.update(null, null, false);
    			}
			}
        };

        // Create the table control.
        fEntriesControl = new EntriesTree(getSection(), toolkit, fPage.transactionManager, this, fPage.getAccount().getSession()); 
		fEntriesControl.addSelectionListener(tableSelectionListener);
		
        getSection().setClient(fEntriesControl);
        toolkit.paintBordersFor(fEntriesControl);
        refresh();
    }

	/* (non-Javadoc)
	 * @see net.sf.jmoney.pages.entries.IEntriesContent#getAllEntryDataObjects()
	 */
	public Vector getAllEntryDataObjects() {
		return fPage.allEntryDataObjects;
	}

	/* (non-Javadoc)
	 * @see net.sf.jmoney.pages.entries.IEntriesContent#getDebitColumnManager()
	 */
	public IEntriesTableProperty getDebitColumnManager() {
		return fPage.debitColumnManager;
	}

	/* (non-Javadoc)
	 * @see net.sf.jmoney.pages.entries.IEntriesContent#getCreditColumnManager()
	 */
	public IEntriesTableProperty getCreditColumnManager() {
		return fPage.creditColumnManager;
	}

	/* (non-Javadoc)
	 * @see net.sf.jmoney.pages.entries.IEntriesContent#getBalanceColumnManager()
	 */
	public IEntriesTableProperty getBalanceColumnManager() {
		return fPage.balanceColumnManager;
	}

	public Collection getEntries() {
/* The caller always sorts, so there is no point in us returning
 * sorted results.  It may be at some point we decide it is more
 * efficient to get the database to sort for us, but that would
 * only help the first time the results are fetched, it would not
 * help on a re-sort.  It also only helps if the database indexes
 * on the date.		
        CurrencyAccount account = fPage.getAccount();
        Collection accountEntries = 
        	account
				.getSortedEntries(TransactionInfo.getDateAccessor(), false);
        return accountEntries;
*/
		return fPage.getAccount().getEntries();
	}

	public boolean isEntryInTable(Entry entry) {
		return fPage.getAccount().equals(entry.getAccount());
	}

	public boolean isEntryInTable(Entry entry, PropertyAccessor propertyAccessor, Object value) {
		Account account;
		if (propertyAccessor == EntryInfo.getAccountAccessor()) {
			account = (Account)value;
		} else {
			account = entry.getAccount();
		}
		return fPage.getAccount().equals(account);
	}

	// TODO: Do we need this????
	public int isEntryInTable(Entry entry, PropertyAccessor propertyAccessor, Object oldValue, Object newValue) {
		if (propertyAccessor == EntryInfo.getAccountAccessor()) {
			boolean wasInTable   = fPage.getAccount().equals(oldValue); 
			boolean isNowInTable = fPage.getAccount().equals(newValue); 
			if (wasInTable && !isNowInTable) {
				return -1; 
			} else if (!wasInTable && isNowInTable) {
				return 1;
			} else {
				return 0;
			}
 		} else {
			return 0;
		}
	}
	
	/* (non-Javadoc)
	 * @see net.sf.jmoney.pages.entries.IEntriesContent#filterEntry(net.sf.jmoney.pages.entries.EntriesTable.DisplayableTransaction)
	 */
	public boolean filterEntry(IDisplayableItem data) {
		return fPage.filter.filterEntry(data);
	}

	/* (non-Javadoc)
	 * @see net.sf.jmoney.pages.entries.IEntriesContent#getStartBalance()
	 */
	public long getStartBalance() {
        return fPage.getAccount().getStartBalance();
	}

	public void setNewEntryProperties(Entry newEntry) {
   		newEntry.setAccount(fPage.getAccount());
	}
}
