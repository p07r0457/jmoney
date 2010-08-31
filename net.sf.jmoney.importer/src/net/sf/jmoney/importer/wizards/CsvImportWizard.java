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

package net.sf.jmoney.importer.wizards;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Date;

import net.sf.jmoney.importer.Activator;
import net.sf.jmoney.importer.model.AccountAssociation;
import net.sf.jmoney.importer.model.ReconciliationAccount;
import net.sf.jmoney.importer.model.ReconciliationAccountInfo;
import net.sf.jmoney.isolation.TransactionManager;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.Session;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IWorkbenchWindow;

import au.com.bytecode.opencsv.CSVReader;

/**
 * A wizard to import data from a comma-separated file that has been down-loaded
 * into a file on the local machine.
 * <P>
 * This wizard is a single page wizard that asks only for the file.
 */
public abstract class CsvImportWizard extends Wizard implements IAccountImportWizard {

	private IWorkbenchWindow window;
	
	private Account accountOutsideTransaction;

	private CsvImportWizardPage mainPage;

	/**
	 * The line currently being processed by this wizard, being valid only while the import is
	 * processing after the 'finish' button is pressed
	 */
	private String [] currentLine;

	/**
	 * Set when <code>importFile</code> is called.
	 */
	private Account accountInsideTransaction;

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
	@Override
	public void init(IWorkbenchWindow window, Account account) {
		this.window = window;
		this.accountOutsideTransaction = account;

		mainPage = new CsvImportWizardPage(window);
		addPage(mainPage);
	}

	@Override
	public boolean performFinish() {
		String fileName = mainPage.getFileName();
		if (fileName != null) {
			File csvFile = new File(fileName);
			importFile(csvFile);
		}

		return true;
	}


	public void importFile(File file) {

		Session session = accountOutsideTransaction.getSession();
		
		try {
			/*
			 * Create a transaction to be used to import the entries.  This allows the entries to
			 * be more efficiently written to the back-end datastore and it also groups
			 * the entire import as a single change for undo/redo purposes.
			 */
			TransactionManager transactionManager = new TransactionManager(session.getDataManager());
			
			// Find the appropriate accounts.
			// These should probably be in preferences rather than forcing
			// the account names to these hard coded values.
			
			accountInsideTransaction = transactionManager.getCopyInTransaction(accountOutsideTransaction);
			setAccount(accountInsideTransaction);
			
        	CSVReader reader = new CSVReader(new FileReader(file));

    		/*
    		 * Get the list of expected columns, validate the header row, and set the column indexes
    		 * into the column objects.  It would be possible to allow the columns to be in any order or
    		 * to allow columns to be optional, setting the column indexes here based on the column in
    		 * which the matching header was found.
    		 * 
    		 * At this time, however, there is no known requirement for that, so we simply validate that
    		 * the first row contains exactly these columns in this order and set the indexes sequentially.
    		 */
			String headerRow[] = reader.readNext();
    		ImportedColumn[] expectedColumns = getExpectedColumns();
    		for (int columnIndex = 0; columnIndex < expectedColumns.length; columnIndex++) {
    			if (!headerRow[columnIndex].equals(expectedColumns[columnIndex].getName())) {
    				MessageDialog.openError(getShell(), "Unexpected Data", "Expected '" + expectedColumns[columnIndex].getName()
    						+ "' in row 1, column " + columnIndex + " but found '" + headerRow[columnIndex] + "'.");
    				return;
    			}
    			
    			expectedColumns[columnIndex].setColumnIndex(columnIndex);
    		}

			/*
			 * Read the data
			 */
			
			currentLine = reader.readNext();
			while (currentLine != null && currentLine.length == expectedColumns.length) {
				importLine(currentLine);
		        
		        currentLine = reader.readNext();
		    }
			
			if (currentLine != null) {
				// Ameritrade contains this.
				assert (currentLine.length == 1);
				assert (currentLine[0].equals("***END OF FILE***"));
			}

			/*
			 * All entries have been imported and all the properties
			 * have been set and should be in a valid state, so we
			 * can now commit the imported entries to the datastore.
			 */
			String transactionDescription = String.format("Import {0} {1}", getSourceLabel(), file.getName());
			transactionManager.commit(transactionDescription);									

		} catch (FileNotFoundException e) {
			// This should not happen because the file dialog only allows selection of existing files.
			throw new RuntimeException(e);
		} catch (IOException e) {
			// This is probably not likely to happen so the default error handling is adequate.
			throw new RuntimeException(e);
		} catch (ImportException e) {
			// There is data in the import file that we are unable to process
			MessageDialog.openError(window.getShell(), "Errors in the downloaded file", e.getMessage());
		}
	}
	
	/**
	 * Given an id for an account association, returns the account that is associated with the
	 * account into which we are importing.
	 * <P>
	 * This account is inside the transaction.
	 * 
	 * @param id
	 * @return
	 */
	protected Account getAssociatedAccount(String id) {
		ReconciliationAccount a = accountInsideTransaction.getExtension(ReconciliationAccountInfo.getPropertySet(), false);
		if (a != null) {
			for (AccountAssociation aa : a.getAssociationCollection()) {
				if (aa.getId().equals(id)) {
					return aa.getAccount();
				}
			}
		}
		return null;
	}

	/**
	 * This method returns a label that describes the source and is suitable for use
	 * in labels and messages shown to the user.  This will typically be the name of the
	 * bank or brokerage firm.
	 */
	protected abstract String getSourceLabel();

	public abstract class ImportedColumn {
		
		/**
		 * Text in the header row that identifies this column
		 */
		private String name;
		
		protected int columnIndex;

		ImportedColumn(String name) {
			this.name = name;
		}
		
		/**
		 * @return text in the header row that identifies this column
		 */
		public String getName() {
			return name;
		}

		public void setColumnIndex(int columnIndex) {
			this.columnIndex = columnIndex;
		}
	}

	public class ImportedTextColumn extends ImportedColumn {

		public ImportedTextColumn(String name) {
			super(name);
		}

		public String getText() {
			return currentLine[columnIndex];
		}
	}

	public class ImportedDateColumn extends ImportedColumn {

		private DateFormat dateFormat;
		
		public ImportedDateColumn(String name, DateFormat dateFormat) {
			super(name);
			this.dateFormat = dateFormat;
		}
		
		public Date getDate() throws ImportException {
			try {
				return dateFormat.parse(currentLine[columnIndex]);
			} catch (ParseException e) {
				throw new ImportException(
						MessageFormat.format(
								"A date in format d/M/yyyy was expected but '{0}' was found.", 
								currentLine[columnIndex]), 
						e);
			}
		}
	}

	public class ImportedAmountColumn extends ImportedColumn {

		public ImportedAmountColumn(String name) {
			super(name);
		}
		
		public long getAmount() throws ImportException {
			String amountString = currentLine[columnIndex];
			
			if (amountString.trim().length() == 0) {
				// Amount cannot be blank
				throw new ImportException("Amount cannot be blank.");
			}
			
			// remove any commas
			amountString = amountString.replace(",", "");
			
			String parts [] = amountString.split("\\.");
			long amount = Long.parseLong(parts[0]) * 100;
			if (parts.length > 1) {
				amount += Long.parseLong(parts[1]);
			}
			return amount;
		}
	}

	protected abstract void setAccount(Account accountInsideTransaction) throws ImportException;

	protected abstract void importLine(String[] line) throws ImportException;

	protected abstract ImportedColumn[] getExpectedColumns();
}