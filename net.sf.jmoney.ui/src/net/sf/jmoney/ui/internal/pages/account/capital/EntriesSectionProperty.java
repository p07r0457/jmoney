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
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.PropertyAccessor;

/**
 * Represents a property that can be displayed in the entries table,
 * edited by the user, or used in the filter.
 * <P>
 * The credit, debit, and balance columns are hard coded at the end
 * of the table and are not represented by objects of this class.
 * 
 * @author Nigel Westbury
 */
abstract class EntriesSectionProperty {
	private PropertyAccessor accessor;
	private IPropertyControl propertyControl = null;
	
	EntriesSectionProperty(PropertyAccessor accessor) {
		this.accessor = accessor;
	}

	public String getText() {
		return accessor.getShortDescription();
	}

	public String getId() {
		return accessor.getName();
	}

	public int getMinimumWidth() {
		// TODO: Are the values from the accessor correct?
		// The values there were set to the values used
		// in Swing and are typed to 'double'.
		// We need to update these and change them to int.
		return (int)accessor.getWidth();
	}

	/**
	 * @param entry
	 * @return
	 */
	public String getValueFormattedForTable(Entry entry) {
		ExtendableObject object = getObjectContainingProperty(entry);
		if (object == null) {
			return "";
		} else {
			return accessor.formatValueForTable(object);
		}
	}

	abstract ExtendableObject getObjectContainingProperty(Entry entry);
/*        
	case 0: // check
        String s = de.entry.getCheck();
        return s == null ? "" : s;
    case 1: // date
        Date d = de.entry.getTransaction().getDate();
        return d == null ? "" : fDateFormat.format(d);
    case 2: // valuta
        d = de.entry.getValuta();
        return d == null ? "" : fDateFormat.format(d);
    case 3: // description
        s = de.entry.getDescription();
        return s == null ? "" : s;
*/

	public void setControl(IPropertyControl propertyControl) {
		this.propertyControl = propertyControl;
	}

	public IPropertyControl getControl() {
		return propertyControl;
	}

	public PropertyAccessor getPropertyAccessor() {
		return accessor;
	}
}


