/*
*
*  JMoney - A Personal Finance Manager
*  Copyright (c) 2010 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.importer;

import net.sf.jmoney.importer.model.ReconciliationAccountInfo;
import net.sf.jmoney.importer.resources.Messages;
import net.sf.jmoney.importer.wizards.IAccountImportWizard;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.views.AccountEditor;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

public class CsvImportHandler extends AbstractHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		Shell shell = HandlerUtil.getActiveShellChecked(event);
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		IEditorPart editor = HandlerUtil.getActiveEditorChecked(event);

		AccountEditor accountEditor = (AccountEditor)editor;
		Account account = accountEditor.getAccount();

		/*
		 * The importDataExtensionId property in the account is an id for a configuration element
		 * in plugin.xml.  This configuration element in turn gives us the id of a suitable import
		 * wizard.  We start the given wizard, first setting the account into it.
		 */
		String importDataExtensionId = account.getPropertyValue(ReconciliationAccountInfo.getImportDataExtensionIdAccessor());
		if (importDataExtensionId == null) {
			MessageDialog.openError(shell, "Import not Available", Messages.bind(Messages.Error_AccountNotConfigured, account.getName()));
			return null;
		}

		// Find the wizard by reading the registry and start it.
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		for (IConfigurationElement element: registry.getConfigurationElementsFor("net.sf.jmoney.importer.importdata")) { //$NON-NLS-1$ $NON-NLS-2$
			if (element.getName().equals("import-format") //$NON-NLS-1$
					&& element.getAttribute("id").equals(importDataExtensionId)) { //$NON-NLS-1$
				try {
					Object executableExtension = element.createExecutableExtension("class"); //$NON-NLS-1$
					IAccountImportWizard wizard = (IAccountImportWizard)executableExtension;
					wizard.init(window, account);
					WizardDialog dialog = new WizardDialog(shell, wizard);
					dialog.setPageSize(600, 300);
					dialog.open();

					return null;
				} catch (CoreException e) {
					throw new ExecutionException("Cannot create import wizard for " + account.getName() + ".", e);
				}
			}
		}

		return null;
	}
}