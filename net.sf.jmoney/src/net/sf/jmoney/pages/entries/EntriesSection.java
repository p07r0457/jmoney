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

import net.sf.jmoney.entrytable.BalanceColumn;
import net.sf.jmoney.entrytable.BaseEntryRowControl;
import net.sf.jmoney.entrytable.Block;
import net.sf.jmoney.entrytable.CellBlock;
import net.sf.jmoney.entrytable.DebitAndCreditColumns;
import net.sf.jmoney.entrytable.EntriesTable;
import net.sf.jmoney.entrytable.EntryData;
import net.sf.jmoney.entrytable.EntryRowControl;
import net.sf.jmoney.entrytable.HorizontalBlock;
import net.sf.jmoney.entrytable.IEntriesContent;
import net.sf.jmoney.entrytable.IRowProvider;
import net.sf.jmoney.entrytable.IndividualBlock;
import net.sf.jmoney.entrytable.OtherEntriesBlock;
import net.sf.jmoney.entrytable.PropertyBlock;
import net.sf.jmoney.entrytable.ReusableRowProvider;
import net.sf.jmoney.entrytable.RowSelectionTracker;
import net.sf.jmoney.entrytable.SingleOtherEntryPropertyBlock;
import net.sf.jmoney.entrytable.SplitEntryRowControl;
import net.sf.jmoney.entrytable.VerticalBlock;
import net.sf.jmoney.isolation.TransactionManager;
import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.CurrencyAccount;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.EntryInfo;
import net.sf.jmoney.model2.Transaction;
import net.sf.jmoney.model2.TransactionInfo;
import net.sf.jmoney.resources.Messages;

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
public class EntriesSection extends SectionPart implements IEntriesContent {

	private CurrencyAccount account;
	private EntriesFilter filter;

    private EntriesTable fEntriesControl;
    
    private EntryRowSelectionListener tableSelectionListener = null;
    
    private Block<EntryData, ? super EntryRowControl> rootBlock;
    
    public EntriesSection(Composite parent, CurrencyAccount account, EntriesFilter filter, FormToolkit toolkit) {
        super(parent, toolkit, Section.TITLE_BAR);
        getSection().setText(Messages.EntriesSection_Text);
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
		IndividualBlock<EntryData, Composite> transactionDateColumn = PropertyBlock.createTransactionColumn(TransactionInfo.getDateAccessor());

		CellBlock<EntryData, BaseEntryRowControl> debitColumnManager = DebitAndCreditColumns.createDebitColumn(account.getCurrency());
		CellBlock<EntryData, BaseEntryRowControl> creditColumnManager = DebitAndCreditColumns.createCreditColumn(account.getCurrency());
    	CellBlock<EntryData, BaseEntryRowControl> balanceColumnManager = new BalanceColumn(account.getCurrency());
		
		rootBlock = new HorizontalBlock<EntryData, EntryRowControl>(
				transactionDateColumn,
				new VerticalBlock<EntryData, EntryRowControl>(
						PropertyBlock.createEntryColumn(EntryInfo.getMemoAccessor()),
						new HorizontalBlock<EntryData, EntryRowControl>(
								PropertyBlock.createEntryColumn(EntryInfo.getCheckAccessor()),
								PropertyBlock.createEntryColumn(EntryInfo.getValutaAccessor())
						)
				),
				new OtherEntriesBlock(
						new HorizontalBlock<Entry, SplitEntryRowControl>(
								new SingleOtherEntryPropertyBlock(EntryInfo.getAccountAccessor()),
								new SingleOtherEntryPropertyBlock(EntryInfo.getMemoAccessor(), Messages.EntriesSection_EntryDescription),
								new SingleOtherEntryPropertyBlock(EntryInfo.getAmountAccessor())
						)
				),
				debitColumnManager,
				creditColumnManager,
				balanceColumnManager
		);

		// Create the table control.
	    IRowProvider<EntryData> rowProvider = new ReusableRowProvider(rootBlock);
		fEntriesControl = new EntriesTable<EntryData>(getSection(), toolkit, rootBlock, this, rowProvider, account.getSession(), transactionDateColumn, new RowSelectionTracker<EntryRowControl>()) {
			@Override
			protected EntryData createEntryRowInput(Entry entry) {
				return new EntryData(entry, session.getDataManager());
			}

			@Override
			protected EntryData createNewEntryRowInput() {
				return new EntryData(null, session.getDataManager());
			}
		}; 
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
        return account.getStartBalance();
	}

	public Entry createNewEntry(Transaction newTransaction) {
		Entry entryInTransaction = newTransaction.createEntry();
		Entry otherEntry = newTransaction.createEntry();

		setNewEntryProperties(entryInTransaction);

		// TODO: See if this code has any effect, and
		// should this be here at all?
		/*
		 * We set the currency by default to be the currency of the
		 * top-level entry.
		 * 
		 * The currency of an entry is not applicable if the entry is an
		 * entry in a currency account or an income and expense account
		 * that is restricted to a single currency.
		 * However, we set it anyway so the value is there if the entry
		 * is set to an account which allows entries in multiple currencies.
		 * 
		 * It may be that the currency of the top-level entry is not
		 * known. This is not possible if entries in a currency account
		 * are being listed, but may be possible if this entries list
		 * control is used for more general purposes. In this case, the
		 * currency is not set and so the user must enter it.
		 */
		if (entryInTransaction.getCommodity() instanceof Currency) {
			otherEntry.setIncomeExpenseCurrency((Currency)entryInTransaction.getCommodity());
		}
		
		return entryInTransaction;
	}
	
	private void setNewEntryProperties(Entry newEntry) {
		// It is assumed that the entry is in a data manager that is a direct
		// child of the data manager that contains the account.
		TransactionManager tm = (TransactionManager)newEntry.getDataManager();
		newEntry.setAccount(tm.getCopyInTransaction(account));
	}
}
