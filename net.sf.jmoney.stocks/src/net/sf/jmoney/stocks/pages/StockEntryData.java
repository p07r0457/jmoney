package net.sf.jmoney.stocks.pages;

import java.util.Iterator;

import org.eclipse.core.runtime.Assert;

import net.sf.jmoney.entrytable.EntryData;
import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.CurrencyAccount;
import net.sf.jmoney.model2.DataManager;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.Transaction.EntryCollection;
import net.sf.jmoney.stocks.Stock;
import net.sf.jmoney.stocks.StockAccount;
import net.sf.jmoney.stocks.pages.StockEntryRowControl.TransactionType;

public class StockEntryData extends EntryData {

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

	public StockEntryData(Entry entry, DataManager dataManager) {
		super(entry, dataManager);

		// Note that there are two versions of this object for every row.
		// One contains the committed entry and the other contains the entry
		// being edited inside a transaction.  If this is the new entry row
		// and is the committed version then entry will be null, so we can't
		// analyze it.
		
		// TODO We should consider merging the two instances into one.
		
		// TODO Call this on-demand.
		if (entry != null) {
			analyzeTransaction();
		}
	}

	private void analyzeTransaction() {
		/*
		 * Analyze the transaction to see which type of transaction this is.
		 */
		StockAccount account = (StockAccount)getEntry().getAccount();

		/*
		 * If just one entry then this is not a valid transaction, so must be
		 * a new transaction.  We set the transaction type to null which means
		 * no selection will be set in the transaction type combo.
		 */
		if (getEntry().getTransaction().getEntryCollection().size() == 1) {
			transactionType = null;
		} else {

			for (Entry entry: getEntry().getTransaction().getEntryCollection()) {
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
	}

	public void forceTransactionToDividend() {
		transactionType = TransactionType.Dividend;

		// Move this to be a field???
		StockAccount account = (StockAccount)getEntry().getAccount();

		EntryCollection entries = getEntry().getTransaction().getEntryCollection();
		for (Iterator<Entry> iter = entries.iterator(); iter.hasNext(); ) {
			Entry entry = iter.next();
			if (entry != mainEntry
					&& entry != dividendEntry
				&& entry != withholdingTaxEntry) {
				iter.remove();
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
		StockAccount account = (StockAccount)getEntry().getAccount();

		EntryCollection entries = getEntry().getTransaction().getEntryCollection();
		for (Iterator<Entry> iter = entries.iterator(); iter.hasNext(); ) {
			Entry entry = iter.next();
			if (entry != mainEntry
					&& entry != commissionEntry
					&& entry != tax1Entry
					&& entry != tax2Entry
					&& entry != purchaseOrSaleEntry) {
				iter.remove();
			}
		}
		
		dividendEntry = null;
		withholdingTaxEntry = null;
		transferEntry = null;
		
		if (purchaseOrSaleEntry == null) {
			purchaseOrSaleEntry = entries.createEntry();
			purchaseOrSaleEntry.setAccount(account);
		}

		if (commissionEntry == null && account.getCommissionAccount() != null) {
			commissionEntry = entries.createEntry();
			commissionEntry.setAccount(account.getCommissionAccount());
		}

		if (tax1Entry == null && account.getTax1Account() != null) {
			tax1Entry = entries.createEntry();
			tax1Entry.setAccount(account.getTax1Account());
		}

		if (tax2Entry == null && account.getTax2Account() != null) {
			tax2Entry = entries.createEntry();
			tax2Entry.setAccount(account.getTax2Account());
		}

		// TODO: What is our strategy on changing values to keep
		// the transaction balanced.  Quicken has a dialog box that
		// asks the user what to adjust (with a 'recommended' choice
		// that in my experience is never the correct choice!).
		
//		dividendEntry.setAmount(-mainEntry.getAmount());
	}

	public void forceTransactionToTransfer() {
		transactionType = TransactionType.Transfer;

		EntryCollection entries = getEntry().getTransaction().getEntryCollection();
		for (Iterator<Entry> iter = entries.iterator(); iter.hasNext(); ) {
			Entry entry = iter.next();
			if (entry != mainEntry
					&& entry != transferEntry) {
				iter.remove();
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

	public void forceTransactionToCustom() {
		transactionType = TransactionType.Other;

		/*
		 * This method is not so much a 'force' as a 'set'.  The other 'force' methods
		 * have to modify the transaction, including the lose of information, in order
		 * to transform the transaction to the required type.  This method does not need
		 * to change the transaction data at all.  It does adjust the UI to give the user
		 * full flexibility.
		 * 
		 * Note that the user may edit the transaction so that it matches one of the
		 * types (buy, sell, dividend etc).  In that case, the transaction will appear
		 * as that type, not as a custom type, if it is saved and re-loaded.
		 */
		
		// Must be at least one entry
		EntryCollection entries = getEntry().getTransaction().getEntryCollection();
		if (entries.size() == 1) {
			entries.createEntry();
		}

		/*
		 * Forget the special entries. It may be that these would be useful to
		 * keep in case the user decides to go back to one of the set
		 * transaction types. However, the user may edit these entries, or
		 * delete them, and it is too complicated to worry about the
		 * consequences.
		 */
		dividendEntry = null;
		withholdingTaxEntry = null;
		commissionEntry = null;
		tax1Entry = null;
		tax2Entry = null;
		purchaseOrSaleEntry = null;
		transferEntry = null;
	}

	public TransactionType getTransactionType() {
		return transactionType;
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

	/**
	 * 
	 * @return the entry that represents the withholding tax, or null if the
	 *         withholding tax is not applicable to this transaction
	 *         (transaction is a transfer or some other type that does not
	 *         represent a transaction type which may potentially include a
	 *         withholding tax)
	 */
	public Entry getWithholdingTaxEntry() {
		return withholdingTaxEntry;
	}

	/**
	 * @return the entry in the transaction that represents the gain or loss in
	 *         the number of shares, or null if this is not a purchase or sale
	 *         transaction
	 */
	public Entry getPurchaseOrSaleEntry() {
		return purchaseOrSaleEntry;
	}

	/**
	 * @return the entry in the transaction that represents the
	 * 		commission, or null if this is not a purchase or sale
	 * 		transaction
	 */
	public Entry getCommissionEntry() {
		return commissionEntry;
	}

	/**
	 * @return the entry in the transaction that represents the
	 * 		tax 1 amount, or null if this is not a purchase or sale
	 * 		transaction
	 */
	public Entry getTax1Entry() {
		return tax1Entry;
	}

	/**
	 * @return the entry in the transaction that represents the
	 * 		tax 2 amount, or null if this is not a purchase or sale
	 * 		transaction
	 */
	public Entry getTax2Entry() {
		return tax2Entry;
	}

	/**
	 * @return the entry in the transaction that is the other entry
	 * 		in a transfer transaction, or null if this is not a transfer
	 * 		transaction
	 */
	public Entry getTransferEntry() {
		return transferEntry;
	}
}
