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

package net.sf.jmoney.pages.entries;

import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.pages.entries.EntriesTree.DisplayableTransaction;

import org.eclipse.swt.widgets.Composite;

/**
 * Represents a column of data that can be displayed in the entries table,
 * edited by the user, sorted, or used in a filter.
 * <P>
 * All columns are managed by an object of this class.  Special
 * implementations exist for the credit, debit, and balance columns.
 * More generic implementations exist for the other properties.
 * 
 * @author Nigel Westbury
 */
public interface IEntriesTableProperty {
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

	int compare(DisplayableTransaction trans1, DisplayableTransaction trans2);
}


