package net.sf.jmoney.amazon;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import net.sf.jmoney.importer.wizards.CsvImportWizard;
import net.sf.jmoney.importer.wizards.ImportException;
import net.sf.jmoney.isolation.TransactionManager;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.BankAccount;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.IncomeExpenseAccount;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.Session.NoAccountFoundException;
import net.sf.jmoney.model2.Session.SeveralAccountsFoundException;
import net.sf.jmoney.model2.Transaction;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

public class AmazonItemImportWizard extends CsvImportWizard {

	private Session session;

//	private BankAccount account;

	/**
	 * Category used for Amazon purchases
	 */
	private IncomeExpenseAccount unknownAmazonPurchaseAccount;
	
	private MultiRowTransaction currentMultiRowProcessor = null;

	private ImportedDateColumn column_orderDate = new ImportedDateColumn("Order date", new SimpleDateFormat("MM-dd-yyyy"));
	private ImportedDateColumn column_shipmentDate = new ImportedDateColumn("Shipment date", new SimpleDateFormat("MM-dd-yyyy"));
	private ImportedTextColumn column_title = new ImportedTextColumn("Title");
	private ImportedTextColumn column_format = new ImportedTextColumn("Format");
	private ImportedTextColumn column_condition = new ImportedTextColumn("Condition");
	private ImportedTextColumn column_seller = new ImportedTextColumn("Seller");
	private ImportedNumberColumn column_quantity = new ImportedNumberColumn("Quantity");
	private ImportedTextColumn column_id = new ImportedTextColumn("ASIN/ISBN");
	private ImportedTextColumn column_paymentCard = new ImportedTextColumn("Payment - last 4 digits");
	private ImportedAmountColumn column_price = new ImportedAmountColumn("Per unit price");
	private ImportedTextColumn column_orderId = new ImportedTextColumn("Amazon order ID");
	private ImportedAmountColumn column_amount = new ImportedAmountColumn("Item subtotal");

//	Pattern patternCheque;
//	Pattern patternWithdrawalDate;

//	private ImportMatcher matcher;

//	@Override
//	protected void setAccount(Account accountInsideTransaction)	throws ImportException {
//		if (!(accountInsideTransaction instanceof BankAccount)) {
//			throw new ImportException("Bad configuration: This import can be used for bank accounts only.");
//		}
//
//		this.account = (BankAccount)accountInsideTransaction;
//		this.session = accountInsideTransaction.getSession();
//
//		try {
//			patternCheque = Pattern.compile("Cheque (\\d\\d\\d\\d\\d\\d)\\.");
//			patternWithdrawalDate = Pattern.compile("(.*) Withdrawal Date (\\d\\d  [A-Z][a-z][a-z] 20\\d\\d)");
//		} catch (PatternSyntaxException e) {
//			throw new RuntimeException("pattern failed", e); 
//		}
//
//		matcher = new ImportMatcher(account.getExtension(PatternMatcherAccountInfo.getPropertySet(), true));
//	}

	@Override
	protected void startImport(TransactionManager transactionManager) throws ImportException {
		try {
			Account unknownAmazonPurchaseAccount2 = session.getAccountByShortName("Amazon purchase");
			if (!(unknownAmazonPurchaseAccount2 instanceof IncomeExpenseAccount)) {
				MessageDialog.openError(Display.getDefault().getActiveShell(), "Account not Set Up", "Account found called 'Amazon purchase' but it is not an income/expense category.");
				throw new RuntimeException(); 
			}
			unknownAmazonPurchaseAccount = (IncomeExpenseAccount)unknownAmazonPurchaseAccount2;
		} catch (NoAccountFoundException e) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Account not Set Up", "No account exists called 'Amazon purchase'");
			throw new RuntimeException(e); 
		} catch (SeveralAccountsFoundException e) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Multiple Accounts Set Up", "Multiple accounts exists called 'Amazon purchase'");
			throw new RuntimeException(e); 
		}

	}

	@Override
	protected ImportedColumn[] getExpectedColumns() {
		return new ImportedColumn [] {
				column_orderDate,
				column_shipmentDate,
				column_title,
				column_format,
				column_condition,
				column_seller,
				column_quantity,
				column_id,
				null,
				null,
				column_paymentCard,
				column_price,
				column_orderId,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				column_amount,
		};
	}

	@Override
	public void importLine(String[] line) throws ImportException {

		Date shipmentDate = column_shipmentDate.getDate();
		String orderId = column_orderId.getText();
		
		/*
		 * Find the account to which this entry has been charged.
		 */
		String lastFourDigits = column_paymentCard.getText();
		if (lastFourDigits == null || lastFourDigits.length() != 4) {
			throw new ImportException("Last four digits of payment card not properly specified.");
		}

		
		
		boolean processed = false;
		if (currentMultiRowProcessor != null) {
			processed = currentMultiRowProcessor.processCurrentRow(session);
			if (currentMultiRowProcessor.isDone()) {
				currentMultiRowProcessor.createTransaction(session);
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


		
		
		BankAccount chargedAccount = null;
		for (Iterator<CapitalAccount> iter = session.getCapitalAccountIterator(); iter.hasNext(); ) {
			CapitalAccount eachAccount = iter.next();
			if (eachAccount instanceof BankAccount) {
				BankAccount eachBankAccount = (BankAccount)eachAccount;
				if (eachBankAccount.getAccountNumber().endsWith(lastFourDigits)) {
					chargedAccount = eachBankAccount;
					break;
				}
			}
		}
		if (chargedAccount == null) {
			throw new ImportException("No account exists with an account number ending with " + lastFourDigits + ".");
		}
		
		/*
		 * Look for a category account that has a name that starts with "Amazon unmatched"
		 * and a currency that matches the currency of the charge account.
		 */
		IncomeExpenseAccount unmatchedAccount = null;
		for (Iterator<IncomeExpenseAccount> iter = session.getIncomeExpenseAccountIterator(); iter.hasNext(); ) {
			IncomeExpenseAccount eachAccount = iter.next();
			if (eachAccount.getName().startsWith("Amazon unmatched")
					&& eachAccount.getCurrency() == chargedAccount.getCurrency()) {
				unmatchedAccount = eachAccount;
				break;
			}
		}
		if (unmatchedAccount == null) {
			throw new ImportException("No account exists with a name that begins 'Amazon unmatched' and a currency of " + chargedAccount.getCurrency().getName() + ".");
		}

		/*
		 * Look in the unmatched entries account for an entry that matches on order id and shipment date.
		 */
		AmazonEntry matchingEntry = null;
		for (Entry entry : unmatchedAccount.getEntries()) {
			AmazonEntry amazonEntry = entry.getExtension(AmazonEntryInfo.getPropertySet(), false);
			if (amazonEntry != null) {
				if (amazonEntry.getOrderId().equals(orderId)
						&& amazonEntry.getShipmentDate().equals(shipmentDate)) {
					matchingEntry = amazonEntry;
				}
			}
		}
		
		// All rows are processed by this
		if (matchingEntry == null) {
			/*
			 * We should check the charge account to make sure there is no entries.
			 * If both the items and the order has already been imported then we should
			 * find the matching entry in the charge account. 
			 */
			for (Entry entry : chargedAccount.getEntries()) {
				AmazonEntry amazonEntry = entry.getExtension(AmazonEntryInfo.getPropertySet(), false);
				if (amazonEntry != null) {
					if (amazonEntry.getOrderId().equals(orderId)
							&& amazonEntry.getShipmentDate().equals(shipmentDate)) {
						throw new ImportException("Items for this shipment have already been imported.");
					}
				}
			}

			currentMultiRowProcessor = new ItemsShippedTransactionUnmatched(unmatchedAccount, shipmentDate, orderId);
		} else {
			/*
			 * We have a matching entry.  Now if the amount is positive then it represents items
			 * that have not yet been imported, and if the amount is negative then it represents
			 * the charge account entry that cannot yet be matched.
			 */
			if (matchingEntry.getAmount() < 0) {
				throw new ImportException("Items for this shipment have already been imported.");
			}
			currentMultiRowProcessor = new ItemsShippedTransactionMatched(unmatchedAccount, shipmentDate, orderId, matchingEntry);
		}

		currentMultiRowProcessor.processCurrentRow(session);
	}
	}

	public interface MultiRowTransaction {
		/**
		 * 
		 * @return true if this row was processed, false if this row is not a
		 * 		part of this transaction and should be separately processed
		 * 		by the caller
		 * @throws ImportException 
		 */
		boolean processCurrentRow(Session session) throws ImportException;

		/**
		 * 
		 * @return true if this transaction has received all its row and is
		 * 		ready to be created in the datastore, false if there may be
		 * 		more rows in this transaction
		 */
		boolean isDone();

		void createTransaction(Session session) throws ImportException;
	}

	public abstract class ItemsShippedTransaction implements MultiRowTransaction {
		protected IncomeExpenseAccount unmatchedAccount;
		protected Date orderDate;
		protected Date shipmentDate;
		protected String orderId;
		protected List<ItemRow> rowItems = new ArrayList<ItemRow>();

		protected boolean done = false;

		/**
		 * Initial constructor called when first item in a shipment found.
		 * 
		 * @param shipmentDate
		 * @param quantity
		 * @param stock
		 * @throws ImportException 
		 */
		public ItemsShippedTransaction(IncomeExpenseAccount unmatchedAccount, Date shipmentDate, String orderId) throws ImportException {
			this.unmatchedAccount = unmatchedAccount;
			this.orderDate = column_orderDate.getDate();
			this.shipmentDate = shipmentDate;
			this.orderId = orderId;
		}

		public boolean processCurrentRow(Session session) throws ImportException {
			if (orderId.equals(column_orderId.getText())
					&& shipmentDate.equals(column_shipmentDate.getDate())) {
				ItemRow item = new ItemRow();

				item.title = column_title.getText();
				item.id = column_id.getText();
				item.quantity = column_quantity.getAmount();
				item.price = column_price.getAmount();

				rowItems.add(item);
				return true;
			} else {
				return false;
			}
		}

		protected void addItemsToTransaction(Transaction trans) {
			for (ItemRow rowItem2 : rowItems) {
				AmazonEntry itemEntry = trans.createEntry().getExtension(AmazonEntryInfo.getPropertySet(), true);
				itemEntry.setAccount(unknownAmazonPurchaseAccount);
				itemEntry.setAmount(rowItem2.subtotal);
				itemEntry.setMemo(rowItem2.title);
				itemEntry.setOrderId(orderId);
				itemEntry.setShipmentDate(shipmentDate);
				itemEntry.setAsinOrIsbn(rowItem2.id);
			}
		}

		public boolean isDone() {
			return done;
		}
	}

	/**
	 * This class handles the import of items from the 'items' file when the matching record from the
	 * 'orders' file has already been imported.
	 * 
	 *  In this case the list of items replaces the entry in the unmatched entries account.
	 */
	public class ItemsShippedTransactionMatched extends ItemsShippedTransaction {

		private AmazonEntry matchingEntry;

		public ItemsShippedTransactionMatched(IncomeExpenseAccount unmatchedAccount, Date shipmentDate, String orderId, AmazonEntry matchingEntry) throws ImportException {
			super(unmatchedAccount, shipmentDate, orderId);
			this.matchingEntry = matchingEntry;
		}
		
		public void createTransaction(Session session) throws ImportException {
			// Modify the existing transaction
			Transaction trans = matchingEntry.getTransaction();
			
			// TODO worry about shipping and handling and stuff like that.
			
			// Distribute the shipping and handling amount
//			distribute(shippingAndHandlingAmount, rowItems);

			addItemsToTransaction(trans);

			// Remove the unmatched entry as it is being replaced by
			// the items.
			trans.deleteEntry(matchingEntry.getBaseObject());
			
			assertValid(trans);
		}

	}

	/**
	 * This class handles the import of items from the 'items' file when no matching record from the
	 * 'orders' file has yet been imported.
	 * 
	 *  In this case the list of items is created.  However we don't know the total charged because details
	 *  such as shipping costs are in the 'orders' file.  We therefore put an entry in the 'unmatched' account
	 *  with the total cost of all the items.
	 */
	public class ItemsShippedTransactionUnmatched extends ItemsShippedTransaction {

		public ItemsShippedTransactionUnmatched(IncomeExpenseAccount unmatchedAccount, Date date, String orderId) throws ImportException {
			super(unmatchedAccount, date, orderId);
		}
		
		public void createTransaction(Session session) throws ImportException {
			// Start a new transaction
			Transaction trans = session.createTransaction();
			trans.setDate(orderDate);

			long total = 0;
			for (ItemRow rowItem2 : rowItems) {
				total += rowItem2.subtotal;
			}

			// Create a single entry in the "unmatched entries" account
			AmazonEntry mainEntry = trans.createEntry().getExtension(AmazonEntryInfo.getPropertySet(), true);
			mainEntry.setAccount(unmatchedAccount);
			mainEntry.setAmount(-total);
			mainEntry.setShipmentDate(shipmentDate);
			mainEntry.setOrderId(orderId);
			
			addItemsToTransaction(trans);

			assertValid(trans);
		}

	}

	private void assertValid(Transaction trans) {
		long total = 0;
		for (Entry entry : trans.getEntryCollection()) {
			total += entry.getAmount();
		}
		if (total != 0) {
			throw new RuntimeException("unbalanced");
		}
	}

	public class ItemRow {
		public String title;
		public String id;
		public long quantity;
		public long price; 
		public long subtotal;
	}
}
