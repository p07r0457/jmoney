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

package net.sf.jmoney.fields;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.widgets.Table;

import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IPropertyControlFactory;
import net.sf.jmoney.model2.PropertyAccessor;

/**
 * A control factory to edit an amount of a commodity.
 * 
 * @author Nigel Westbury
 * @author Johann Gyger
 */
public abstract class AmountControlFactory implements IPropertyControlFactory {

	public CellEditor createCellEditor(Table table) {
		// TODO Complete this
		return new TextCellEditor(table);
	}

	public Object getValueTypedForCellEditor(ExtendableObject extendableObject, PropertyAccessor propertyAccessor) {
        Long amount = (Long) extendableObject.getPropertyValue(propertyAccessor);
        if (amount == null) {
            return "";
        } else {
            return getCommodity(extendableObject).format(amount.longValue());
        }
	}

	public void setValueTypedForCellEditor(ExtendableObject extendableObject, PropertyAccessor propertyAccessor, Object value) {
		String amountString = (String)value;
        if (amountString.length() == 0 && propertyAccessor.getValueClass() == Long.class) {
            // The text box is empty and the property is Long
            // (not long) thus allowing nulls.  Therefore
            // we set the property value to be null.
            extendableObject.setPropertyValue(propertyAccessor, null);
        } else {
            long amount = getCommodity(extendableObject).parse(amountString);
            extendableObject.setLongPropertyValue(propertyAccessor, amount);
        }
	}

    public String formatValueForMessage(ExtendableObject extendableObject, PropertyAccessor propertyAccessor) {
        Long amount = (Long) extendableObject.getPropertyValue(propertyAccessor);
        if (amount == null) {
            return "none";
        } else {
            return "'" + getCommodity(extendableObject).format(amount.longValue()) + "'";
        }
    }

    public String formatValueForTable(ExtendableObject extendableObject, PropertyAccessor propertyAccessor) {
        Long amount = (Long) extendableObject.getPropertyValue(propertyAccessor);
        if (amount == null) {
            return "";
        } else {
            return getCommodity(extendableObject).format(amount.longValue());
        }
    }

	public boolean isEditable() {
		return true;
	}

	protected abstract Commodity getCommodity(ExtendableObject object);
}