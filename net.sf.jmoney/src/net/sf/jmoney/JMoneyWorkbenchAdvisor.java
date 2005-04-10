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

package net.sf.jmoney;

import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;

/**
 * @author Nigel
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class JMoneyWorkbenchAdvisor extends WorkbenchAdvisor {
		public String getInitialWindowPerspectiveId() {
	    	// This method will not be called if 
	    	// org.eclipse.ui/defaultPerspectiveId is set in
	    	// plugin_customization.ini in org.eclipse.platform
	    	// plugin.  (This seems to be a design flaw - if
	    	// the application wants this perspective then it
	    	// would stupid to use some other).
	    	// TODO sort this out
	        return "net.sf.jmoney.JMoneyPerspective";
	    }

		public void initialize(IWorkbenchConfigurer configurer) {
			super.initialize(configurer);

			// Turn on support for the saving and restoring of
			// view states through IMemento interfaces.
			configurer.setSaveAndRestore(true);
		}

	    public void preWindowOpen(IWorkbenchWindowConfigurer configurer) {
			super.preWindowOpen(configurer);
			configurer.setShowCoolBar(false);
			configurer.setShowStatusLine(false);
		}

		public void postWindowCreate(IWorkbenchWindowConfigurer windowConfigurer) {
			super.postWindowCreate(windowConfigurer);
/* does not compile
			// determines if the workbench has any intro to show
			boolean hasIntro = getWorkbenchConfigurer().getWorkbench().getIntroManager().hasIntro();
			    
			if (hasIntro) {
				// shows the intro
				getWorkbenchConfigurer()
					.getWorkbench()
					.getIntroManager()
					.showIntro(
						windowConfigurer.getWindow(), 
						false);
			}
*/
		}  	

		public void fillActionBars(
			    IWorkbenchWindow window,
			    IActionBarConfigurer configurer,
			    int flags) {
			    super.fillActionBars(window, configurer, flags);
		      if ((flags & FILL_MENU_BAR) != 0) {
			        fillMenuBar(window, configurer);
			    }
			}

		private void fillMenuBar(
			    IWorkbenchWindow window,
			    IActionBarConfigurer configurer) {
			    IMenuManager menuBar = configurer.getMenuManager();
			    menuBar.add(createFileMenu(window));
			    menuBar.add(createHelpMenu(window));
			}

		private MenuManager createFileMenu(IWorkbenchWindow window) {
			// TODO: currently language uses MainFrame.file.mnemonic, not an ampersand.
		    MenuManager menu = new MenuManager("&File"/*Constants.LANGUAGE.getString("MainFrame.file")*/,
		        IWorkbenchActionConstants.M_FILE);
		    menu.add(new GroupMarker(IWorkbenchActionConstants.FILE_START));
		    menu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
		    menu.add(ActionFactory.QUIT.create(window));
		    menu.add(new GroupMarker(IWorkbenchActionConstants.FILE_END));
		    return menu;
		}

		private MenuManager createHelpMenu(IWorkbenchWindow window) {
		    MenuManager menu = new MenuManager("Help"/*Messages.getString("File")*/, //$NON-NLS-1$
		        IWorkbenchActionConstants.M_HELP);
		    menu.add(new GroupMarker(IWorkbenchActionConstants.HELP_START));
		    menu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
		    menu.add(ActionFactory.QUIT.create(window));
		    menu.add(new GroupMarker(IWorkbenchActionConstants.HELP_END));
		    return menu;
		}

		public boolean preWindowShellClose(IWorkbenchWindowConfigurer configurer) {
			// If a session is open, ensure we have all the information we
			// need to close it.  Some datastores need additional information
			// to save the session.  For example the serialized XML datastore
			// requires a file name which will not have been requested from the
			// user if the datastore has not yet been saved.
			
			// This call must be done here for two reasons.
			// 1. It ensures that the session data can be saved.
			// 2. The navigation view saves, as part of its state,
			//    the datastore which was open when the workbench was
			//    last shut down, allowing it to open the same session
			//    when the workbench is next opened.  In order to do this,
			//    the navigation view saves the session details.

			return JMoneyPlugin.getDefault().saveOldSession(configurer.getWindow());
		}
}
