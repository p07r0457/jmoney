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
import net.sf.jmoney.entrytable.BalanceColumn;
import net.sf.jmoney.entrytable.Block;
import net.sf.jmoney.entrytable.CellBlock;
import net.sf.jmoney.entrytable.DebitAndCreditColumns;
import net.sf.jmoney.entrytable.EntriesTable;
import net.sf.jmoney.entrytable.EntryData;
import net.sf.jmoney.entrytable.HorizontalBlock;
import net.sf.jmoney.entrytable.IEntriesContent;
import net.sf.jmoney.entrytable.IndividualBlock;
import net.sf.jmoney.entrytable.OtherEntriesBlock;
import net.sf.jmoney.entrytable.PropertyBlock;
import net.sf.jmoney.entrytable.RowSelectionTracker;
import net.sf.jmoney.entrytable.SingleOtherEntryPropertyBlock;
import net.sf.jmoney.fields.EntryInfo;
import net.sf.jmoney.fields.TransactionInfo;
import net.sf.jmoney.isolation.TransactionManager;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.IncomeExpenseAccount;

import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * TODO
 * 
 * @author Johann Gyger
 */
public class CategoryEntriesSection extends SectionPart implements IEntriesContent {

	private IncomeExpenseAccount account;
	private EntriesFilter filter;
	
    private EntriesTable fEntriesControl;
    
    private EntryRowSelectionListener tableSelectionListener = null;
    
    private Block<EntryData> rootBlock;
    
    public CategoryEntriesSection(Composite parent, IncomeExpenseAccount account, EntriesFilter filter, FormToolkit toolkit) {
        super(parent, toolkit, Section.TITLE_BAR);
        getSection().setText("All Entries");
        this.account = account;
        this.filter = filter;
        createClient(toolkit);
    }

    public void refreshEntryList() {
    	fEntriesControl.refreshEntryList();
    }

    protected void createClient(FormToolkit toolkit) {
    	
    	tableSelectionListener = new EntryRowSelectionAdapter() {
			@Override
    		public void widgetSelected(EntryData selectedObject) {
    			Assert.isNotNull(selectedObject);
    			
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
		IndividualBlock<EntryData> transactionDateColumn = PropertyBlock.createTransactionColumn(TransactionInfo.getDateAccessor());

		CellBlock<EntryData> debitColumnManager = DebitAndCreditColumns.createDebitColumn(account.getCurrency());
		CellBlock<EntryData> creditColumnManager = DebitAndCreditColumns.createCreditColumn(account.getCurrency());
    	CellBlock<EntryData> balanceColumnManager = new BalanceColumn(account.getCurrency());
		
		rootBlock = new HorizontalBlock<EntryData>(
				transactionDateColumn,
				PropertyBlock.createEntryColumn(EntryInfo.getMemoAccessor(), JMoneyPlugin.getResourceString("Entry.description")),
				new OtherEntriesBlock(
						new HorizontalBlock<Entry>(
								new SingleOtherEntryPropertyBlock(EntryInfo.getAccountAccessor()),
								new SingleOtherEntryPropertyBlock(EntryInfo.getMemoAccessor()),
								new SingleOtherEntryPropertyBlock(EntryInfo.getAmountAccessor())
						)
				),
				debitColumnManager,
				creditColumnManager,
				balanceColumnManager
		);

		// Create the table control.
		fEntriesControl = new EntriesTable(getSection(), toolkit, rootBlock, this, account.getSession(), transactionDateColumn, new RowSelectionTracker()); 
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
		return account.getEntries();
	}

	public boolean isEntryInTable(Entry entry) {
		return account.equals(entry.getAccount());
	}

	/* (non-Javadoc)
	 * @see net.sf.jmoney.pages.entries.IEntriesContent#filterEntry(net.sf.jmoney.pages.entries.EntriesTable.DisplayableTransaction)
	 */
	public boolean filterEntry(EntryData data) {
		return filter.filterEntry(data);
	}

	/* (non-Javadoc)
	 * @see net.sf.jmoney.pages.entries.IEntriesContent#getStartBalance()
	 */
	public long getStartBalance() {
        return 0;
	}

	public void setNewEntryProperties(Entry newEntry) {
		// It is assumed that the entry is in a data manager that is a direct
		// child of the data manager that contains the account.
		TransactionManager tm = (TransactionManager)newEntry.getDataManager();
		newEntry.setAccount(tm.getCopyInTransaction(account));
	}
}
