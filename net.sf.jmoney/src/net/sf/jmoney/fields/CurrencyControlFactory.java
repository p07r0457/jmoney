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

import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.PropertyControlFactory;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.model2.Session;

import org.eclipse.swt.widgets.Composite;

/**
 * A control factory to select a currency.
 * 
 * @author Nigel Westbury
 * @author Johann Gyger
 */
public class CurrencyControlFactory extends PropertyControlFactory<Currency> {

    public IPropertyControl createPropertyControl(Composite parent, ScalarPropertyAccessor<Currency> propertyAccessor, Session session) {
        return new CurrencyEditor(parent, propertyAccessor);
    }

    public String formatValueForMessage(ExtendableObject extendableObject, ScalarPropertyAccessor<? extends Currency> propertyAccessor) {
        Currency value = (Currency) extendableObject.getPropertyValue(propertyAccessor);
        return value == null ? "none" : "'" + value.getName() + "'";
    }

    public String formatValueForTable(ExtendableObject extendableObject, ScalarPropertyAccessor<? extends Currency> propertyAccessor) {
        Currency value = (Currency) extendableObject.getPropertyValue(propertyAccessor);
        return value == null ? "" : value.getCode();
    }

	public Currency getDefaultValue() {
		return null;
	}

	public boolean isEditable() {
		return true;
	}
}