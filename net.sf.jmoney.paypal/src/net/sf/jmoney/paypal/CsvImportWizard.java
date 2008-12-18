/*
 * 
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2004 Nigel Westbury <westbury@users.sourceforge.net>
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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import net.sf.jmoney.isolation.TransactionManager;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.DatastoreManager;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.Transaction;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import au.com.bytecode.opencsv.CSVReader;

/**
 * A wizard to import data from a comma-separated file that has been downloaded
 * from Paypal.
 * 
 * Currently this wizard if a single page wizard that asks only for the file.
 * This feature is implemented as a wizard because the Eclipse workbench import
 * action requires all import implementations to be wizards.
 */
public class CsvImportWizard extends Wizard implements IImportWizard {

	private IWorkbenchWindow window;

	private CsvImportWizardPage filePage;

	/**
	 * The transaction manager for all changes made by the import, the
	 * transaction being committed when the file has been fully imported.
	 */
	TransactionManager transactionManager;
	
	/**
	 * Session, being the version inside the transaction so changes
	 * are not applied to the datastore until the transaction is
	 * committed.
	 */
	private Session session;

	private PaypalAccount paypalAccount;
	
	public CsvImportWizard() {
		IDialogSettings workbenchSettings = Activator.getDefault().getDialogSettings();
		IDialogSettings section = workbenchSettings.getSection("CsvImportWizard");//$NON-NLS-1$
		if (section == null) {
			section = workbenchSettings.addNewSection("CsvImportWizard");//$NON-NLS-1$
		}
		setDialogSettings(section);
	}

	/**
	 * We will cache window object in order to be able to provide parent shell
	 * for the message dialog.
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {

		this.window = workbench.getActiveWorkbenchWindow();

		DatastoreManager sessionManager = (DatastoreManager)window.getActivePage().getInput();

		// Original JMoney disabled the import menu items when no
		// session was open. I don't know how to do that in Eclipse,
		// so we display a message instead.
		if (sessionManager == null) {
			MessageDialog waitDialog = new MessageDialog(
					window.getShell(),
					"Disabled Action Selected",
					null, // accept the default window icon
					"You cannot import data into an accounting session unless you have a session open.  You must first open a session or create a new session.",
					MessageDialog.INFORMATION,
					new String[] { IDialogConstants.OK_LABEL }, 0);
			waitDialog.open();
			return;
		}

		/*
		 * Create a transaction to be used to import the entries.  This allows the entries to
		 * be more efficiently written to the back-end datastore and it also groups
		 * the entire import as a single change for undo/redo purposes.
		 */
		transactionManager = new TransactionManager(sessionManager);
		session = transactionManager.getSession();
		
		/*
		 * Find the Paypal account. Currently this assumes that there is
		 * only one Paypal account. If you have more than one then you will
		 * need to implement some changes, perhaps require the account be
		 * selected before the wizard is run, or add a wizard page that asks
		 * which of the Paypal accounts is to be used.
		 */
		PaypalAccount accountOutside = null;
		for (Iterator<CapitalAccount> iter = sessionManager.getSession().getCapitalAccountIterator(); iter.hasNext(); ) {
			CapitalAccount eachAccount = iter.next();
			if (eachAccount instanceof PaypalAccount) {
				if (accountOutside != null) {
					MessageDialog.openError(window.getShell(), "Problem", "Multiple Paypal accounts.  Don't know which to use.  If you have multiple Paypal accounts, please submit a patch.");
					return;
				}	
				accountOutside = (PaypalAccount)eachAccount;
			}
		}
		
		if (accountOutside == null) {
			MessageDialog.openError(window.getShell(), "Problem", "No Paypal account has been created");
			return;
		}

		paypalAccount = transactionManager.getCopyInTransaction(accountOutside);
		
		filePage = new CsvImportWizardPage(window);
		addPage(filePage);
	}

	@Override
	public boolean performFinish() {
		String fileName = filePage.getFileName();
		if (fileName != null) {
			File csvFile = new File(fileName);
			importFile(csvFile);
		}

		return true;
	}


	public void importFile(File file) {

		try {
			CSVReader reader = new CSVReader(new FileReader(file));
			
			Transaction trans = null;
			
			ShoppingCart cart = null;
	
			Collection<Refund> refunds = new ArrayList<Refund>();
			Collection<Refund> reversals = new ArrayList<Refund>();

			// Pass the header
			reader.readNext();
			
			String [] nextLine = reader.readNext();
			while (nextLine != null && (nextLine.length == 42 || nextLine.length == 43)) {
				
				String dateString = nextLine[0];
				String payeeName = nextLine[3];
				String type = nextLine[4];
				String status = nextLine[5];
				Long grossAmount = getAmount(nextLine[6]);
				Long fee = getAmount(nextLine[7]);
				Long netAmount = getAmount(nextLine[8]);
				String merchantEmail = nextLine[10];
				//String transactionId = nextLine[11];
				String memo = nextLine[14];
				long shippingAndHandlingAmount = getAmount(nextLine[16]);
				long insurance = getAmount(nextLine[17]);
				long salesTax = getAmount(nextLine[18]);
				String itemUrlString = nextLine[25];
				String quantityString = nextLine[32];
				//Long balance = getAmount(nextLine[34]);

		        DateFormat df = new SimpleDateFormat("MM/dd/yyyy");
		        Date date;
				try {
					date = df.parse(dateString);
				} catch (ParseException e) {
					throw new UnexpectedDataException("Date", dateString);
				}

				if (cart != null && 
						!type.equals("Shopping Cart Item")) {
					cart.createTransaction();
					cart = null;
				}
				
		        if (type.equals("Shopping Cart Payment Sent")) {
		        	cart = new ShoppingCart(date, grossAmount, payeeName + " / " + memo, merchantEmail, shippingAndHandlingAmount);
		        	
		        	/*
		        	 * Look for a refunds that match.  Move them into the cart so they can
		        	 * be processed as part of the same transaction.
		        	 */
		        	if (status.equals("Partially Refunded")) {
		        		for (Iterator<Refund> iter = refunds.iterator(); iter.hasNext(); ) {
		        			Refund refund = iter.next();
		        			if (refund.payeeName.equals(payeeName)) {
		        				// Move refund into cart 
		        				cart.addRefund(refund);
		        				iter.remove();
		        			}
		        		}
		        	}
		        } else if (type.equals("Shopping Cart Item")) {
		        	if (cart == null) {
		        		throw new UnexpectedDataException("'Shopping Cart Item' row found but it is not preceeded by a 'Shopping Cart Payment Sent' or 'eBay Payment Sent' row.");
		        	}
		        	
		        	// Add a split
		        	cart.addSplit(grossAmount, memo, quantityString, itemUrlString, shippingAndHandlingAmount);
		        } else if (type.equals("Refund")) {
		        	/*
		        	 * Refunds are combined with the original transaction.
		        	 * Because the input file is in reverse date order, we find
		        	 * the refund first.  We save the refund information in a collection.
		        	 * Whenever a 'Shopping Cart Payment Sent' or a 'eBayPaymentSent' or 'Express Checkout Payment Sent'
		        	 * Status of 'Partially Refunded' or 'Refunded'
		        	 * Name exactly matches
		        	 */
		        	refunds.add(new Refund(date, grossAmount, payeeName));
		        } else if (type.equals("Reversal")) {
		        	/*
		        	 * Reversals are combined with the original transaction.
		        	 * Because the input file is in reverse date order, we find
		        	 * the refund first.  We save the refund information in a collection.
		        	 * Whenever a 'Payment Sent'
		        	 * Status of 'Reversed'
		        	 * Name exactly matches
		        	 */
		        	reversals.add(new Refund(date, grossAmount, payeeName));
		        } else if (type.equals("eBay Payment Sent")
		        		|| type.equals("eBay Payment Received")
		        		|| type.equals("Payment Received")
		        		|| type.equals("Payment Sent")) {

		        	if (status.equals("Reversed")) {
		        		/*
		        		 * Find the reversal entry.  We don't create anything if an
		        		 * entry was reversed. 
		        		 */
		        		Refund match = null;
		        		for (Refund refund : reversals) {
		        			if (refund.payeeName.equals(payeeName)
		        					&& refund.grossAmount == -grossAmount) {
		        				match = refund;
		        				break;
		        			}
		        		}
		        		if (match == null) {
		        			throw new UnexpectedDataException("An entry was found that says it was reversed, but no matching 'Reversal' entry was found.");
		        		}
		        		reversals.remove(match);
		        	} else {
		        		if (type.equals("eBay Payment Sent")) {
		        			// This one might be itemized.
		        			if (grossAmount == 999) {
		        				System.out.println("");
		        			}
		        					        			cart = new ShoppingCart(date, grossAmount, payeeName + " / " + memo, merchantEmail, shippingAndHandlingAmount);
		        		} else {
		        			trans = session.createTransaction();
		        			trans.setDate(date);

		        			PaypalEntry mainEntry = trans.createEntry().getExtension(PaypalEntryInfo.getPropertySet(), true);
		        			mainEntry.setAccount(paypalAccount);
		        			mainEntry.setAmount(netAmount);
		        			mainEntry.setMemo(payeeName);
		        			mainEntry.setMerchantEmail(merchantEmail);

		        			PaypalEntry lineItemEntry = trans.createEntry().getExtension(PaypalEntryInfo.getPropertySet(), true);
		        			// TODO: Support pattern matching
		        			lineItemEntry.setAccount(paypalAccount.getSaleAndPurchaseAccount());
		        			lineItemEntry.setAmount(netAmount);

		        			String combinedMemo = memo; 
		        			if (quantityString.length() != 0) {
		        				combinedMemo = combinedMemo + " x" + quantityString;
		        			}
		        			if (fee != 0) {
		        				// For these types, treat the Paypal fee is a bank service
		        				// charge.  For E-bay sales, absorb in proceeds?

		        				if (paypalAccount.getPaypalFeesAccount() == null) {
		        					throw new UnexpectedDataException("A Paypal fee has been found in the imported data.  However, no category has been configured in the properties for this Paypal account for such fees.");
		        				}

		        				if (type.equals("Payment Received")
		        						|| type.equals("Payment Sent")) {
		        					Entry feeEntry = trans.createEntry();
		        					feeEntry.setAccount(paypalAccount.getPaypalFeesAccount());
		        					feeEntry.setAmount(fee);
		        					feeEntry.setMemo("Paypal");
		        				} else {
		        					combinedMemo = combinedMemo + " (Paypal fee $" + nextLine[7] + ")";
		        				}
		        			}
		        			if (shippingAndHandlingAmount != 0) {
		        				combinedMemo = combinedMemo + " (s&h $" + nextLine[16] + ")";
		        			}
		        			if (insurance != 0) {
		        				combinedMemo = combinedMemo + " (insurance $" + nextLine[17] + ")";
		        			}
		        			if (salesTax != 0) {
		        				combinedMemo = combinedMemo + " (tax $" + nextLine[18] + ")";
		        			}
		        			lineItemEntry.setMemo(combinedMemo);

		        			if (itemUrlString.length() != 0) {
		        				URL itemUrl = new URL(itemUrlString);
		        				lineItemEntry.setItemUrl(itemUrl);
		        			}
		        		}
		        	}
		        } else if (type.equals("Donation Sent")) {
		        	if (paypalAccount.getDonationAccount() == null) {
		        		throw new UnexpectedDataException("A donation has been found in the imported data.  However, no category was set for donations.  Please go to the Paypal account properties and select a category to be used for donations.");
		        	}
		        	createTransaction(date, grossAmount, "donation sent", paypalAccount.getDonationAccount(), "transfer to Paypal");
		        } else if (type.equals("Web Accept Payment Sent")) {
		        } else if (type.equals("Add Funds from a Bank Account")) {
		        	if (paypalAccount.getTransferBank() == null) {
		        		throw new UnexpectedDataException("A bank account transfer has been found in the imported data.  However, no bank account has been set in the properties for this Paypal account.");
		        	}
		        	createTransaction(date, grossAmount, "transfer from bank", paypalAccount.getTransferBank(), "transfer to Paypal");
		        } else if (type.equals("Update to eCheck Sent")) {
		        	// Updates do not involve a financial transaction
		        	// so nothing to import.
		        } else if (type.equals("Update to eCheck Received")) {
		        	// Updates do not involve a financial transaction
		        	// so nothing to import.
		        } else if (type.equals("Update to Payment Received")) {
		        	// Updates do not involve a financial transaction
		        	// so nothing to import.
		        } else if (type.equals("eCheck Sent")) {
		        	if (paypalAccount.getSaleAndPurchaseAccount() == null) {
		        		throw new UnexpectedDataException("An eCheck entry has been found in the imported data.  However, no sale and purchase account has been set in the properties for this Paypal account.");
		        	}
		        	createTransaction(date, grossAmount, "payment by transfer", paypalAccount.getSaleAndPurchaseAccount(), "transfer from Paypal");
		        } else if (type.equals("Express Checkout Payment Sent")) {
		        	if (paypalAccount.getSaleAndPurchaseAccount() == null) {
		        		throw new UnexpectedDataException("An 'Express Checkout' entry has been found in the imported data.  However, no sale and purchase account has been set in the properties for this Paypal account.");
		        	}
		        	
		        	if (status.equals("Refunded")) {
		        		/*
		        		 * Find the refund entry.  We create a single transaction with two entries both
		        		 * in this Paypal account. 
		        		 */
		        		Refund match = null;
		        		for (Refund refund : refunds) {
		        			if (refund.payeeName.equals(payeeName)
		        					&& refund.grossAmount == -grossAmount) {
		        				match = refund;
		        				break;
		        			}
		        		}
		        		if (match == null) {
		        			throw new UnexpectedDataException("An entry was found that says it was refunded, but no matching 'Refund' entry was found.");
		        		}
		        		refunds.remove(match);

		        		createTransaction(date, grossAmount, payeeName, match.date, paypalAccount, payeeName + " - refund");
		        	} else {
			        	createTransaction(date, grossAmount, payeeName, paypalAccount.getSaleAndPurchaseAccount(), payeeName + " - Paypal payment");
		        	}
		        } else if (type.equals("Charge From Credit Card")) {
		        	if (paypalAccount.getTransferCreditCard() == null) {
		        		throw new UnexpectedDataException("A credit card charge has been found in the imported data.  However, no credit card account has been set in the properties for this Paypal account.");
		        	}
		        	createTransaction(date, grossAmount, "payment from credit card", paypalAccount.getTransferCreditCard(), "transfer to Paypal");
		        } else if (type.equals("Credit to Credit Card")) {
		        	if (paypalAccount.getTransferCreditCard() == null) {
		        		throw new UnexpectedDataException("A credit card refund has been found in the imported data.  However, no credit card account has been set in the properties for this Paypal account.");
		        	}
		        	createTransaction(date, -grossAmount, "refund to credit card", paypalAccount.getTransferCreditCard(), "refund from Paypal");
		        } else {
//		        	throw new UnexpectedData("type", type);
					MessageDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Unable to read CSV file", "Entry found with unknown type: '" + type + "'.");
		        }
				
		        nextLine = reader.readNext();
		    }
			
			assert (nextLine.length == 0);

			/*
			 * All entries have been imported and all the properties
			 * have been set and should be in a valid state, so we
			 * can now commit the imported entries to the datastore.
			 */
			transactionManager.commit("Import Paypal " + file.getName());									

		} catch (UnexpectedDataException e) {
    		MessageDialog.openError(window.getShell(), "Import Failed", e.getLocalizedMessage());
		} catch (IOException e) {
    		MessageDialog.openError(window.getShell(), "Import Failed", "A file I/O error occurred. " + e.getLocalizedMessage());
		}
	}

	private void createTransaction(Date date, Long grossAmount, String paypalAccountMemo, Account otherAccount, String otherAccountMemo) {
		createTransaction(date, grossAmount, otherAccountMemo, null, otherAccount, otherAccountMemo);
	}

	/**
	 * This is a helper method that creates a transaction where there is just one other entry
	 * other in addition to the Paypal account entry and where there is nothing other than
	 * the amount and memo to be set.
	 * 
	 * @param date
	 * @param grossAmount
	 * @param paypalAccountMemo
	 * @param otherAccount
	 * @param otherAccountMemo
	 */
	private void createTransaction(Date date, Long grossAmount, String paypalAccountMemo, Date otherEntryValueDate, Account otherAccount, String otherAccountMemo) {
		Transaction trans = session.createTransaction();
		trans.setDate(date);
		
		PaypalEntry mainEntry = trans.createEntry().getExtension(PaypalEntryInfo.getPropertySet(), true);
		mainEntry.setAccount(paypalAccount);
		mainEntry.setAmount(grossAmount);
		mainEntry.setMemo(paypalAccountMemo);
		
		Entry otherEntry = trans.createEntry();
		otherEntry.setAccount(otherAccount);
		otherEntry.setAmount(-grossAmount);
		otherEntry.setMemo(otherAccountMemo);
		if (otherEntryValueDate != null) {
			otherEntry.setValuta(otherEntryValueDate);
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
			amount += Long.parseLong(parts[1]);
		}
		return negate ? -amount : amount;
		} catch (NumberFormatException e) {
			return 0;
		}
	}
	
	/**
	 * Shopping cart entries are split across multiple rows, with a 'Payment Sent' row
	 * following by one or more 'Item' rows.  These must be combined into a single
	 * transaction.  To enable us to do this, this class is used to put each row into,
	 * and it can then output the transaction when a row is found that is in the
	 * next transaction.
	 */
	public class ShoppingCart {

		private Transaction trans;
		private PaypalEntry mainEntry;
		private long shippingAndHandlingAmount;

		private Collection<Refund> refunds = new ArrayList<Refund>();
		
		public ShoppingCart(Date date, Long grossAmount, String memo, String merchantEmail, long shippingAndHandlingAmount) {
        	// Start a new transaction
        	trans = session.createTransaction();
        	trans.setDate(date);
        	
        	mainEntry = trans.createEntry().getExtension(PaypalEntryInfo.getPropertySet(), true);
        	mainEntry.setAccount(paypalAccount);
        	mainEntry.setAmount(grossAmount);
        	mainEntry.setMemo(memo);
        	mainEntry.setMerchantEmail(merchantEmail);

        	this.shippingAndHandlingAmount = shippingAndHandlingAmount;
		}

		public void addRefund(Refund refund) {
			refunds.add(refund);
		}

		public void createTransaction() {
			/*
			 * We distribute the shipping and handling among the items in proportion
			 * to the price of each item.  This is the preference of the author.
			 * If this is not your preference then please add a preference to the preferences
			 * to indicate if a separate line item should instead be created for the
			 * shipping and handling and implement it.
			 */
			long toDistribute = shippingAndHandlingAmount;
			long netTotal = - mainEntry.getAmount() - shippingAndHandlingAmount;
			for (Entry entry : trans.getEntryCollection()) {
				if (entry != mainEntry.getBaseObject()) {
					long amount = shippingAndHandlingAmount * entry.getAmount() / netTotal;
					entry.setAmount(entry.getAmount() + amount);
					toDistribute -= amount;
				}
			}
			
			// We have rounded down, so we may be under.  We now distribute
			// a penny to each to get a balanced transaction.
			for (Entry entry : trans.getEntryCollection()) {
				if (entry != mainEntry.getBaseObject()) {
					if (toDistribute > 0) {
						entry.setAmount(entry.getAmount() + 1);
						toDistribute--;
					}
				}
			}
				
			assert(toDistribute == 0);	
		}

		public void addSplit(Long grossAmount,
				String memo, String quantityString, String itemUrlString, long shippingAndHandlingAmount) {
			
			if (shippingAndHandlingAmount != this.shippingAndHandlingAmount) {
				// TODO report an error
			}
			
        	PaypalEntry lineItemEntry = trans.createEntry().getExtension(PaypalEntryInfo.getPropertySet(), true);
        	// TODO: Support pattern matching
        	lineItemEntry.setAccount(paypalAccount.getSaleAndPurchaseAccount());
        	lineItemEntry.setAmount(grossAmount);
        	lineItemEntry.setMemo(memo + " x" + quantityString);
        	
        	if (itemUrlString.length() != 0) {
				try {
					URL itemUrl = new URL(itemUrlString);
	        		lineItemEntry.setItemUrl(itemUrl);
				} catch (MalformedURLException e) {
					// Leave the URL blank
				}
        	}
		}

	}

	/*
	 * A refund or reversal that has not yet been matched to the original transaction.
	 */
	class Refund {

		Date date;
		Long grossAmount;
		String payeeName;
		
		public Refund(Date date, Long grossAmount, String payeeName) {
			this.date = date;
			this.grossAmount = grossAmount;
			this.payeeName = payeeName;
		}
		
	}
}