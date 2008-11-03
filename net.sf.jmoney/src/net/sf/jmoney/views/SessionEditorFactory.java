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

package net.sf.jmoney.views;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.handlers.SessionEditorInput;
import net.sf.jmoney.model2.Session;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;

public class SessionEditorFactory implements IElementFactory {

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IElementFactory#createElement(org.eclipse.ui.IMemento)
	 * 
	 */
	// While debugging this code, one can inspect the memento data
	// in the file runtime-workspace\.metadata\.plugins\org.eclipse.ui.workbench\workbench.xml.
	public IAdaptable createElement(IMemento memento) {
		// Get the session from the data in the memento.
		Session session = JMoneyPlugin.openSession(memento.getChild("session")); //$NON-NLS-1$
		if (session == null) {
			// null indicates the element could not be re-created.
			return null;
		}
		
		return new SessionEditorInput(session, memento);
	}
}