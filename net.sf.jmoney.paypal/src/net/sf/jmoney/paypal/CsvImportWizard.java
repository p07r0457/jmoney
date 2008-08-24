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
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.isolation.TransactionManager;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.Transaction;
import net.sf.jmoney.model2.Session.NoAccountFoundException;
import net.sf.jmoney.model2.Session.SeveralAccountsFoundException;

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

	private Session session;

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

		session = JMoneyPlugin.getDefault().getSession();
		
		// Original JMoney disabled the import menu items when no
		// session was open. I don't know how to do that in Eclipse,
		// so we display a message instead.
		if (session == null) {
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
			/*
			 * Create a transaction to be used to import the entries.  This allows the entries to
			 * be more efficiently written to the back-end datastore and it also groups
			 * the entire import as a single change for undo/redo purposes.
			 */
			TransactionManager transactionManager = new TransactionManager(session.getDataManager());
			Session sessionInTransaction = transactionManager.getSession();
			
			
			
			// Find the appropriate accounts.
			// These should probably be in preferences rather than forcing
			// the account names to these hard coded values.

			PaypalAccount accountOutside = null;
			for (Iterator<CapitalAccount> iter = session.getCapitalAccountIterator(); iter.hasNext(); ) {
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

			PaypalAccount paypalAccount = transactionManager.getCopyInTransaction(accountOutside);
			
//			BankAccount bankAccountOutside = (BankAccount)session.getAccountByShortName("Meg - Bank of America 6303");
//			BankAccount bankAccount = transactionManager.getCopyInTransaction(bankAccountOutside);
//			
//			BankAccount creditCardAccountOutside = (BankAccount)session.getAccountByShortName("Meg - credit card 1775");
//			BankAccount creditCardAccount = transactionManager.getCopyInTransaction(creditCardAccountOutside);

			Account unreconciledAccountOutside = session.getAccountByShortName("unreconciled - Paypal");
			Account unreconciledAccount = transactionManager.getCopyInTransaction(unreconciledAccountOutside);
			
			CSVReader reader = new CSVReader(new FileReader(file));
			
			Transaction trans = null;
			
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
				String transactionId = nextLine[11];
				String memo = nextLine[14];
				long shippingAndHandlingAmount = getAmount(nextLine[16]);
				long insurance = getAmount(nextLine[17]);
				long salesTax = getAmount(nextLine[18]);
				String itemUrlString = nextLine[25];
				String quantityString = nextLine[32];
				Long balance = getAmount(nextLine[34]);

		        DateFormat df = new SimpleDateFormat("MM/dd/yyyy");
		        Date date;
				try {
					date = df.parse(dateString);
				} catch (ParseException e) {
					throw new UnexpectedDataException("Date", dateString);
				}
		        
		        if (type.equals("Shopping Cart Payment Sent")) {
		        	// Start a new transaction
		        	trans = sessionInTransaction.createTransaction();
		        	trans.setDate(date);
		        	
		        	PaypalEntry mainEntry = trans.createEntry().getExtension(PaypalEntryInfo.getPropertySet(), true);
		        	mainEntry.setAccount(paypalAccount);
		        	mainEntry.setAmount(grossAmount);
		        	mainEntry.setMemo(payeeName + " / " + memo);
		        	mainEntry.setMerchantEmail(merchantEmail);
		        	
		        	
		        } else if (type.equals("Shopping Cart Item")) {
		        	// Add a split
		        	
		        	PaypalEntry lineItemEntry = trans.createEntry().getExtension(PaypalEntryInfo.getPropertySet(), true);
		        	// TODO: Support pattern matching
		        	lineItemEntry.setAccount(unreconciledAccount);
		        	lineItemEntry.setAmount(grossAmount);
		        	lineItemEntry.setMemo(memo + " x" + quantityString);
		        	
		        	if (itemUrlString.length() != 0) {
		        		URL itemUrl = new URL(itemUrlString);
		        		lineItemEntry.setItemUrl(itemUrl);
		        	}
		        } else if (type.equals("Refund")) {
		        } else if (type.equals("Reversal")) {
		        } else if (type.equals("eBay Payment Sent")
		         || type.equals("eBay Payment Received")
		         || type.equals("Payment Received")
		         || type.equals("Payment Sent")) {
		        	trans = sessionInTransaction.createTransaction();
		        	trans.setDate(date);
		        	
		        	PaypalEntry mainEntry = trans.createEntry().getExtension(PaypalEntryInfo.getPropertySet(), true);
		        	mainEntry.setAccount(paypalAccount);
		        	mainEntry.setAmount(netAmount);
		        	mainEntry.setMemo(payeeName);
		        	mainEntry.setMerchantEmail(merchantEmail);
		        	
		        	PaypalEntry lineItemEntry = trans.createEntry().getExtension(PaypalEntryInfo.getPropertySet(), true);
		        	// TODO: Support pattern matching
		        	lineItemEntry.setAccount(unreconciledAccount);
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
		        } else if (type.equals("Donation Sent")) {
		        } else if (type.equals("Web Accept Payment Sent")) {
		        } else if (type.equals("Add Funds from a Bank Account")) {
		        	
		        	if (paypalAccount.getTransferBank() == null) {
		        		throw new UnexpectedDataException("A bank account transfer has been found in the imported data.  However, no bank account has been set in the properties for this Paypal account.");
		        	}
		        	
		        	trans = sessionInTransaction.createTransaction();
		        	trans.setDate(date);
		        	
		        	PaypalEntry mainEntry = trans.createEntry().getExtension(PaypalEntryInfo.getPropertySet(), true);
		        	mainEntry.setAccount(paypalAccount);
		        	mainEntry.setAmount(grossAmount);
		        	mainEntry.setMemo("transfer from bank");
		        	
		        	PaypalEntry bankEntry = trans.createEntry().getExtension(PaypalEntryInfo.getPropertySet(), true);
		        	bankEntry.setAccount(paypalAccount.getTransferBank());
		        	bankEntry.setAmount(-grossAmount);
		        	bankEntry.setMemo("transfer to Paypal");
		        } else if (type.equals("Update to eCheck Sent")) {
		        } else if (type.equals("eCheck Sent")) {
		        } else if (type.equals("Update to eCheck Received")) {
		        } else if (type.equals("Update to Payment Received")) {
		        } else if (type.equals("Express Checkout Payment Sent")) {
		        } else if (type.equals("Charge From Credit Card")) {

		        	if (paypalAccount.getTransferCreditCard() == null) {
		        		throw new UnexpectedDataException("A credit charge charge has been found in the imported data.  However, no credit card account has been set in the properties for this Paypal account.");
		        	}
		        	
		        	trans = sessionInTransaction.createTransaction();
		        	trans.setDate(date);
		        	
		        	PaypalEntry mainEntry = trans.createEntry().getExtension(PaypalEntryInfo.getPropertySet(), true);
		        	mainEntry.setAccount(paypalAccount);
		        	mainEntry.setAmount(grossAmount);
		        	mainEntry.setMemo("payment from credit card");
//		        	mainEntry.setUniqueId(transactionId);
		        	
		        	PaypalEntry bankEntry = trans.createEntry().getExtension(PaypalEntryInfo.getPropertySet(), true);
		        	bankEntry.setAccount(paypalAccount.getTransferCreditCard());
		        	bankEntry.setAmount(-grossAmount);
		        	bankEntry.setMemo("transfer to Paypal");
		        } else if (type.equals("Credit to Credit Card")) {
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
		} catch (SeveralAccountsFoundException e) {
			// TODO Remove this when accounts set up properly
			e.printStackTrace();
		} catch (NoAccountFoundException e) {
			// TODO Remove this when accounts set up properly
			e.printStackTrace();
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
}