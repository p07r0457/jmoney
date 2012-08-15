package net.sf.jmoney.amazon;

import java.io.IOException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.sf.jmoney.importer.matcher.EntryData;
import net.sf.jmoney.importer.matcher.ImportMatcher;
import net.sf.jmoney.importer.model.PatternMatcherAccountInfo;
import net.sf.jmoney.importer.model.ReconciliationEntryInfo;
import net.sf.jmoney.importer.wizards.AssociationMetadata;
import net.sf.jmoney.importer.wizards.CsvImportWizard;
import net.sf.jmoney.importer.wizards.ImportException;
import net.sf.jmoney.isolation.TransactionManager;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.BankAccount;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.Session;

/**
 * Items are grouped based on order id and shipment date.  A single order may be split into multiple
 * shipments.  Items are charged as they are shipped, and it is assumed that all items shipped on the
 * same day are charged as a single charge.
 * <P>
 * Having grouped the items, we still may not know the amount charged.  That is because there may be
 * shipping and other costs.  Those other costs are not available in the item import.  Therefore we do
 * not try to match items to charges on a credit card.  It is the import of the orders that makes this
 * connection.  If items have been imported but the orders have not then the items are added up and charged
 * to a special 'pending Amazon charges' account.  When the orders are imported, these entries are matched
 * to the credit card or bank account entries as appropriate.
 * <P>
 * The idea is that the orders and items can both be imported separately.  Data is put into the datastore.
 * If only one is imported then the data from that one import is in the datastore, but if both are imported
 * then everything is matched.  It should not matter what order the Amazon items, the Amazon orders, and the
 * charge or bank account are imported.
 *  
 * @author westbury.nigel2
 *
 */
public class AmazonOrderImportWizard extends CsvImportWizard {

	private Session session;

	private BankAccount account;
	
	private ImportedDateColumn column_orderDate = new ImportedDateColumn("Order date", new SimpleDateFormat("MM-dd-yyyy"));
	private ImportedDateColumn column_shipmentDate = new ImportedDateColumn("Shipment date", new SimpleDateFormat("MM-dd-yyyy"));
	private ImportedTextColumn column_paymentCard = new ImportedTextColumn("Payment - last 4 digits");
	private ImportedTextColumn column_orderId = new ImportedTextColumn("Amazon order ID");
	private ImportedTextColumn column_status = new ImportedTextColumn("Shipment/order condition");
	private ImportedAmountColumn column_subtotal = new ImportedAmountColumn("Subtotal");
	private ImportedAmountColumn column_shippingAmount = new ImportedAmountColumn("Shipping");
	private ImportedAmountColumn column_promotion = new ImportedAmountColumn("Total Promotions");
	private ImportedAmountColumn column_totalCharged = new ImportedAmountColumn("Total Charged");

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
	}

	@Override
	protected ImportedColumn[] getExpectedColumns() {
		return new ImportedColumn [] {
				column_orderDate,
				column_shipmentDate,
				column_paymentCard,
				column_orderId,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				column_status,
				column_subtotal,
				column_shippingAmount,
				null,
				column_promotion,
				null,
				column_totalCharged,
				null,
				null,
		};
	}

	@Override
	public void importLine(String[] line) throws ImportException {
		EntryData entryData = new EntryData();

		Date date = column_orderDate.getDate();
		
		if (column_credits.getAmount() != null 
				&& column_debits.getAmount() == null) {
			entryData.setAmount(column_credits.getAmount());
		} else if (column_debits.getAmount() != null 
				&& column_credits.getAmount() == null) {
			entryData.setAmount(-column_debits.getAmount());
		} else {
			throw new ImportException("One or other of credits or debits must be specified.");
		}

		/*
		 * We don't have a unique id.  However we make one up.  This is made
		 * up from the order id and the product id.  If the same product is ordered
		 * more than once in the same order then it shows as a single row with a quantity
		 * of more than one.  We can therefore be sure that we have a unique id.
		 */
		String uniqueId = column_orderId.getText() + "~" + column_id.getText();

		/*
		 * See if an entry already exists with this uniqueId.
		 */
		for (Entry entry : account.getEntries()) {
			if (uniqueId.equals(entry.getPropertyValue(ReconciliationEntryInfo.getUniqueIdAccessor()))) {
				// This row has already been imported so ignore it.
				return;
			}
		}
		
		
		Matcher chequeMatcher = patternCheque.matcher(column_title.getText());
		if (chequeMatcher.matches()) {
			String chequeNumber = chequeMatcher.group(1);
			entryData.setCheck(chequeNumber);
			entryData.valueDate = date;
		} else {
			String memo = column_title.getText();
			
			/*
			 * Nationwide put a '.' at the end of every memo.  Remove them.
			 */
			if (memo.endsWith(".")) {
				memo = memo.substring(0, memo.length() - 1);
			}
			
			/*
			 * If the transaction happened on a different day from the date the transaction
			 * hit the account then this is noted in the memo by appending
			 * " Withdrawal Date dd  MMM yyyy" to the memo.
			 */
			Matcher withdrawalDateMatcher = patternWithdrawalDate.matcher(memo);
			if (withdrawalDateMatcher.matches()) {
				String datePart = withdrawalDateMatcher.group(2);
				
				DateFormat sf2 = new SimpleDateFormat("dd  MMM yyyy");
				Date transactionDate;
				try {
					transactionDate = sf2.parse(datePart);
					entryData.valueDate = transactionDate;
					entryData.clearedDate = date;
				} catch (ParseException e) {
					// should not happen, but just ignore the date part if it does
					entryData.valueDate = date;
				}

				memo = withdrawalDateMatcher.group(1);
			} else {
				entryData.valueDate = date;
			}
			
			entryData.setMemo(memo);
		}

		matcher.process(entryData, session);
	}

	@Override
	protected String getSourceLabel() {
		return "Amazon Orders";
	}

	@Override
	public AssociationMetadata[] getAssociationMetadata() {
		return new AssociationMetadata[] {
				new AssociationMetadata("net.sf.jmoney.amazon.holdingaccount", "Holding Account")
		};
	}
}
