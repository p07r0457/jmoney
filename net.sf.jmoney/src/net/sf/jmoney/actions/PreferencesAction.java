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

package net.sf.jmoney.actions;

import net.sf.jmoney.preferences.PreferencePage;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceNode;
import org.eclipse.jface.preference.IPreferencePage;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.jface.preference.PreferenceNode;
import org.eclipse.jface.viewers.ISelection;
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
public class PreferencesAction implements IWorkbenchWindowActionDelegate {
	private IWorkbenchWindow window;
	/**
	 * The constructor.
	 */
	public PreferencesAction() {
	}

	/**
	 * The action has been activated. The argument of the
	 * method represents the 'real' action sitting
	 * in the workbench UI.
	 * @see IWorkbenchWindowActionDelegate#run
	 */
	public void run(IAction action) {
		PreferencePage preferencePage = new PreferencePage();
/*
		IPreferenceNode node = new PreferenceNode(
				"net.sf.jmoney.preferences.preferencePage",
				"core JMoney preferences",
				null,
				"net.sf.jmoney.preferences.PreferencePage");
*/
		// Load the prefence page extensions.
		// Note that there are two constructors to PreferenceNode.
		// One takes an instatiated object as a parameter,
		// the other takes the class name, title, and image.
		// The latter constructor is preferable because it loads
		// the page lazily.  i.e. the class is not loaded unless
		// the user selects the page.
		// However, this does not work because by the time the
		// page is lazily loaded, the class path does not include
		// the path to the plug-in.  We must therefore use the
		// createExecutableExtension method to load the class.
		// TODO: figure out how we load lazily.
		
		PreferenceManager manager = new PreferenceManager();

		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IExtensionPoint extensionPoint = registry.getExtensionPoint("org.eclipse.ui.preferencePages");
		IExtension[] extensions = extensionPoint.getExtensions();
		
		for (int i = 0; i < extensions.length; i++) {
			IConfigurationElement[] elements =
				extensions[i].getConfigurationElements();
			for (int j = 0; j < elements.length; j++) {
				if (elements[j].getName().equals("page")) {
					try {
						String id = elements[j].getAttribute("id");
						String label = elements[j].getAttribute("name");
						// String className = elements[j].getAttribute("class");
						IPreferencePage listener = (IPreferencePage)elements[j].createExecutableExtension("class");
						// IPreferenceNode node = new PreferenceNode(
						//		id,
						//		label,
						//		null,
						//		className);
						IPreferenceNode node = new PreferenceNode(
								id,
								listener);
						manager.addToRoot(node);
					} catch (CoreException e) {
						e.printStackTrace();
					}
				}
			}
		}

/*
IPreferenceNode node = new PreferenceNode(
				"net.sf.jmoney.preferences.preferencePage",
				preferencePage);
*/
		
		PreferenceDialog preferenceDialog = new PreferenceDialog(window.getShell(), manager);
		preferenceDialog.open();
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