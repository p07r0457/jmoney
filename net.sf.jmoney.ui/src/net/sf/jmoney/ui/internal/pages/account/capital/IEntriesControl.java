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

package net.sf.jmoney.ui.internal.pages.account.capital;

import net.sf.jmoney.model2.Entry;

import org.eclipse.jface.viewers.StructuredSelection;

/**
 * All classes that implement a view of a list of account entries
 * must implement this interface.
 *
 * @author Nigel Westbury
 */
public interface IEntriesControl {

	/**
	 * @param selection
	 */
	void setSelection(StructuredSelection selection);

	/**
	 * @return
	 */
	Entry getSelectedEntry();

	/**
	 * 
	 */
	void dispose();

	/**
	 * This method is called when a substantial change has
	 * been made to data that affects the entries list.
	 * The view should be fully refreshed. 
	 */
	void refresh();

	/**
	 * @param de
	 * @param properties
	 */
	void update(Object element);

}
