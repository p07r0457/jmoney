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

import org.eclipse.core.runtime.IPlatformRunnable;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.WorkbenchAdvisor;

/**
 * @author Nigel
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
	public class JMoneyApplication implements IPlatformRunnable {
	    public Object run(Object args) {
	        WorkbenchAdvisor workbenchAdvisor = new JMoneyWorkbenchAdvisor();
	        Display display = PlatformUI.createDisplay();
	        int returnCode = PlatformUI.createAndRunWorkbench(display,
	            workbenchAdvisor);
	        if (returnCode == PlatformUI.RETURN_RESTART) {
	            return IPlatformRunnable.EXIT_RESTART;
	        } else {
	            return IPlatformRunnable.EXIT_OK;
	        }
	    }
	}

