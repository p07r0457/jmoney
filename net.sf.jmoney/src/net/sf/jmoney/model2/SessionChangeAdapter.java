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

package net.sf.jmoney.model2;


/**
 * Empty implementation of the SessionChangeListener interface.
 * Listeners implementing the SessionChangeListener interface may
 * instead extend this class to avoid implementing empty methods
 * for events on which no action is necessary.
 *
 * @author Nigel Westbury
 */
public class SessionChangeAdapter implements SessionChangeListener {
	/**
	 * The session has been replaced.  All views of session data
	 * should be fully refreshed.
	 */
    public void sessionReplaced(Session oldSession, Session newSession) {
    }
	
	public void objectInserted(ExtendableObject newObject) {
	}

	public void objectCreated(ExtendableObject newObject) {
	}

	public void objectRemoved(ExtendableObject deletedObject) {
	}

	public void objectDestroyed(ExtendableObject deletedObject) {
	}

	public void objectChanged(ExtendableObject changedObject, ScalarPropertyAccessor changedProperty, Object oldValue, Object newValue) {
	}

	public void performRefresh() {
	}
}
