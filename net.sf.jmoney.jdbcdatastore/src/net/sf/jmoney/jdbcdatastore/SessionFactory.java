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

package net.sf.jmoney.jdbcdatastore;

import net.sf.jmoney.model2.ISessionFactory;

import org.eclipse.ui.IMemento;

/**
 * Our sample action implements workbench action delegate.
 * The action proxy will be created by the workbench and
 * shown in the UI. When the user tries to use the action,
 * this delegate will be created and execution will be 
 * delegated to it.
 */
public class SessionFactory implements ISessionFactory {
//	private IWorkbenchWindow window;
	/**
	 * The constructor.
	 */
	public SessionFactory() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IElementFactory#createElement(org.eclipse.ui.IMemento)
	 */
	public void openSession(IMemento memento) {
		// TODO: Write this for JDBC databases and get it working.
/*		
		String fileName = memento.getString("fileName");
        if (fileName != null) {
            File sessionFile = new File(fileName);
            SerializedDatastorePlugin.getDefault().readSession(sessionFile, window);
        } else {
        	// No file name is set.  This can happen if the workbench was last closed
        	// with a session that had never been saved.  Although the user was prompted
        	// to save the session when the workbench closed, the user may have pressed
        	// the 'no' button.  We create an new empty session.
            JMoneyPlugin.getDefault().setSessionManager(
            		SerializedDatastorePlugin.getDefault().newSession());
        }
*/        
	}
}