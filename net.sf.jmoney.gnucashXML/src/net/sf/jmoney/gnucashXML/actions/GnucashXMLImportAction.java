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

package net.sf.jmoney.gnucashXML.actions;

import java.io.File;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.gnucashXML.*;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

/**
 * Our sample action implements workbench action delegate.
 * The action proxy will be created by the workbench and
 * shown in the UI. When the user tries to use the action,
 * this delegate will be created and execution will be 
 * delegated to it.
 * @see IWorkbenchWindowActionDelegate
 */
public class GnucashXMLImportAction implements IWorkbenchWindowActionDelegate {
	private IWorkbenchWindow window;
	/**
	 * The constructor.
	 */
	public GnucashXMLImportAction() {
	}

	/**
	 * The action has been activated. The argument of the
	 * method represents the 'real' action sitting
	 * in the workbench UI.
	 * @see IWorkbenchWindowActionDelegate#run
	 */
	public void run(IAction action) {
		Session session = JMoneyPlugin.getDefault().getSession(); 

		// Original JMoney disabled the import menu items when no
		// session was open.  I don't know how to do that in Eclipse,
		// so we display a message instead.
		if (session == null) {
	        MessageDialog waitDialog =
				new MessageDialog(
						window.getShell(), 
						"Disabled Action Selected", 
						null, // accept the default window icon
						"You cannot import data into an accounting session unless you have a session open.  You must first open a session or create a new session.", 
						MessageDialog.INFORMATION, 
						new String[] { IDialogConstants.OK_LABEL }, 0);
	        waitDialog.open();
			return;
		}
		
	    FileDialog xmlFileChooser = new FileDialog(window.getShell());
		xmlFileChooser.setText(
				GnucashXMLPlugin.getResourceString("MainFrame.import"));
		xmlFileChooser.setFilterExtensions(new String[] { "*.xml;*.xac" });
		xmlFileChooser.setFilterNames(new String[] { "XML Gnucash Files (*.xml; *.xac)" });
		// TODO: Faucheux - delete Directory 
		xmlFileChooser.setFilterPath("D:\\Program Files\\Eclipse\\eclipse\\workspace\\ReadGnucashFile");
		String fileName = xmlFileChooser.open();

	    if (fileName != null) {
	        File qifFile = new File(fileName);
	        GnucashXML gnucashXML = new GnucashXML(window);
			gnucashXML.importFile(session, qifFile);
		}
	}

	/**
	 * Selection in the workbench has been changed. We 
	 * can change the state of the 'real' action here
	 * if we want, but this can only happen after 
	 * the delegate has been created.
	 * @see IWorkbenchWindowActionDelegate#selectionChanged
	 */
	public void selectionChanged(IAction action, ISelection selection) {
	}

	/**
	 * We can use this method to dispose of any system
	 * resources we previously allocated.
	 * @see IWorkbenchWindowActionDelegate#dispose
	 */
	public void dispose() {
	}

	/**
	 * We will cache window object in order to
	 * be able to provide parent shell for the message dialog.
	 * @see IWorkbenchWindowActionDelegate#init
	 */
	public void init(IWorkbenchWindow window) {
		this.window = window;
	}
}