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

import net.sf.jmoney.model2.IPropertyControl;

import org.eclipse.swt.widgets.Composite;

/**
 * Represents a property that can be displayed in the entries table,
 * edited by the user, or used in the filter.
 * <P>
 * The credit, debit, and balance columns are hard coded at the end
 * of the table and are not represented by objects of this class.
 * 
 * @author Nigel Westbury
 */
interface IEntriesTableProperty {
	String getText();

	String getId();

	int getWeight();

	int getMinimumWidth();

	/**
	 * @param entry
	 * @return
	 */
	String getValueFormattedForTable(IDisplayableItem data);

	/**
	 * @param table
	 * @param data
	 * @return
	 */
	IPropertyControl createAndLoadPropertyControl(Composite parent, IDisplayableItem data);

	/**
	 * @return
	 */
	boolean isTransactionProperty();
}


