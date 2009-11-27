package net.sf.jmoney.stocks.pages;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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
	 * The share price currently shown to the user, or null if
	 * this is a new entry and no share price has been entered.
	 * <P>
	 * This is saved here (not fetched each time) because we often
	 * update other fields when fields are updated in order to keep
	 * the transaction balanced.  We need to know the previous price,
	 * not the price that would have balanced the transaction, when
	 * we do these calculations.
	 */
	private BigDecimal sharePrice;

	/**
	 * true if this is a new transaction and the user has not manually entered
	 * the withholding tax (any amount shown being a value automatically
	 * calculated from other values in the transaction), false if this was not a
	 * new transaction or the user typed the amount of withholding tax
	 */
	private boolean withholdingTaxIsFluid = true;

	private boolean quantityIsFluid = true;

	private boolean sharePriceIsFluid = true;

	private boolean commissionIsFluid = true;

	private boolean tax1RatesIsFluid = true;

	private boolean tax2RatesIsFluid = true;

	private boolean netAmountIsFluid = true;

	private List<IPropertyChangeListener<BigDecimal>> stockPriceListeners = new ArrayList<IPropertyChangeListener<BigDecimal>>();

	public StockEntryRowControl(final Composite parent, int style, VirtualRowTable rowTable, Block<StockEntryData, ? super StockEntryRowControl> rootBlock, final RowSelectionTracker selectionTracker, final FocusCellTracker focusCellTracker) {
		super(parent, style, rowTable, rootBlock, selectionTracker, focusCellTracker);
		init(this, this, rootBlock);
	}

	@Override
	public void setInput(StockEntryData inputEntryData) {
		if (inputEntryData.getTransactionType() == null) {
			/*
			 * This is a new transaction so start with everything fluid.
			 */
		} else {
			/*
			 * This is an existing transaction.  Entries are fluid only if they are
			 * not applicable for the current transaction type.  So only if the user
			 * changes the transaction type will any values be updated with calculated values.
			 */
			netAmountIsFluid = false;

			switch (inputEntryData.getTransactionType()) {
			case Buy:
			case Sell:
				quantityIsFluid = false;
				sharePriceIsFluid = false;
				commissionIsFluid = false;
				tax1RatesIsFluid = false;
				tax2RatesIsFluid = false;
				break;
			case Dividend:
				withholdingTaxIsFluid = false;
				break;
			case Transfer:
				break;
			case Other:
				break;
			}
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

		/*
		 * This must be called after we have set our own stuff up.  The reason
		 * being that this call loads controls (such as the stock price control).
		 * These controls will not load correctly if this object is not set up.
		 */
		super.setInput(inputEntryData);
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
		netAmountIsFluid = false;

		StockAccount account = (StockAccount)getUncommittedEntryData().getEntry().getAccount();

		Entry entry = uncommittedEntryData.getEntry();

		TransactionType transactionType = uncommittedEntryData.getTransactionType();
		if (transactionType == null) {
			/*
			 * The user has not yet entered enough information into the transaction
			 * to guess the transaction type.  In particular, the user has not selected
			 * the transaction type.
			 * 
			 * We have already set the net amount to be no longer fluid.  There is nothing to do in this case.
			 */
		} else {
			switch (transactionType) {
			case Buy:
			case Sell:
				/*
				 * The user would not usually enter the net amount for the
				 * transaction because it is hard to calculate backwards from this.
				 * The rates tables are all based on the gross amount. Also, there
				 * may be a number of calculated commissions and taxes and we would
				 * not know which to adjust. Therefore in most cases we leave the
				 * transaction unbalanced and force the user to correct it when the
				 * transaction is saved.
				 * 
				 * However, if there are no commission or taxes configured for the
				 * commodity type in this account then we can calculate backwards.
				 */
				if (account.getCommissionAccount() == null
						&& account.getTax1Account() == null
						&& account.getTax2Account() == null) {

					if (quantityIsFluid && !sharePriceIsFluid) {
						BigDecimal grossAmount = new BigDecimal(uncommittedEntryData.getEntry().getAmount()).movePointLeft(2);
						BigDecimal quantity = grossAmount.divide(sharePrice);
						uncommittedEntryData.getPurchaseOrSaleEntry().setAmount(quantity.movePointRight(3).longValue());
					}

					if (sharePriceIsFluid && !quantityIsFluid) {
						BigDecimal grossAmount = new BigDecimal(uncommittedEntryData.getEntry().getAmount()).movePointLeft(2);
						BigDecimal quantity = new BigDecimal(uncommittedEntryData.getPurchaseOrSaleEntry().getAmount());
						sharePrice = grossAmount.divide(quantity);
					}
				}
				break;
			case Dividend:
				if (withholdingTaxIsFluid) {
					long rate = 30L;
					long tax = entry.getAmount() * rate / (100 - rate);
					uncommittedEntryData.setWithholdingTax(-tax);
				}
				long dividend = entry.getAmount() + uncommittedEntryData.getWithholdingTax();
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
					Commodity commodity1 = entry.getCommodityInternal();
					Commodity commodity2 = otherEntry.getCommodityInternal();
					if (commodity1 == null || commodity2 == null || commodity1.equals(commodity2)) {
						otherEntry.setAmount(-entry.getAmount());
					}
				}
				break;
			}
		}
	}

	public void quantityChanged() {
		assert(uncommittedEntryData.isPurchaseOrSale());
		quantityIsFluid = false;

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
		commissionIsFluid = false;

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
		tax1RatesIsFluid = false;

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
		tax2RatesIsFluid = false;

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
		sharePriceIsFluid = false;

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
			if (account.getCommissionAccount() != null && commissionRates != null && commissionIsFluid) {
				uncommittedEntryData.setCommission(commissionRates.calculateRate(grossAmount));
			}

			if (account.getTax1Account() != null && account.getTax1Rates() != null && tax1RatesIsFluid) {
				uncommittedEntryData.setTax1Amount(account.getTax1Rates().calculateRate(grossAmount));
			}

			if (account.getTax2Account() != null && account.getTax2Rates() != null && tax2RatesIsFluid) {
				uncommittedEntryData.setTax2Amount(account.getTax2Rates().calculateRate(grossAmount));
			}

			updateNetAmount();
	}

	private void updateNetAmount() {
		if (netAmountIsFluid) {
			// TODO: Can we clean this up a little?  Stock quantities are to three decimal places,
			// (long value is number of thousanths) hence why we shift the long value three places.
			long lQuantity = uncommittedEntryData.getPurchaseOrSaleEntry().getAmount();
			BigDecimal quantity = BigDecimal.valueOf(lQuantity).movePointLeft(3);
			BigDecimal grossAmount1 = sharePrice.multiply(quantity);
			long amount = grossAmount1.movePointRight(2).longValue();

			amount += uncommittedEntryData.getCommission();
			amount += uncommittedEntryData.getTax1Amount();
			amount += uncommittedEntryData.getTax2Amount();

			uncommittedEntryData.getEntry().setAmount(-amount);
		}
	}

	@Override
	protected void specificValidation() throws InvalidUserEntryException {
		// TODO: We should remove this method and call the EntryData method directly.
		uncommittedEntryData.specificValidation();
	}

	public BigDecimal getSharePrice() {
		return sharePrice;
	}

	public void setSharePrice(BigDecimal sharePrice) {
		if (!sharePrice.equals(this.sharePrice)) {
			this.sharePrice = sharePrice;
			for (IPropertyChangeListener<BigDecimal> listener : stockPriceListeners) {
				listener.propertyChanged(sharePrice);
			}
		}
	}

	public void addStockPriceChangeListener(
			IPropertyChangeListener<BigDecimal> listener) {
		stockPriceListeners.add(listener);

	}
}