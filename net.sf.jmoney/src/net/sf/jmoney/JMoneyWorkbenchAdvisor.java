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

import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.*;
import org.eclipse.jface.action.*;

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

	    public void preWindowOpen(IWorkbenchWindowConfigurer configurer) {
			super.preWindowOpen(configurer);
			configurer.setShowCoolBar(false);
			configurer.setShowStatusLine(false);
		}

		public void postWindowOpen(IWorkbenchWindowConfigurer configurer) {
			super.postWindowOpen(configurer);
			configurer.setTitle("JMoney 1.0.0");
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

}