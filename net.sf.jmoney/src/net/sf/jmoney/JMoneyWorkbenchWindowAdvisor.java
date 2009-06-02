/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2005 Johann Gyger <jgyger@users.sourceforge.net>
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

import net.sf.jmoney.model2.DatastoreManager;

import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

/**
 * @author Johann Gyger
 */
public class JMoneyWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor {

	public JMoneyWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
		super(configurer);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.application.WorkbenchWindowAdvisor#createActionBarAdvisor(org.eclipse.ui.application.IActionBarConfigurer)
	 */
    @Override	
	public ActionBarAdvisor createActionBarAdvisor(IActionBarConfigurer configurer) {
		return new JMoneyActionBarAdvisor(configurer);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.application.WorkbenchWindowAdvisor#preWindowOpen()
	 */
	@Override
	public void preWindowOpen() {
		IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
		configurer.setShowCoolBar(true);
		configurer.setShowStatusLine(true);
	}

	@Override
	public void postWindowRestore() {
		/*
		 * The title of a window should show a brief description of the input.
		 * This is set whenever a session is opened by the code that processed the
		 * action or handler that opened the session.  However in the case where
		 * a session is restored as a part of workbench restore, the title must be
		 * set here.
		 */
		IWorkbenchWindow window = getWindowConfigurer().getWindow();
		DatastoreManager sessionManager = (DatastoreManager)window.getActivePage().getInput();
		
		/*
		 * It is possible we are restoring a workbench window that has no session opened in it.
		 * In such a situation, the input will be null and we want to leave the title as it is
		 * with just the product name.
		 */
		if (sessionManager != null) {
			String productName = Platform.getProduct().getName();
			window.getShell().setText(
					productName + " - "	+ sessionManager.getBriefDescription());
		}
	}

}
