package net.sf.jmoney.stocks.pages;

import java.math.BigDecimal;
import java.util.ArrayList;

import net.sf.jmoney.entrytable.BaseEntryRowControl;
import net.sf.jmoney.entrytable.Block;
import net.sf.jmoney.entrytable.FocusCellTracker;
import net.sf.jmoney.entrytable.InvalidUserEntryException;
import net.sf.jmoney.entrytable.RowSelectionTracker;
import net.sf.jmoney.entrytable.VirtualRowTable;
import net.sf.jmoney.isolation.TransactionManager;
import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.stocks.model.RatesTable;
import net.sf.jmoney.stocks.model.StockAccount;

import org.eclipse.swt.widgets.Composite;

public class StockEntryRowControl extends BaseEntryRowControl<StockEntryData, StockEntryRowControl> {

	public enum TransactionType {
		Buy,
		Sell,
		Dividend,
		Transfer,
		Other
	}

	private ArrayList<ITransactionTypeChangeListener> transactionTypeChangeListeners = new ArrayList<ITransactionTypeChangeListener>();
	
	/**
	 * true if the user has manually edited the withholding tax, false
	 * if no amount has been entered or the amount was calculated
	 */
	private boolean withholdingTaxManuallyEdited = false;

	private boolean quantityManuallyEdited = false;

	/**
	 * The share price currently shown to the user, or null if
	 * this is a new entry and no share price has been entered.
	 * 
	 * This is saved here (not fetched each time) because we often
	 * update other fields when fields are updated in order to keep
	 * the transaction balanced.  We need to know the previous price,
	 * not the price that would have balanced the transaction, when
	 * we do these calculations.
	 */
	private BigDecimal sharePrice;

	private boolean sharePriceManuallyEdited = false;

	private boolean commissionManuallyEdited = false;

	private boolean tax1RatesManuallyEdited = false;

	private boolean tax2RatesToBeCalculated = false;

	private boolean netAmountManuallyEdited = false;

	public StockEntryRowControl(final Composite parent, int style, VirtualRowTable rowTable, Block<StockEntryData, ? super StockEntryRowControl> rootBlock, final RowSelectionTracker selectionTracker, final FocusCellTracker focusCellTracker) {
		super(parent, style, rowTable, rootBlock, selectionTracker, focusCellTracker);
		init(this, this, rootBlock);
	}
	
	@Override
	public void setContent(StockEntryData committedEntryData) {
		super.setContent(committedEntryData);
		if (committedEntryData == null) {
			// TODO: Is there a better way of seeing if there is a tax table?
			StockAccount stockAccount = (StockAccount)uncommittedEntryData.getEntry().getAccount();
			tax2RatesToBeCalculated = (stockAccount.getTax2Account() != null && stockAccount.getTax2Rates() != null);
		} else {
			tax2RatesToBeCalculated = false;
		}
		
		if (uncommittedEntryData.isPurchaseOrSale()) {
			sharePrice = uncommittedEntryData.calculatePrice();
		} else {
			sharePrice = null;
		}
		
		this.addTransactionTypeChangeListener(new ITransactionTypeChangeListener() {
			public void transactionTypeChanged() {
				if (uncommittedEntryData.isPurchaseOrSale()) {
					sharePrice = uncommittedEntryData.calculatePrice();
				} else {
					sharePrice = null;
				}
			}
		});
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

	@Override
	protected StockEntryData createUncommittedEntryData(
			Entry entryInTransaction, TransactionManager transactionManager) {
		StockEntryData entryData = new StockEntryData(entryInTransaction, transactionManager);
		return entryData;
	}

	@Override
	protected StockEntryRowControl getThis() {
		return this;
	}

	@Override
	public void amountChanged() {
		
		StockAccount account = (StockAccount)getUncommittedEntryData().getEntry().getAccount();
		
		
		Entry entry = uncommittedEntryData.getEntry();
		switch (uncommittedEntryData.getTransactionType()) {
		case Buy:
		case Sell:
			/**
			 * The user would not usually enter the net amount for the transaction
			 * because it is hard to calculate backwards from this.  The rates tables
			 * are all based on the gross amount.  Also, there may be a number of
			 * calculated commissions and taxes and we would not know which to adjust.
			 * Therefore we leave the transaction unbalanced and force the user to
			 * correct it when the transaction is saved.
			 */
			break;
		case Dividend:
			if (!withholdingTaxManuallyEdited) {
				long rate = 30L;
				long tax = entry.getAmount() * rate / (100 - rate);
				uncommittedEntryData.getWithholdingTaxEntry().setAmount(-tax);
			}
			long dividend = entry.getAmount() + uncommittedEntryData.getWithholdingTaxEntry().getAmount();
			uncommittedEntryData.getDividendEntry().setAmount(-dividend);
			break;
		case Transfer:
			uncommittedEntryData.getTransferEntry().setAmount(-entry.getAmount());
			break;
		case Other:
			// If there are two entries in the transaction and
			// if both entries have accounts in the same currency or
			// one or other account is not known or one or other account
			// is a multi-currency account then we set the amount in
			// the other entry to be the same but opposite signed amount.
			if (entry.getTransaction().hasTwoEntries()) {
				Entry otherEntry = entry.getTransaction().getOther(entry);
				Commodity commodity1 = entry.getCommodity();
				Commodity commodity2 = otherEntry.getCommodity();
				if (commodity1 == null || commodity2 == null || commodity1.equals(commodity2)) {
					otherEntry.setAmount(-entry.getAmount());
				}
			}
			break;
		}
	}

	public void quantityChanged() {
		assert(uncommittedEntryData.isPurchaseOrSale());
		quantityManuallyEdited = true;
		
		/*
		 * If the share price has been entered then we can calculate the gross amount
		 * and we can fill in any commission and tax deductions for which we
		 * have tables. We can then calculate and fill in the net amount.
		 * 
		 * Note that any amounts that can be calculated but that have been
		 * manually edited by the user will be left alone. If there are no rates
		 * tables for a deduction then leave it blank and calculate the net
		 * amount as though the deduction were zero.
		 */
		if (sharePrice != null) {

//		if (sharePriceManuallyEdited) {
			calculateExpenses();
		}
	}

	public void commissionChanged() {
		assert(uncommittedEntryData.isPurchaseOrSale());
		commissionManuallyEdited = true;
		
		/*
		 * If both the share price and the quantity have been entered then we
		 * can update the gross amount based on the new amount of this expense.
		 */
		if (sharePrice != null && uncommittedEntryData.getPurchaseOrSaleEntry().getAmount() != 0) {
			updateNetAmount();
		}
	}

	public void tax1Changed() {
		assert(uncommittedEntryData.isPurchaseOrSale());
		tax1RatesManuallyEdited = true;
		
		/*
		 * If both the share price and the quantity have been entered then we
		 * can update the gross amount based on the new amount of this expense.
		 */
		if (sharePrice != null && uncommittedEntryData.getPurchaseOrSaleEntry().getAmount() != 0) {
			updateNetAmount();
		}
	}

	public void tax2Changed() {
		assert(uncommittedEntryData.isPurchaseOrSale());
		tax2RatesToBeCalculated = false;
		
		/*
		 * If both the share price and the quantity have been entered then we
		 * can update the gross amount based on the new amount of this expense.
		 */
		if (sharePrice != null && uncommittedEntryData.getPurchaseOrSaleEntry().getAmount() != 0) {
			updateNetAmount();
		}
	}

	public void sharePriceChanged(BigDecimal sharePrice) {
		assert(uncommittedEntryData.isPurchaseOrSale());
		this.sharePrice = sharePrice;
		sharePriceManuallyEdited = true;

		/*
		 * If we know the share quantity then we can calculate the gross amount
		 * and we can fill in any commission and tax deductions for which we
		 * have tables. We can then calculate and fill in the net amount.
		 * 
		 * Note that any amounts that can be calculated but that have been
		 * manually edited by the user will be left alone. If there are no rates
		 * tables for a deduction then leave it blank and calculate the net
		 * amount as though the deduction were zero.
		 */
		long quantity = uncommittedEntryData.getPurchaseOrSaleEntry().getAmount();
		if (quantity != 0) {
			calculateExpenses();
		}
	}

	/**
	 * Given the share price and quantity, this method calculates
	 * any commissions or taxes for which rates tables are available
	 * and then calculates the net amount.
	 */
	private void calculateExpenses() {
		// TODO: Can we clean this up a little?  Stock quantities are to three decimal places,
		// (long value is number of thousandths) hence why we shift the long value three places.
		long quantity = uncommittedEntryData.getPurchaseOrSaleEntry().getAmount();
		BigDecimal grossAmount1 = sharePrice.multiply(BigDecimal.valueOf(quantity).movePointLeft(3));
		long grossAmount = grossAmount1.movePointRight(2).longValue();
		
		StockAccount account = (StockAccount)getUncommittedEntryData().getEntry().getAccount();
		
		RatesTable commissionRates = 
			(uncommittedEntryData.getTransactionType() == TransactionType.Buy)
			? account.getBuyCommissionRates()
					: account.getSellCommissionRates();
		if (account.getCommissionAccount() != null && commissionRates != null && !commissionManuallyEdited) {
			uncommittedEntryData.getCommissionEntry().setAmount(commissionRates.calculateRate(grossAmount));
		}
		
		if (account.getTax1Account() != null && account.getTax1Rates() != null && !tax1RatesManuallyEdited) {
			uncommittedEntryData.getTax1Entry().setAmount(account.getTax1Rates().calculateRate(grossAmount));
		}
		
		if (tax2RatesToBeCalculated) {
			uncommittedEntryData.getTax2Entry().setAmount(account.getTax2Rates().calculateRate(grossAmount));
		}
		
		updateNetAmount();
	}

	private void updateNetAmount() {
		if (!netAmountManuallyEdited) {
			// TODO: Can we clean this up a little?  Stock quantities are to three decimal places,
			// (long value is number of thousanths) hence why we shift the long value three places.
			long lQuantity = uncommittedEntryData.getPurchaseOrSaleEntry().getAmount();
			BigDecimal quantity = BigDecimal.valueOf(lQuantity).movePointLeft(3);
			BigDecimal grossAmount1 = sharePrice.multiply(quantity);
			long amount = grossAmount1.movePointRight(2).longValue();
			
			if (uncommittedEntryData.getCommissionEntry() != null) {
				amount += uncommittedEntryData.getCommissionEntry().getAmount();
			}
			
			if (uncommittedEntryData.getTax1Entry() != null) {
				amount += uncommittedEntryData.getTax1Entry().getAmount();
			}
			
			if (uncommittedEntryData.getTax2Entry() != null) {
				amount += uncommittedEntryData.getTax2Entry().getAmount();
			}

			uncommittedEntryData.getEntry().setAmount(-amount);
		}
	}

	@Override
	protected void specificValidation() throws InvalidUserEntryException {
		// TODO: We should remove this method and call the EntryData method directly.
		uncommittedEntryData.specificValidation();
	}
}