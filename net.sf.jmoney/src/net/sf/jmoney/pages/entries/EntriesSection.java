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

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.entrytable.Block;
import net.sf.jmoney.entrytable.CellBlock;
import net.sf.jmoney.entrytable.EntriesSectionProperty;
import net.sf.jmoney.entrytable.EntriesTable;
import net.sf.jmoney.entrytable.EntryData;
import net.sf.jmoney.entrytable.HorizontalBlock;
import net.sf.jmoney.entrytable.IEntriesContent;
import net.sf.jmoney.entrytable.VerticalBlock;
import net.sf.jmoney.fields.EntryInfo;
import net.sf.jmoney.fields.TransactionInfo;
import net.sf.jmoney.model2.Entry;

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

    private EntriesTable fEntriesControl;
    
    private EntryRowSelectionListener tableSelectionListener = null;
    
    private Block rootBlock;
    
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
    		public void widgetSelected(EntryData selectedObject) {
    			JMoneyPlugin.myAssert(selectedObject != null);
    			
    			// Do we want to keep the entry section at the bottom?
    			// Do we need this listener at all?
			}
        };

        

		// By default, do not include the column for the currency
		// of the entry in the category (which applies only when
		// the category is a multi-currency income/expense category)
		// and the column for the amount (which applies only when
		// the currency is different from the entry in the capital 
		// account)
//		if (!entriesSectionProperty.getId().equals("common2.net.sf.jmoney.entry.incomeExpenseCurrency")
//				&& !entriesSectionProperty.getId().equals("other.net.sf.jmoney.entry.amount")) {
  
        
		/*
		 * Setup the layout structure of the header and rows.
		 */
		rootBlock = new HorizontalBlock(new Block [] {
				new CellBlock(EntriesSectionProperty.createTransactionColumn(TransactionInfo.getDateAccessor())),
				new VerticalBlock(new Block [] {
						new CellBlock(EntriesSectionProperty.createEntryColumn(EntryInfo.getMemoAccessor())),
						new HorizontalBlock(new Block [] {
								new CellBlock(EntriesSectionProperty.createEntryColumn(EntryInfo.getCheckAccessor())),
								new CellBlock(EntriesSectionProperty.createEntryColumn(EntryInfo.getValutaAccessor())),
						}),
				}),
				new CellBlock(EntriesSectionProperty.createOtherEntryColumn(EntryInfo.getAccountAccessor())),
				new CellBlock(EntriesSectionProperty.createOtherEntryColumn(EntryInfo.getDescriptionAccessor())),
				new CellBlock(EntriesSectionProperty.createOtherEntryColumn(EntryInfo.getAmountAccessor())),
				new CellBlock(fPage.debitColumnManager),
				new CellBlock(fPage.creditColumnManager),
				new CellBlock(fPage.balanceColumnManager),
		});
		
//		cellList = new ArrayList<IEntriesTableProperty>();
//		rootBlock.buildCellList(cellList);

		// Create the table control.
		fEntriesControl = new EntriesTable(getSection(), toolkit, rootBlock, this, fPage.getAccount().getSession(), new EntriesTable.IMenuItem [] {}); 
		fEntriesControl.addSelectionListener(tableSelectionListener);
			
        getSection().setClient(fEntriesControl);
        toolkit.paintBordersFor(fEntriesControl);
        refresh();
    }

	public Collection<Entry> getEntries() {
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

	/* (non-Javadoc)
	 * @see net.sf.jmoney.pages.entries.IEntriesContent#filterEntry(net.sf.jmoney.pages.entries.EntriesTable.DisplayableTransaction)
	 */
	public boolean filterEntry(EntryData data) {
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
