/*
 * 
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2004,2009 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.paypal;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import net.sf.jmoney.importer.wizards.AssociationMetadata;
import net.sf.jmoney.importer.wizards.CsvImportToAccountWizard;
import net.sf.jmoney.importer.wizards.ImportException;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.IncomeExpenseAccount;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.Session.NoAccountFoundException;
import net.sf.jmoney.model2.Session.SeveralAccountsFoundException;
import net.sf.jmoney.model2.Transaction;
import net.sf.jmoney.reconciliation.ReconciliationEntryInfo;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

/**
 * A wizard to import data from a comma-separated file that has been downloaded
 * from Paypal.
 */
public class PaypalImportWizard extends CsvImportToAccountWizard {

	// TODO check format of all amount columns
	// TODO check column names
	private ImportedDateColumn   column_date                = new ImportedDateColumn("Date", new SimpleDateFormat("M/d/yyyy"));
	private ImportedTextColumn   column_payeeName           = new ImportedTextColumn("Name");
	private ImportedTextColumn   column_type                = new ImportedTextColumn("Type");
	private ImportedTextColumn   column_status              = new ImportedTextColumn("Status");
	private ImportedTextColumn   column_currency            = new ImportedTextColumn("Currency");
	private ImportedAmountColumn column_grossAmount         = new ImportedAmountColumn("Gross");
	private ImportedAmountColumn column_fee                 = new ImportedAmountColumn("Fee");
	private ImportedAmountColumn column_netAmount           = new ImportedAmountColumn("Net");
	private ImportedTextColumn   column_payerEmail          = new ImportedTextColumn("From Email Address");
	private ImportedTextColumn   column_payeeEmail          = new ImportedTextColumn("To Email Address");
	private ImportedTextColumn   column_transactionId       = new ImportedTextColumn("Transaction ID");
	private ImportedTextColumn   column_memo                = new ImportedTextColumn("Item Title");
	private ImportedAmountColumn column_shippingAndHandling = new ImportedAmountColumn("Shipping and Handling Amount");
	private ImportedAmountColumn column_insurance           = new ImportedAmountColumn("Insurance Amount");
	private ImportedAmountColumn column_salesTax            = new ImportedAmountColumn("Sales Tax");
	private ImportedTextColumn   column_itemUrl             = new ImportedTextColumn("Item URL");
	private ImportedTextColumn   column_quantity            = new ImportedTextColumn("Quantity");
	private ImportedAmountColumn column_balance             = new ImportedAmountColumn("Balance");
	
	/**
	 * Account inside transaction
	 */
	private PaypalAccount paypalAccount;


	Collection<RefundRow> refunds = new ArrayList<RefundRow>();
	Collection<ReversalRow> reversals = new ArrayList<ReversalRow>();

	/**
	 * Currency of the Paypal account, being the currency in which
	 * amounts are to be formatted when put in the memo
	 */
	Currency currency;

	private MultiRowTransaction currentMultiRowProcessor = null;





	// These are Ameritrade fields and should probably be removed.
	private long priorIntraAccountTransferAmount = 0;
	private Account interestAccount;
	private Account expensesAccount;

	// Don't put this here.  Methods should all be using sessionInTransaction
	private Session session;

	private static DateFormat maturityDateFormat = new SimpleDateFormat("MM/dd/yyyy"); 

	public PaypalImportWizard() {
		// TODO check these dialog settings are used by the base class
		// so the default filename location is separate for each import type.
		IDialogSettings workbenchSettings = Activator.getDefault().getDialogSettings();
		IDialogSettings section = workbenchSettings.getSection("AmeritradeImportWizard");//$NON-NLS-1$
		if (section == null) {
			section = workbenchSettings.addNewSection("AmeritradeImportWizard");//$NON-NLS-1$
		}
		setDialogSettings(section);
	}


	@Override
	protected void setAccount(Account accountInsideTransaction)	throws ImportException {
		if (!(accountInsideTransaction instanceof PaypalAccount)) {
			throw new ImportException("Bad configuration: This import can be used for Paypal accounts only.");
		}

		this.paypalAccount = (PaypalAccount)accountInsideTransaction;
		this.session = accountInsideTransaction.getSession();

		currency = paypalAccount.getCurrency();

		currentMultiRowProcessor = null;

		// The following is Ameritrade stuff
		priorIntraAccountTransferAmount = 0;

		try {
			interestAccount = session.getAccountByShortName("Interest - Ameritrade");
		} catch (NoAccountFoundException e) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Account not Set Up", "No account exists called 'Interest - Ameritrade'");
			throw new RuntimeException(e); 
		} catch (SeveralAccountsFoundException e) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Multiple Accounts Set Up", "Multiple accounts exists called 'Interest - Ameritrade'");
			throw new RuntimeException(e); 
		}

//		try {
//			expensesAccount = session.getAccountByShortName("Stock - Expenses (US)");
//		} catch (NoAccountFoundException e) {
//			MessageDialog.openError(Display.getDefault().getActiveShell(), "Account not Set Up", "No account exists called 'Stock - Expenses (US)'");
//			throw new RuntimeException(e); 
//		} catch (SeveralAccountsFoundException e) {
//			MessageDialog.openError(Display.getDefault().getActiveShell(), "Multiple Accounts Set Up", "Multiple accounts exists called 'Stock - Expenses (US)'");
//			throw new RuntimeException(e); 
//		}
	}

	@Override
	public void importLine(String[] linex) throws ImportException {
		String rowType = column_type.getText();

		/*
		 * We are not interested in our own e-mail.  The merchant
		 * e-mail may be either in the 'from' or the 'to' e-mail address
		 * column, depending on the row type.
		 */
		String rowMerchantEmail;
		if (rowType.equals("Refund")
				|| rowType.equals("Reversal")
				|| rowType.equals("Payment Received")
				|| rowType.equals("eBay Payment Received")) {
			rowMerchantEmail = column_payerEmail.getText();
		} else {
			rowMerchantEmail = column_payeeEmail.getText();
		}

		boolean processed = false;
		if (currentMultiRowProcessor != null) {
			processed = currentMultiRowProcessor.processCurrentRow(session, paypalAccount);
			if (currentMultiRowProcessor.isDone()) {
				currentMultiRowProcessor.createTransaction(session, paypalAccount);
				currentMultiRowProcessor = null;
			}
		}

		if (!processed) {
			/*
			 * If a row is not processed in a multi-row processor then the processor must be done.
			 * Multi-row processors can be used to combine consecutive rows only.
			 */
			if (currentMultiRowProcessor != null) {
				throw new RuntimeException("something is wrong");
			}

			if (rowType.equals("Shopping Cart Payment Sent")) {
				/**
				 * Shopping cart entries are split across multiple rows, with a 'Payment Sent' row
				 * following by one or more 'Item' rows.  These must be combined into a single
				 * transaction.  To enable us to do this, this class is used to put each row into,
				 * and it can then output the transaction when a row is found that is in the
				 * next transaction.
				 */

				currentMultiRowProcessor = new ShoppingCartPaymentSent(column_date.getDate(), column_shippingAndHandling.getAmount());
			} else if (rowType.equals("Shopping Cart Item")) {
				throw new ImportException("'Shopping Cart Item' row found but it is not preceeded by a 'Shopping Cart Payment Sent' or 'eBay Payment Sent' row.");
			} else if (rowType.equals("Refund")) {
				/*
				 * Refunds are combined with the original transaction.
				 * 
				 * Because the input file is in reverse date order, we find
				 * the refund first. We save the refund information in a
				 * collection. Whenever a 'Shopping Cart Payment Sent' or a
				 * 'eBayPaymentSent' or 'Express Checkout Payment Sent' is
				 * found with a status of 'Partially Refunded' or 'Refunded'
				 * and the payee name exactly matches then we add the refund
				 * as another pair of split entries in the same transaction.
				 */
				refunds.add(new RefundRow());
			} else if (rowType.equals("Reversal")) {
				/*
				 * Reversals are processed in a similar way to refunds.  We keep
				 * and list and match them to later entries.
				 */
				reversals.add(new ReversalRow());
			} else if (rowType.equals("eBay Payment Sent")
					|| rowType.equals("eBay Payment Received")
					|| rowType.equals("Payment Received")
					|| rowType.equals("Payment Sent")
					|| rowType.equals("Web Accept Payment Sent")) {

				if (column_status.getText().equals("Refunded")) {
					/*
					 * Find the refund entry.  We create a single transaction with two entries both
					 * in this Paypal account. 
					 */
					RefundRow match = null;
					for (RefundRow refund : refunds) {
						if (refund.payeeName.equals(column_payeeName.getText())
								&& refund.grossAmount == -column_grossAmount.getAmount()) {
							match = refund;
							break;
						}
					}
					if (match == null) {
						throw new ImportException("An entry was found that says it was refunded, but no matching 'Refund' entry was found.");
					}
					refunds.remove(match);

					createRefundTransaction(match);
				} else if (column_status.getText().equals("Reversed")) {
					/*
					 * Find the reversal entry.  We don't create anything if an
					 * entry was reversed. 
					 */
					ReversalRow match = null;
					for (ReversalRow reversal : reversals) {
						if (reversal.payeeName.equals(column_payeeName.getText())
								&& reversal.grossAmount == -column_grossAmount.getAmount()) {
							match = reversal;
							break;
						}
					}
					if (match == null) {
						throw new ImportException("An entry was found that says it was reversed, but no matching 'Reversal' entry was found.");
					}
					reversals.remove(match);
				} else if (rowType.equals("eBay Payment Sent")) {
						/*
						 * Rows have been found where there is a row of type 'eBay Payment Sent' followed
						 * by a row of type 'Shopping Cart Item'.  This seems very strange because 'Shopping
						 * Cart Item' rows normally follow a 'Shopping Cart Payment Sent' row.  Furthermore,
						 * the 'Shopping Cart Item' row does not have any data in it of any use except for
						 * a quantity (in known cases always 1, but presumably this would be some other number
						 * if the quantity were not 1).  The 'eBay Payment Sent' row does not have a quantity
						 * so we use the quantity from the 'Shopping Cart Item' row.
						 * 
						 * Also, ebay Payment Sent may be followed by a currency exchange.
						 */
						currentMultiRowProcessor = new EbayPaymentSent();
				} else {
					Transaction trans = session.createTransaction();
					trans.setDate(column_date.getDate());

					PaypalEntry mainEntry = trans.createEntry().getExtension(PaypalEntryInfo.getPropertySet(), true);
					mainEntry.setAccount(paypalAccount);
					mainEntry.setAmount(column_netAmount.getAmount());
					mainEntry.setMemo("payment - " + column_payeeName.getText());
					mainEntry.setValuta(column_date.getDate());
					mainEntry.setMerchantEmail(rowMerchantEmail);
					mainEntry.setPropertyValue(ReconciliationEntryInfo.getUniqueIdAccessor(), column_transactionId.getText());

					/**
					 * The memo, being set initially to the memo from the input file but may
					 * be modified
					 */
					String rowMemo = column_memo.getText();
					long rowNetAmount = column_netAmount.getAmount();
					long rowGrossAmount = column_grossAmount.getAmount();
					long rowFee = column_fee.getAmount();
					
					if (column_status.getText().equals("Partially Refunded")) {
						/*
						 * Look for a refunds that match.  Put them in this transaction.
						 * If the transaction is not itemized then we reduce the expense entry
						 * by the amount of the refund.  If the transaction is itemized then we
						 * create a separate entry for the total amount refunded.
						 * 
						 * (Though currently we have no cases of itemized transactions here so this
						 * is not supported.  We probably need to merge this with "Shopping Cart Payment Sent"
						 * processing).
						 */

						long refundAmount = 0;

						for (Iterator<RefundRow> iter = refunds.iterator(); iter.hasNext(); ) {
							RefundRow refund = iter.next();
							if (refund.payeeName.equals(column_payeeName.getText())) {
								/*
								 * Create the refund entry in the Paypal account
								 */
								PaypalEntry refundEntry = trans.createEntry().getExtension(PaypalEntryInfo.getPropertySet(), true);
								refundEntry.setAccount(paypalAccount);
								refundEntry.setAmount(refund.grossAmount);
								refundEntry.setMemo("refund - " + refund.payeeName);
								refundEntry.setValuta(refund.date);
								refundEntry.setMerchantEmail(refund.merchantEmail);
								refundEntry.setPropertyValue(ReconciliationEntryInfo.getUniqueIdAccessor(), refund.transactionId);

								refundAmount += refund.grossAmount;

								iter.remove();
							}
						}

						if (-rowNetAmount - refundAmount == column_shippingAndHandling.getAmount()) {
							// All was refunded except s&h, so indicate accordingly in the memo
							rowMemo = rowMemo + " (s&h not refunded after return)";
						} else {
							// Indicate the original amount paid and refund amount in the memo 
							rowMemo = rowMemo + " ($" + currency.format(-rowNetAmount) + " less $" + currency.format(refundAmount) + " refunded)";
						}

						// Note that the amounts in the row will be negative, which is
						// why we add the refund amount when it may seem we should deduct
						// the refund amount.
						rowNetAmount += refundAmount;
						rowGrossAmount += refundAmount;
					}

					if (rowFee != 0) {
						// For non-sale transfers, treat the Paypal fee as a bank service
						// charge.  For E-bay sales, absorb in the price or proceeds.

						if (rowType.equals("Payment Received")
								|| rowType.equals("Payment Sent")) {
							if (paypalAccount.getPaypalFeesAccount() == null) {
								throw new ImportException("A Paypal fee has been found in the imported data.  However, no category has been configured in the properties for this Paypal account for such fees.");
							}

							// Note that fee shows up as a negative amount, and we want
							// a positive amount in the category account to be used for the fee.
							Entry feeEntry = trans.createEntry();
							feeEntry.setAccount(paypalAccount.getPaypalFeesAccount());
							feeEntry.setAmount(-column_fee.getAmount());
							feeEntry.setMemo("Paypal");
							// Set fee to zero so it does not appear in the memo
							rowFee = 0L;
							rowNetAmount = rowGrossAmount;
						}
					}

					if (rowMemo.length() == 0) {
						// Certain transactions don't have memos, so we fill one in
						if (rowType.equals("Payment Received")) {
							rowMemo = column_payeeName.getText() + " - gross payment";
						}
						if (rowType.equals("Payment Sent")) {
							rowMemo = column_payeeName.getText() + " - payment";
						}
					}
					createCategoryEntry(trans, rowMemo, rowGrossAmount, rowNetAmount, column_shippingAndHandling.getAmount(), column_insurance.getAmount(), column_salesTax.getAmount(), rowFee, column_itemUrl.getText(), paypalAccount.getSaleAndPurchaseAccount());

					assertValid(trans);
				}
			} else if (rowType.equals("Donation Sent")) {
				if (paypalAccount.getDonationAccount() == null) {
					throw new ImportException("A donation has been found in the imported data.  However, no category was set for donations.  Please go to the Paypal account properties and select a category to be used for donations.");
				}

				// Donations do not have memos set, so the payee name is used as the memo in the
				// expense category entry.
				createTransaction("donation sent", paypalAccount.getDonationAccount(), column_payeeName.getText());
			} else if (rowType.equals("Add Funds from a Bank Account")) {
				if (paypalAccount.getTransferBank() == null) {
					throw new ImportException("A bank account transfer has been found in the imported data.  However, no bank account has been set in the properties for this Paypal account.");
				}
				createTransaction("transfer from bank", paypalAccount.getTransferBank(), "transfer to Paypal");
			} else if (rowType.equals("Update to eCheck Sent")) {
				// Updates do not involve a financial transaction
				// so nothing to import.
			} else if (rowType.equals("Update to eCheck Received")) {
				// Updates do not involve a financial transaction
				// so nothing to import.
			} else if (rowType.equals("Update to Payment Received")) {
				// Updates do not involve a financial transaction
				// so nothing to import.
			} else if (rowType.equals("eCheck Sent")) {
				if (paypalAccount.getSaleAndPurchaseAccount() == null) {
					throw new ImportException("An eCheck entry has been found in the imported data.  However, no sale and purchase account has been set in the properties for this Paypal account.");
				}
				createTransaction("payment by transfer", paypalAccount.getSaleAndPurchaseAccount(), "transfer from Paypal");
			} else if (rowType.equals("Express Checkout Payment Sent")) {
				if (paypalAccount.getSaleAndPurchaseAccount() == null) {
					throw new ImportException("An 'Express Checkout' entry has been found in the imported data.  However, no sale and purchase account has been set in the properties for this Paypal account.");
				}

				if (column_status.getText().equals("Refunded")) {
					/*
					 * Find the refund entry.  We create a single transaction with two entries both
					 * in this Paypal account. 
					 */
					RefundRow match = null;
					for (RefundRow refund : refunds) {
						if (refund.payeeName.equals(column_payeeName.getText())
								&& refund.grossAmount == -column_grossAmount.getAmount()) {
							match = refund;
							break;
						}
					}
					if (match == null) {
						throw new ImportException("An entry was found that says it was refunded, but no matching 'Refund' entry was found.");
					}
					refunds.remove(match);

					createRefundTransaction(match);
				} else {
					createTransaction(column_payeeName.getText(), paypalAccount.getSaleAndPurchaseAccount(), column_payeeName.getText() + " - Paypal payment");
				}
			} else if (rowType.equals("Charge From Credit Card")) {
				if (paypalAccount.getTransferCreditCard() == null) {
					throw new ImportException("A credit card charge has been found in the imported data.  However, no credit card account has been set in the properties for this Paypal account.");
				}
				createTransaction("payment from credit card", paypalAccount.getTransferCreditCard(), "transfer to Paypal");
			} else if (rowType.equals("Credit to Credit Card")) {
				if (paypalAccount.getTransferCreditCard() == null) {
					throw new ImportException("A credit card refund has been found in the imported data.  However, no credit card account has been set in the properties for this Paypal account.");
				}
				createTransaction("refund to credit card", paypalAccount.getTransferCreditCard(), "refund from Paypal");
			} else {
				//        	throw new UnexpectedData("type", type);
				MessageDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Unable to read CSV file", "Entry found with unknown type: '" + rowType + "'.");
			}
		} // if !processed
	}

	private void assertValid(Transaction trans) {
		long total = 0;
		for (Entry entry : trans.getEntryCollection()) {
			total += entry.getAmount();
		}
		if (total != 0) {
			System.out.println("unbalanced");
		}
		assert(total == 0);
	}

	/**
	 * The gross and net amounts differ only by the fee.  This method will
	 * absorb the fee into the proceeds (i.e. the amount shown in the accounts
	 * that the item was sold for will be reduced by the fee).  If this is not
	 * an item sale but a funds transfer then the fee is not absorbed.  It is
	 * accounted for as a separate split entry in the transaction.  In that case
	 * the caller will have zeroed out the fee and set the net amount to be the same
	 * as the gross amount.
	 * 
	 * @param trans
	 * @param rowItem
	 * @param account
	 * @throws ImportException 
	 */
	private void createCategoryEntry(Transaction trans, String memo, long grossAmount, long netAmount, long shippingAndHandling, Long insurance, Long salesTax, long fee, String url, IncomeExpenseAccount account) {
		PaypalEntry lineItemEntry = trans.createEntry().getExtension(PaypalEntryInfo.getPropertySet(), true);
		// TODO: Support pattern matching
		lineItemEntry.setAccount(account);

		/*
		 * Shopping cart items have positive amounts in the 'gross amount' field
		 * only, others have negative amounts that are in both the 'gross
		 * amount' and the 'net amount' fields. We want to set a positive amount
		 * in the category. (Though signs may be opposite if a refund or
		 * something).
		 */ 
		if (column_type.getText().equals("Shopping Cart Item")) {
			lineItemEntry.setAmount(grossAmount);
		} else {
			lineItemEntry.setAmount(-netAmount);
		}

		if (column_itemUrl.getText().length() != 0) {
			try {
				URL itemUrl = new URL(column_itemUrl.getText());
				lineItemEntry.setItemUrl(itemUrl);
			} catch (MalformedURLException e) {
				// Leave the URL blank
			}
		}

		StringBuffer adjustmentsBuffer = new StringBuffer();

		Currency currency = paypalAccount.getCurrency();
		String separator = "";
		long baseAmount = lineItemEntry.getAmount();
		String ourMemo = memo;

		if (column_quantity.getText().length() != 0 
				&& !column_quantity.getText().equals("0")
				&& !column_quantity.getText().equals("1")) {
			ourMemo = ourMemo + " x" + column_quantity.getText();
		}

		if (shippingAndHandling != 0) {
			adjustmentsBuffer.append("s&h $")
			.append(currency.format(shippingAndHandling))
			.append(separator);
			separator = ", ";
			baseAmount -= shippingAndHandling;
		}
		if (insurance != null) {
			adjustmentsBuffer.append("insurance $")
			.append(currency.format(insurance))
			.append(separator);
			separator = ", ";
			baseAmount -= insurance;
		}
		if (salesTax != null) {
			adjustmentsBuffer.append("tax $")
			.append(currency.format(salesTax))
			.append(separator);
			separator = ", ";
			baseAmount -= salesTax;
		}
		if (fee != 0) {
			adjustmentsBuffer.append("less Paypal fee $")
			.append(currency.format(fee))
			.append(separator);
			separator = ", ";
			baseAmount -= fee;
		}

		if (adjustmentsBuffer.length() == 0) {
			lineItemEntry.setMemo(ourMemo);
		} else {
			lineItemEntry.setMemo(ourMemo + " ($" + currency.format(baseAmount) + " + " + adjustmentsBuffer.toString() + ")");
		}
	}

	/**
	 * We distribute the shipping and handling among the items in proportion
	 * to the price of each item.  This is the preference of the author.
	 * If this is not your preference then please add a preference to the preferences
	 * to indicate if a separate line item should instead be created for the
	 * shipping and handling and implement it.
	 * @throws ImportException 
	 */
	private void distribute(long toDistribute, List<ShoppingCartRow> rowItems) throws ImportException {
		long netTotal = 0;
		for (ShoppingCartRow rowItem : rowItems) {
			if (rowItem.grossAmount <= 0) {
				throw new ImportException("Shopping Cart Item with zero or negative gross amount");
			}
			netTotal += rowItem.grossAmount;
		}

		long leftToDistribute = toDistribute;

		for (ShoppingCartRow rowItem : rowItems) {
			long amount = toDistribute * rowItem.grossAmount / netTotal;
			rowItem.shippingAndHandling = amount;
			leftToDistribute -= amount;
		}

		// We have rounded down, so we may be under.  We now distribute
		// a penny to each to get a balanced transaction.
		for (ShoppingCartRow rowItem : rowItems) {
			if (leftToDistribute > 0) {
				rowItem.shippingAndHandling++;
				leftToDistribute--;
			}
		}

		assert(leftToDistribute == 0);

		/*
		 * normally both the gross and net amounts have the s&h included. The
		 * itemized rows don't, and they have just the amount as a positive
		 * value in the 'gross amount' field (nothing in the 'net amount' field)
		 * so to make it consistent we adjust these amounts (which are positive
		 * amounts for normal sales) by the s&h amount.
		 */
		for (ShoppingCartRow rowItem : rowItems) {
			rowItem.grossAmount += rowItem.shippingAndHandling;
		}		
	}

	private void createTransaction(String paypalAccountMemo, Account otherAccount, String otherAccountMemo) throws ImportException {
		Transaction trans = session.createTransaction();
		trans.setDate(column_date.getDate());

		PaypalEntry mainEntry = trans.createEntry().getExtension(PaypalEntryInfo.getPropertySet(), true);
		mainEntry.setAccount(paypalAccount);
		mainEntry.setAmount(column_grossAmount.getAmount());
		mainEntry.setMemo(paypalAccountMemo);
		mainEntry.setValuta(column_date.getDate());
		mainEntry.setPropertyValue(ReconciliationEntryInfo.getUniqueIdAccessor(), column_transactionId.getText());

		Entry otherEntry = trans.createEntry();
		otherEntry.setAccount(otherAccount);
		otherEntry.setAmount(-column_grossAmount.getAmount());
		otherEntry.setMemo(otherAccountMemo);
	}

	/**
	 * This is a helper method that creates a transaction where there are just two entries
	 * and both are in the Paypal account.  This occurs when an entry is refunded in full.
	 * <P>
	 * The original row is always the current row.  The refunded row is passed in.
	 * @throws ImportException 
	 */
	private void createRefundTransaction(RefundRow refundRow) throws ImportException {
		Transaction trans = session.createTransaction();
		trans.setDate(column_date.getDate());

		PaypalEntry mainEntry = trans.createEntry().getExtension(PaypalEntryInfo.getPropertySet(), true);
		mainEntry.setAccount(paypalAccount);
		mainEntry.setAmount(column_grossAmount.getAmount());
		mainEntry.setMemo(column_payeeName.getText());
		mainEntry.setValuta(column_date.getDate());
		mainEntry.setPropertyValue(ReconciliationEntryInfo.getUniqueIdAccessor(), column_transactionId.getText());

		Entry refundEntry = trans.createEntry();
		refundEntry.setAccount(paypalAccount);
		refundEntry.setAmount(-column_grossAmount.getAmount());
		refundEntry.setMemo("refund - " + column_payeeName.getText());
		refundEntry.setValuta(refundRow.date);
		refundEntry.setPropertyValue(ReconciliationEntryInfo.getUniqueIdAccessor(), refundRow.transactionId);
	}

	public interface MultiRowTransaction {

		/**
		 * 
		 * @return true if this row was processed, false if this row is not a
		 * 		part of this transaction and should be separately processed
		 * 		by the caller
		 * @throws ImportException 
		 */
		boolean processCurrentRow(Session session, PaypalAccount account) throws ImportException;

		/**
		 * 
		 * @return true if this transaction has received all its row and is
		 * 		ready to be created in the datastore, false if there may be
		 * 		more rows in this transaction
		 */
		boolean isDone();

		void createTransaction(Session session, PaypalAccount account) throws ImportException;
	}

	public class ShoppingCartPaymentSent implements MultiRowTransaction {

		private Date date;
		private String merchantEmail;
		private long shippingAndHandlingAmount;
		private List<ShoppingCartRow> rowItems = new ArrayList<ShoppingCartRow>();


		private boolean done = false;

		/**
		 * Initial constructor called when first "Mandatory Exchange" row found.
		 * 
		 * @param date
		 * @param quantity
		 * @param stock
		 */
		public ShoppingCartPaymentSent(Date date, long shippingAndHandlingAmount) {
			this.date = date;
			this.merchantEmail = column_payeeEmail.getText();
			this.shippingAndHandlingAmount = shippingAndHandlingAmount;
		}

		public void createTransaction(Session session, PaypalAccount account) throws ImportException {
			// Distribute the shipping and handling amount
			distribute(shippingAndHandlingAmount, rowItems);
			//        	long [] amounts = distribute(row.shippingAndHandlingAmount, rowItems);
			//        	for (int i = 0; i < rowItems.size(); i++) {
			//        		rowItems.get(i).shippingAndHandlingAmount = amounts[i];
			//        	}

			// Start a new transaction
			Transaction trans = session.createTransaction();
			trans.setDate(date);

			PaypalEntry mainEntry = trans.createEntry().getExtension(PaypalEntryInfo.getPropertySet(), true);
			mainEntry.setAccount(paypalAccount);
			mainEntry.setAmount(column_grossAmount.getAmount());
			mainEntry.setMemo("payment - " + column_payeeName.getText());
			mainEntry.setMerchantEmail(merchantEmail);
			mainEntry.setPropertyValue(ReconciliationEntryInfo.getUniqueIdAccessor(), column_transactionId.getText());

			for (ShoppingCartRow rowItem2 : rowItems) {
				createCategoryEntry(trans, rowItem2.memo, rowItem2.grossAmount, rowItem2.netAmount, rowItem2.shippingAndHandling, rowItem2.insurance, rowItem2.salesTax, rowItem2.fee, rowItem2.url, paypalAccount.getSaleAndPurchaseAccount());
			}

			/*
			 * Look for a refunds that match.  Move them into the cart so they can
			 * be processed as part of the same transaction.
			 */
			if (column_status.getText().equals("Partially Refunded")) {
				long refundAmount = 0;
				for (Iterator<RefundRow> iter = refunds.iterator(); iter.hasNext(); ) {
					RefundRow refund = iter.next();
					if (refund.payeeName.equals(column_payeeName.getText())) {
						/*
						 * Create the refund entry in the Paypal account
						 */
						PaypalEntry refundEntry = trans.createEntry().getExtension(PaypalEntryInfo.getPropertySet(), true);
						refundEntry.setAccount(paypalAccount);
						refundEntry.setAmount(refund.grossAmount);
						refundEntry.setMemo("refund - " + refund.payeeName);
						refundEntry.setValuta(refund.date);
						refundEntry.setMerchantEmail(refund.merchantEmail);
						refundEntry.setPropertyValue(ReconciliationEntryInfo.getUniqueIdAccessor(), refund.transactionId);

						refundAmount += refund.grossAmount;

						iter.remove();
					}
				}

				// Create a single income entry with the total amount refunded
				PaypalEntry lineItemEntry = trans.createEntry().getExtension(PaypalEntryInfo.getPropertySet(), true);
				lineItemEntry.setAccount(paypalAccount.getSaleAndPurchaseAccount());
				lineItemEntry.setAmount(-refundAmount);
				lineItemEntry.setMemo(column_payeeName.getText() + " - amount refunded");
			}
			assertValid(trans);
		}

		public boolean processCurrentRow(Session session, PaypalAccount account) throws ImportException {

			String rowItemType = column_type.getText();
			long itemShippingAndHandlingAmount = column_shippingAndHandling.getAmount();
			
			if (rowItemType.equals("Shopping Cart Item")) {
				if (itemShippingAndHandlingAmount != shippingAndHandlingAmount) {
					throw new ImportException("shipping and handling amounts in different rows in the same transaction do not match.");
				}

				ShoppingCartRow shoppingCartRow = new ShoppingCartRow();
				shoppingCartRow.memo = column_memo.getText();
				shoppingCartRow.grossAmount = column_grossAmount.getAmount();
				shoppingCartRow.netAmount = column_netAmount.getAmount();
				shoppingCartRow.insurance = column_insurance.getAmount();
				shoppingCartRow.salesTax = column_salesTax.getAmount();
				shoppingCartRow.fee = column_fee.getAmount();
				shoppingCartRow.url = column_itemUrl.getText();
				rowItems.add(shoppingCartRow);
				return true;
			}

			return false;
		}

		public boolean isDone() {
			return done;
		}
	}

	/**
	 * Rows have been found where there is a row of type 'eBay Payment Sent' followed
	 * by a row of type 'Shopping Cart Item'.  This seems very strange because 'Shopping
	 * Cart Item' rows normally follow a 'Shopping Cart Payment Sent' row.  Furthermore,
	 * the 'Shopping Cart Item' row does not have any data in it of any use except for
	 * a quantity (in known cases always 1, but presumably this would be some other number
	 * if the quantity were not 1).  The 'eBay Payment Sent' row does not have a quantity
	 * so we use the quantity from the 'Shopping Cart Item' row.
	 * 
	 * Note: Paypal may have fixed this.  No occurances of this have been seen for a while.
	 */
	public class EbayPaymentSent implements MultiRowTransaction {

		// The following fields are saved from the first row
		// (the 'Ebay Payment Set' row).
		
		private Date date;
		private String transactionId;
		private String memo;
		private long grossAmount;
		private long netAmount;
		private long fee;
		private String merchantEmail = column_payeeEmail.getText();
		private String quantityString;
		private long shippingAndHandling;


		private boolean done = false;
		private Long insurance;
		private Long salesTax;
		private String url;
		
		/**
		 * Currently only GBP is valid here.
		 */
		private String toCurrency;
		
		private long fromAmount;

		/**
		 * Initial constructor called when first "Ebay Payment Sent" row found.
		 * 
		 * @param date
		 * @param quantity
		 * @param stock
		 * @throws ImportException 
		 */
		public EbayPaymentSent() throws ImportException {
			this.date = column_date.getDate();
			this.transactionId = column_transactionId.getText();
			this.memo = column_memo.getText();
			
			this.toCurrency = column_currency.getText();
			
			this.grossAmount = column_grossAmount.getAmount();
			this.netAmount = column_netAmount.getAmount();
			this.shippingAndHandling = column_shippingAndHandling.getAmount() == null ? 0 : column_shippingAndHandling.getAmount();
			this.insurance = column_insurance.getAmount();
			this.salesTax = column_salesTax.getAmount();
			this.fee = column_fee.getAmount();
			this.url = column_itemUrl.getText();
		}

		public boolean processCurrentRow(Session session, PaypalAccount account) throws ImportException {
			done = true;

			if (column_type.getText().equals("Shopping Cart Item")) {
				quantityString = column_quantity.getText();
				return true;

			} else if (column_type.getText().equals("Currency Conversion")) {
				long amount = column_grossAmount.getAmount();
				String currency = column_currency.getText();

				if (amount > 0) {
					/* This is the 'to' currency.
					 * Both the currency and the amount must match the values in
					 * the preceding 'eBay Payment Sent' row.
					 */
					if (amount != grossAmount) {
						throw new ImportException("bad currency conversion");
					}
					if (!toCurrency.equals(currency)) {
						throw new ImportException("bad currency conversion");
					}
				} else if (amount < 0) {
					/*
					 * This is the 'from' currency. It must be the same as the
					 * currency in which the Paypal account is being kept.
					 */
					if (fromAmount != 0) {
						throw new ImportException("bad currency conversion");
					}
					if (paypalAccount.getCurrency().getCode().equals(currency)) {
						throw new ImportException("bad currency conversion - from currency must be same as currency of Paypal account.");
					}
					fromAmount = amount;
				} else {
					throw new ImportException("bad currency conversion - gross amount is zero");
				}
				return true;
			}

			return false;
		}

		public void createTransaction(Session session, PaypalAccount account) throws ImportException {
			Transaction trans = session.createTransaction();
			trans.setDate(date);

			PaypalEntry mainEntry = trans.createEntry().getExtension(PaypalEntryInfo.getPropertySet(), true);
			mainEntry.setAccount(paypalAccount);
			mainEntry.setAmount(netAmount);
			mainEntry.setMemo("payment - " + column_payeeName.getText());
			mainEntry.setValuta(date);
			mainEntry.setMerchantEmail(merchantEmail);
			mainEntry.setPropertyValue(ReconciliationEntryInfo.getUniqueIdAccessor(), transactionId);

			IncomeExpenseAccount categoryAccount;
			if (toCurrency == null) {
				// There was no currency conversion.
				categoryAccount = paypalAccount.getSaleAndPurchaseAccount();
			} else if (toCurrency.equals("GBP")) {
				// TODO: think of a way of avoiding this cast.
				categoryAccount = (IncomeExpenseAccount)getAssociatedAccount("net.sf.jmoney.paypal.expenses.GBP");
				if (categoryAccount == null) {
					throw new ImportException("A GBP purchase has been found.  This is a foreign exchange purchase but no account has been set up for GBP purchases.");
				}
			} else {
				throw new ImportException("The given currency is not supported.  Only transactions in USD or GBP currently supported.");
			}
			
			createCategoryEntry(trans, memo, grossAmount, netAmount, shippingAndHandling, insurance, salesTax, fee, url, categoryAccount);
			assertValid(trans);
		}

		public boolean isDone() {
			return done;
		}
	}

	@Override
	protected ImportedColumn[] getExpectedColumns() {
		return new ImportedColumn [] {
				column_date,
				null,
				null,
				column_payeeName,
				column_type,
				column_status,
				column_currency,
				column_grossAmount,
				column_fee,
				column_netAmount,
				column_payerEmail,
				column_payeeEmail,
				column_transactionId,
				null,
				null,
				column_memo,
				null,
				column_shippingAndHandling,
				column_insurance,
				column_salesTax,
				null,
				null,
				null,
				null,
				null,
				null,
				column_itemUrl,
				null,
				null,
				null,
				null,
				null,
				null,
				column_quantity,
				null,
				column_balance
		};
	}


	@Override
	protected String getSourceLabel() {
		return "Paypal";
	}

	@Override
	public AssociationMetadata[] getAssociationMetadata() {
		return new AssociationMetadata[] {
				new AssociationMetadata("net.sf.jmoney.paypal.interest", "Interest Account"),
				new AssociationMetadata("net.sf.jmoney.paypal.expenses", "Expenses Account"),
				new AssociationMetadata("net.sf.jmoney.paypal.expenses.GBP", "Purchases (GBP)"),
		};
	}
}
