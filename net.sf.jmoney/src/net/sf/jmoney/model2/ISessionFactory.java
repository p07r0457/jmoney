/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2002 Johann Gyger <johann.gyger@switzerland.org>
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

package net.sf.jmoney.model2;

import net.sf.jmoney.JMoneyPlugin;

import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * A factory for re-creating sessions from a previously saved memento.
 * <p>
 * Plug-ins implementing a datastore should implement this interface 
 * and include the name of their class
 * in an extension to the platform extension point named
 * <code>"org.eclipse.ui.elementFactories"</code>.
 * For example, the plug-in's XML markup might contain:
 * <pre>
 * &LT;extension point="org.eclipse.ui.elementFactories"&GT;
 *    &LT;factory id="com.example.myplugin.MyFactory" class="com.example.myplugin.MyFactory" /&GT; 
 * &LT;/extension&GT;
 * </pre>
 * </p>
 *
 * @see IPersistableElement
 * @see IMemento
 * @see org.eclipse.ui.IWorkbench#getElementFactory
 * @author Nigel Westbury
 */
public interface ISessionFactory {
	
	/**
	 * Re-opens a session from the state captured within the given 
	 * memento. 
	 * The opened session is set as the current open JMoney session. 
	 * If no session can be opened then an appropriate message is
	 * displayed to the user and the previous session, if any, is
	 * left open.
	 * <P>
	 * A session may fail to open for a number of reasons.  The most
	 * likely are that the plug-in that had originally created the session is not
	 * installed or an error occurred while opening the session.
	 *
	 * @param memento a memento containing the state for the object
	 * @param window the window to which message are to be displayed
	 */
	public void openSession(IMemento memento);
}
