package net.sf.jmoney.stocks.pages;

import java.util.ArrayList;

import net.sf.jmoney.entrytable.BaseEntryRowControl;
import net.sf.jmoney.entrytable.Block;
import net.sf.jmoney.entrytable.EntryData;
import net.sf.jmoney.entrytable.FocusCellTracker;
import net.sf.jmoney.entrytable.RowSelectionTracker;
import net.sf.jmoney.entrytable.VirtualRowTable;
import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.CurrencyAccount;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.Transaction.EntryCollection;
import net.sf.jmoney.stocks.Stock;
import net.sf.jmoney.stocks.StockAccount;

import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.widgets.Composite;

public class StockEntryRowControl extends BaseEntryRowControl {

	private enum TransactionType {
		Buy,
		Sell,
		Dividend,
		Transfer,
		Other
	}

	private TransactionType transactionType;

	private Entry mainEntry;
	private Entry dividendEntry;
	private Entry withholdingTaxEntry;
	private Entry commissionEntry;
	private Entry tax1Entry;
	private Entry tax2Entry;
	private Entry purchaseOrSaleEntry;
	private Entry transferEntry;

	private boolean unknownTransactionType;

	private ArrayList<ITransactionTypeChangeListener> transactionTypeChangeListeners = new ArrayList<ITransactionTypeChangeListener>();

	public StockEntryRowControl(final Composite parent, int style, VirtualRowTable rowTable, Block<EntryData, ? super StockEntryRowControl> rootBlock, final RowSelectionTracker selectionTracker, final FocusCellTracker focusCellTracker) {
		super(parent, style, rowTable, rootBlock);
		init(this, rootBlock, selectionTracker, focusCellTracker);
	}
	
	@Override
	public void setContent(EntryData committedEntryData) {
		super.setContent(committedEntryData);

		/*
		 * Analyze the transaction to see which type of transaction this is.
		 */
		StockAccount account = (StockAccount)uncommittedEntryData.getEntry().getAccount();

		for (Entry entry: uncommittedEntryData.getEntry().getTransaction().getEntryCollection()) {
			if (entry.getAccount() == account.getDividendAccount()) {
				if (dividendEntry != null) {
					unknownTransactionType = true;
				}
				dividendEntry = entry;
			} else if (entry.getAccount() == account.getWithholdingTaxAccount()) {
				if (withholdingTaxEntry != null) {
					unknownTransactionType = true;
				}
				withholdingTaxEntry = entry;
			} else if (entry.getAccount() == account.getCommissionAccount()) {
				if (commissionEntry != null) {
					unknownTransactionType = true;
				}
				commissionEntry = entry;
			} else if (entry.getAccount() == account.getTax1Account()) {
				if (tax1Entry != null) {
					unknownTransactionType = true;
				}
				tax1Entry = entry;
			} else if (entry.getAccount() == account.getTax2Account()) {
				if (tax2Entry != null) {
					unknownTransactionType = true;
				}
				tax2Entry = entry;
			} else if (entry.getAccount() == account) {
				if (entry.getCommodity() instanceof Stock) {
					if (purchaseOrSaleEntry != null) {
						unknownTransactionType = true;
					}
					purchaseOrSaleEntry = entry;
				} else if (entry.getCommodity() instanceof Currency) {  //TODO: check for actual currency of account.
					if (mainEntry != null) {
						unknownTransactionType = true;
					}
					mainEntry = entry;
				}
			} else if (entry.getAccount() instanceof CurrencyAccount) {
				if (transferEntry != null) {
					unknownTransactionType = true;
				}
				transferEntry = entry;
			} else {
				unknownTransactionType = true;
			}
		}

		if (unknownTransactionType) {
			transactionType = TransactionType.Other;
		} else if (dividendEntry != null
				&& commissionEntry == null
				&& tax1Entry == null
				&& tax2Entry == null
				&& purchaseOrSaleEntry == null
				&& transferEntry == null) {
			transactionType = TransactionType.Dividend;
		} else if (dividendEntry == null
				&& withholdingTaxEntry == null
				&& purchaseOrSaleEntry != null
				&& transferEntry == null) {
			if (purchaseOrSaleEntry.getAmount() >= 0) {
				transactionType = TransactionType.Buy;
			} else {
				transactionType = TransactionType.Sell;
			}
		} else if (dividendEntry == null
				&& withholdingTaxEntry == null
				&& commissionEntry == null
				&& tax1Entry == null
				&& tax2Entry == null
				&& purchaseOrSaleEntry == null
				&& transferEntry != null) {
			transactionType = TransactionType.Transfer;
		} else {
			transactionType = TransactionType.Other;
		}
	}

	public void forceTransactionToTransfer() {
		transactionType = TransactionType.Transfer;

		EntryCollection entries = uncommittedEntryData.getEntry().getTransaction().getEntryCollection();
		for (Entry entry: entries) {
			if (entry != mainEntry
					&& entry != transferEntry) {
				entries.deleteEntry(entry);
			}
		}
		
		dividendEntry = null;
		withholdingTaxEntry = null;
		commissionEntry = null;
		tax1Entry = null;
		tax2Entry = null;
		purchaseOrSaleEntry = null;
		
		if (transferEntry == null) {
			transferEntry = entries.createEntry();
		}
		transferEntry.setAmount(-mainEntry.getAmount());
	}

	public void forceTransactionToDividend() {
		transactionType = TransactionType.Dividend;

		// Move this to be a field???
		StockAccount account = (StockAccount)uncommittedEntryData.getEntry().getAccount();

		EntryCollection entries = uncommittedEntryData.getEntry().getTransaction().getEntryCollection();
		for (Entry entry: entries) {
			if (entry != mainEntry
					&& entry != dividendEntry
				&& entry != withholdingTaxEntry) {
				entries.deleteEntry(entry);
			}
		}
		
		commissionEntry = null;
		tax1Entry = null;
		tax2Entry = null;
		purchaseOrSaleEntry = null;
		transferEntry = null;
		
		if (dividendEntry == null) {
			dividendEntry = entries.createEntry();
			dividendEntry.setAccount(account.getDividendAccount());
		}

		if (withholdingTaxEntry == null && account.getWithholdingTaxAccount() != null) {
			withholdingTaxEntry = entries.createEntry();
			withholdingTaxEntry.setAccount(account.getWithholdingTaxAccount());
		}

		dividendEntry.setAmount(-mainEntry.getAmount());
	}

	public void forceTransactionToBuy() {
		forceTransactionToBuyOrSell(TransactionType.Buy);
	}
	
	public void forceTransactionToSell() {
		forceTransactionToBuyOrSell(TransactionType.Sell);
	}
	
	private void forceTransactionToBuyOrSell(TransactionType transactionType) {
		this.transactionType = transactionType;
			
		// Move this to be a field???
		StockAccount account = (StockAccount)uncommittedEntryData.getEntry().getAccount();

		EntryCollection entries = uncommittedEntryData.getEntry().getTransaction().getEntryCollection();
		for (Entry entry: entries) {
			if (entry != mainEntry
					&& entry != commissionEntry
					&& entry != tax1Entry
					&& entry != tax2Entry
					&& entry != purchaseOrSaleEntry) {
				entries.deleteEntry(entry);
			}
		}
		
		dividendEntry = null;
		withholdingTaxEntry = null;
		transferEntry = null;
		
		if (purchaseOrSaleEntry == null) {
			purchaseOrSaleEntry = entries.createEntry();
			purchaseOrSaleEntry.setAccount(account);
		}

		// TODO: What is our strategy on changing values to keep
		// the transaction balanced.  Quicken has a dialog box that
		// asks the user what to adjust (with a 'recommended' choice
		// that in my experience is never the correct choice!).
		
//		dividendEntry.setAmount(-mainEntry.getAmount());
	}

	public boolean isPurchaseOrSale() {
		return transactionType == TransactionType.Buy
		|| transactionType == TransactionType.Sell;
	}

	public boolean isDividend() {
		return transactionType == TransactionType.Dividend;
	}

	public Entry getDividendEntry() {
		Assert.isTrue(isDividend());
		return dividendEntry;
	}

	public Entry getWithholdingTaxEntry() {
		Assert.isTrue(isDividend());
		return withholdingTaxEntry;
	}

	public Entry getPurchaseOrSaleEntry() {
		Assert.isTrue(isPurchaseOrSale());
		return purchaseOrSaleEntry;
	}

	/*
	 * Notify listeners when the transaction type (purchase, sale, dividend etc.)
	 * changes.
	 */
	public void fireTransactionTypeChange() {
		for (ITransactionTypeChangeListener listener: transactionTypeChangeListeners) {
			listener.transactionTypeChanged();
		}
	}
	
	public void addTransactionTypeChangeListener(ITransactionTypeChangeListener listener) {
		transactionTypeChangeListeners.add(listener);
	}

}
