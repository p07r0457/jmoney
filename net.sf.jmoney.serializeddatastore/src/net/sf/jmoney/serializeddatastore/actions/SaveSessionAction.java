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

package net.sf.jmoney.serializeddatastore.actions;

import java.io.File;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.model2.ISessionManagement;
import net.sf.jmoney.model2.SessionImpl;
import net.sf.jmoney.serializeddatastore.*;
import net.sf.jmoney.serializeddatastore.SerializedDatastorePlugin;

/**
 * Our sample action implements workbench action delegate.
 * The action proxy will be created by the workbench and
 * shown in the UI. When the user tries to use the action,
 * this delegate will be created and execution will be 
 * delegated to it.
 * @see IWorkbenchWindowActionDelegate
 */
public class SaveSessionAction implements IWorkbenchWindowActionDelegate {
	private IWorkbenchWindow window;
	/**
	 * The constructor.
	 */
	public SaveSessionAction() {
	}

	/**
	 * The action has been activated. The argument of the
	 * method represents the 'real' action sitting
	 * in the workbench UI.
	 * @see IWorkbenchWindowActionDelegate#run
	 */
	public void run(IAction action) {
		ISessionManagement sessionManager = JMoneyPlugin.getDefault().getSessionManager();
		if (sessionManager instanceof SessionManagementImpl) {
			SessionManagementImpl ourSessionManager = (SessionManagementImpl)sessionManager;
			if (ourSessionManager.getFile() == null) {
				File sessionFile = SerializedDatastorePlugin.getDefault().obtainFileName(window);
				if (sessionFile != null) {
					SerializedDatastorePlugin.getDefault().writeSession(ourSessionManager.getSession(), sessionFile, window);
					ourSessionManager.setFile(sessionFile);
					ourSessionManager.setModified(false);
				}
			} else {
				SerializedDatastorePlugin.getDefault().writeSession(ourSessionManager.getSession(), ourSessionManager.getFile(), window);
				ourSessionManager.setModified(false);
			}
		} else {
			MessageDialog waitDialog =
				new MessageDialog(
						window.getShell(), 
						"Menu item unavailable", 
						null, // accept the default window icon
						"This session cannot be saved using this 'save' action.  " +
						"More than one plug-in is installed that provides a" +
						"datastore implementation.  The current session was" +
						"created using a different plug-in from the plug-in that" +
						"created this 'save' action.  You can only use this 'save'" +
						"action if the session was created using the corresponding" +
						"'new' or 'open' action.", 
						MessageDialog.ERROR, 
						new String[] { IDialogConstants.OK_LABEL }, 0);
			waitDialog.open();
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