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

package net.sf.jmoney.serializeddatastore;

import java.io.File;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.serializeddatastore.SerializedDatastorePlugin;
import net.sf.jmoney.model2.SessionImpl;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

/**
 * Our sample action implements workbench action delegate.
 * The action proxy will be created by the workbench and
 * shown in the UI. When the user tries to use the action,
 * this delegate will be created and execution will be 
 * delegated to it.
 * @see IWorkbenchWindowActionDelegate
 */
public class SessionFactory implements IElementFactory {
//	private IWorkbenchWindow window;
	/**
	 * The constructor.
	 */
	public SessionFactory() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IElementFactory#createElement(org.eclipse.ui.IMemento)
	 */
	public IAdaptable createElement(IMemento memento) {
		String fileName = memento.getString("fileName");
        if (fileName != null) {
            File sessionFile = new File(fileName);
            try {
                SessionManagementImpl sessionManager = SerializedDatastorePlugin.getDefault().readSessionQuietly(sessionFile);
                if (sessionManager != null) {
                    JMoneyPlugin.getDefault().setSessionManager(sessionManager);
                }
                return sessionManager;
            } catch (Exception ex) {
            	// If we could not read the session, we log the error
            	// but are silent to the user.
            	// TODO log error
            }
        }
        return null;
	}
}