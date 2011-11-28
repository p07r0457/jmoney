package net.sf.jmoney.stocks.pages;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.sf.jmoney.entrytable.EntryData;
import net.sf.jmoney.entrytable.InvalidUserEntryException;
import net.sf.jmoney.isolation.TransactionManager;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.DataManager;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.EntryInfo;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.ListPropertyAccessor;
import net.sf.jmoney.model2.ReferenceViolationException;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.model2.SessionChangeListener;
import net.sf.jmoney.model2.Transaction.EntryCollection;
import net.sf.jmoney.stocks.model.Security;
import net.sf.jmoney.stocks.model.StockAccount;
import net.sf.jmoney.stocks.model.StockEntryInfo;
import net.sf.jmoney.stocks.pages.StockEntryRowControl.TransactionType;

import org.eclipse.core.runtime.Assert;

public class StockEntryData extends EntryData {

	private StockAccount account;
	
	private TransactionType transactionType;

	private Entry dividendEntry;
	private Entry withholdingTaxEntry;
	
	/**
	 * the entry for the commission, or null if this is not a purchase or sale
	 * transaction or if no commission account is configured for this stock
	 * account because commissions are never charged on any purchases or sales
	 * in this account, and possibly null if there can be a commission but none
	 * has been entered for this entry
	 */
	private Entry commissionEntry;
	
	private Entry tax1Entry;
	private Entry tax2Entry;
	private Entry purchaseOrSaleEntry;
	private Entry transferEntry;

	private boolean unknownTransactionType;

	private List<IPropertyChangeListener<Long>> withholdingTaxChangeListeners = new ArrayList<IPropertyChangeListener<Long>>();
	private List<IPropertyChangeListener<Long>> commissionChangeListeners = new ArrayList<IPropertyChangeListener<Long>>();
	private List<IPropertyChangeListener<Long>> tax1ChangeListeners = new ArrayList<IPropertyChangeListener<Long>>();
	private List<IPropertyChangeListener<Long>> tax2ChangeListeners = new ArrayList<IPropertyChangeListener<Long>>();

	public StockEntryData(Entry entry, DataManager dataManager) {
		super(entry, dataManager);


		// Note that there are two versions of this object for every row.
		// One contains the committed entry and the other contains the entry
		// being edited inside a transaction.  If this is the new entry row
		// and is the committed version then entry will be null, so we can't
		// analyze it, we can't determine the account, and we don't want to
		// listen for changes (doing so will likely cause a NPE).

		// TODO We should consider merging the two instances into one.

		// TODO Call this on-demand.
		if (entry != null) {
			account = (StockAccount)entry.getAccount();
			analyzeTransaction();

			/*
			 * Listen for changes that affect the calculated values exposed by this
			 * wrapper object.
			 */
			dataManager.addChangeListenerWeakly(new SessionChangeListener() {

				public void objectInserted(ExtendableObject newObject) {
					if (newObject instanceof Entry) {
						Entry newEntry = (Entry)newObject;
						if (newEntry.getTransaction() == getEntry().getTransaction()) {
							if (newEntry.getAccount() == account.getCommissionAccount()) {
								if (commissionEntry != null) {
									throw new RuntimeException("already has a commission entry!");
								}
								commissionEntry = newEntry;
								fireCommmissionChangedEvent(newEntry.getAmount());
							} else if (newEntry.getAccount() == account.getTax1Account()) {
								if (tax1Entry != null) {
									throw new RuntimeException("already has a tax 1 entry!");
								}
								tax1Entry = newEntry;
								fireTax1ChangedEvent(newEntry.getAmount());
							} else if (newEntry.getAccount() == account.getTax2Account()) {
								if (tax2Entry != null) {
									throw new RuntimeException("already has a tax 2 entry!");
								}
								tax2Entry = newEntry;
								fireTax2ChangedEvent(newEntry.getAmount());
							}
						}
					}
				}

				public void objectCreated(ExtendableObject newObject) {
					// TODO Auto-generated method stub

				}

				public void objectRemoved(ExtendableObject deletedObject) {
					if (deletedObject instanceof Entry) {
						Entry deletedEntry = (Entry)deletedObject;
						if (deletedEntry.getTransaction() == getEntry().getTransaction()) {
							if (deletedEntry.getAccount() == account.getCommissionAccount()) {
								if (commissionEntry == null) {
									throw new RuntimeException("but no commission entry was set!");
								}
								commissionEntry = null;
								fireCommmissionChangedEvent(0L);
							} else if (deletedEntry.getAccount() == account.getTax1Account()) {
								if (tax1Entry == null) {
									throw new RuntimeException("but no tax 1 entry was set!");
								}
								tax1Entry = null;
								fireTax1ChangedEvent(0L);
							} else if (deletedEntry.getAccount() == account.getTax2Account()) {
								if (tax2Entry == null) {
									throw new RuntimeException("but no tax 1 entry was set!");
								}
								tax2Entry = null;
								fireTax2ChangedEvent(0L);
							}
						}
					}
				}

				public void objectDestroyed(ExtendableObject deletedObject) {
					// TODO Auto-generated method stub

				}

				public void objectChanged(ExtendableObject changedObject,
						ScalarPropertyAccessor changedProperty, Object oldValue,
						Object newValue) {
					if (changedObject instanceof Entry) {
						Entry changedEntry = (Entry)changedObject;
						if (changedEntry.getTransaction() == getEntry().getTransaction()) {
							if (changedProperty == EntryInfo.getAmountAccessor()) {
								Long newAmount = (Long)newValue;

								if (changedEntry.getAccount() == account.getCommissionAccount()) {
									if (commissionEntry == null) {
										throw new RuntimeException("but no commission entry was set!");
									}
									fireCommmissionChangedEvent(newAmount);
								} else if (changedEntry.getAccount() == account.getTax1Account()) {
									if (tax1Entry == null) {
										throw new RuntimeException("but no tax 1 entry was set!");
									}
									fireTax1ChangedEvent(newAmount);
								} else if (changedEntry.getAccount() == account.getTax2Account()) {
									if (tax2Entry == null) {
										throw new RuntimeException("but no tax 1 entry was set!");
									}
									fireTax2ChangedEvent(newAmount);
								}
							}
						}
					}
				}

				public void objectMoved(ExtendableObject movedObject,
						ExtendableObject originalParent,
						ExtendableObject newParent,
						ListPropertyAccessor originalParentListProperty,
						ListPropertyAccessor newParentListProperty) {
					// TODO Auto-generated method stub

				}

				public void performRefresh() {
					// TODO Auto-generated method stub

				}

				private void fireCommmissionChangedEvent(Long newValue) {
					for (IPropertyChangeListener<Long> listener  : commissionChangeListeners) {
						listener.propertyChanged(newValue);
					}				
				}

				private void fireTax1ChangedEvent(Long newValue) {
					for (IPropertyChangeListener<Long> listener  : tax1ChangeListeners) {
						listener.propertyChanged(newValue);
					}				
				}

				private void fireTax2ChangedEvent(Long newValue) {
					for (IPropertyChangeListener<Long> listener  : tax2ChangeListeners) {
						listener.propertyChanged(newValue);
					}				
				}
			});
		}
	}

	private void analyzeTransaction() {
		/*
		 * Analyze the transaction to see which type of transaction this is.
		 */

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
					if (entry.getCommodityInternal() instanceof Security) {
						if (purchaseOrSaleEntry != null) {
							unknownTransactionType = true;
						}
						purchaseOrSaleEntry = entry;
					} else if (entry.getCommodityInternal() instanceof Currency) {  //TODO: check for actual currency of account.
						// The only entry affecting the currency balance in this account
						// should be the main entry.
						if (entry != getEntry()) {
							unknownTransactionType = true;
						}
					}
				} else if (entry.getAccount() instanceof CapitalAccount
						&& entry.getAccount() != account
						&& entry.getCommodityInternal() == account.getCurrency()) {
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
		// Get the security from the old transaction, which must be done
		// before we start messing with this transaction.
		Security security = getSecurityFromTransaction();
		
		transactionType = TransactionType.Dividend;

		EntryCollection entries = getEntry().getTransaction().getEntryCollection();
		for (Iterator<Entry> iter = entries.iterator(); iter.hasNext(); ) {
			Entry entry = iter.next();
			if (entry != getEntry()
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
			dividendEntry.setPropertyValue(StockEntryInfo.getSecurityAccessor(), security);
		}

		long grossDividend = -getEntry().getAmount();
		if (withholdingTaxEntry != null) {
			grossDividend += withholdingTaxEntry.getAmount();
		}
		dividendEntry.setAmount(grossDividend);
	}

	/**
	 * This security is used only when a transaction is being forced by the user from one
	 * transaction type to another.  For example if the user decided that a stock purchase
	 * transaction was really a dividend payment.  The value returned by this method is
	 * used to ensure the stock stays set but it is not critical if no stock is returned.
	 * 
	 * @return the security involved in this transaction, or null if the transaction
	 * 			does not involve a security (eg cash transfer), involves multiple securities
	 * 			(eg merger), or the transaction is not complete
	 */
	private Security getSecurityFromTransaction() {
		if (isPurchaseOrSale()) {
			return (Security)purchaseOrSaleEntry.getCommodityInternal();
		} else if (isDividend()) {
			return dividendEntry.getPropertyValue(StockEntryInfo.getSecurityAccessor());
		} else {
			return null;
		}
	}

	public void forceTransactionToBuy() {
		forceTransactionToBuyOrSell(TransactionType.Buy);
	}
	
	public void forceTransactionToSell() {
		forceTransactionToBuyOrSell(TransactionType.Sell);
	}
	
	private void forceTransactionToBuyOrSell(TransactionType transactionType) {
		Security security = getSecurityFromTransaction();

		this.transactionType = transactionType;
			
		EntryCollection entries = getEntry().getTransaction().getEntryCollection();
		for (Iterator<Entry> iter = entries.iterator(); iter.hasNext(); ) {
			Entry entry = iter.next();
			if (entry != getEntry()
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
			purchaseOrSaleEntry.setCommodity(security);
		}

		// Commission, tax 1, and tax 2 entries may be null in this transaction type.
		// They are created when needed if non-zero amounts are entered.

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
			if (entry != getEntry()
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
		transferEntry.setAmount(-getEntry().getAmount());
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
	 * @return the withholding tax amount in a dividend transaction if a
	 *         withholding tax account is configured for the account, being zero if
	 *         no entry exists in the transaction in the withholding tax account
	 */
	public long getWithholdingTax() {
		assert(isDividend());
		assert(account.getWithholdingTaxAccount() != null);
		
		if (withholdingTaxEntry == null) {
			return 0;
		} else {
			return withholdingTaxEntry.getAmount();
		}
	}

	public void setWithholdingTax(long withholdingTax) {
		assert(isDividend());
		assert(account.getWithholdingTaxAccount() != null);
		
		if (withholdingTax == 0) {
			if (withholdingTaxEntry != null) {
				try {
					getEntry().getTransaction().getEntryCollection().deleteElement(withholdingTaxEntry);
				} catch (ReferenceViolationException e) {
					// This should not happen because entries are never referenced
					throw new RuntimeException("Internal error", e);
				}
				withholdingTaxEntry = null;
			}
		} else {
			if (withholdingTaxEntry == null) {
				withholdingTaxEntry = getEntry().getTransaction().createEntry();
				withholdingTaxEntry.setAccount(account.getWithholdingTaxAccount());
				withholdingTaxEntry.setPropertyValue(StockEntryInfo.getSecurityAccessor(), dividendEntry.getPropertyValue(StockEntryInfo.getSecurityAccessor()));
			}
			withholdingTaxEntry.setAmount(withholdingTax);
		}
	}

	public void addWithholdingTaxChangeListener(IPropertyChangeListener<Long> listener) {
		withholdingTaxChangeListeners.add(listener);
	}

	public void removeWithholdingTaxChangeListener(IPropertyChangeListener<Long> listener) {
		withholdingTaxChangeListeners.remove(listener);
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
	 * @return the commission amount in a purchase or sale transaction if a
	 *         commission account is configured for the account, being zero if
	 *         no entry exists in the transaction in the commission account
	 */
	public long getCommission() {
		assert(isPurchaseOrSale());
		assert(account.getCommissionAccount() != null);
		
		if (commissionEntry == null) {
			return 0;
		} else {
			return commissionEntry.getAmount();
		}
	}

	public void setCommission(long commission) {
		assert(isPurchaseOrSale());
		assert(account.getTax1Account() != null);
		
		if (commission == 0) {
			if (commissionEntry != null) {
				try {
					getEntry().getTransaction().getEntryCollection().deleteElement(commissionEntry);
				} catch (ReferenceViolationException e) {
					// This should not happen because entries are never referenced
					throw new RuntimeException("Internal error", e);
				}
				commissionEntry = null;
			}
		} else {
			if (commissionEntry == null) {
				commissionEntry = getEntry().getTransaction().createEntry();
				commissionEntry.setAccount(account.getCommissionAccount());
				commissionEntry.setPropertyValue(StockEntryInfo.getSecurityAccessor(), (Security)purchaseOrSaleEntry.getCommodity());
			}
			commissionEntry.setAmount(commission);
		}
	}

	public void addCommissionChangeListener(IPropertyChangeListener<Long> listener) {
		commissionChangeListeners.add(listener);
	}

	public void removeCommissionChangeListener(IPropertyChangeListener<Long> listener) {
		commissionChangeListeners.remove(listener);
	}

	/**
	 * @return the tax 1 amount in a purchase or sale transaction if a tax 1 is
	 *         configured for the account, being zero if no entry exists in the
	 *         transaction in the tax 1 account
	 */
	public long getTax1Amount() {
		assert(isPurchaseOrSale());
		assert(account.getTax1Account() != null);
		
		if (tax1Entry == null) {
			return 0;
		} else {
			return tax1Entry.getAmount();
		}
	}

	public void setTax1Amount(long tax1Amount) {
		assert(isPurchaseOrSale());
		assert(account.getTax1Account() != null);
		
		if (tax1Amount == 0) {
			if (tax1Entry != null) {
				try {
					getEntry().getTransaction().getEntryCollection().deleteElement(tax1Entry);
				} catch (ReferenceViolationException e) {
					// This should not happen because entries are never referenced
					throw new RuntimeException("Internal error", e);
				}
				tax1Entry = null;
			}
		} else {
			if (tax1Entry == null) {
				tax1Entry = getEntry().getTransaction().createEntry();
				tax1Entry.setAccount(account.getTax1Account());
				tax1Entry.setPropertyValue(StockEntryInfo.getSecurityAccessor(), (Security)purchaseOrSaleEntry.getCommodity());
			}
			tax1Entry.setAmount(tax1Amount);
		}
	}

	public void addTax1ChangeListener(IPropertyChangeListener<Long> listener) {
		tax1ChangeListeners.add(listener);
	}

	public void removeTax1ChangeListener(IPropertyChangeListener<Long> listener) {
		tax1ChangeListeners.remove(listener);
	}

	/**
	 * @return the tax 2 amount in a purchase or sale transaction if a tax 2 is
	 *         configured for the account, being zero if no entry exists in the
	 *         transaction in the tax 2 account
	 */
	public long getTax2Amount() {
		assert(isPurchaseOrSale());
		assert(account.getTax2Account() != null);
		
		if (tax2Entry == null) {
			return 0;
		} else {
			return tax2Entry.getAmount();
		}
	}

	public void setTax2Amount(long tax2Amount) {
		assert(isPurchaseOrSale());
		assert(account.getTax2Account() != null);
		
		if (tax2Amount == 0) {
			if (tax2Entry != null) {
				try {
					getEntry().getTransaction().getEntryCollection().deleteElement(tax2Entry);
				} catch (ReferenceViolationException e) {
					// This should not happen because entries are never referenced
					throw new RuntimeException("Internal error", e);
				}
				tax2Entry = null;
			}
		} else {
			if (tax2Entry == null) {
				tax2Entry = getEntry().getTransaction().createEntry();
				tax2Entry.setAccount(account.getTax2Account());
				tax2Entry.setPropertyValue(StockEntryInfo.getSecurityAccessor(), (Security)purchaseOrSaleEntry.getCommodity());
			}
			tax2Entry.setAmount(tax2Amount);
		}
	}

	public void addTax2ChangeListener(IPropertyChangeListener<Long> listener) {
		tax2ChangeListeners.add(listener);
	}

	public void removeTax2ChangeListener(IPropertyChangeListener<Long> listener) {
		tax2ChangeListeners.remove(listener);
	}

	/**
	 * @return the entry in the transaction that is the other entry
	 * 		in a transfer transaction, or null if this is not a transfer
	 * 		transaction
	 */
	public Entry getTransferEntry() {
		return transferEntry;
	}

	/*
	 * The price is calculated, not stored in the model. This method
	 * calculates the share price from the data in the model.  It does
	 * this by adding up all the cash entries to get the gross proceeds
	 * or cost and then dividing by the number of shares.
	 * 
	 * @return the calculated price to four decimal places, or null
	 * 		if the price cannot be calculated (e.g. if the share quantity
	 * 		is zero)
	 */
	public BigDecimal calculatePrice() {
		assert(isPurchaseOrSale());

		BigDecimal totalShares = BigDecimal.valueOf(purchaseOrSaleEntry.getAmount())
				.movePointLeft(3);

		long totalCash = 0;
		for (Entry eachEntry: getEntry().getTransaction().getEntryCollection()) {
			if (eachEntry.getCommodityInternal() instanceof Currency) {
				totalCash += eachEntry.getAmount();
			}
		}
		
		BigDecimal price = null;
		if (totalCash != 0 && totalShares.compareTo(BigDecimal.ZERO) != 0) {
			/*
			 * Either we gain cash and lose stock, or we lose cash and gain
			 * stock. Hence we need to negate to get a positive value.
			 */
			price = BigDecimal.valueOf(-totalCash).movePointLeft(2).divide(totalShares, 4, RoundingMode.HALF_UP);
		}
		
		return price;
	}

	public void specificValidation() throws InvalidUserEntryException {
		if (transactionType == null) {
			throw new InvalidUserEntryException("No transaction type selected.", null);
		}
		
		/*
		 * Check for zero amounts. Some fields may be zeroes (for example, commissions and
		 * withheld taxes), others may not (for example, quantity of stock sold).
		 * 
		 * We do leave entries with zero amounts.  This makes the code simpler
		 * because the transaction is already set up for the transaction type,
		 * and it is easier to determine the transaction type.  
		 * 
		 * It is possible that the total proceeds of a sale are zero.  Anyone who
		 * has disposed of shares in a sub-prime mortgage company in order to
		 * claim the capital loss will know that the commission may equal the sale
		 * price.  It is probably good that the transaction still shows up in
		 * the cash entries list for the account.
		 */
		switch (transactionType) {
		case Buy:
		case Sell:
			if (purchaseOrSaleEntry.getAmount() == 0) {
				throw new InvalidUserEntryException("The quantity of stock in a purchase or sale cannot be zero.", null);
			}
			if (commissionEntry != null 
					&& commissionEntry.getAmount() == 0) {
				getEntry().getTransaction().deleteEntry(commissionEntry);
				commissionEntry = null;
			}
			if (tax1Entry != null 
					&& tax1Entry.getAmount() == 0) {
				getEntry().getTransaction().deleteEntry(tax1Entry);
				tax1Entry = null;
			}
			if (tax2Entry != null 
					&& tax2Entry.getAmount() == 0) {
				getEntry().getTransaction().deleteEntry(tax2Entry);
				tax2Entry = null;
			}
			break;
		case Dividend:
			if (dividendEntry.getAmount() == 0) {
				throw new InvalidUserEntryException("The amount of a dividend cannot be zero.", null);
			}
			if (withholdingTaxEntry != null 
					&& withholdingTaxEntry.getAmount() == 0
					&& withholdingTaxEntry.getMemo() == null) {
				getEntry().getTransaction().deleteEntry(withholdingTaxEntry);
				withholdingTaxEntry = null;
			}
			break;
		case Transfer:
			if (transferEntry.getAmount() == 0) {
				throw new InvalidUserEntryException("The amount of a transfer cannot be zero.", null);
			}
			break;
		case Other:
			// We don't allow any amounts to be zero except the listed entry
			// (the listed entry is used to ensure a transaction appears in this
			// list even if the transaction does not result in a change in the cash
			// balance).
			Entry mainEntry = getEntry();
			if (mainEntry.getTransaction().getEntryCollection().size() == 1) {
				// TODO: create another entry when 'other' selected and don't allow it to be
				// deleted, thus this check is not necessary.
				// TODO: should not be 'other' when no transaction has been selected
				// (should be null)
				throw new InvalidUserEntryException("Must have another entry.", null);
			}
			for (Entry entry : mainEntry.getTransaction().getEntryCollection()) {
				if (entry != mainEntry) {
					if (entry.getAmount() == 0) {
						throw new InvalidUserEntryException("The amount of an entry in this transaction cannot be zero.", null);
					}
				}
			}
			break;
		}
	}

	/**
	 * Copies data from the given object into this object.  This method is used
	 * only when duplicating a transaction.  This object will be the object for the 'new entry'
	 * row that appears at the bottom on the transaction table.
	 */
	public void copyFrom(EntryData sourceEntryData) {
//		StockEntryData sourceEntryData = ()sourceEntryData2;
		
		Entry selectedEntry = sourceEntryData.getEntry();
		
		Entry newEntry = getEntry();
		TransactionManager transactionManager = (TransactionManager)newEntry.getDataManager();
		
//		newEntry.setMemo(selectedEntry.getMemo());
//		newEntry.setAmount(selectedEntry.getAmount());

		/*
		 * Copy all values that are numbers, flags, text, or references to accounts or commodities.
		 * We do not copy dates or statement numbers.
		 */
		for (ScalarPropertyAccessor accessor : EntryInfo.getPropertySet().getScalarProperties3()) {
			Object value = selectedEntry.getPropertyValue(accessor);
			if (value instanceof Integer
					|| value instanceof Long
					|| value instanceof Boolean
					|| value instanceof String) {
				newEntry.setPropertyValue(accessor, value);
			}
			if (value instanceof Commodity
					|| value instanceof Account) {
				newEntry.setPropertyValue(accessor, transactionManager.getCopyInTransaction((ExtendableObject)value));
			}
		}
		
		/*
		 * In the bank account entries, the new entry row will always have a second entry created.
		 * In other enty types such as a stock entry, the new entry row will have only one row.
		 */
		Entry thisEntry = getSplitEntries().isEmpty()
		? null : getOtherEntry();

		for (Entry origEntry: sourceEntryData.getSplitEntries()) {
			if (thisEntry == null) {
				thisEntry = getEntry().getTransaction().createEntry();
			}
//			thisEntry.setAccount(transactionManager.getCopyInTransaction(origEntry.getAccount()));
//			thisEntry.setMemo(origEntry.getMemo());
//			thisEntry.setAmount(origEntry.getAmount());
			
			/*
			 * Copy all values that are numbers, flags, text, or references to accounts or commodities.
			 * We do not copy dates or statement numbers.
			 */
			for (ScalarPropertyAccessor accessor : EntryInfo.getPropertySet().getScalarProperties3()) {
				Object value = origEntry.getPropertyValue(accessor);
				if (value instanceof Integer
						|| value instanceof Long
						|| value instanceof Boolean
						|| value instanceof String) {
				thisEntry.setPropertyValue(accessor, value);
				}
				if (value instanceof Commodity
						|| value instanceof Account) {
				thisEntry.setPropertyValue(accessor, transactionManager.getCopyInTransaction((ExtendableObject)value));
				}
			}
			thisEntry = null;
		}

		// Hack, because analyze assumes this has not yet been set.
//		mainEntry = null;
		
		analyzeTransaction();
	}

	/**
	 * This method is called when the user edits the security.
	 * <P>
	 * The purchase and sale entry contains the security as the commodity of the value in
	 * the amount field.  For the other entries the security is only a reference.  For example
	 * for a dividend payment the commodity must be set to the currency of the dividend payment.
	 *   
	 * @param security
	 */
	public void setSecurity(Security security) {
		if (isPurchaseOrSale()) {
			purchaseOrSaleEntry.setCommodity(security);
			if (commissionEntry != null) {
				commissionEntry.setPropertyValue(StockEntryInfo.getSecurityAccessor(), security);
			}
			if (tax1Entry != null) {
				tax1Entry.setPropertyValue(StockEntryInfo.getSecurityAccessor(), security);
			}
			if (tax2Entry != null) {
				tax2Entry.setPropertyValue(StockEntryInfo.getSecurityAccessor(), security);
			}
		} else if (isDividend()) {
			dividendEntry.setPropertyValue(StockEntryInfo.getSecurityAccessor(), security);
			if (withholdingTaxEntry != null) {
				withholdingTaxEntry.setPropertyValue(StockEntryInfo.getSecurityAccessor(), security);
			}
		}
	}
}
