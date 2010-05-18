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

import net.sf.jmoney.importer.Activator;
import net.sf.jmoney.isolation.TransactionManager;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.Session;

import org.eclipse.jface.dialogs.IDialogSettings;
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

	private Account accountOutsideTransaction;

	private CsvImportWizardPage mainPage;

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
			
			Account accountInsideTransaction = transactionManager.getCopyInTransaction(accountOutsideTransaction);
			setAccount(accountInsideTransaction);
			
        	CSVReader reader = new CSVReader(new FileReader(file));
			
			// Pass the header
			reader.readNext();
			
			String [] nextLine = reader.readNext();
			while (nextLine != null && nextLine.length == 12) {
				importLine(nextLine);
		        
		        nextLine = reader.readNext();
		    }
			
			assert (nextLine.length == 1);
			assert (nextLine[0].equals("***END OF FILE***"));

			/*
			 * All entries have been imported and all the properties
			 * have been set and should be in a valid state, so we
			 * can now commit the imported entries to the datastore.
			 */
			String transactionDescription = String.format("Import Ameritrade {0}", file.getName());
			transactionManager.commit(transactionDescription);									

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ImportException e) {
			// Bad date
			e.printStackTrace();
		}
	}
	
	protected abstract void setAccount(Account accountInsideTransaction) throws ImportException;

	protected abstract void importLine(String[] line) throws ImportException;
}