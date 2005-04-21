/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2004 Johann Gyger <jgyger@users.sf.net>
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

import java.util.Date;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.VerySimpleDateFormat;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.IPropertyControlFactory;
import net.sf.jmoney.model2.PropertyAccessor;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;

/**
 * A control factory to edit date values.
 * 
 * @author Johann Gyger
 */
public class DateControlFactory implements IPropertyControlFactory {

    // TODO Listen to date format changes.
    protected VerySimpleDateFormat fDateFormat = new VerySimpleDateFormat(JMoneyPlugin.getDefault().getDateFormat());

    protected boolean readOnly;
    
    public DateControlFactory() {
    	this.readOnly = false;
    }
    
    public DateControlFactory(boolean readOnly) {
    	this.readOnly = readOnly;
    }
    
    public IPropertyControl createPropertyControl(Composite parent, PropertyAccessor propertyAccessor) {
  		return new DateEditor(parent, propertyAccessor, fDateFormat);
    }

    public String formatValueForMessage(ExtendableObject extendableObject, PropertyAccessor propertyAccessor) {
        Date value = (Date) extendableObject.getPropertyValue(propertyAccessor);
        if (value == null) {
            return "none";
        } else {
            return "'" + fDateFormat.format(value) + "'";
        }
    }

    public String formatValueForTable(ExtendableObject extendableObject, PropertyAccessor propertyAccessor) {
        Date value = (Date) extendableObject.getPropertyValue(propertyAccessor);
        return fDateFormat.format(value);
    }

	public boolean isEditable() {
		return !readOnly;
	}
}