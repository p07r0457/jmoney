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

import java.util.Iterator;
import java.util.Vector;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.IPropertyControlFactory;
import net.sf.jmoney.model2.PropertyAccessor;
import net.sf.jmoney.model2.Session;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;

/**
 * A control factory to select a currency.
 * 
 * @author Nigel Westbury
 * @author Johann Gyger
 */
public class CurrencyControlFactory implements IPropertyControlFactory {

    public IPropertyControl createPropertyControl(Composite parent, PropertyAccessor propertyAccessor) {
        return new CurrencyEditor(parent, propertyAccessor);
    }

	public CellEditor createCellEditor(Table table) {
        Session session = JMoneyPlugin.getDefault().getSession();

        Vector items = new Vector();
        for (Iterator iter = session.getCommodityIterator(); iter.hasNext();) {
            Commodity commodity = (Commodity) iter.next();
            if (commodity instanceof Currency) {
                items.add(commodity.getName());
            }
        }

		return new ComboBoxCellEditor(table, (String[])items.toArray(new String[0]));
	}

	public Object getValueTypedForCellEditor(ExtendableObject extendableObject, PropertyAccessor propertyAccessor) {
        Currency currency = (Currency) extendableObject.getPropertyValue(propertyAccessor);
		// TODO complete this.
        int index = 0;
		return new Integer(index);
	}

	public void setValueTypedForCellEditor(ExtendableObject extendableObject, PropertyAccessor propertyAccessor, Object value) {
		int index = ((Integer)value).intValue();
		// TODO complete this.
		Currency currency = null;
        extendableObject.setPropertyValue(propertyAccessor, currency);
	}

    public String formatValueForMessage(ExtendableObject extendableObject, PropertyAccessor propertyAccessor) {
        Currency value = (Currency) extendableObject.getPropertyValue(propertyAccessor);
        return value == null ? "none" : "'" + value.getName() + "'";
    }

    public String formatValueForTable(ExtendableObject extendableObject, PropertyAccessor propertyAccessor) {
        Currency value = (Currency) extendableObject.getPropertyValue(propertyAccessor);
        return value == null ? "" : value.getCode();
    }

	public boolean isEditable() {
		return true;
	}
}