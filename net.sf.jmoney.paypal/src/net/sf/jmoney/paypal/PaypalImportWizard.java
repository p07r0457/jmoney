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

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.sf.jmoney.importer.wizards.CsvImportWizard;
import net.sf.jmoney.importer.wizards.ImportException;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.IncomeExpenseAccount;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.Transaction;
import net.sf.jmoney.model2.Session.NoAccountFoundException;
import net.sf.jmoney.model2.Session.SeveralAccountsFoundException;
import net.sf.jmoney.paypal.CsvImportWizard.Row;
import net.sf.jmoney.reconciliation.ReconciliationEntryInfo;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

/**
 * A wizard to import data from a comma-separated file that has been downloaded
 * from Paypal.
 */
public class PaypalImportWizard extends CsvImportWizard {

	/**
	 * Account inside transaction
	 */
	private PaypalAccount paypalAccount;


	Collection<Row> refunds = new ArrayList<Row>();
	Collection<Row> reversals = new ArrayList<Row>();

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

		try {
			expensesAccount = session.getAccountByShortName("Stock - Expenses (US)");
		} catch (NoAccountFoundException e) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Account not Set Up", "No account exists called 'Stock - Expenses (US)'");
			throw new RuntimeException(e); 
		} catch (SeveralAccountsFoundException e) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Multiple Accounts Set Up", "Multiple accounts exists called 'Stock - Expenses (US)'");
			throw new RuntimeException(e); 
		}
	}

	@Override
	public void importLine(String[] line) throws ImportException {
		String dateString = line[0];
		String rowPayeeName = line[3];
		String rowType = line[4];
		String rowStatus = line[5];
		long rowGrossAmount = getAmount(line[6]);
		long rowFee = getAmount(line[7]);
		long rowNetAmount = getAmount(line[8]);

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
			rowMerchantEmail = line[9];
		} else {
			rowMerchantEmail = line[10];
		}

		String rowTransactionId = line[11];
		String rowMemo = line[14];
		long rowShippingAndHandlingAmount = getAmount(line[16]);
		long rowInsurance = getAmount(line[17]);
		long rowSalesTax = getAmount(line[18]);
		String rowItemUrlString = line[25];
		String rowQuantityString = line[32];
		long rowBalance = getAmount(line[34]);

		DateFormat df = new SimpleDateFormat("MM/dd/yy");
		Date rowDate;
		try {
			rowDate = df.parse(dateString);
		} catch (ParseException e) {
			throw new UnexpectedDataException("Date", dateString);
		}

		boolean processed = false;
		if (currentMultiRowProcessor != null) {
			processed = currentMultiRowProcessor.process(line, session, paypalAccount);
			if (currentMultiRowProcessor.isDone()) {
				currentMultiRowProcessor.createTransaction(session, paypalAccount);
				currentMultiRowProcessor = null;
			}
		}

		if (!processed) {	
			if (rowType.equals("Shopping Cart Payment Sent")) {
				/**
				 * Shopping cart entries are split across multiple rows, with a 'Payment Sent' row
				 * following by one or more 'Item' rows.  These must be combined into a single
				 * transaction.  To enable us to do this, this class is used to put each row into,
				 * and it can then output the transaction when a row is found that is in the
				 * next transaction.
				 */

				if (currentMultiRowProcessor != null) {
					throw new RuntimeException("something is wrong");
				}

				currentMultiRowProcessor = new ShoppingCartPaymentSent(date, rowShippingAndHandlingAmount);
			} else if (rowType.equals("Shopping Cart Item")) {
				throw new UnexpectedDataException("'Shopping Cart Item' row found but it is not preceeded by a 'Shopping Cart Payment Sent' or 'eBay Payment Sent' row.");
			} else if (rowType.equals("Refund")) {
				/*
				 * Refunds are combined with the original transaction.
				 * 
				 * Because the input file is in reverse date order, we find
				 * the refund first. We save the refund information in a
				 * collection. Whenever a 'Shopping Cart Payment Sent' or a
				 * 'eBayPaymentSent' or 'Express Checkout Payment Sent' is
				 * found with a status of 'Partially Refunded' or 'Refunded'
				 * and the payee name exactly matches the we add the refund
				 * as another pair of split entries in the same transaction.
				 */
				refunds.add(row);
			} else if (rowType.equals("Reversal")) {
				/*
				 * Reversals are processed in a similar way to refunds.  We keep
				 * and list and match them to later entries.
				 */
				reversals.add(row);
			} else if (rowType.equals("eBay Payment Sent")
					|| rowType.equals("eBay Payment Received")
					|| rowType.equals("Payment Received")
					|| rowType.equals("Payment Sent")
					|| rowType.equals("Web Accept Payment Sent")) {

				if (rowStatus.equals("Refunded")) {
					/*
					 * Find the refund entry.  We create a single transaction with two entries both
					 * in this Paypal account. 
					 */
					Row match = null;
					for (Row refund : refunds) {
						if (refund.payeeName.equals(rowPayeeName)
								&& refund.grossAmount == -row.grossAmount) {
							match = refund;
							break;
						}
					}
					if (match == null) {
						throw new UnexpectedDataException("An entry was found that says it was refunded, but no matching 'Refund' entry was found.");
					}
					refunds.remove(match);

					createRefundTransaction(row, match);
				} else if (rowStatus.equals("Reversed")) {
					/*
					 * Find the reversal entry.  We don't create anything if an
					 * entry was reversed. 
					 */
					Row match = null;
					for (Row reversal : reversals) {
						if (reversal.payeeName.equals(rowPayeeName)
								&& reversal.grossAmount == -row.grossAmount) {
							match = reversal;
							break;
						}
					}
					if (match == null) {
						throw new UnexpectedDataException("An entry was found that says it was reversed, but no matching 'Reversal' entry was found.");
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
						 */
						currentMultiRowProcessor = new EbayPaymentSent(line);
				} else {
					Transaction trans = session.createTransaction();
					trans.setDate(row.date);

					PaypalEntry mainEntry = trans.createEntry().getExtension(PaypalEntryInfo.getPropertySet(), true);
					mainEntry.setAccount(paypalAccount);
					mainEntry.setAmount(row.netAmount);
					mainEntry.setMemo("payment - " + rowPayeeName);
					mainEntry.setValuta(row.date);
					mainEntry.setMerchantEmail(row.merchantEmail);
					mainEntry.setPropertyValue(ReconciliationEntryInfo.getUniqueIdAccessor(), row.transactionId);

					if (rowStatus.equals("Partially Refunded")) {
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

						for (Iterator<Row> iter = refunds.iterator(); iter.hasNext(); ) {
							Row refund = iter.next();
							if (refund.payeeName.equals(rowPayeeName)) {
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

						if (-row.netAmount - refundAmount == row.shippingAndHandlingAmount) {
							// All was refunded except s&h, so indicate accordingly in the memo
							row.memo = row.memo + " (s&h not refunded after return)";
						} else {
							// Indicate the original amount paid and refund amount in the memo 
							row.memo = row.memo + " ($" + currency.format(-row.netAmount) + " less $" + currency.format(refundAmount) + " refunded)";
						}

						// Note that the amounts in the row will be negative, which is
						// why we add the refund amount when it may seem we should deduct
						// the refund amount.
						row.netAmount += refundAmount;
						row.grossAmount += refundAmount;
					}

					if (row.fee != 0) {
						// For non-sale transfers, treat the Paypal fee as a bank service
						// charge.  For E-bay sales, absorb in the price or proceeds.

						if (rowType.equals("Payment Received")
								|| rowType.equals("Payment Sent")) {
							if (paypalAccount.getPaypalFeesAccount() == null) {
								throw new UnexpectedDataException("A Paypal fee has been found in the imported data.  However, no category has been configured in the properties for this Paypal account for such fees.");
							}

							// Note that fee shows up as a negative amount, and we want
							// a positive amount in the category account to be used for the fee.
							Entry feeEntry = trans.createEntry();
							feeEntry.setAccount(paypalAccount.getPaypalFeesAccount());
							feeEntry.setAmount(-row.fee);
							feeEntry.setMemo("Paypal");
							// Set fee to zero so it does not appear in the memo
							row.fee = 0L;
							row.netAmount = row.grossAmount;
						}
					}

					if (row.memo.length() == 0) {
						// Certain transactions don't have memos, so we fill one in
						if (rowType.equals("Payment Received")) {
							row.memo = rowPayeeName + " - gross payment";
						}
						if (rowType.equals("Payment Sent")) {
							row.memo = rowPayeeName + " - payment";
						}
					}
					createCategoryEntry(trans, row, paypalAccount.getSaleAndPurchaseAccount());

					assertValid(trans);
				}
			} else if (rowType.equals("Donation Sent")) {
				if (paypalAccount.getDonationAccount() == null) {
					throw new UnexpectedDataException("A donation has been found in the imported data.  However, no category was set for donations.  Please go to the Paypal account properties and select a category to be used for donations.");
				}

				// Donations do not have memos set, so the payee name is used as the memo in the
				// expense category entry.
				createTransaction(row, "donation sent", paypalAccount.getDonationAccount(), rowPayeeName);
			} else if (rowType.equals("Add Funds from a Bank Account")) {
				if (paypalAccount.getTransferBank() == null) {
					throw new UnexpectedDataException("A bank account transfer has been found in the imported data.  However, no bank account has been set in the properties for this Paypal account.");
				}
				createTransaction(row, "transfer from bank", paypalAccount.getTransferBank(), "transfer to Paypal");
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
					throw new UnexpectedDataException("An eCheck entry has been found in the imported data.  However, no sale and purchase account has been set in the properties for this Paypal account.");
				}
				createTransaction(row, "payment by transfer", paypalAccount.getSaleAndPurchaseAccount(), "transfer from Paypal");
			} else if (rowType.equals("Express Checkout Payment Sent")) {
				if (paypalAccount.getSaleAndPurchaseAccount() == null) {
					throw new UnexpectedDataException("An 'Express Checkout' entry has been found in the imported data.  However, no sale and purchase account has been set in the properties for this Paypal account.");
				}

				if (rowStatus.equals("Refunded")) {
					/*
					 * Find the refund entry.  We create a single transaction with two entries both
					 * in this Paypal account. 
					 */
					Row match = null;
					for (Row refund : refunds) {
						if (refund.payeeName.equals(rowPayeeName)
								&& refund.grossAmount == -row.grossAmount) {
							match = refund;
							break;
						}
					}
					if (match == null) {
						throw new UnexpectedDataException("An entry was found that says it was refunded, but no matching 'Refund' entry was found.");
					}
					refunds.remove(match);

					createRefundTransaction(row, match);
				} else {
					createTransaction(row, rowPayeeName, paypalAccount.getSaleAndPurchaseAccount(), rowPayeeName + " - Paypal payment");
				}
			} else if (rowType.equals("Charge From Credit Card")) {
				if (paypalAccount.getTransferCreditCard() == null) {
					throw new UnexpectedDataException("A credit card charge has been found in the imported data.  However, no credit card account has been set in the properties for this Paypal account.");
				}
				createTransaction(row, "payment from credit card", paypalAccount.getTransferCreditCard(), "transfer to Paypal");
			} else if (rowType.equals("Credit to Credit Card")) {
				if (paypalAccount.getTransferCreditCard() == null) {
					throw new UnexpectedDataException("A credit card refund has been found in the imported data.  However, no credit card account has been set in the properties for this Paypal account.");
				}
				createTransaction(row, "refund to credit card", paypalAccount.getTransferCreditCard(), "refund from Paypal");
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
	 */
	private void createCategoryEntry(Transaction trans, Row row, IncomeExpenseAccount account) {
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
		if (rowType.equals("Shopping Cart Item")) {
			lineItemEntry.setAmount(row.grossAmount);
		} else {
			lineItemEntry.setAmount(-row.netAmount);
		}

		if (row.itemUrlString.length() != 0) {
			try {
				URL itemUrl = new URL(row.itemUrlString);
				lineItemEntry.setItemUrl(itemUrl);
			} catch (MalformedURLException e) {
				// Leave the URL blank
			}
		}

		StringBuffer adjustmentsBuffer = new StringBuffer();

		Currency currency = paypalAccount.getCurrency();
		String separator = "";
		long baseAmount = lineItemEntry.getAmount();
		String memo = row.memo;

		if (row.quantityString.length() != 0 
				&& !row.quantityString.equals("0")
				&& !row.quantityString.equals("1")) {
			memo = memo + " x" + row.quantityString;
		}

		if (row.shippingAndHandlingAmount != 0) {
			adjustmentsBuffer.append("s&h $")
			.append(currency.format(row.shippingAndHandlingAmount))
			.append(separator);
			separator = ", ";
			baseAmount -= row.shippingAndHandlingAmount;
		}
		if (row.insurance != 0) {
			adjustmentsBuffer.append("insurance $")
			.append(currency.format(row.insurance))
			.append(separator);
			separator = ", ";
			baseAmount -= row.insurance;
		}
		if (row.salesTax != 0) {
			adjustmentsBuffer.append("tax $")
			.append(currency.format(row.salesTax))
			.append(separator);
			separator = ", ";
			baseAmount -= row.salesTax;
		}
		if (row.fee != 0) {
			adjustmentsBuffer.append("less Paypal fee $")
			.append(currency.format(row.fee))
			.append(separator);
			separator = ", ";
			baseAmount -= row.fee;
		}

		if (adjustmentsBuffer.length() == 0) {
			lineItemEntry.setMemo(memo);
		} else {
			lineItemEntry.setMemo(memo + " ($" + currency.format(baseAmount) + " + " + adjustmentsBuffer.toString() + ")");
		}
	}

	/**
	 * We distribute the shipping and handling among the items in proportion
	 * to the price of each item.  This is the preference of the author.
	 * If this is not your preference then please add a preference to the preferences
	 * to indicate if a separate line item should instead be created for the
	 * shipping and handling and implement it.
	 * @throws UnexpectedDataException 
	 */
	private void distribute(long toDistribute, List<Row> rowItems) throws UnexpectedDataException {
		long netTotal = 0;
		for (Row rowItem : rowItems) {
			if (rowItem.grossAmount <= 0) {
				throw new UnexpectedDataException("Shopping Cart Item with zero or negative gross amount");
			}
			netTotal += rowItem.grossAmount;
		}

		long leftToDistribute = toDistribute;

		for (Row rowItem : rowItems) {
			long amount = toDistribute * rowItem.grossAmount / netTotal;
			rowItem.shippingAndHandlingAmount = amount;
			leftToDistribute -= amount;
		}

		// We have rounded down, so we may be under.  We now distribute
		// a penny to each to get a balanced transaction.
		for (Row rowItem : rowItems) {
			if (leftToDistribute > 0) {
				rowItem.shippingAndHandlingAmount++;
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
		for (Row rowItem : rowItems) {
			rowItem.grossAmount += rowItem.shippingAndHandlingAmount;
		}		
	}

	private void createTransaction(Row row, String paypalAccountMemo, Account otherAccount, String otherAccountMemo) {
		Transaction trans = session.createTransaction();
		trans.setDate(row.date);

		PaypalEntry mainEntry = trans.createEntry().getExtension(PaypalEntryInfo.getPropertySet(), true);
		mainEntry.setAccount(paypalAccount);
		mainEntry.setAmount(row.grossAmount);
		mainEntry.setMemo(paypalAccountMemo);
		mainEntry.setValuta(row.date);
		mainEntry.setPropertyValue(ReconciliationEntryInfo.getUniqueIdAccessor(), row.transactionId);

		Entry otherEntry = trans.createEntry();
		otherEntry.setAccount(otherAccount);
		otherEntry.setAmount(-row.grossAmount);
		otherEntry.setMemo(otherAccountMemo);
	}

	/**
	 * This is a helper method that creates a transaction where there are just two entries
	 * and both are in the Paypal account.  This occurs when an entry is refunded in full.
	 */
	private void createRefundTransaction(Row originalRow, Row refundRow) {
		Transaction trans = session.createTransaction();
		trans.setDate(originalRow.date);

		PaypalEntry mainEntry = trans.createEntry().getExtension(PaypalEntryInfo.getPropertySet(), true);
		mainEntry.setAccount(paypalAccount);
		mainEntry.setAmount(originalRow.grossAmount);
		mainEntry.setMemo(originalRow.payeeName);
		mainEntry.setValuta(originalRow.date);
		mainEntry.setPropertyValue(ReconciliationEntryInfo.getUniqueIdAccessor(), originalRow.transactionId);

		Entry refundEntry = trans.createEntry();
		refundEntry.setAccount(paypalAccount);
		refundEntry.setAmount(-originalRow.grossAmount);
		refundEntry.setMemo("refund - " + originalRow.payeeName);
		refundEntry.setValuta(refundRow.date);
		refundEntry.setPropertyValue(ReconciliationEntryInfo.getUniqueIdAccessor(), refundRow.transactionId);
	}

	public interface MultiRowTransaction {

		/**
		 * 
		 * @return true if this row was processed, false if this row is not a
		 * 		part of this transaction and should be separately processed
		 * 		by the caller
		 */
		boolean process(String[] line, Session session, PaypalAccount account);

		/**
		 * 
		 * @return true if this transaction has received all its row and is
		 * 		ready to be created in the datastore, false if there may be
		 * 		more rows in this transaction
		 */
		boolean isDone();

		void createTransaction(Session session, PaypalAccount account);
	}

	public class ShoppingCartPaymentSent implements MultiRowTransaction {

		private Date date;
		private long shippingAndHandlingAmount;
		private List<Row> rowItems = new ArrayList<Row>();


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
			this.shippingAndHandlingAmount = shippingAndHandlingAmount;
		}

		public void createTransaction(Session session, PaypalAccount account) {
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
			mainEntry.setAmount(rowGrossAmount);
			mainEntry.setMemo("payment - " + rowPayeeName);
			mainEntry.setMerchantEmail(rowMerchantEmail);
			mainEntry.setPropertyValue(ReconciliationEntryInfo.getUniqueIdAccessor(), rowTransactionId);

			for (Row rowItem2 : rowItems) {
				createCategoryEntry(trans, rowItem2, paypalAccount.getSaleAndPurchaseAccount());
			}

			/*
			 * Look for a refunds that match.  Move them into the cart so they can
			 * be processed as part of the same transaction.
			 */
			if (rowStatus.equals("Partially Refunded")) {
				long refundAmount = 0;
				for (Iterator<Row> iter = refunds.iterator(); iter.hasNext(); ) {
					Row refund = iter.next();
					if (refund.payeeName.equals(rowPayeeName)) {
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
				lineItemEntry.setMemo(rowPayeeName + " - amount refunded");
			}
			assertValid(trans);
		}

		public boolean process(String[] itemRow, Session session, PaypalAccount account) {

			String rowItemType = itemRow[];
			long itemShippingAndHandlingAmount = itemRow[];
			
			if (rowItemType.equals("Shopping Cart Item")) {
				if (itemShippingAndHandlingAmount != shippingAndHandlingAmount) {
					throw new UnexpectedDataException("shipping and handling amounts in different rows in the same transaction do not match.");
				}

				rowItems.add(itemRow);
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
	 */
	public class EbayPaymentSent implements MultiRowTransaction {

		private Date date;
		private long shippingAndHandlingAmount;
		private List<Row> rowItems = new ArrayList<Row>();


		private boolean done = false;

		/**
		 * Initial constructor called when first "Mandatory Exchange" row found.
		 * 
		 * @param date
		 * @param quantity
		 * @param stock
		 */
		public EbayPaymentSent(String [] line) {
			this.line = line;
		}

		public void createTransaction(Session session, PaypalAccount account) {
			Transaction trans = session.createTransaction();
			trans.setDate(row.date);

			PaypalEntry mainEntry = trans.createEntry().getExtension(PaypalEntryInfo.getPropertySet(), true);
			mainEntry.setAccount(paypalAccount);
			mainEntry.setAmount(row.netAmount);
			mainEntry.setMemo("payment - " + rowPayeeName);
			mainEntry.setValuta(row.date);
			mainEntry.setMerchantEmail(row.merchantEmail);
			mainEntry.setPropertyValue(ReconciliationEntryInfo.getUniqueIdAccessor(), row.transactionId);

			createCategoryEntry(trans, thisRow, paypalAccount.getSaleAndPurchaseAccount());
			assertValid(trans);
		}

		public boolean process(String[] itemRow, Session session, PaypalAccount account) {

			String rowItemType = itemRow[];
			long itemShippingAndHandlingAmount = itemRow[];
			
			done = true;

			if (rowItemType.equals("Shopping Cart Item")) {
				thisRow.quantityString = nextRow.quantityString;
				return true;
			}

			return false;
		}

		public boolean isDone() {
			return done;
		}
	}

	long getAmount(String amountString) {
		if (amountString.length() == 0) {
			return 0;
		}

		boolean negate = false;
		if (amountString.charAt(0) == '-') {
			amountString = amountString.substring(1);
			negate = true;
		}

		try {
			String parts [] = amountString.replaceAll(",", "").split("\\.");
			long amount = Long.parseLong(parts[0]) * 100;
			if (parts.length > 1) {
				if (parts[1].length() == 1) {
					parts[1] += "0"; 
				}
				amount += Long.parseLong(parts[1]);
			}
			return negate ? -amount : amount;
		} catch (NumberFormatException e) {
			return 0;
		}
	}

}
